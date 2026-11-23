package br.com.medflow.app.ui.paciente

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.medflow.app.data.Formatadores
import br.com.medflow.app.ui.BotaoPrincipal
import br.com.medflow.app.ui.BotaoSecundario
import br.com.medflow.app.ui.BotaoTexto
import br.com.medflow.app.ui.CartaoMensagem
import br.com.medflow.app.ui.Titulo
import br.com.medflow.app.ui.entradaSuave

private fun rotuloReceita(status: String) = when (status) {
    "Pendente" -> "Em análise"
    "Aprovada" -> "Aprovada"
    "Recusada" -> "Recusada"
    else -> status
}

// ───────────────────────── Receitas ─────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceitasScreen(vm: ReceitasViewModel, onVoltar: () -> Unit) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    var consentimento by rememberSaveable { mutableStateOf(false) }

    val escolher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val tipo = contexto.contentResolver.getType(uri) ?: "application/octet-stream"
            val nome = contexto.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (c.moveToFirst() && i >= 0) c.getString(i) else null
            } ?: "receita"
            val bytes = runCatching { contexto.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull() ?: ByteArray(0)
            vm.enviar(nome, tipo, bytes)
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Minhas receitas") }, navigationIcon = { BotaoTexto("Voltar", onVoltar, descricao = "Voltar") }) }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Titulo("Enviar nova receita")
                        Text("PDF, PNG ou JPG, até 10 MB. Foto nítida, com nome do paciente, assinatura e carimbo do prescritor.", style = MaterialTheme.typography.bodyMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = consentimento, onCheckedChange = { consentimento = it })
                            Text(
                                "Autorizo, de forma específica, o uso dos dados de saúde desta receita para a análise farmacêutica e a montagem da minha Box (LGPD, art. 11, I).",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        BotaoPrincipal(
                            if (estado.enviando) "Enviando..." else "Escolher o arquivo e enviar",
                            { escolher.launch(arrayOf("application/pdf", "image/png", "image/jpeg")) },
                            habilitado = consentimento && !estado.enviando,
                        )
                        estado.mensagem?.let { CartaoMensagem(it, aoFechar = vm::limparMensagem) }
                        estado.erro?.let { CartaoMensagem(it, erro = true, aoFechar = vm::limparMensagem) }
                    }
                }
            }
            item { Titulo("Receitas enviadas", Modifier.padding(top = 6.dp)) }
            if (estado.carregando) item { Carregando("Carregando as receitas") }
            if (!estado.carregando && estado.receitas.isEmpty()) item { Text("Você ainda não enviou nenhuma receita.", style = MaterialTheme.typography.bodyLarge) }
            items(estado.receitas.size) { i ->
                val r = estado.receitas[i]
                Card(Modifier.fillMaxWidth().entradaSuave(i)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(r.nomeOriginal, style = MaterialTheme.typography.titleSmall)
                        Text("${rotuloReceita(r.status)} · enviada em ${Formatadores.data(r.enviadaEm)}", style = MaterialTheme.typography.bodyMedium)
                        if (r.status == "Aprovada") Text("Válida até ${Formatadores.data(r.validaAte)}" + (r.prescritorNome?.let { " · $it" } ?: ""), style = MaterialTheme.typography.bodySmall)
                        r.motivoRecusa?.takeIf { it.isNotBlank() }?.let { Text("Motivo da recusa: $it", style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
    }
}

// ───────────────────────── Renovação automática (tela) ─────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssinaturaTela(vm: AssinaturaViewModel, onVoltar: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Renovação automática") }, navigationIcon = { BotaoTexto("Voltar", onVoltar, descricao = "Voltar") }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AssinaturaCard(vm)
            Text(
                "A renovação automática só acontece com receita aprovada e válida e sem outra Box em andamento. Se a receita vencer, avisamos para enviar uma nova. Nenhum valor real é cobrado.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

// ───────────────────────── Meus dados e privacidade ─────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeusDadosScreen(vm: MeusDadosViewModel, onVoltar: () -> Unit, onContaExcluida: () -> Unit) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    var confirmaExclusao by remember { mutableStateOf(false) }
    var confirmaRevogar by remember { mutableStateOf(false) }

    LaunchedEffect(estado.exportacao) {
        estado.exportacao?.let { json ->
            val envio = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_SUBJECT, "Meus dados no MedFlow"); putExtra(Intent.EXTRA_TEXT, json) }
            contexto.startActivity(Intent.createChooser(envio, "Compartilhar uma cópia dos meus dados"))
            vm.exportacaoEntregue()
        }
    }
    LaunchedEffect(estado.contaExcluida) { if (estado.contaExcluida) onContaExcluida() }

    Scaffold(topBar = { TopAppBar(title = { Text("Meus dados") }, navigationIcon = { BotaoTexto("Voltar", onVoltar, descricao = "Voltar") }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val dados = estado.dados
            if (estado.carregando) Carregando("Carregando os seus dados")
            estado.mensagem?.let { CartaoMensagem(it) }
            estado.erro?.let { CartaoMensagem(it, erro = true) }
            if (dados != null) {
                var nome by rememberSaveable(dados.id, dados.nome) { mutableStateOf(dados.nome) }
                var email by rememberSaveable(dados.id, dados.email) { mutableStateOf(dados.email) }
                var endereco by rememberSaveable(dados.id, dados.endereco) { mutableStateOf(dados.endereco.orEmpty()) }
                var telefone by rememberSaveable(dados.id, dados.telefone) { mutableStateOf(dados.telefone.orEmpty()) }

                Titulo("Seus dados")
                Text("CPF: ${br.com.medflow.app.data.Cpf.formatar(dados.cpf)} (não pode ser alterado)", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(nome, { nome = it }, label = { Text("Nome") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(email, { email = it }, label = { Text("E-mail") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(endereco, { endereco = it }, label = { Text("Endereço de entrega") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(telefone, { telefone = it }, label = { Text("Telefone") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                BotaoPrincipal(if (estado.ocupado) "Salvando..." else "Salvar alterações", { vm.salvar(nome, email, endereco, telefone) }, habilitado = !estado.ocupado)

                Titulo("Consentimentos", Modifier.padding(top = 8.dp))
                dados.consentimentos.forEach { c ->
                    Text(
                        "${rotuloConsentimento(c.tipo)}: " + if (c.revogadoEm == null) "concedido em ${Formatadores.data(c.concedidoEm)}" else "revogado em ${Formatadores.data(c.revogadoEm)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                val saudeAtiva = dados.consentimentos.any { it.tipo == "DadosDeSaude" && it.revogadoEm == null }
                if (saudeAtiva) BotaoSecundario("Revogar consentimento de dados de saúde", { confirmaRevogar = true }, habilitado = !estado.ocupado)

                Titulo("Privacidade (LGPD)", Modifier.padding(top = 8.dp))
                BotaoSecundario("Baixar uma cópia dos meus dados", vm::exportar, habilitado = !estado.ocupado)

                Titulo("Trocar a senha", Modifier.padding(top = 8.dp))
                var atual by remember { mutableStateOf("") }
                var nova by remember { mutableStateOf("") }
                OutlinedTextField(atual, { atual = it }, label = { Text("Senha atual") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(nova, { nova = it }, label = { Text("Nova senha (8 ou mais, com letras e números)") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                BotaoSecundario("Alterar senha", { vm.alterarSenha(atual, nova); atual = ""; nova = "" }, habilitado = !estado.ocupado && atual.isNotBlank() && nova.isNotBlank())

                Titulo("Excluir a conta", Modifier.padding(top = 8.dp))
                Text("Seus dados pessoais e as receitas são apagados ou anonimizados. Pedidos pendentes são cancelados. Isso não pode ser desfeito.", style = MaterialTheme.typography.bodyMedium)
                BotaoSecundario("Excluir minha conta", { confirmaExclusao = true }, habilitado = !estado.ocupado)
            }
        }
    }

    if (confirmaRevogar) {
        AlertDialog(
            onDismissRequest = { confirmaRevogar = false },
            title = { Text("Revogar o consentimento?") },
            text = { Text("Sem o consentimento para dados de saúde você não poderá enviar receitas nem fazer novos pedidos, até consentir de novo em uma nova receita.") },
            confirmButton = { TextButton(onClick = { confirmaRevogar = false; vm.revogarSaude() }) { Text("Revogar") } },
            dismissButton = { TextButton(onClick = { confirmaRevogar = false }) { Text("Manter") } },
        )
    }
    if (confirmaExclusao) {
        AlertDialog(
            onDismissRequest = { confirmaExclusao = false },
            title = { Text("Excluir a conta?") },
            text = { Text("Esta ação não pode ser desfeita. Você será desconectado.") },
            confirmButton = { TextButton(onClick = { confirmaExclusao = false; vm.excluirConta() }) { Text("Excluir") } },
            dismissButton = { TextButton(onClick = { confirmaExclusao = false }) { Text("Cancelar") } },
        )
    }
}

private fun rotuloConsentimento(tipo: String) = when (tipo) {
    "TermosDeUso" -> "Termos de Uso"
    "DadosDeSaude" -> "Dados de saúde"
    else -> tipo
}
