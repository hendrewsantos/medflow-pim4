package br.com.medflow.app.ui.paciente

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.medflow.app.AppContainer
import br.com.medflow.app.data.AssinaturaDto
import br.com.medflow.app.data.AtualizarMeusDadosRequest
import br.com.medflow.app.data.AuthRepository
import br.com.medflow.app.data.CadastroRequest
import br.com.medflow.app.data.ConfirmarPedidoRequest
import br.com.medflow.app.data.Cpf
import br.com.medflow.app.data.ItemBoxRequest
import br.com.medflow.app.data.MedicamentoCatalogoDto
import br.com.medflow.app.data.MedicamentoDto
import br.com.medflow.app.data.MeusDadosDto
import br.com.medflow.app.data.PacienteRepository
import br.com.medflow.app.data.PedidoDto
import br.com.medflow.app.data.PedidosRepository
import br.com.medflow.app.data.ProximaRenovacaoDto
import br.com.medflow.app.data.ReceitaDto
import br.com.medflow.app.data.Resultado
import br.com.medflow.app.data.SenhaRegras
import br.com.medflow.app.data.SimulacaoDto
import br.com.medflow.app.data.TipoErro
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

private fun mensagemDe(f: Resultado.Falha): String =
    if (f.tipo == TipoErro.SEM_REDE) "Sem conexão com o servidor. Verifique o Wi-Fi e se a API está no ar." else f.mensagem

// ───────────────────────── Cadastro ─────────────────────────

data class CadastroUiState(val enviando: Boolean = false, val erro: String? = null)

class CadastroViewModel(private val auth: AuthRepository) : ViewModel() {
    private val _estado = MutableStateFlow(CadastroUiState())
    val estado: StateFlow<CadastroUiState> = _estado.asStateFlow()

    /** Validação local para responder rápido; a API valida de novo. */
    fun validar(nome: String, email: String, cpf: String, senha: String, endereco: String, aceite: Boolean): String? = when {
        nome.trim().length < 3 -> "Informe o seu nome completo."
        !email.contains('@') || email.trim().length < 6 -> "Informe um e-mail válido."
        !Cpf.valido(cpf) -> "O CPF informado não é válido."
        !SenhaRegras.forte(senha) -> "A senha deve ter ao menos 8 caracteres, com letras e números."
        endereco.trim().length < 10 -> "Informe o endereço completo de entrega."
        !aceite -> "Para criar a conta é preciso aceitar os Termos de Uso."
        else -> null
    }

    fun cadastrar(nome: String, email: String, cpf: String, senha: String, endereco: String, telefone: String, aceite: Boolean) {
        validar(nome, email, cpf, senha, endereco, aceite)?.let { erro -> _estado.value = CadastroUiState(erro = erro); return }
        viewModelScope.launch {
            _estado.value = CadastroUiState(enviando = true)
            val pedido = CadastroRequest(nome, email, Cpf.formatar(cpf), senha, endereco.trim(), telefone.ifBlank { null }, aceite)
            _estado.value = when (val r = auth.cadastrar(pedido)) {
                is Resultado.Ok -> CadastroUiState()   // a sessão aberta leva o app para a área do paciente
                is Resultado.Falha -> CadastroUiState(erro = mensagemDe(r))
            }
        }
    }

    companion object {
        fun factory(c: AppContainer) = viewModelFactory { initializer { CadastroViewModel(c.authRepo) } }
    }
}

// ───────────────────────── Medicamentos (consulta) ─────────────────────────

data class CatalogoUiState(val carregando: Boolean = true, val itens: List<MedicamentoCatalogoDto> = emptyList(), val erro: String? = null)

class CatalogoViewModel(private val repo: PacienteRepository) : ViewModel() {
    private val _estado = MutableStateFlow(CatalogoUiState())
    val estado: StateFlow<CatalogoUiState> = _estado.asStateFlow()

