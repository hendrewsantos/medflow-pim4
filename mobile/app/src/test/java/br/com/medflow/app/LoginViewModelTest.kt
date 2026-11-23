package br.com.medflow.app

import br.com.medflow.app.data.AutenticacaoRepositorio
import br.com.medflow.app.data.Resultado
import br.com.medflow.app.data.Sessao
import br.com.medflow.app.data.TipoErro
import br.com.medflow.app.ui.LoginViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

private class AuthFalso(var resposta: Resultado<Sessao>) : AutenticacaoRepositorio {
    var chamadas = 0
    override suspend fun entrar(email: String, senha: String): Resultado<Sessao> {
        chamadas++
        return resposta
    }

    override suspend fun sair(): String? = null
}

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    @Before
    fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun depois() = Dispatchers.resetMain()

    @Test
    fun camposVaziosNaoChamamARede() {
        val auth = AuthFalso(Resultado.Ok(Sessao("t", 1, "Ana", "Paciente")))
        val vm = LoginViewModel(auth)
        vm.entrar("  ", "")
        assertEquals(0, auth.chamadas)
        assertNotNull(vm.estado.value.erro)
    }

    @Test
    fun falhaMostraMensagemEFinalizaCarregando() {
        val auth = AuthFalso(Resultado.Falha(TipoErro.NAO_AUTORIZADO, "E-mail ou senha incorretos."))
        val vm = LoginViewModel(auth)
        vm.entrar("a@b.com", "x")
        assertEquals("E-mail ou senha incorretos.", vm.estado.value.erro)
        assertFalse(vm.estado.value.carregando)
    }

    @Test
    fun sucessoLimpaErro() {
        val auth = AuthFalso(Resultado.Ok(Sessao("t", 1, "Ana", "Paciente")))
        val vm = LoginViewModel(auth)
        vm.entrar("a@b.com", "x")
        assertEquals(1, auth.chamadas)
        assertNull(vm.estado.value.erro)
        assertFalse(vm.estado.value.carregando)
    }
}
