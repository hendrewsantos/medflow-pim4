package br.com.medflow.app.data

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Agenda o reenvio da fila quando houver rede (implementação real usa WorkManager). */
fun interface AgendadorSync {
    fun agendar()
}

interface AutenticacaoRepositorio {
    suspend fun entrar(email: String, senha: String): Resultado<Sessao>

    /** Retorna uma mensagem de bloqueio (não saiu) ou null se a sessão foi encerrada. */
    suspend fun sair(): String?
}

class AuthRepository(
    private val api: MedFlowApi,
    private val sessao: SessaoStore,
    private val db: MedFlowDb,
) : AutenticacaoRepositorio {

    override suspend fun entrar(email: String, senha: String): Resultado<Sessao> {
        return when (val r = chamar { api.login(LoginRequest(email.trim(), senha)) }) {
            is Resultado.Ok -> {
                val u = r.valor.usuario
                when {
                    !u.ativo -> Resultado.Falha(TipoErro.SEM_PERMISSAO, "Esta conta está inativa. Procure a farmácia.")
                    u.perfil != Perfis.ENTREGADOR && u.perfil != Perfis.PACIENTE ->
                        Resultado.Falha(
                            TipoErro.SEM_PERMISSAO,
                            "Este aplicativo é para entregadores e pacientes. Para o seu perfil, use a versão web.",
                        )
                    else -> {
                        // Dados em cache de outra pessoa não devem aparecer para este usuário.
                        db.entregaDao().limparTudo()
                        db.pedidoDao().limparTudo()
                        val nova = Sessao(r.valor.token, u.id, u.nome, u.perfil)
                        sessao.salvar(nova)
                        Resultado.Ok(nova)
                    }
                }
            }
            is Resultado.Falha ->
                if (r.tipo == TipoErro.NAO_AUTORIZADO) {
                    Resultado.Falha(TipoErro.NAO_AUTORIZADO, "E-mail ou senha incorretos.")
                } else {
                    r
                }
        }
    }

    override suspend fun sair(): String? {
        val pendentes = db.acaoDao().contar()
        if (pendentes > 0) {
            return "Há $pendentes ação(ões) aguardando envio. Conecte-se à internet e espere o envio antes de sair, " +
                "senão elas seriam perdidas."
        }
        sessao.limpar()
        db.entregaDao().limparTudo()
        db.pedidoDao().limparTudo()
        return null
    }
}

sealed interface EnvioAcao {
    data object Enviada : EnvioAcao
    data object Enfileirada : EnvioAcao
    data class Falhou(val mensagem: String) : EnvioAcao
}