    init { carregar() }

    fun carregar() {
        viewModelScope.launch {
            _estado.value = CatalogoUiState(carregando = true)
            _estado.value = when (val r = repo.catalogo()) {
                is Resultado.Ok -> CatalogoUiState(carregando = false, itens = r.valor)
                is Resultado.Falha -> CatalogoUiState(carregando = false, erro = mensagemDe(r))
            }
        }
    }

    companion object {
        fun factory(c: AppContainer) = viewModelFactory { initializer { CatalogoViewModel(c.pacienteRepo) } }
    }
}

// ───────────────────────── Início do paciente ─────────────────────────

data class InicioPacienteUiState(
    val carregando: Boolean = true,
    val ativo: PedidoDto? = null,
    val ultimo: PedidoDto? = null,
    val renovacao: ProximaRenovacaoDto? = null,
    val erro: String? = null,
)

private val EM_ANDAMENTO = setOf("AguardandoPagamento", "Pendente", "EmSeparacao", "AguardandoEntregador", "EmRota")

class InicioPacienteViewModel(private val pacientes: PacienteRepository, private val pedidos: PedidosRepository) : ViewModel() {
    private val _estado = MutableStateFlow(InicioPacienteUiState())
    val estado: StateFlow<InicioPacienteUiState> = _estado.asStateFlow()

    init { carregar() }

    fun carregar() {
        viewModelScope.launch {
            _estado.update { it.copy(carregando = true, erro = null) }
            when (val lista = pacientes.meusPedidos()) {
                is Resultado.Falha -> _estado.value = InicioPacienteUiState(carregando = false, erro = mensagemDe(lista))
                is Resultado.Ok -> {
                    val ativoId = lista.valor.firstOrNull { it.status in EM_ANDAMENTO }?.id
                    val ultimoId = lista.valor.firstOrNull { it.status == "Entregue" }?.id
                    val ativo = ativoId?.let { (pedidos.detalhe(it) as? Resultado.Ok)?.valor }
                    val ultimo = ultimoId?.let { (pedidos.detalhe(it) as? Resultado.Ok)?.valor }
                    val renov = (pedidos.proximaRenovacao() as? Resultado.Ok)?.valor
                    _estado.value = InicioPacienteUiState(carregando = false, ativo = ativo, ultimo = ultimo, renovacao = renov)
                }
            }
        }
    }

    companion object {
        fun factory(c: AppContainer) = viewModelFactory { initializer { InicioPacienteViewModel(c.pacienteRepo, c.pedidosRepo) } }
    }
}

// ───────────────────────── Montar Box ─────────────────────────

data class MontarBoxUiState(
    val carregando: Boolean = true,
    val passo: Int = 1,
    val emPreparacaoId: Int? = null,
    val receitas: List<ReceitaDto> = emptyList(),
    val receitaId: Int? = null,
    val medicamentos: List<MedicamentoDto> = emptyList(),
    val itens: List<ItemBoxRequest> = emptyList(),
    val simulacao: SimulacaoDto? = null,
    val simulando: Boolean = false,
    val endereco: String = "",
    val erro: String? = null,
    val confirmando: Boolean = false,
    val pedidoCriadoId: Int? = null,
)

class MontarBoxViewModel(private val pacientes: PacienteRepository, private val pedidos: PedidosRepository) : ViewModel() {
    private val _estado = MutableStateFlow(MontarBoxUiState())
    val estado: StateFlow<MontarBoxUiState> = _estado.asStateFlow()
    private var jobSimulacao: Job? = null

    init { carregar() }

    private fun receitaUsavel(r: ReceitaDto): Boolean {
        val hoje = LocalDate.now().toString()
        return r.status == "Pendente" || (r.status == "Aprovada" && !r.validaAte.isNullOrBlank() && r.validaAte.take(10) >= hoje)
    }

