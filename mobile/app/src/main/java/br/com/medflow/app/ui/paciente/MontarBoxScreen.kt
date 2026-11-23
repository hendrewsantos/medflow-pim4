package br.com.medflow.app.ui.paciente

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.medflow.app.data.Formatadores
import br.com.medflow.app.data.Interacoes
import br.com.medflow.app.data.ItemBoxRequest
import br.com.medflow.app.data.MedicamentoDto
import br.com.medflow.app.data.Programas
import br.com.medflow.app.ui.BotaoPrincipal
import br.com.medflow.app.ui.BotaoSecundario
import br.com.medflow.app.ui.CartaoMensagem
import br.com.medflow.app.ui.Titulo

private val HORARIOS = listOf("06:00", "07:00", "08:00", "10:00", "12:00", "14:00", "18:00", "20:00", "22:00")
private const val DIAS_CICLO = 30
private val PASSOS = listOf("Receita", "Medicamentos e horários", "Revisão e confirmação")

/** Montar Box em três passos: receita, medicamentos com horários e dose, e revisão com valores e pagamento simulado. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MontarBoxScreen(vm: MontarBoxViewModel, onEnviarReceita: () -> Unit, onAbrirPedido: (Int) -> Unit) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    LaunchedEffect(estado.pedidoCriadoId) {
        estado.pedidoCriadoId?.let { onAbrirPedido(it); vm.pedidoAberto() }
    }
    val total = (estado.simulacao?.valorMedicamentos ?: 0.0) + (estado.simulacao?.taxaEntrega ?: 0.0)

    Scaffold(
        topBar = { TopAppBar(title = { Text("Montar Box") }) },
        bottomBar = {
            if (estado.passo == 2 && estado.itens.isNotEmpty() && estado.emPreparacaoId == null) {
                Surface(tonalElevation = 3.dp) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Sua Box · ${estado.itens.size} ${if (estado.itens.size == 1) "item" else "itens"}" +
                                if (estado.simulacao != null) " · ${Formatadores.reais(estado.simulacao!!.valorMedicamentos)} em medicamentos" else "",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        if (estado.simulacao?.podeConfirmar == false) {
                            Text("Algum item não tem saldo suficiente para os $DIAS_CICLO dias. Reduza a dose ou os horários, ou remova o item.", style = MaterialTheme.typography.bodySmall)
                        }
                        BotaoPrincipal("Revisar pedido", vm::revisar, habilitado = !estado.simulando && estado.simulacao?.podeConfirmar == true)
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when {
                estado.carregando -> item { Carregando("Carregando a montagem da Box") }
                estado.emPreparacaoId != null -> item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Você já tem uma Box em preparação", style = MaterialTheme.typography.titleMedium)
                            Text("O pedido ${estado.emPreparacaoId} está em andamento. Para evitar reserva em dobro, só é possível ter uma Box em preparação por vez.", style = MaterialTheme.typography.bodyMedium)
                            BotaoPrincipal("Acompanhar pedido", { onAbrirPedido(estado.emPreparacaoId!!) })
                        }
                    }
                }
                else -> {
                    item { Text("Passo ${estado.passo} de 3: ${PASSOS[estado.passo - 1]}. Um ciclo de $DIAS_CICLO dias, em sachês separados por horário.", style = MaterialTheme.typography.bodyMedium) }
                    estado.erro?.let { erro -> item { CartaoMensagem(erro, erro = true) } }
                    when (estado.passo) {
                        1 -> passoReceita(estado, vm, onEnviarReceita)
                        2 -> passoMedicamentos(estado, vm)
                        else -> passoRevisao(estado, vm, total)
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.passoReceita(estado: MontarBoxUiState, vm: MontarBoxViewModel, onEnviarReceita: () -> Unit) {
    item { Titulo("Qual receita sustenta este pedido?") }
    if (estado.receitas.isEmpty()) {
        item { CartaoMensagem("Você não tem receita válida. Envie uma para continuar.") }
    }
    items(estado.receitas, key = { it.id }) { r ->
        Card(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().selectable(selected = estado.receitaId == r.id, role = Role.RadioButton, onClick = { vm.escolherReceita(r.id) }).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = estado.receitaId == r.id, onClick = null)
                Column(Modifier.padding(start = 12.dp)) {
                    Text(r.nomeOriginal, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Enviada em ${Formatadores.data(r.enviadaEm)} · " + if (r.status == "Aprovada") "aprovada, válida até ${Formatadores.data(r.validaAte)}" else "em análise pela farmacêutica",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
    item { Text("Com receita aprovada e válida, a Box vai direto para separação. Receita em análise: a separação começa depois da aprovação.", style = MaterialTheme.typography.bodySmall) }
    item { BotaoSecundario("Enviar nova receita", onEnviarReceita) }
    item { BotaoPrincipal("Continuar", vm::continuar) }
}

@OptIn(ExperimentalLayoutApi::class)
private fun androidx.compose.foundation.lazy.LazyListScope.passoMedicamentos(estado: MontarBoxUiState, vm: MontarBoxViewModel) {
    item { PassoMedicamentosConteudo(estado, vm) }
}

@Composable
private fun PassoMedicamentosConteudo(estado: MontarBoxUiState, vm: MontarBoxViewModel) {
    var busca by rememberSaveable { mutableStateOf("") }
    var programa by rememberSaveable { mutableStateOf<String?>(null) }
    var editando by remember { mutableStateOf<MedicamentoDto?>(null) }
    var limite by remember { mutableStateOf(24) }
    val programas = Programas.todos.filter { p -> estado.medicamentos.any { Programas.de(it.principioAtivo) == p } }
    val filtrados = estado.medicamentos.filter {
        (programa == null || Programas.de(it.principioAtivo) == programa) &&
            (busca.isBlank() || "${it.nome} ${it.principioAtivo}".lowercase().contains(busca.trim().lowercase()))
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Titulo("Escolha os medicamentos")
        OutlinedTextField(
            value = busca, onValueChange = { busca = it; limite = 24 }, label = { Text("Buscar por nome ou princípio ativo") },
            singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), modifier = Modifier.fillMaxWidth(),
        )
        ChipsDeProgramas(programas, programa) { programa = it; limite = 24 }
        Text("${filtrados.size} ${if (filtrados.size == 1) "medicamento" else "medicamentos"}", style = MaterialTheme.typography.labelLarge)
        filtrados.take(limite).forEach { m ->
            val naBox = estado.itens.any { it.medicamentoId == m.id }
            val restam = m.disponivelParaCiclo
            Card(onClick = { editando = m }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("${m.nome} ${m.concentracao}", style = MaterialTheme.typography.titleSmall)
                    Text("${m.principioAtivo} · ${m.formaFarmaceutica}", style = MaterialTheme.typography.bodySmall)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (restam == 0) "Indisponível" else if (restam < 60) "Restam $restam un." else "Disponível", style = MaterialTheme.typography.labelMedium)
                        Text(if (naBox) "Na Box · editar" else "Adicionar", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        if (filtrados.size > limite) BotaoSecundario("Mostrar mais (${filtrados.size - limite})", { limite += 24 })
        Text("Medicamentos de controle especial (Portaria SVS/MS 344/1998) não aparecem aqui: eles não podem ser fracionados na Box.", style = MaterialTheme.typography.bodySmall)
    }
    editando?.let { m ->
        val atual = estado.itens.firstOrNull { it.medicamentoId == m.id }
        EdicaoDeItem(m, atual, aoSalvar = { vm.salvarItem(it); editando = null }, aoRemover = { vm.removerItem(m.id); editando = null }, aoFechar = { editando = null })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EdicaoDeItem(m: MedicamentoDto, atual: ItemBoxRequest?, aoSalvar: (ItemBoxRequest) -> Unit, aoRemover: () -> Unit, aoFechar: () -> Unit) {
    var horarios by remember { mutableStateOf(atual?.horarios?.toSet() ?: emptySet()) }
    var dose by remember { mutableStateOf(atual?.dosePorHorario ?: 1) }
    val totalCiclo = horarios.size * dose * DIAS_CICLO
    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text("${m.nome} ${m.concentracao}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${m.principioAtivo} · ${m.formaFarmaceutica}", style = MaterialTheme.typography.bodyMedium)
                Text("Horários (siga o que está na receita)", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HORARIOS.forEach { h ->
                        FilterChip(selected = h in horarios, onClick = { horarios = if (h in horarios) horarios - h else horarios + h }, label = { Text(h) })
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Unidades por dose", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = { dose = (dose - 1).coerceAtLeast(1) }) { Text("−") }
                    Text("$dose", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { dose = (dose + 1).coerceAtMost(4) }) { Text("+") }
                }
                Text(
                    "${horarios.size} horário(s) × $dose un. × $DIAS_CICLO dias = $totalCiclo un. no ciclo. Em estoque para este ciclo: ${m.disponivelParaCiclo} un.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = { TextButton(enabled = horarios.isNotEmpty(), onClick = { aoSalvar(ItemBoxRequest(m.id, horarios.sorted(), dose)) }) { Text(if (atual != null) "Atualizar" else "Adicionar à Box") } },
        dismissButton = {
            Row {
                if (atual != null) TextButton(onClick = aoRemover) { Text("Remover") }
                TextButton(onClick = aoFechar) { Text("Cancelar") }
            }
        },
    )
}

private fun androidx.compose.foundation.lazy.LazyListScope.passoRevisao(estado: MontarBoxUiState, vm: MontarBoxViewModel, total: Double) {
    val porId = estado.medicamentos.associateBy { it.id }
    val sim = estado.simulacao
    item { Titulo("Revise o pedido") }
    items(estado.itens, key = { it.medicamentoId }) { i ->
        val m = porId[i.medicamentoId]
        val necessario = sim?.itens?.firstOrNull { it.medicamentoId == i.medicamentoId }?.necessario ?: (i.horarios.size * i.dosePorHorario * DIAS_CICLO)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(if (m != null) "${m.nome} ${m.concentracao}" else "Medicamento ${i.medicamentoId}", style = MaterialTheme.typography.titleSmall)
                Text("${i.horarios.joinToString(" · ")} · ${i.dosePorHorario} un. por dose", style = MaterialTheme.typography.bodySmall)
                Text("Total de $DIAS_CICLO dias: $necessario un." + if (m != null && m.precoUnitario > 0) " · ${Formatadores.reais(necessario * m.precoUnitario)}" else "", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    val avisos = Interacoes.de(estado.itens.map { i -> porId[i.medicamentoId]?.let { "${it.nome} ${it.principioAtivo}" }.orEmpty() })
    if (avisos.isNotEmpty()) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(if (avisos.size == 1) "Um ponto de atenção nesta Box" else "${avisos.size} pontos de atenção nesta Box", style = MaterialTheme.typography.titleSmall)
                    Text("Isso não impede o pedido: a farmacêutica vai ver e, se for o caso, orienta você.", style = MaterialTheme.typography.bodySmall)
                    avisos.forEach { Text("${it.titulo}. ${it.texto}", style = MaterialTheme.typography.bodyMedium) }
                }
            }
        }
    }
    if (sim != null && sim.valorMedicamentos > 0) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Medicamentos: ${Formatadores.reais(sim.valorMedicamentos)}", style = MaterialTheme.typography.bodyLarge)
                Text("Entrega: ${Formatadores.reais(sim.taxaEntrega)}", style = MaterialTheme.typography.bodyLarge)
                Text("Total: ${Formatadores.reais(total)}", style = MaterialTheme.typography.titleMedium)
                Text("Ao confirmar, os medicamentos ficam reservados e você terá ${sim.prazoPagamentoMinutos} min para pagar (pagamento simulado, sem cobrança real).", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    item {
        OutlinedTextField(
            value = estado.endereco, onValueChange = vm::alterarEndereco, label = { Text("Endereço de entrega") },
            modifier = Modifier.fillMaxWidth(),
        )
    }
    item { BotaoPrincipal(if (estado.confirmando) "Confirmando..." else "Confirmar pedido", vm::confirmar, habilitado = !estado.confirmando) }
    item { BotaoSecundario("Voltar e ajustar", { vm.irPara(2) }) }
}
