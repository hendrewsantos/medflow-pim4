package br.com.medflow.app.ui.paciente

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.medflow.app.BuildConfig
import br.com.medflow.app.data.Formatadores
import br.com.medflow.app.data.MedicamentoCatalogoDto
import br.com.medflow.app.data.PedidoDto
import br.com.medflow.app.data.Programas
import br.com.medflow.app.ui.BotaoPrincipal
import br.com.medflow.app.ui.BotaoSecundario
import br.com.medflow.app.ui.CartaoMensagem
import br.com.medflow.app.ui.Titulo
import br.com.medflow.app.ui.entradaSuave
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// ───────────────────────── Peças comuns ─────────────────────────

@Composable
fun Carregando(descricao: String = "Carregando") {
    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.semantics { contentDescription = descricao })
    }
}

@Composable
fun ChipsDeProgramas(programas: List<String>, escolhido: String?, onEscolher: (String?) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item { FilterChip(selected = escolhido == null, onClick = { onEscolher(null) }, label = { Text("Todos") }) }
        items(programas) { p -> FilterChip(selected = escolhido == p, onClick = { onEscolher(if (escolhido == p) null else p) }, label = { Text(p) }) }
    }
}

fun rotuloTarja(tarja: String): String = when (tarja) {
    "Livre" -> "Sem tarja"
    "" -> ""
    else -> "Tarja ${tarja.lowercase()}"
}

// ───────────────────────── Início ─────────────────────────

/** Início do paciente: atalhos do dia, Box em andamento, fita de sachês e renovação automática. */
@Composable
fun InicioPacienteScreen(
    vm: InicioPacienteViewModel,
    assinaturaVm: AssinaturaViewModel,
    nome: String,
    onIrPara: (String) -> Unit,
    onAbrirPedido: (Int) -> Unit,
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Text("Olá, $nome", style = MaterialTheme.typography.titleMedium) }
            item { Titulo("Como podemos cuidar de você hoje?") }
            item {
                Column(Modifier.entradaSuave(0), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Atalho("Montar minha Box", "Escolha medicamentos e horários", Modifier.weight(1f)) { onIrPara("box") }
                        Atalho("Enviar receita", "PDF ou foto, direto do celular", Modifier.weight(1f)) { onIrPara("receitas") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Atalho("Medicamentos", "Consulte tudo o que oferecemos", Modifier.weight(1f)) { onIrPara("medicamentos") }
                        Atalho("Acompanhar entrega", "Histórico dos seus pedidos", Modifier.weight(1f)) { onIrPara("pedidos") }
                    }
                }
            }
            if (estado.carregando) item { Carregando("Carregando o seu início") }
            estado.erro?.let { erro -> item { CartaoMensagem(erro, erro = true); BotaoSecundario("Tentar de novo", vm::carregar) } }

            estado.ativo?.let { ativo ->
                item { CartaoBoxAtiva(ativo) { onAbrirPedido(ativo.id) } }
            } ?: estado.ultimo?.let { ultimo ->
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Nenhuma Box em preparação", style = MaterialTheme.typography.titleMedium)
                            estado.renovacao?.takeIf { it.temBox }?.let {
                                Text("Sua Box atual dura até ${Formatadores.data(it.dataEstimadaFimDaBox)} (${it.diasRestantes ?: 0} dias).", style = MaterialTheme.typography.bodyMedium)
                            }
                            BotaoPrincipal("Montar a próxima Box", { onIrPara("box") })
                        }
                    }
                }
            }
            if (estado.ativo == null && estado.ultimo == null && !estado.carregando && estado.erro == null) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Comece pela sua receita", style = MaterialTheme.typography.titleMedium)
                            Text("Envie a receita, escolha medicamentos e horários e receba os sachês prontos em casa.", style = MaterialTheme.typography.bodyMedium)
                            BotaoPrincipal("Montar minha primeira Box", { onIrPara("box") })
                        }
                    }
                }
            }
            val base = estado.ativo ?: estado.ultimo
            if (base != null) {
                item { Titulo("Seus horários", Modifier.padding(top = 4.dp)) }
                item { FitaDeSaches(base) }
            }
            item { AssinaturaCard(assinaturaVm, aoMudar = vm::carregar) }
        }
    }
}

@Composable
private fun Atalho(titulo: String, subtitulo: String, modifier: Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier.semantics { contentDescription = "$titulo. $subtitulo" }) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleSmall)
            Text(subtitulo, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun CartaoBoxAtiva(p: PedidoDto, onAbrir: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Box em andamento · pedido ${p.id}", style = MaterialTheme.typography.titleMedium)
            Text(Formatadores.rotuloStatus(p.status), style = MaterialTheme.typography.bodyLarge)
            if (p.status == "AguardandoPagamento") {
                Text("Falta pagar ${Formatadores.reais(p.valorMedicamentos + p.valorEntrega)}. Abra o pedido para ver o prazo.", style = MaterialTheme.typography.bodyMedium)
            }
            if (p.status == "AguardandoPagamento") BotaoPrincipal("Pagar agora", onAbrir) else BotaoSecundario("Ver linha do tempo", onAbrir)
        }
    }
}

