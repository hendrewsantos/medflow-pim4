package br.com.medflow.app

import br.com.medflow.app.data.Formatadores
import br.com.medflow.app.data.ProblemaDto
import br.com.medflow.app.data.TipoErro
import br.com.medflow.app.data.Validacoes
import br.com.medflow.app.data.mapearHttp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RegrasTest {
    @Test
    fun recebedorObrigatorio() {
        assertNotNull(Validacoes.recebedor("   "))
        assertNull(Validacoes.recebedor("Maria"))
    }

    @Test
    fun motivoObrigatorio() {
        assertNotNull(Validacoes.motivo(""))
        assertNull(Validacoes.motivo("Paciente ausente"))
    }

    @Test
    fun http409VirraConflito() {
        val f = mapearHttp(409, ProblemaDto(detail = "Transição inválida"))
        assertEquals(TipoErro.CONFLITO, f.tipo)
        assertEquals("Transição inválida", f.mensagem)
    }

    @Test
    fun http403ESemPermissao() {
        assertEquals(TipoErro.SEM_PERMISSAO, mapearHttp(403, null).tipo)
    }

    @Test
    fun http500ESemTextoTecnico() {
        val f = mapearHttp(500, ProblemaDto(detail = "NullReferenceException em X"))
        assertEquals(TipoErro.SERVIDOR, f.tipo)
        assertEquals(false, f.mensagem.contains("NullReference"))
    }

    @Test
    fun rotuloDeStatus() {
        assertEquals("Em rota", Formatadores.rotuloStatus("EmRota"))
        assertEquals("Aguardando entregador", Formatadores.rotuloStatus("AguardandoEntregador"))
    }

    @Test
    fun formatacaoDeDatas() {
        assertEquals("20/12/2026", Formatadores.data("2026-12-20"))
        assertEquals("20/12/2026", Formatadores.data("2026-12-20T10:00:00"))
        assertEquals("10/11/2026 14:30", Formatadores.dataHora("2026-11-10T14:30:00"))
        assertEquals("-", Formatadores.data(null))
    }

    @Test
    fun telefoneParaDiscagem() {
        assertEquals("11999990000", Formatadores.telefoneParaDiscagem("(11) 99999-0000"))
    }
}
