package br.com.medflow.app.ui.entregador

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.heightIn
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.medflow.app.data.EntregaEntity
import br.com.medflow.app.data.Formatadores
import br.com.medflow.app.data.StatusPedido
import br.com.medflow.app.ui.BotaoPrincipal
import br.com.medflow.app.ui.BotaoSecundario
import br.com.medflow.app.ui.BotaoTexto
import br.com.medflow.app.ui.CartaoMensagem
import br.com.medflow.app.ui.DetalheEntregaViewModel

private enum class Dialogo { NENHUM, CONFIRMAR, OCORRENCIA }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalheEntregaScreen(vm: DetalheEntregaViewModel, onVoltar: () -> Unit) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    var dialogo by rememberSaveable { mutableStateOf(Dialogo.NENHUM) }
    var erroExterno by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalhe da entrega") },
                navigationIcon = { BotaoTexto("Voltar", onVoltar, descricao = "Voltar para a lista de entregas") },
            )
        },
    ) { padding ->
        val entrega = estado.entrega
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (entrega == null) {
                Text("Entrega não encontrada neste aparelho. Volte e atualize a lista.")
                return@Column
            }

            estado.mensagem?.let { CartaoMensagem(it, aoFechar = vm::fecharMensagem) }
            erroExterno?.let { CartaoMensagem(it, erro = true, aoFechar = { erroExterno = null }) }
            if (estado.pendente != null) {
                CartaoMensagem(
                    "Há uma ação desta entrega aguardando envio. Ela será enviada assim que houver internet.",
                )
            }

            DadosEntrega(entrega)

            if (!entrega.telefone.isNullOrBlank()) {
                BotaoSecundario(
                    texto = "Ligar para o paciente",
                    onClick = { erroExterno = discar(contexto, entrega.telefone.orEmpty()) },
                    descricao = "Ligar para ${entrega.paciente}",
                )
            }
            BotaoSecundario(
                texto = "Abrir endereço no mapa",
                onClick = { erroExterno = abrirMapa(contexto, entrega.endereco) },
                descricao = "Abrir no mapa o endereço ${entrega.endereco}",
            )

            val bloqueado = estado.ocupado || estado.pendente != null
            when (entrega.status) {
                StatusPedido.AGUARDANDO_ENTREGADOR -> BotaoPrincipal(
                    texto = "Iniciar rota",
                    onClick = vm::iniciarRota,
                    habilitado = !bloqueado,
                    descricao = "Iniciar rota desta entrega",
                )
                StatusPedido.EM_ROTA -> {
                    BotaoPrincipal(
                        texto = "Confirmar entrega",
                        onClick = { dialogo = Dialogo.CONFIRMAR },
                        habilitado = !bloqueado,
                    )
                    BotaoSecundario(
                        texto = "Registrar ocorrência",
                        onClick = { dialogo = Dialogo.OCORRENCIA },
                        habilitado = !bloqueado,
                        descricao = "Registrar ocorrência, por exemplo paciente ausente",
                    )
                }
                else -> Text(
                    "Nenhuma ação disponível para a situação atual (${Formatadores.rotuloStatus(entrega.status)}).",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }

    when (dialogo) {
        Dialogo.CONFIRMAR -> DialogoTexto(
            titulo = "Confirmar entrega",
            rotulo = "Nome de quem recebeu",
            textoBotao = "Confirmar",
            aoConfirmar = { texto ->
                val erro = vm.confirmarEntrega(texto)
                if (erro == null) dialogo = Dialogo.NENHUM
                erro
            },
            aoCancelar = { dialogo = Dialogo.NENHUM },
        )
        Dialogo.OCORRENCIA -> DialogoTexto(
            titulo = "Registrar ocorrência",
            rotulo = "Motivo (ex.: paciente ausente)",
            textoBotao = "Registrar",
            aoConfirmar = { texto ->
                val erro = vm.registrarOcorrencia(texto)
                if (erro == null) dialogo = Dialogo.NENHUM
                erro
            },
            aoCancelar = { dialogo = Dialogo.NENHUM },
        )
        Dialogo.NENHUM -> Unit
    }
}

@Composable
private fun DadosEntrega(entrega: EntregaEntity) {
    Text(
        entrega.codigo,
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.semantics { heading() },
    )
    Campo("Paciente", entrega.paciente)
    Campo("Endereço", entrega.endereco)
    Campo("Telefone", entrega.telefone?.takeIf { it.isNotBlank() } ?: "Não informado")
    Campo("Quantidade", "${entrega.totalSaches} sachês")
    Campo("Situação", Formatadores.rotuloStatus(entrega.status))
    if (entrega.tentativa > 0) Campo("Tentativa", "${entrega.tentativa}")
}

@Composable
private fun Campo(rotulo: String, valor: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(rotulo, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(valor, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun DialogoTexto(
    titulo: String,
    rotulo: String,
    textoBotao: String,
    aoConfirmar: (String) -> String?,
    aoCancelar: () -> Unit,
) {
    var texto by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf<String?>(null) }
    val mensagemErro = erro
    AlertDialog(
        onDismissRequest = aoCancelar,
        title = { Text(titulo) },
        text = {
            OutlinedTextField(
                value = texto,
                onValueChange = {
                    texto = it
                    erro = null
                },
                label = { Text(rotulo) },
                isError = mensagemErro != null,
                supportingText = if (mensagemErro != null) {
                    { Text(mensagemErro) }
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(
                onClick = { erro = aoConfirmar(texto) },
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text(textoBotao) }
        },
        dismissButton = { BotaoTexto("Cancelar", aoCancelar) },
    )
}

/** Abre o discador (sem permissão; o usuário toca em ligar). Retorna mensagem de erro ou null. */
private fun discar(contexto: Context, telefone: String): String? = try {
    contexto.startActivity(
        Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Formatadores.telefoneParaDiscagem(telefone)))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
    null
} catch (_: ActivityNotFoundException) {
    "Este aparelho não tem um aplicativo de telefone."
}

/** Abre o endereço no app de mapas. Retorna mensagem de erro ou null. */
private fun abrirMapa(contexto: Context, endereco: String): String? = try {
    contexto.startActivity(
        Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(endereco)))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
    null
} catch (_: ActivityNotFoundException) {
    "Este aparelho não tem um aplicativo de mapas."
}