class EntregasRepository(
    private val api: MedFlowApi,
    db: MedFlowDb,
    private val agendador: AgendadorSync,
) {
    private val entregaDao = db.entregaDao()
    private val acaoDao = db.acaoDao()

    val disponiveis: Flow<List<EntregaEntity>> = entregaDao.observarLista(LISTA_DISPONIVEIS)
    val minhas: Flow<List<EntregaEntity>> = entregaDao.observarLista(LISTA_MINHAS)
    val pendentes: Flow<List<AcaoPendenteEntity>> = acaoDao.observarTodas()

    private val _aviso = MutableStateFlow<String?>(null)

    /** Aviso para o usuário (ex.: ação da fila descartada por conflito). Vale só enquanto o app está aberto. */
    val aviso: StateFlow<String?> = _aviso.asStateFlow()

    fun limparAviso() {
        _aviso.value = null
    }

    fun observar(pedidoId: Int): Flow<EntregaEntity?> = entregaDao.observar(pedidoId)

    suspend fun atualizar(): Resultado<Unit> {
        val r = chamar {
            coroutineScope {
                val d = async { api.entregasDisponiveis() }
                val m = async { api.entregasMinhas() }
                Pair(d.await(), m.await())
            }
        }
        return when (r) {
            is Resultado.Ok -> {
                val todas = r.valor.first.map { it.paraEntidade(LISTA_DISPONIVEIS) } +
                    r.valor.second.map { it.paraEntidade(LISTA_MINHAS) }
                entregaDao.substituirTudo(todas)
                Resultado.Ok(Unit)
            }
            is Resultado.Falha -> r
        }
    }

    /** Iniciar rota exige rede (não entra na fila offline). */
    suspend fun iniciar(pedidoId: Int): Resultado<Unit> {
        return when (val r = chamar { api.iniciar(pedidoId) }) {
            is Resultado.Ok -> {
                entregaDao.inserir(r.valor.paraEntidade(LISTA_MINHAS))
                Resultado.Ok(Unit)
            }
            is Resultado.Falha -> {
                if (r.tipo == TipoErro.CONFLITO) atualizar()
                r
            }
        }
    }

    suspend fun confirmar(pedidoId: Int, recebedor: String): EnvioAcao =
        executar(TipoAcao.CONFIRMAR, pedidoId, recebedor.trim())

    suspend fun registrarOcorrencia(pedidoId: Int, motivo: String): EnvioAcao =
        executar(TipoAcao.OCORRENCIA, pedidoId, motivo.trim())

    private suspend fun executar(tipo: TipoAcao, pedidoId: Int, texto: String): EnvioAcao {
        if (acaoDao.contarDoPedido(pedidoId) > 0) {
            return EnvioAcao.Falhou("Esta entrega já tem uma ação aguardando envio.")
        }
        return when (val r = chamar { enviarParaApi(tipo, pedidoId, texto) }) {
            is Resultado.Ok -> {
                entregaDao.inserir(r.valor.paraEntidade(LISTA_MINHAS))
                EnvioAcao.Enviada
            }
            is Resultado.Falha -> when (r.tipo) {
                TipoErro.SEM_REDE -> {
                    acaoDao.inserir(
                        AcaoPendenteEntity(
                            pedidoId = pedidoId,
                            tipo = tipo.name,
                            texto = texto,
                            criadoEm = System.currentTimeMillis(),
                        ),
                    )
                    agendador.agendar()
                    EnvioAcao.Enfileirada
                }
                TipoErro.CONFLITO -> {
                    atualizar()
                    EnvioAcao.Falhou("Esta entrega já foi alterada por outra pessoa. A tela foi atualizada.")
                }
                else -> EnvioAcao.Falhou(r.mensagem)
            }
        }
    }

    private suspend fun enviarParaApi(tipo: TipoAcao, pedidoId: Int, texto: String): EntregaResumoDto =
        when (tipo) {
            TipoAcao.CONFIRMAR -> api.confirmar(pedidoId, ConfirmarEntregaRequest(texto))
            TipoAcao.OCORRENCIA -> api.ocorrencia(pedidoId, OcorrenciaRequest(texto))
        }

    private suspend fun enviarDaFila(acao: AcaoPendente): DestinoAcao =
        when (val r = chamar { enviarParaApi(acao.tipo, acao.pedidoId, acao.texto) }) {
            is Resultado.Ok -> DestinoAcao.ENVIADA
            is Resultado.Falha -> when (r.tipo) {
                // Rede, erro de servidor ou token expirado: tenta de novo depois.
                TipoErro.SEM_REDE, TipoErro.SERVIDOR, TipoErro.NAO_AUTORIZADO -> DestinoAcao.REPETIR_DEPOIS
                // 409 (já alterada) e demais recusas: descarta e deixa a tela refletir o estado real.
                else -> DestinoAcao.DESCARTAR
            }
        }

    private val fila = FilaOffline(RoomFilaStore(acaoDao)) { enviarDaFila(it) }

    /** Chamado pelo WorkManager. */
    suspend fun sincronizarFila(): FilaOffline.Resumo {
        val resumo = fila.processar()
        if (resumo.descartadas > 0) {
            _aviso.value = "Uma ou mais ações feitas sem internet não puderam ser enviadas porque a entrega " +
                "já tinha sido alterada. Confira a situação atual na lista."
        }
        if (resumo.enviadas + resumo.descartadas > 0) atualizar()
        return resumo
    }

    suspend fun reagendarSeHouverPendentes() {
        if (acaoDao.contar() > 0) agendador.agendar()
    }
}

class PedidosRepository(private val api: MedFlowApi, db: MedFlowDb) {
    private val dao = db.pedidoDao()

    val pedidos: Flow<List<PedidoEntity>> = dao.observarTodos()

    suspend fun atualizar(): Resultado<Unit> =
        when (val r = chamar { api.meusPedidos() }) {
            is Resultado.Ok -> {
                dao.substituirTudo(r.valor.map { it.paraEntidade() })
                Resultado.Ok(Unit)
            }
            is Resultado.Falha -> r
        }

    suspend fun detalhe(id: Int): Resultado<PedidoDto> = chamar { api.pedido(id) }

    suspend fun proximaRenovacao(): Resultado<ProximaRenovacaoDto> = chamar { api.proximaRenovacao() }
}