    fun carregar() {
        viewModelScope.launch {
            _estado.update { it.copy(carregando = true, erro = null) }
            val receitas = pacientes.receitas()
            val medicamentos = pacientes.medicamentos()
            val dados = pacientes.meusDados()
            val lista = pacientes.meusPedidos()
            val falha = listOf(receitas, medicamentos).filterIsInstance<Resultado.Falha>().firstOrNull()
            if (falha != null) { _estado.update { it.copy(carregando = false, erro = mensagemDe(falha)) }; return@launch }
            val usaveis = ((receitas as Resultado.Ok).valor).filter(::receitaUsavel)
            val emPreparacao = (lista as? Resultado.Ok)?.valor?.firstOrNull { it.status in setOf("AguardandoPagamento", "Pendente", "EmSeparacao") }?.id
            _estado.update {
                it.copy(
                    carregando = false,
                    receitas = usaveis,
                    receitaId = it.receitaId ?: usaveis.firstOrNull { r -> r.status == "Aprovada" }?.id ?: usaveis.firstOrNull()?.id,
                    medicamentos = (medicamentos as Resultado.Ok).valor,
                    endereco = it.endereco.ifBlank { (dados as? Resultado.Ok)?.valor?.endereco.orEmpty() },
                    emPreparacaoId = emPreparacao,
                )
            }
        }
    }

    fun escolherReceita(id: Int) = _estado.update { it.copy(receitaId = id, erro = null) }
    fun alterarEndereco(texto: String) = _estado.update { it.copy(endereco = texto) }
    fun irPara(passo: Int) = _estado.update { it.copy(passo = passo, erro = null) }

    fun continuar() {
        if (_estado.value.receitaId == null) { _estado.update { it.copy(erro = "Escolha ou envie uma receita para continuar.") }; return }
        irPara(2)
    }

    fun salvarItem(item: ItemBoxRequest) {
        _estado.update { s -> s.copy(itens = s.itens.filter { it.medicamentoId != item.medicamentoId } + item) }
        simular()
    }

    fun removerItem(medicamentoId: Int) {
        _estado.update { s -> s.copy(itens = s.itens.filter { it.medicamentoId != medicamentoId }) }
        simular()
    }

    private fun simular() {
        jobSimulacao?.cancel()
        val itens = _estado.value.itens
        if (itens.isEmpty()) { _estado.update { it.copy(simulacao = null, simulando = false) }; return }
        jobSimulacao = viewModelScope.launch {
            _estado.update { it.copy(simulando = true) }
            when (val r = pacientes.simular(itens)) {
                is Resultado.Ok -> _estado.update { it.copy(simulacao = r.valor, simulando = false) }
                is Resultado.Falha -> _estado.update { it.copy(simulacao = null, simulando = false, erro = mensagemDe(r)) }
            }
        }
    }

    fun revisar() {
        val s = _estado.value
        if (s.itens.isEmpty()) return
        if (s.simulacao?.podeConfirmar != true) { _estado.update { it.copy(erro = "Algum item não tem saldo suficiente. Reduza a dose, os horários ou remova o item.") }; return }
        irPara(3)
    }

    fun confirmar() {
        val s = _estado.value
        val receita = s.receitaId ?: return
        if (s.endereco.trim().length < 10) { _estado.update { it.copy(erro = "Informe o endereço completo de entrega.") }; return }
        viewModelScope.launch {
            _estado.update { it.copy(confirmando = true, erro = null) }
            when (val r = pacientes.confirmar(ConfirmarPedidoRequest(receita, s.itens, s.endereco.trim()))) {
                is Resultado.Ok -> {
                    pedidos.atualizar()
                    _estado.update { it.copy(confirmando = false, pedidoCriadoId = r.valor.id) }
                }
                is Resultado.Falha -> _estado.update { it.copy(confirmando = false, erro = mensagemDe(r)) }
            }
        }
    }

