package br.com.medflow.app

import br.com.medflow.app.data.ApiJson
import br.com.medflow.app.data.AssinaturaDto
import br.com.medflow.app.data.Cpf
import br.com.medflow.app.data.Interacoes
import br.com.medflow.app.data.MedicamentoCatalogoDto
import br.com.medflow.app.data.Programas
import br.com.medflow.app.data.SenhaRegras
import br.com.medflow.app.data.SimulacaoDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PacienteRegrasTest {
    @Test
    fun cpfValidoEInvalido() {
        assertTrue(Cpf.valido("529.982.247-25"))
        assertTrue(Cpf.valido("52998224725"))
        assertFalse(Cpf.valido("529.982.247-24"))
        assertFalse(Cpf.valido("111.111.111-11"))
        assertFalse(Cpf.valido("123"))
        assertFalse(Cpf.valido(""))
    }

    @Test
    fun cpfFormatadoEnquantoDigita() {
        assertEquals("529.982.247-25", Cpf.formatar("52998224725"))
        assertEquals("529.98", Cpf.formatar("52998"))
        assertEquals("529.982.247-25", Cpf.formatar("529.982.247-25999"))
    }

    @Test
    fun senhaForteExigeOitoCaracteresLetrasENumeros() {
        assertTrue(SenhaRegras.forte("abc12345"))
        assertFalse(SenhaRegras.forte("abcdefgh"))
        assertFalse(SenhaRegras.forte("12345678"))
        assertFalse(SenhaRegras.forte("abc123"))
    }

    @Test
    fun avisosDeCombinacaoInformamSemInventarAlertas() {
        val levo = Interacoes.de(listOf("Levotiroxina sódica levotiroxina sódica", "Omeprazol omeprazol", "Carbonato de cálcio com vitamina D carbonato de cálcio + colecalciferol"))
        assertEquals(setOf("Levotiroxina com inibidor de bomba de prótons", "Levotiroxina com cálcio ou ferro"), levo.map { it.titulo }.toSet())
        assertTrue(Interacoes.de(listOf("Omeprazol omeprazol", "Dipirona dipirona sódica")).isEmpty())
        assertEquals(1, Interacoes.de(listOf("Sinvastatina sinvastatina", "Anlodipino besilato de anlodipino")).size)
        assertEquals(1, Interacoes.de(listOf("AAS ácido acetilsalicílico", "Varfarina varfarina sódica")).size)
        assertTrue(Interacoes.de(listOf("Omeprazol omeprazol", "Pantoprazol pantoprazol sódico")).isEmpty())
        assertTrue(Interacoes.de(emptyList()).isEmpty())
    }

    @Test
    fun programasAgrupamPeloPrincipioAtivo() {
        assertEquals("Hipertensão", Programas.de("Losartana potássica"))
        assertEquals("Diabetes", Programas.de("cloridrato de metformina"))
        assertEquals("Tireoide", Programas.de("levotiroxina sódica"))
        assertEquals("Outros cuidados", Programas.de("algo que não está na lista"))
        assertEquals(8, Programas.todos.size)
    }

    @Test
    fun leDadosDaApiComCamposNovosOuAusentes() {
        val ficha = ApiJson.decodeFromString(
            MedicamentoCatalogoDto.serializer(),
            """{"id":3,"nome":"Clonazepam","principioAtivo":"clonazepam","concentracao":"2 mg","formaFarmaceutica":"comprimido","tarja":"Preta","categoria":"ControleEspecial","termolabil":false,"precoUnitario":1.2,"permitidoNaBox":false,"disponivel":true,"motivoForaDaBox":"Controle especial (Portaria SVS/MS 344/1998)","campoNovo":1}""",
        )
        assertFalse(ficha.permitidoNaBox)
        assertNotNull(ficha.motivoForaDaBox)
        val sim = ApiJson.decodeFromString(SimulacaoDto.serializer(), """{"podeConfirmar":true,"itens":[{"medicamentoId":1,"medicamento":"Losartana","necessario":60,"disponivel":300,"atende":true}],"valorMedicamentos":36.0,"taxaEntrega":9.9,"prazoPagamentoMinutos":3}""")
        assertTrue(sim.podeConfirmar)
        assertEquals(45.9, sim.valorMedicamentos + sim.taxaEntrega, 0.001)
        val semPagamento = ApiJson.decodeFromString(SimulacaoDto.serializer(), """{"podeConfirmar":true,"itens":[]}""")
        assertEquals(0, semPagamento.prazoPagamentoMinutos)
        val assinatura = ApiJson.decodeFromString(AssinaturaDto.serializer(), """{"ativa":false}""")
        assertFalse(assinatura.ativa)
        assertNull(assinatura.proximaRenovacaoEm)
    }
}
