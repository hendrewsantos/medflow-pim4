package br.com.medflow.app.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.com.medflow.app.AppContainer
import br.com.medflow.app.data.Perfis
import br.com.medflow.app.data.Sessao
import br.com.medflow.app.ui.entregador.DetalheEntregaScreen
import br.com.medflow.app.ui.entregador.ListaEntregasScreen
import br.com.medflow.app.ui.paciente.CadastroViewModel
import br.com.medflow.app.ui.paciente.PacienteShell
import br.com.medflow.app.ui.publico.CadastroScreen
import br.com.medflow.app.ui.publico.InicioPublicoScreen

/** Raiz: sem sessão mostra o login; com sessão, o fluxo do perfil (Entregador ou Paciente). */
@Composable
fun MedFlowRoot(container: AppContainer) {
    val sessao by container.sessao.sessao.collectAsStateWithLifecycle()
    val saida: SaidaViewModel = viewModel(factory = SaidaViewModel.factory(container))
    val avisoSaida by saida.aviso.collectAsStateWithLifecycle()

    var aberturaVisivel by rememberSaveable { mutableStateOf(true) }
    if (aberturaVisivel) {
        Abertura(onFim = { aberturaVisivel = false })
        return
    }

    val atual = sessao
    if (atual == null) {
        FluxoPublico(container)
    } else if (atual.perfil == Perfis.ENTREGADOR) {
        FluxoEntregador(container, atual, onSair = saida::sair)
    } else {
        FluxoPaciente(container, atual, onSair = saida::sair)
    }

    avisoSaida?.let { mensagem ->
        AlertDialog(
            onDismissRequest = saida::fecharAviso,
            title = { Text("Não foi possível sair agora") },
            text = { Text(mensagem) },
            confirmButton = { BotaoTexto("Entendi", saida::fecharAviso) },
        )
    }
}

@Composable
private fun FluxoEntregador(container: AppContainer, sessao: Sessao, onSair: () -> Unit) {
    val nav = rememberNavController()
    val listaVm: EntregasViewModel = viewModel(
        key = "entregas-${sessao.usuarioId}",
        factory = EntregasViewModel.factory(container),
    )
    NavHost(navController = nav, startDestination = "lista", enterTransition = entradaDeTela(), exitTransition = saidaDeTela(), popEnterTransition = entradaDeTela(), popExitTransition = saidaDeTela()) {
        composable("lista") {
            ListaEntregasScreen(
                vm = listaVm,
                nome = sessao.nome,
                onAbrir = { id -> nav.navigate("detalhe/$id") },
                onSair = onSair,
            )
        }
        composable(
            route = "detalhe/{pedidoId}",
            arguments = listOf(navArgument("pedidoId") { type = NavType.IntType }),
        ) { entrada ->
            val id = entrada.arguments?.getInt("pedidoId") ?: 0
            val vm: DetalheEntregaViewModel = viewModel(
                key = "detalhe-${sessao.usuarioId}-$id",
                factory = DetalheEntregaViewModel.factory(container, id),
            )
            DetalheEntregaScreen(vm = vm, onVoltar = { nav.popBackStack() })
        }
    }
}

@Composable
private fun FluxoPublico(container: AppContainer) {
    val nav = rememberNavController()
    NavHost(
        navController = nav, startDestination = "inicio",
        enterTransition = entradaDeTela(), exitTransition = saidaDeTela(), popEnterTransition = entradaDeTela(), popExitTransition = saidaDeTela(),
    ) {
        composable("inicio") {
            InicioPublicoScreen(onEntrar = { nav.navigate("entrar") }, onCriarConta = { nav.navigate("cadastro") })
        }
        composable("entrar") {
            LoginScreen(viewModel(factory = LoginViewModel.factory(container)), onCriarConta = { nav.navigate("cadastro") }, onVoltar = { nav.popBackStack() })
        }
        composable("cadastro") {
            CadastroScreen(viewModel(factory = CadastroViewModel.factory(container)), onVoltar = { nav.popBackStack() })
        }
    }
}

@Composable
private fun FluxoPaciente(container: AppContainer, sessao: Sessao, onSair: () -> Unit) = PacienteShell(container, sessao, onSair)

/** Telas entram esmaecendo e deslizando de leve; com "remover animações" do sistema, só esmaecem. */
@Composable
private fun entradaDeTela(): AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> androidx.compose.animation.EnterTransition {
    val reduzido = movimentoReduzido()
    return {
        if (reduzido) fadeIn(tween(1)) else fadeIn(tween(320)) + slideInHorizontally(tween(320)) { it / 12 }
    }
}

@Composable
private fun saidaDeTela(): AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> androidx.compose.animation.ExitTransition {
    val reduzido = movimentoReduzido()
    return {
        if (reduzido) fadeOut(tween(1)) else fadeOut(tween(220)) + slideOutHorizontally(tween(220)) { -it / 12 }
    }
}
