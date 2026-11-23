package br.com.medflow.app.ui.paciente

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.medflow.app.BuildConfig
import br.com.medflow.app.data.Formatadores
import br.com.medflow.app.data.PedidoDto
import br.com.medflow.app.data.StatusPedido
import br.com.medflow.app.ui.BotaoPrincipal
import br.com.medflow.app.ui.entradaSuave
import kotlinx.coroutines.delay
import br.com.medflow.app.data.HistoricoDto
import br.com.medflow.app.ui.BotaoSecundario
import br.com.medflow.app.ui.BotaoTexto
import br.com.medflow.app.ui.CartaoMensagem
import br.com.medflow.app.ui.LinhaDoTempoViewModel
import br.com.medflow.app.ui.Titulo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinhaDoTempoScreen(vm: LinhaDoTempoViewModel, onVoltar: () -> Unit) {
    val estado by vm.estado.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Linha do tempo") },
                navigationIcon = { BotaoTexto("Voltar", onVoltar, descricao = "Voltar para meus pedidos") },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (estado.carregando) {
                CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = "Carregando pedido" })
            }
            estado.erro?.let {
                CartaoMensagem(it, erro = true)
                BotaoSecundario("Tentar de novo", vm::carregar)
            }
            estado.pedido?.let { pedido ->
                Text(
                    "Pedido ${pedido.id} — ${Formatadores.rotuloStatus(pedido.status)}",
                    style = MaterialTheme.typography.titleLarge,
                )
                if (pedido.status == StatusPedido.AGUARDANDO_PAGAMENTO && pedido.pagamentoPrazoEm != null) {
                    CartaoPagamento(pedido, estado.pagando, estado.aviso, onAprovar = vm::aprovarPagamento, onPrazo = vm::carregar)
                }
                if (pedido.enderecoEntrega.isNotBlank()) {
                    Text("Entrega em: ${pedido.enderecoEntrega}", style = MaterialTheme.typography.bodyLarge)
                }
                if (pedido.itens.isNotEmpty()) {
                    Titulo("Medicamentos")
                    pedido.itens.forEach {
                        Text("${it.medicamento} — ${it.quantidade} un.", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Titulo("O que já aconteceu")
                if (pedido.historico.isEmpty()) {
                    Text("Ainda não há registros para este pedido.")
                }
                // Mais recente primeiro.
                pedido.historico.asReversed().forEachIndexed { i, h -> Etapa(h, Modifier.entradaSuave(i)) }
            }
        }
    }
}

@Composable
private fun Etapa(h: HistoricoDto, modifier: Modifier = Modifier) {
    val titulo = Formatadores.rotuloStatus(h.para)
    val quando = Formatadores.dataHora(h.em)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = buildString {
                    append("$titulo, em $quando.")
                    if (!h.observacao.isNullOrBlank()) append(" Observação: ${h.observacao}.")
                }
            },
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            Text(quando, style = MaterialTheme.typography.bodyMedium)
            if (!h.observacao.isNullOrBlank()) Text(h.observacao, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Valor, prazo com contagem regressiva e, só na versão de demonstração, o botão que imita o retorno do pagamento. */
@Composable
private fun CartaoPagamento(pedido: PedidoDto, pagando: Boolean, aviso: String?, onAprovar: () -> Unit, onPrazo: () -> Unit) {
    val prazo = pedido.pagamentoPrazoEm
    var restante by remember(prazo) { mutableLongStateOf(Formatadores.milissegundosAte(prazo)) }
    LaunchedEffect(prazo) {
        while (restante > 0L) {
            delay(1000)
            restante = Formatadores.milissegundosAte(prazo)
        }
        onPrazo()
    }
    val total = pedido.valorMedicamentos + pedido.valorEntrega
    val tempo = Formatadores.contagem(restante)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "Pagamento pendente. Total ${Formatadores.reais(total)}. Tempo restante para pagar: $tempo."
            },
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Titulo("Pagamento")
            Text("Pague dentro do prazo para manter a reserva dos medicamentos.", style = MaterialTheme.typography.bodyMedium)
            Text("Tempo restante: $tempo", style = MaterialTheme.typography.headlineSmall)
            Text("Medicamentos: ${Formatadores.reais(pedido.valorMedicamentos)}", style = MaterialTheme.typography.bodyLarge)
            Text("Entrega: ${Formatadores.reais(pedido.valorEntrega)}", style = MaterialTheme.typography.bodyLarge)
            Text("Total: ${Formatadores.reais(total)}", style = MaterialTheme.typography.titleMedium)
            Text("Pagamento simulado: nenhum valor real é cobrado.", style = MaterialTheme.typography.bodyMedium)
            aviso?.let { CartaoMensagem(it, erro = true) }
            if (BuildConfig.DEBUG) {
                BotaoPrincipal(
                    texto = if (pagando) "Aprovando..." else "Aprovar pagamento (demonstração)",
                    onClick = onAprovar,
                    habilitado = !pagando,
                )
            }
        }
    }
}