    fun pedidoAberto() = _estado.update { it.copy(pedidoCriadoId = null) }

    companion object {
        fun factory(c: AppContainer) = viewModelFactory { initializer { MontarBoxViewModel(c.pacienteRepo, c.pedidosRepo) } }
    }
}

// ───────────────────────── Receitas ─────────────────────────

data class ReceitasUiState(
    val carregando: Boolean = true,
    val receitas: List<ReceitaDto> = emptyList(),
    val enviando: Boolean = false,
    val mensagem: String? = null,
    val erro: String? = null,
)

class ReceitasViewModel(private val repo: PacienteRepository) : ViewModel() {
    private val _estado = MutableStateFlow(ReceitasUiState())
    val estado: StateFlow<ReceitasUiState> = _estado.asStateFlow()

    init { carregar() }

    fun carregar() {
        viewModelScope.launch {
            _estado.update { it.copy(carregando = true) }
            when (val r = repo.receitas()) {
                is Resultado.Ok -> _estado.update { it.copy(carregando = false, receitas = r.valor, erro = null) }
                is Resultado.Falha -> _estado.update { it.copy(carregando = false, erro = mensagemDe(r)) }
            }
        }
    }

    fun enviar(nome: String, tipo: String, bytes: ByteArray) {
        if (bytes.isEmpty()) { _estado.update { it.copy(erro = "Não consegui ler o arquivo escolhido.") }; return }
        if (bytes.size > 10 * 1024 * 1024) { _estado.update { it.copy(erro = "O arquivo passa de 10 MB. Envie um menor.") }; return }
        viewModelScope.launch {
            _estado.update { it.copy(enviando = true, erro = null, mensagem = null) }
            when (val r = repo.enviarReceita(nome, tipo, bytes)) {
                is Resultado.Ok -> {
                    _estado.update { it.copy(enviando = false, mensagem = "Receita enviada. A farmacêutica vai analisar.") }
                    carregar()
                }
                is Resultado.Falha -> _estado.update { it.copy(enviando = false, erro = mensagemDe(r)) }
            }
        }
    }

    fun limparMensagem() = _estado.update { it.copy(mensagem = null, erro = null) }

    companion object {
        fun factory(c: AppContainer) = viewModelFactory { initializer { ReceitasViewModel(c.pacienteRepo) } }
    }
}

// ───────────────────────── Renovação automática ─────────────────────────

data class AssinaturaUiState(
    val carregando: Boolean = true,
    val assinatura: AssinaturaDto? = null,
    val ocupado: Boolean = false,
    val mensagem: String? = null,
    val erro: String? = null,
)

class AssinaturaViewModel(private val repo: PacienteRepository) : ViewModel() {
    private val _estado = MutableStateFlow(AssinaturaUiState())
    val estado: StateFlow<AssinaturaUiState> = _estado.asStateFlow()

    init { carregar() }

    fun carregar() {
        viewModelScope.launch {
            when (val r = repo.assinatura()) {
                is Resultado.Ok -> _estado.update { it.copy(carregando = false, assinatura = r.valor, erro = null) }
                is Resultado.Falha -> _estado.update { it.copy(carregando = false, erro = mensagemDe(r)) }
            }
        }
    }

    private fun agir(aviso: String, chamada: suspend () -> Resultado<AssinaturaDto>) {
        viewModelScope.launch {
            _estado.update { it.copy(ocupado = true, mensagem = null, erro = null) }
            when (val r = chamada()) {
                is Resultado.Ok -> _estado.update { it.copy(ocupado = false, assinatura = r.valor, mensagem = aviso) }
                is Resultado.Falha -> _estado.update { it.copy(ocupado = false, erro = mensagemDe(r)) }
            }
        }
    }

    fun ativar() = agir("Renovação automática ativada. Você pode cancelar quando quiser.") { repo.ativarAssinatura() }
    fun cancelar() = agir("Renovação automática cancelada.") { repo.cancelarAssinatura() }

