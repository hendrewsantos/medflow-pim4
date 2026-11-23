package br.com.medflow.app.ui.publico

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.medflow.app.R
import br.com.medflow.app.data.Cpf
import br.com.medflow.app.ui.BotaoPrincipal
import br.com.medflow.app.ui.BotaoSecundario
import br.com.medflow.app.ui.BotaoTexto
import br.com.medflow.app.ui.CartaoMensagem
import br.com.medflow.app.ui.Titulo
import br.com.medflow.app.ui.entradaSuave
import br.com.medflow.app.ui.paciente.CadastroViewModel

private val PASSOS = listOf(
    "Envie a receita" to "Foto ou PDF, com o seu consentimento específico para o uso dos dados de saúde.",
    "Monte a sua Box" to "Medicamentos, horários e dose, conferidos contra o estoque na hora.",
    "A farmácia confere" to "A farmacêutica analisa a receita e a equipe separa os sachês pelo lote que vence primeiro.",
    "Receba em casa" to "O entregador leva até a porta. Perto do fim do ciclo, avisamos para renovar.",
)

private val SEGURANCA = listOf(
    "Consentimento antes da receita" to "O envio só acontece depois do seu aceite específico (LGPD). Você pode revogar quando quiser.",
    "Cada pessoa vê só o que precisa" to "O entregador não vê medicamentos, lotes nem receita.",
    "Seus dados, sob o seu comando" to "Consulte, corrija, baixe uma cópia ou exclua a conta em \"Meus dados\".",
    "Senha guardada com segurança" to "Nunca como texto. Cinco erros seguidos bloqueiam a conta por 15 minutos.",
)

/** Página inicial do MedFlow no app: o que é, como funciona e os caminhos para entrar ou criar conta. */
@Composable
fun InicioPublicoScreen(onEntrar: () -> Unit, onCriarConta: () -> Unit) {
    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Image(painterResource(R.drawable.mao_medflow), contentDescription = null, modifier = Modifier.width(64.dp))
                    Text("MedFlow Solutions", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.entradaSuave(0)) {
                    Text("A dose certa, no horário certo, na sua porta.", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
                    Text(
                        "O MedFlow organiza os seus medicamentos de uso contínuo em sachês separados por horário, confere a receita com uma farmacêutica e entrega em casa. Você não precisa lembrar de renovar: avisamos antes.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    BotaoPrincipal("Criar conta", onCriarConta)
                    BotaoSecundario("Já tenho conta: entrar", onEntrar)
                }
            }
            item { Titulo("Como funciona", Modifier.padding(top = 8.dp)) }
            items(PASSOS.size) { i ->
                val (titulo, texto) = PASSOS[i]
                Card(Modifier.fillMaxWidth().entradaSuave(i)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${i + 1}. $titulo", style = MaterialTheme.typography.titleMedium)
                        Text(texto, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            item { Titulo("Segurança e LGPD", Modifier.padding(top = 8.dp)) }
            items(SEGURANCA.size) { i ->
                val (titulo, texto) = SEGURANCA[i]
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(titulo, style = MaterialTheme.typography.titleMedium)
                        Text(texto, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            item {
                Text(
                    "Projeto acadêmico (UNIP). Ambiente de demonstração com dados fictícios; o pagamento é simulado. Existem outras empresas chamadas Medflow, em nichos diferentes: o nosso é entrega rastreável de medicamento de uso contínuo, em sachês por horário.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

/** Criar conta de paciente: nome, e-mail, CPF, senha, endereço e aceite dos termos. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CadastroScreen(vm: CadastroViewModel, onVoltar: () -> Unit) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    var nome by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var cpf by rememberSaveable { mutableStateOf("") }
    var telefone by rememberSaveable { mutableStateOf("") }
    var endereco by rememberSaveable { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var aceite by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Criar conta") }, navigationIcon = { BotaoTexto("Voltar", onVoltar, descricao = "Voltar") }) },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Conta de paciente. Equipe da farmácia e entregadores recebem acesso pela administração.", style = MaterialTheme.typography.bodyMedium)
            Campo("Nome completo", nome, { nome = it }, ImeAction.Next)
            Campo("E-mail", email, { email = it }, ImeAction.Next, KeyboardType.Email)
            Campo("CPF", cpf, { cpf = Cpf.somenteDigitos(it).take(11) }, ImeAction.Next, KeyboardType.Number, MascaraCpf)
            Campo("Telefone (opcional)", telefone, { telefone = it }, ImeAction.Next, KeyboardType.Phone)
            Campo("Endereço completo de entrega", endereco, { endereco = it }, ImeAction.Next)
            OutlinedTextField(
                value = senha,
                onValueChange = { senha = it },
                label = { Text("Senha (8 ou mais, com letras e números)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { vm.cadastrar(nome, email, cpf, senha, endereco, telefone, aceite) }),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = aceite, onCheckedChange = { aceite = it })
                Text("Li e aceito os Termos de Uso e a Política de Privacidade (disponíveis no site).", style = MaterialTheme.typography.bodyMedium)
            }
            estado.erro?.let { CartaoMensagem(it, erro = true) }
            BotaoPrincipal(
                texto = if (estado.enviando) "Criando conta..." else "Criar conta",
                onClick = { vm.cadastrar(nome, email, cpf, senha, endereco, telefone, aceite) },
                habilitado = !estado.enviando,
            )
            if (estado.enviando) CircularProgressIndicator(Modifier.semantics { contentDescription = "Criando a conta, aguarde" })
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Campo(rotulo: String, valor: String, aoMudar: (String) -> Unit, ime: ImeAction, teclado: KeyboardType = KeyboardType.Text, mascara: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None) {
    OutlinedTextField(
        visualTransformation = mascara,
        value = valor,
        onValueChange = aoMudar,
        label = { Text(rotulo) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = teclado, imeAction = ime),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Mostra 000.000.000-00 enquanto o valor guardado continua só com dígitos (o cursor não pula). */
private val MascaraCpf = androidx.compose.ui.text.input.VisualTransformation { texto ->
    val d = texto.text
    val saida = StringBuilder()
    d.forEachIndexed { i, c ->
        if (i == 3 || i == 6) saida.append('.')
        if (i == 9) saida.append('-')
        saida.append(c)
    }
    val mapa = object : androidx.compose.ui.text.input.OffsetMapping {
        override fun originalToTransformed(offset: Int): Int = offset + (if (offset > 3) 1 else 0) + (if (offset > 6) 1 else 0) + (if (offset > 9) 1 else 0)
        override fun transformedToOriginal(offset: Int): Int = (offset - (if (offset > 3) 1 else 0) - (if (offset > 7) 1 else 0) - (if (offset > 11) 1 else 0)).coerceIn(0, d.length)
    }
    androidx.compose.ui.text.input.TransformedText(androidx.compose.ui.text.AnnotatedString(saida.toString()), mapa)
}
