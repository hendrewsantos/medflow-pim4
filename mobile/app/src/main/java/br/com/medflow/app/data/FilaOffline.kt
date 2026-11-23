package br.com.medflow.app.data

enum class TipoAcao { CONFIRMAR, OCORRENCIA }

data class AcaoPendente(val id: Long, val pedidoId: Int, val tipo: TipoAcao, val texto: String)

enum class DestinoAcao {
    /** API aceitou: remove da fila. */
    ENVIADA,

    /** API recusou de forma definitiva (ex.: 409 conflito): remove da fila, não adianta reenviar. */
    DESCARTAR,

    /** Sem rede / erro temporário: mantém na fila e para (preserva a ordem). */
    REPETIR_DEPOIS,
}

interface FilaStore {
    suspend fun listar(): List<AcaoPendente>
    suspend fun remover(id: Long)
}

/**
 * Regra da fila offline (confirmar entrega / registrar ocorrência).
 * Processa em ordem de criação; ao primeiro erro temporário, interrompe e mantém o restante para nova tentativa.
 */
class FilaOffline(
    private val store: FilaStore,
    private val enviar: suspend (AcaoPendente) -> DestinoAcao,
) {
    data class Resumo(val enviadas: Int, val descartadas: Int, val restantes: Int) {
        val completo: Boolean get() = restantes == 0
    }

    suspend fun processar(): Resumo {
        val itens = store.listar()
        var enviadas = 0
        var descartadas = 0
        for ((indice, acao) in itens.withIndex()) {
            when (enviar(acao)) {
                DestinoAcao.ENVIADA -> {
                    store.remover(acao.id)
                    enviadas++
                }
                DestinoAcao.DESCARTAR -> {
                    store.remover(acao.id)
                    descartadas++
                }
                DestinoAcao.REPETIR_DEPOIS -> return Resumo(enviadas, descartadas, itens.size - indice)
            }
        }
        return Resumo(enviadas, descartadas, 0)
    }
}

/** Adaptador Room -> FilaStore. */
class RoomFilaStore(private val dao: AcaoDao) : FilaStore {
    override suspend fun listar(): List<AcaoPendente> = dao.listar().map {
        AcaoPendente(it.id, it.pedidoId, TipoAcao.valueOf(it.tipo), it.texto)
    }

    override suspend fun remover(id: Long) = dao.remover(id)
}
