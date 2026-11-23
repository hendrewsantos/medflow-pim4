package br.com.medflow.app

import br.com.medflow.app.data.AcaoPendente
import br.com.medflow.app.data.DestinoAcao
import br.com.medflow.app.data.FilaOffline
import br.com.medflow.app.data.FilaStore
import br.com.medflow.app.data.TipoAcao
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class FilaFalsa(vararg acoes: AcaoPendente) : FilaStore {
    val itens = acoes.toMutableList()
    override suspend fun listar(): List<AcaoPendente> = itens.toList()
    override suspend fun remover(id: Long) {
        itens.removeAll { it.id == id }
    }
}

class FilaOfflineTest {
    private fun acao(id: Long, pedido: Int, tipo: TipoAcao = TipoAcao.CONFIRMAR) =
        AcaoPendente(id, pedido, tipo, "texto")

    @Test
    fun enviaTudoERemoveDaFila() = runTest {
        val store = FilaFalsa(acao(1, 10), acao(2, 11, TipoAcao.OCORRENCIA))
        val resumo = FilaOffline(store) { DestinoAcao.ENVIADA }.processar()
        assertEquals(2, resumo.enviadas)
        assertTrue(resumo.completo)
        assertTrue(store.itens.isEmpty())
    }

    @Test
    fun conflitoDescartaDaFilaSemReenviar() = runTest {
        val store = FilaFalsa(acao(1, 10))
        val resumo = FilaOffline(store) { DestinoAcao.DESCARTAR }.processar()
        assertEquals(1, resumo.descartadas)
        assertEquals(0, resumo.enviadas)
        assertTrue(resumo.completo)
        assertTrue(store.itens.isEmpty())
    }

    @Test
    fun semRedeMantemTudoEPreservaOrdem() = runTest {
        val store = FilaFalsa(acao(1, 10), acao(2, 11), acao(3, 12))
        val tentadas = mutableListOf<Long>()
        val resumo = FilaOffline(store) {
            tentadas += it.id
            DestinoAcao.REPETIR_DEPOIS
        }.processar()
        assertFalse(resumo.completo)
        assertEquals(3, resumo.restantes)
        assertEquals(listOf(1L), tentadas) // para no primeiro erro temporário
        assertEquals(3, store.itens.size)
    }

    @Test
    fun falhaNoMeioMantemSomenteOsRestantes() = runTest {
        val store = FilaFalsa(acao(1, 10), acao(2, 11), acao(3, 12))
        val resumo = FilaOffline(store) {
            if (it.id == 2L) DestinoAcao.REPETIR_DEPOIS else DestinoAcao.ENVIADA
        }.processar()
        assertEquals(1, resumo.enviadas)
        assertEquals(2, resumo.restantes)
        assertEquals(listOf(2L, 3L), store.itens.map { it.id })
    }
}
