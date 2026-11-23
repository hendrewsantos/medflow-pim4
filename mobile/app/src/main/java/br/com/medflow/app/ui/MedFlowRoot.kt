package br.com.medflow.app.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import br.com.medflow.app.ui.paciente.LinhaDoTempoScreen
import br.com.medflow.app.ui.paciente.PedidosScreen

/** Raiz: sem sessão mostra o login; com sessão, o fluxo do perfil (Entregador ou Paciente). */
@Composable
fun MedFlowRoot(container: AppContainer) {
    val sessao by container.sessao.sessao.collectAsStateWithLifecycle()
    val saida: SaidaViewModel = viewModel(factory = SaidaViewModel.factory(container))
    val avisoSaida by saida.aviso.collectAsStateWithLifecycle()

    val atual = sessao
    if (atual == null) {
        LoginScreen(viewModel(factory = LoginViewModel.factory(container)))
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
    NavHost(navController = nav, startDestination = "lista") {
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
private fun FluxoPaciente(container: AppContainer, sessao: Sessao, onSair: () -> Unit) {
    val nav = rememberNavController()
    val pedidosVm: PedidosViewModel = viewModel(
        key = "pedidos-${sessao.usuarioId}",
        factory = PedidosViewModel.factory(container),
    )
    NavHost(navController = nav, startDestination = "pedidos") {
        composable("pedidos") {
            PedidosScreen(
                vm = pedidosVm,
                nome = sessao.nome,
                onAbrir = { id -> nav.navigate("pedido/$id") },
                onSair = onSair,
            )
        }
        composable(
            route = "pedido/{pedidoId}",
            arguments = listOf(navArgument("pedidoId") { type = NavType.IntType }),
        ) { entrada ->
            val id = entrada.arguments?.getInt("pedidoId") ?: 0
            val vm: LinhaDoTempoViewModel = viewModel(
                key = "tempo-${sessao.usuarioId}-$id",
                factory = LinhaDoTempoViewModel.factory(container, id),
            )
            LinhaDoTempoScreen(vm = vm, onVoltar = { nav.popBackStack() })
        }
    }
}