    /** Só na demonstração: antecipa a renovação para mostrar o efeito. */
    fun renovarAgora() {
        viewModelScope.launch {
            _estado.update { it.copy(ocupado = true, mensagem = null, erro = null) }
            when (val r = repo.renovarAgora()) {
                is Resultado.Ok -> _estado.update {
                    it.copy(ocupado = false, assinatura = r.valor.assinatura,
                        mensagem = if (r.valor.renovou) "Renovação feita: a nova Box já foi pedida e paga (simulado)." else "Não foi possível renovar agora: ${r.valor.motivo}")
                }
                is Resultado.Falha -> _estado.update { it.copy(ocupado = false, erro = mensagemDe(r)) }
            }
        }
    }

    companion object {
        fun factory(c: AppContainer) = viewModelFactory { initializer { AssinaturaViewModel(c.pacienteRepo) } }
    }
}

// ───────────────────────── Meus dados (LGPD) ─────────────────────────

data class MeusDadosUiState(
    val carregando: Boolean = true,
    val dados: MeusDadosDto? = null,
    val ocupado: Boolean = false,
    val mensagem: String? = null,
    val erro: String? = null,
    val exportacao: String? = null,
    val contaExcluida: Boolean = false,
)

class MeusDadosViewModel(private val repo: PacienteRepository) : ViewModel() {
    private val _estado = MutableStateFlow(MeusDadosUiState())
    val estado: StateFlow<MeusDadosUiState> = _estado.asStateFlow()

    init { carregar() }

    fun carregar() {
        viewModelScope.launch {
            when (val r = repo.meusDados()) {
                is Resultado.Ok -> _estado.update { it.copy(carregando = false, dados = r.valor, erro = null) }
                is Resultado.Falha -> _estado.update { it.copy(carregando = false, erro = mensagemDe(r)) }
            }
        }
    }

    private fun <T> executar(aviso: String?, chamada: suspend () -> Resultado<T>, ok: (T) -> Unit = {}) {
        viewModelScope.launch {
            _estado.update { it.copy(ocupado = true, mensagem = null, erro = null) }
            when (val r = chamada()) {
                is Resultado.Ok -> { ok(r.valor); _estado.update { it.copy(ocupado = false, mensagem = aviso) } }
                is Resultado.Falha -> _estado.update { it.copy(ocupado = false, erro = mensagemDe(r)) }
            }
        }
    }

    fun salvar(nome: String, email: String, endereco: String, telefone: String) = executar(
        "Dados atualizados.",
        { repo.atualizarMeusDados(AtualizarMeusDadosRequest(nome.trim(), email.trim(), endereco.trim().ifBlank { null }, telefone.trim().ifBlank { null })) },
    ) { novo -> _estado.update { it.copy(dados = novo) } }

    fun revogarSaude() = executar("Consentimento de dados de saúde revogado. Novos pedidos e receitas ficam bloqueados.", { repo.revogarSaude() }) { novo ->
        _estado.update { it.copy(dados = novo) }
    }

    fun exportar() = executar(null, { repo.exportarMeusDados() }) { json -> _estado.update { it.copy(exportacao = json) } }
    fun exportacaoEntregue() = _estado.update { it.copy(exportacao = null) }

    fun alterarSenha(atual: String, nova: String) {
        if (!SenhaRegras.forte(nova)) { _estado.update { it.copy(erro = "A nova senha deve ter ao menos 8 caracteres, com letras e números.", mensagem = null) }; return }
        executar("Senha alterada.", { repo.alterarSenha(atual, nova) })
    }

    fun excluirConta() = executar(null, { repo.excluirConta() }) { _estado.update { it.copy(contaExcluida = true) } }

    companion object {
        fun factory(c: AppContainer) = viewModelFactory { initializer { MeusDadosViewModel(c.pacienteRepo) } }
    }
}
