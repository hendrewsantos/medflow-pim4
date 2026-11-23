package br.com.medflow.app.ui.paciente

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.com.medflow.app.AppContainer
import br.com.medflow.app.data.Sessao
import br.com.medflow.app.ui.LinhaDoTempoViewModel
import br.com.medflow.app.ui.PedidosViewModel
import br.com.medflow.app.ui.movimentoReduzido

private data class Aba(val rota: String, val rotulo: String, val icone: ImageVector)

private val ABAS = listOf(
    Aba("inicio", "Início", Icons.Filled.Home),
    Aba("medicamentos", "Catálogo", Icons.Filled.Search),
    Aba("box", "Montar Box", Icons.Filled.Add),
    Aba("pedidos", "Pedidos", Icons.AutoMirrored.Filled.List),
    Aba("mais", "Mais", Icons.Filled.Menu),
)

/** Área do paciente: barra de navegação com Início, Medicamentos, Montar Box, Pedidos e Mais (receitas, renovação, dados). */
@Composable
fun PacienteShell(container: AppContainer, sessao: Sessao, onSair: () -> Unit) {
    val nav = rememberNavController()
    val entrada by nav.currentBackStackEntryAsState()
    val rota = entrada?.destination?.route
    val chave = sessao.usuarioId
    val reduzido = movimentoReduzido()

    val inicioVm: InicioPacienteViewModel = viewModel(key = "inicio-$chave", factory = InicioPacienteViewModel.factory(container))
    val assinaturaVm: AssinaturaViewModel = viewModel(key = "assinatura-$chave", factory = AssinaturaViewModel.factory(container))
    val pedidosVm: PedidosViewModel = viewModel(key = "pedidos-$chave", factory = PedidosViewModel.factory(container))

    fun ir(destino: String) {
        nav.navigate(destino) {
            popUpTo("inicio") { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    fun abrirPedido(id: Int) { nav.navigate("pedido/$id") { launchSingleTop = true } }

    val entra: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
        { if (reduzido) fadeIn(tween(1)) else fadeIn(tween(300)) + slideInHorizontally(tween(300)) { it / 14 } }
    val sai: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
        { if (reduzido) fadeOut(tween(1)) else fadeOut(tween(200)) }

    Scaffold(
        bottomBar = {
            if (rota in ABAS.map { it.rota }) {
                NavigationBar {
                    ABAS.forEach { aba ->
                        NavigationBarItem(
                            selected = rota == aba.rota,
                            onClick = { ir(aba.rota) },
                            icon = { Icon(aba.icone, contentDescription = null) },
                            label = { Text(aba.rotulo) },
                            alwaysShowLabel = true,
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = "inicio",
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
            enterTransition = entra, exitTransition = sai, popEnterTransition = entra, popExitTransition = sai,
        ) {
            composable("inicio") {
                InicioPacienteScreen(inicioVm, assinaturaVm, sessao.nome, onIrPara = ::ir, onAbrirPedido = ::abrirPedido)
            }
            composable("medicamentos") {
                val vm: CatalogoViewModel = viewModel(key = "catalogo-$chave", factory = CatalogoViewModel.factory(container))
                MedicamentosScreen(vm, onMontarBox = { ir("box") })
            }
            composable("box") {
                val vm: MontarBoxViewModel = viewModel(key = "box-$chave", factory = MontarBoxViewModel.factory(container))
                MontarBoxScreen(vm, onEnviarReceita = { nav.navigate("receitas") }, onAbrirPedido = ::abrirPedido)
            }
            composable("pedidos") {
                br.com.medflow.app.ui.paciente.PedidosScreen(vm = pedidosVm, nome = sessao.nome, onAbrir = ::abrirPedido, onSair = onSair)
            }
            composable("mais") { MaisScreen(sessao.nome, onIrPara = { nav.navigate(it) }, onSair = onSair) }
            composable("receitas") {
                val vm: ReceitasViewModel = viewModel(key = "receitas-$chave", factory = ReceitasViewModel.factory(container))
                ReceitasScreen(vm, onVoltar = { nav.popBackStack() })
            }
            composable("assinatura") { AssinaturaTela(assinaturaVm, onVoltar = { nav.popBackStack() }) }
            composable("dados") {
                val vm: MeusDadosViewModel = viewModel(key = "dados-$chave", factory = MeusDadosViewModel.factory(container))
                MeusDadosScreen(vm, onVoltar = { nav.popBackStack() }, onContaExcluida = {
                    container.sessao.limpar()
                })
            }
            composable("pedido/{pedidoId}", arguments = listOf(navArgument("pedidoId") { type = NavType.IntType })) { e ->
                val id = e.arguments?.getInt("pedidoId") ?: 0
                val vm: LinhaDoTempoViewModel = viewModel(key = "tempo-$chave-$id", factory = LinhaDoTempoViewModel.factory(container, id))
                LinhaDoTempoScreen(vm = vm, onVoltar = { nav.popBackStack() })
            }
        }
    }
}
