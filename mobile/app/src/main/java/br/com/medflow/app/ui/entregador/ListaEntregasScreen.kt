package br.com.medflow.app.ui.entregador

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import br.com.medflow.app.ui.entradaSuave
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.medflow.app.data.EntregaEntity
import br.com.medflow.app.data.Formatadores
import br.com.medflow.app.ui.BotaoSecundario
import br.com.medflow.app.ui.BotaoTexto
import br.com.medflow.app.ui.CartaoMensagem
import br.com.medflow.app.ui.EntregasViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaEntregasScreen(
    vm: EntregasViewModel,
    nome: String,
    onAbrir: (Int) -> Unit,
    onSair: () -> Unit,
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    var aba by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Entregas") },
                actions = { BotaoTexto("Sair", onSair, descricao = "Sair da conta") },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Text(
                "Olá, $nome",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            TabRow(selectedTabIndex = aba) {
                Tab(
                    selected = aba == 0,
                    onClick = { aba = 0 },
                    text = { Text("Boxes prontas (${estado.disponiveis.size})") },
                )
                Tab(
                    selected = aba == 1,
                    onClick = { aba = 1 },
                    text = { Text("Minhas entregas (${estado.minhas.size})") },
                )
            }

            val lista = if (aba == 0) estado.disponiveis else estado.minhas

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                estado.aviso?.let { item { CartaoMensagem(it, erro = true, aoFechar = vm::fecharAviso) } }
                estado.mensagem?.let { item { CartaoMensagem(it, aoFechar = vm::fecharMensagem) } }
                item {
                    BotaoSecundario(
                        texto = if (estado.atualizando) "Atualizando..." else "Atualizar lista",
                        onClick = vm::atualizar,
                        habilitado = !estado.atualizando,
                    )
                }
                if (lista.isEmpty()) {
                    item {
                        Text(
                            if (aba == 0) "Nenhuma box pronta para retirada no momento."
                            else "Você não tem entregas em andamento.",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
                itemsIndexed(lista, key = { _, x -> x.pedidoId }) { i, entrega ->
                    CartaoEntrega(
                        modifier = Modifier.entradaSuave(i),
                        entrega = entrega,
                        aguardandoEnvio = entrega.pedidoId in estado.pedidosPendentes,
                        onClick = { onAbrir(entrega.pedidoId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CartaoEntrega(entrega: EntregaEntity, aguardandoEnvio: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val situacao = Formatadores.rotuloStatus(entrega.status)
    val descricao = buildString {
        append("Entrega ${entrega.codigo}. Paciente ${entrega.paciente}. Endereço ${entrega.endereco}. ")
        append("${entrega.totalSaches} sachês. Situação: $situacao.")
        if (aguardandoEnvio) append(" Ação aguardando envio.")
        append(" Toque para abrir.")
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Abrir detalhes da entrega", onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = descricao },
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(entrega.codigo, style = MaterialTheme.typography.titleMedium)
            Text(entrega.paciente, style = MaterialTheme.typography.bodyLarge)
            Text(entrega.endereco, style = MaterialTheme.typography.bodyMedium)
            Text("${entrega.totalSaches} sachês · $situacao", style = MaterialTheme.typography.bodyMedium)
            if (aguardandoEnvio) {
                Text(
                    "Aguardando envio (sem internet)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