/** A Box como uma fita de sachês, um por horário; a próxima dose vem primeiro e fica destacada. */
@Composable
private fun FitaDeSaches(p: PedidoDto) {
    val agora = LocalTime.now().toString().take(5)
    val porHora = linkedMapOf<String, MutableList<Pair<String, Int>>>()
    p.itens.forEach { i -> i.horarios.forEach { h -> porHora.getOrPut(h) { mutableListOf() }.add(i.medicamento to i.dosePorHorario) } }
    val horas = porHora.keys.sorted()
    val ordem = horas.filter { it >= agora } + horas.filter { it < agora }
    val hoje = LocalDate.now()
    val formato = DateTimeFormatter.ofPattern("EEE, dd/MM", Locale("pt", "BR"))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Hoje é ${hoje.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("pt", "BR")))}." + (ordem.firstOrNull()?.let { " Próxima dose: $it." } ?: ""),
            style = MaterialTheme.typography.bodyMedium,
        )
        if (ordem.isEmpty()) Text("Os horários aparecem aqui assim que a Box tiver itens com horário.", style = MaterialTheme.typography.bodyMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(ordem.size) { i ->
                val hora = ordem[i]
                val itens = porHora.getValue(hora)
                val proxima = i == 0
                val dia = (if (hora < agora) hoje.plusDays(1) else hoje).format(formato)
                val unidades = itens.sumOf { it.second }
                Card(
                    modifier = Modifier
                        .width(190.dp)
                        .entradaSuave(i)
                        .semantics { contentDescription = "Sachê das $hora: ${itens.joinToString { it.first }}" + if (proxima) ". Próxima dose" else "" },
                    border = if (proxima) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(periodoDoDia(hora), style = MaterialTheme.typography.labelMedium)
                            Text(dia, style = MaterialTheme.typography.labelMedium)
                        }
                        Text(hora, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                        itens.forEach { (nome, dose) -> Text("$nome · $dose un.", style = MaterialTheme.typography.bodySmall) }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("$unidades ${if (unidades == 1) "unidade" else "unidades"}", style = MaterialTheme.typography.labelSmall)
                            if (proxima) Text("Próxima dose", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

private fun periodoDoDia(hora: String): String {
    val h = hora.take(2).toIntOrNull() ?: 0
    return if (h < 12) "Manhã" else if (h < 18) "Tarde" else "Noite"
}

// ───────────────────────── Renovação automática ─────────────────────────

@Composable
fun AssinaturaCard(vm: AssinaturaViewModel, aoMudar: () -> Unit = {}) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val a = estado.assinatura
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Titulo("Renovação automática")
            when {
                estado.carregando -> Carregando("Carregando a renovação automática")
                a == null -> Text("Não foi possível carregar a renovação automática agora.", style = MaterialTheme.typography.bodyMedium)
                a.ativa -> {
                    val renovou = a.ultimaRenovacaoEm != null && a.proximaRenovacaoEm != null && a.ultimaRenovacaoEm >= a.proximaRenovacaoEm
                    Text(
                        when {
                            renovou -> "A Box atual foi pedida automaticamente em ${Formatadores.data(a.ultimaRenovacaoEm)}. A próxima será pedida sozinha depois da entrega desta."
                            a.proximaRenovacaoEm != null && Formatadores.milissegundosAte(a.proximaRenovacaoEm) > 0L ->
                                "A próxima Box será pedida sozinha em ${Formatadores.data(a.proximaRenovacaoEm)}, com os mesmos medicamentos e horários."
                            a.proximaRenovacaoEm != null -> "A próxima Box já está na hora de ser pedida e será renovada automaticamente assim que possível."
                            else -> "Assim que você receber a primeira Box, a renovação passa a valer."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    BotaoSecundario("Cancelar renovação automática", { vm.cancelar(); aoMudar() }, habilitado = !estado.ocupado)
                    if (BuildConfig.DEBUG) BotaoSecundario("Simular renovação agora (demonstração)", { vm.renovarAgora(); aoMudar() }, habilitado = !estado.ocupado)
                }
                else -> {
                    Text(
                        "Ative para não precisar lembrar de renovar: 5 dias antes de a Box acabar, o sistema pede a próxima, com os mesmos medicamentos e horários, e faz a cobrança sozinho (simulada, sem valor real). Você cancela quando quiser.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    BotaoPrincipal("Ativar renovação automática", { vm.ativar(); aoMudar() }, habilitado = !estado.ocupado)
                }
            }
            estado.mensagem?.let { CartaoMensagem(it) }
            estado.erro?.let { CartaoMensagem(it, erro = true) }
        }
    }
}

// ───────────────────────── Medicamentos (consulta) ─────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicamentosScreen(vm: CatalogoViewModel, onMontarBox: () -> Unit) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    var busca by rememberSaveable { mutableStateOf("") }
    var programa by rememberSaveable { mutableStateOf<String?>(null) }
    var soBox by rememberSaveable { mutableStateOf(false) }
    var limite by remember { mutableIntStateOf(30) }
    var aberto by remember { mutableStateOf<MedicamentoCatalogoDto?>(null) }

    val programas = Programas.todos.filter { p -> estado.itens.any { Programas.de(it.principioAtivo) == p } }
    val filtrados = estado.itens.filter {
        (programa == null || Programas.de(it.principioAtivo) == programa) &&
            (!soBox || it.permitidoNaBox) &&
            (busca.isBlank() || "${it.nome} ${it.principioAtivo} ${it.formaFarmaceutica}".lowercase().contains(busca.trim().lowercase()))
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Medicamentos") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text("Tudo o que o MedFlow oferece, com as especificações. Para pedir é preciso uma receita aprovada; a consulta é livre.", style = MaterialTheme.typography.bodyMedium)
            }
            item {
                OutlinedTextField(
                    value = busca, onValueChange = { busca = it; limite = 30 }, label = { Text("Buscar por nome, princípio ativo ou forma") },
                    singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), modifier = Modifier.fillMaxWidth(),
                )
            }
            item { ChipsDeProgramas(programas, programa) { programa = it; limite = 30 } }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Só os que entram na Box", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = soBox, onCheckedChange = { soBox = it; limite = 30 })
                }
            }
            if (estado.carregando) item { Carregando("Carregando os medicamentos") }
            estado.erro?.let { erro -> item { CartaoMensagem(erro, erro = true); BotaoSecundario("Tentar de novo", vm::carregar) } }
            if (!estado.carregando && estado.erro == null) {
                item { Text("${filtrados.size} ${if (filtrados.size == 1) "medicamento" else "medicamentos"}", style = MaterialTheme.typography.labelLarge) }
                items(filtrados.take(limite), key = { it.id }) { m ->
                    Card(onClick = { aberto = m }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("${m.nome} ${m.concentracao}", style = MaterialTheme.typography.titleSmall)
                            Text("${m.principioAtivo} · ${m.formaFarmaceutica}", style = MaterialTheme.typography.bodySmall)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    if (!m.permitidoNaBox) "Fora da Box" else if (m.disponivel) "Disponível" else "Indisponível",
                                    style = MaterialTheme.typography.labelMedium,
                                )
                                Text(if (m.permitidoNaBox) "${Formatadores.reais(m.precoUnitario)} por unidade" else "Ver ficha", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
                if (filtrados.isEmpty()) item { Text("Nenhum medicamento encontrado. Tente outro nome ou outro programa.", style = MaterialTheme.typography.bodyLarge) }
                if (filtrados.size > limite) item { BotaoSecundario("Mostrar mais (${filtrados.size - limite})", { limite += 30 }) }
                item {
                    Text("Os preços são fictícios, só para a demonstração do pagamento simulado. A farmacêutica responsável confere cada receita antes da separação.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }

    aberto?.let { m ->
        AlertDialog(
            onDismissRequest = { aberto = null },
            title = { Text("${m.nome} ${m.concentracao}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Linha("Princípio ativo", m.principioAtivo)
                    Linha("Forma", m.formaFarmaceutica)
                    Linha("Venda", rotuloTarja(m.tarja))
                    Linha("Programa de cuidado", Programas.de(m.principioAtivo))
                    Linha("Categoria", m.categoria + if (m.termolabil) " · exige cadeia fria" else "")
                    if (m.permitidoNaBox) Linha("Preço (fictício)", "${Formatadores.reais(m.precoUnitario)} por unidade")
                    Linha(
                        "Na Box",
                        if (!m.permitidoNaBox) m.motivoForaDaBox.orEmpty()
                        else if (m.disponivel) "Pode entrar e há estoque para um ciclo de 30 dias." else "Pode entrar, mas está sem estoque para um ciclo de 30 dias.",
                    )
                }
            },
            confirmButton = { if (m.permitidoNaBox) androidx.compose.material3.TextButton(onClick = { aberto = null; onMontarBox() }) { Text("Montar Box") } },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { aberto = null }) { Text("Fechar") } },
        )
    }
}

@Composable
private fun Linha(rotulo: String, valor: String) {
    Column {
        Text(rotulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.bodyMedium)
    }
}

// ───────────────────────── Mais ─────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaisScreen(nome: String, onIrPara: (String) -> Unit, onSair: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Mais") }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(nome, style = MaterialTheme.typography.titleMedium)
            Opcao("Minhas receitas", "Veja o status e envie uma nova receita") { onIrPara("receitas") }
            Opcao("Renovação automática", "Peça a próxima Box sozinha, antes de acabar") { onIrPara("assinatura") }
            Opcao("Meus dados e privacidade", "Corrigir dados, baixar uma cópia, revogar consentimento, trocar a senha ou excluir a conta") { onIrPara("dados") }
            BotaoSecundario("Sair da conta", onSair)
        }
    }
}

@Composable
private fun Opcao(titulo: String, descricao: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().semantics { contentDescription = "$titulo. $descricao" }) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            Text(descricao, style = MaterialTheme.typography.bodySmall)
        }
    }
}
