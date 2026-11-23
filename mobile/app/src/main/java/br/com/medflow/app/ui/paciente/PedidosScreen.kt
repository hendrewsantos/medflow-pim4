package br.com.medflow.app.ui.paciente

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.medflow.app.data.Formatadores
import br.com.medflow.app.data.PedidoEntity
import br.com.medflow.app.data.ProximaRenovacaoDto
import br.com.medflow.app.ui.BotaoSecundario
import br.com.medflow.app.ui.BotaoTexto
import br.com.medflow.app.ui.CartaoMensagem
import br.com.medflow.app.ui.PedidosViewModel
import br.com.medflow.app.ui.Titulo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PedidosScreen(
    vm: PedidosViewModel,
    nome: String,
    onAbrir: (Int) -> Unit,
    onSair: () -> Unit,
) {
    val estado by vm.estado.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meus pedidos") },
                actions = { BotaoTexto("Sair", onSair, descricao = "Sair da conta") },
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            item { Text("Olá, $nome", style = MaterialTheme.typography.titleMedium) }
            estado.mensagem?.let { item { CartaoMensagem(it) } }
            item { CartaoRenovacao(estado.renovacao) }
            item {
                BotaoSecundario(
                    texto = if (estado.atualizando) "Atualizando..." else "Atualizar",
                    onClick = vm::atualizar,
                    habilitado = !estado.atualizando,
                )
            }
            item { Titulo("Pedidos") }
            if (estado.pedidos.isEmpty()) {
                item { Text("Você ainda não tem pedidos.", style = MaterialTheme.typography.bodyLarge) }
            }
            items(estado.pedidos, key = { it.id }) { pedido ->
                CartaoPedido(pedido) { onAbrir(pedido.id) }
            }
        }
    }
}

@Composable
private fun CartaoRenovacao(renovacao: ProximaRenovacaoDto?) {
    val texto = when {
        renovacao == null -> "Carregando a previsão da próxima renovação..."
        !renovacao.temBox -> "Você não tem uma box ativa no momento."
        else -> buildString {
            append("Sua box termina por volta de ${Formatadores.data(renovacao.dataEstimadaFimDaBox)}")
            renovacao.diasRestantes?.let { append(" (faltam $it dias)") }
            append(".")
            if (renovacao.receitaVenceAntes) {
                append(
                    " Atenção: sua receita vale só até ${Formatadores.data(renovacao.receitaValidaAte)}, " +
                        "antes do fim da box. Envie uma receita nova pelo site.",
                )
            }
        }
    }
    val alerta = renovacao?.receitaVenceAntes == true
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (alerta) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
            contentColor = if (alerta) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = "Próxima renovação. $texto" },
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Próxima renovação", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            Text(texto, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun CartaoPedido(pedido: PedidoEntity, onClick: () -> Unit) {
    val situacao = Formatadores.rotuloStatus(pedido.status)
    val descricao = "Pedido número ${pedido.id}. Situação: $situacao. " +
        "Atualizado em ${Formatadores.dataHora(pedido.atualizadoEm)}. Toque para ver a linha do tempo."
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Ver linha do tempo do pedido", onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = descricao },
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Pedido ${pedido.id}", style = MaterialTheme.typography.titleMedium)
            Text(situacao, style = MaterialTheme.typography.bodyLarge)
            Text("Atualizado em ${Formatadores.dataHora(pedido.atualizadoEm)}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
