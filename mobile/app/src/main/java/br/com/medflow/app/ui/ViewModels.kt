package br.com.medflow.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.medflow.app.AppContainer
import br.com.medflow.app.data.AcaoPendenteEntity
import br.com.medflow.app.data.AutenticacaoRepositorio
import br.com.medflow.app.data.EntregaEntity
import br.com.medflow.app.data.EntregasRepository
import br.com.medflow.app.data.EnvioAcao
import br.com.medflow.app.data.PedidoDto
import br.com.medflow.app.data.PedidoEntity
import br.com.medflow.app.data.PedidosRepository
import br.com.medflow.app.data.ProximaRenovacaoDto
import br.com.medflow.app.data.Resultado
import br.com.medflow.app.data.TipoErro
import br.com.medflow.app.data.Validacoes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ───────────────────────── Login / sessão ─────────────────────────

data class LoginUiState(val carregando: Boolean = false, val erro: String? = null)

class LoginViewModel(private val auth: AutenticacaoRepositorio) : ViewModel() {
    private val _estado = MutableStateFlow(LoginUiState())
    val estado: StateFlow<LoginUiState> = _estado.asStateFlow()

    /** Em caso de sucesso a sessão é salva e a navegação reage a ela. */
    fun entrar(email: String, senha: String) {
        if (_estado.value.carregando) return
        if (email.isBlank() || senha.isBlank()) {
            _estado.update { it.copy(erro = "Preencha o e-mail e a senha.") }
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(carregando = true, erro = null) }
            when (val r = auth.entrar(email, senha)) {
                is Resultado.Ok -> _estado.update { it.copy(carregando = false) }
                is Resultado.Falha -> _estado.update { it.copy(carregando = false, erro = r.mensagem) }
            }
        }
    }

    companion object {
        fun factory(c: AppContainer) = viewModelFactory { initializer { LoginViewModel(c.authRepo) } }
    }
}

class SaidaViewModel(private val auth: AutenticacaoRepositorio) : ViewModel() {
    private val _aviso = MutableStateFlow<String?>(null)
    val aviso: StateFlow<String?> = _aviso.asStateFlow()

    fun sair() {
        viewModelScope.launch { _aviso.value = auth.sair() }
    }

    fun fecharAviso() {
        _aviso.value = null
    }

    companion object {
        fun factory(c: AppContainer) = viewModelFactory { initializer { SaidaViewModel(c.authRepo) } }
    }
}

// ───────────────────────── Entregador: listas ─────────────────────────

data class EntregasUiState(
    val disponiveis: List<EntregaEntity> = emptyList(),
    val minhas: List<EntregaEntity> = emptyList(),
    val pedidosPendentes: Set<Int> = emptySet(),
    val atualizando: Boolean = false,
    val mensagem: String? = null,
    val aviso: String? = null,
)

private data class Extra(val atualizando: Boolean = false, val mensagem: String? = null)

class EntregasViewModel(private val repo: EntregasRepository) : ViewModel() {
    private val extra = MutableStateFlow(Extra())

    val estado: StateFlow<EntregasUiState> = combine(
        repo.disponiveis,
        repo.minhas,
        repo.pendentes.map { lista -> lista.map(AcaoPendenteEntity::pedidoId).toSet() },
        extra,
        repo.aviso,
    ) { disp, min, pend, ex, aviso ->
        EntregasUiState(disp, min, pend, ex.atualizando, ex.mensagem, aviso)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EntregasUiState())

    init {
        viewModelScope.launch { repo.reagendarSeHouverPendentes() }
        atualizar()
    }

    fun atualizar() {
        if (extra.value.atualizando) return
        viewModelScope.launch {
            extra.value = Extra(atualizando = true)
            extra.value = when (val r = repo.atualizar()) {
                is Resultado.Ok -> Extra()
                is Resultado.Falha ->
                    if (r.tipo == TipoErro.SEM_REDE) {
                        Extra(mensagem = "Sem conexão. Mostrando os dados salvos neste aparelho.")
                    } else {
                        Extra(mensagem = r.mensagem)
                    }
            }
        }
    }

    fun fecharAviso() = repo.limparAviso()

    fun fecharMensagem() {
        extra.update { it.copy(mensagem = null) }
    }

    companion object {
        fun factory(c: AppContainer) = viewModelFactory { initializer { EntregasViewModel(c.entregasRepo) } }
    }
}

// ───────────────────────── Entregador: detalhe ─────────────────────────

data class DetalheUiState(
    val entrega: EntregaEntity? = null,
    val pendente: AcaoPendenteEntity? = null,
    val ocupado: Boolean = false,
    val mensagem: String? = null,
)

private data class ExtraDetalhe(val ocupado: Boolean = false, val mensagem: String? = null)

class DetalheEntregaViewModel(
    private val repo: EntregasRepository,
    private val pedidoId: Int,
) : ViewModel() {
    private val extra = MutableStateFlow(ExtraDetalhe())

    val estado: StateFlow<DetalheUiState> = combine(
        repo.observar(pedidoId),
        repo.pendentes.map { l -> l.firstOrNull { it.pedidoId == pedidoId } },
        extra,
    ) { entrega, pendente, ex ->
        DetalheUiState(entrega, pendente, ex.ocupado, ex.mensagem)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetalheUiState())

    fun iniciarRota() {
        if (extra.value.ocupado) return
        viewModelScope.launch {
            extra.value = ExtraDetalhe(ocupado = true)
            extra.value = when (val r = repo.iniciar(pedidoId)) {
                is Resultado.Ok -> ExtraDetalhe(mensagem = "Rota iniciada.")
                is Resultado.Falha ->
                    if (r.tipo == TipoErro.SEM_REDE) {
                        ExtraDetalhe(mensagem = "Sem internet. Para iniciar a rota é preciso estar conectado.")
                    } else {
                        ExtraDetalhe(mensagem = r.mensagem)
                    }
            }
        }
    }

    /** Retorna o erro de validação (o diálogo continua aberto) ou null se foi aceito. */
    fun confirmarEntrega(recebedor: String): String? {
        val erro = Validacoes.recebedor(recebedor)
        if (erro != null) return erro
        enviar(sucesso = "Entrega confirmada.") { repo.confirmar(pedidoId, recebedor) }
        return null
    }

    fun registrarOcorrencia(motivo: String): String? {
        val erro = Validacoes.motivo(motivo)
        if (erro != null) return erro
        enviar(sucesso = "Ocorrência registrada.") { repo.registrarOcorrencia(pedidoId, motivo) }
        return null
    }

    private fun enviar(sucesso: String, acao: suspend () -> EnvioAcao) {
        if (extra.value.ocupado) return
        viewModelScope.launch {
            extra.value = ExtraDetalhe(ocupado = true)
            extra.value = when (val r = acao()) {
                EnvioAcao.Enviada -> ExtraDetalhe(mensagem = sucesso)
                EnvioAcao.Enfileirada -> ExtraDetalhe(
                    mensagem = "Sem internet. A ação foi guardada neste aparelho e será enviada " +
                        "automaticamente quando houver conexão.",
                )
                is EnvioAcao.Falhou -> ExtraDetalhe(mensagem = r.mensagem)
            }
        }
    }

    fun fecharMensagem() {
        extra.update { it.copy(mensagem = null) }
    }

    companion object {
        fun factory(c: AppContainer, pedidoId: Int) =
            viewModelFactory { initializer { DetalheEntregaViewModel(c.entregasRepo, pedidoId) } }
    }
}

// ───────────────────────── Paciente ─────────────────────────

data class PedidosUiState(
    val pedidos: List<PedidoEntity> = emptyList(),
    val renovacao: ProximaRenovacaoDto? = null,
    val atualizando: Boolean = false,
    val mensagem: String? = null,
)

private data class ExtraPedidos(
    val renovacao: ProximaRenovacaoDto? = null,
    val atualizando: Boolean = false,
    val mensagem: String? = null,
)

class PedidosViewModel(private val repo: PedidosRepository) : ViewModel() {
    private val extra = MutableStateFlow(ExtraPedidos())

    val estado: StateFlow<PedidosUiState> = combine(repo.pedidos, extra) { pedidos, ex ->
        PedidosUiState(pedidos, ex.renovacao, ex.atualizando, ex.mensagem)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PedidosUiState())

    init {
        atualizar()
    }

    fun atualizar() {
        if (extra.value.atualizando) return
        viewModelScope.launch {
            extra.update { it.copy(atualizando = true, mensagem = null) }
            var mensagem: String? = null
            when (val r = repo.atualizar()) {
                is Resultado.Ok -> Unit
                is Resultado.Falha ->
                    mensagem = if (r.tipo == TipoErro.SEM_REDE) {
                        "Sem conexão. Mostrando os pedidos salvos neste aparelho."
                    } else {
                        r.mensagem
                    }
            }
            when (val r = repo.proximaRenovacao()) {
                is Resultado.Ok -> extra.update { it.copy(renovacao = r.valor) }
                is Resultado.Falha -> if (mensagem == null && r.tipo != TipoErro.SEM_REDE) mensagem = r.mensagem
            }
            extra.update { it.copy(atualizando = false, mensagem = mensagem) }
        }
    }

    companion object {
        fun factory(c: AppContainer) = viewModelFactory { initializer { PedidosViewModel(c.pedidosRepo) } }
    }
}

data class LinhaDoTempoUiState(
    val carregando: Boolean = true,
    val pedido: PedidoDto? = null,
    val erro: String? = null,
    val pagando: Boolean = false,
    val aviso: String? = null,
)

class LinhaDoTempoViewModel(private val repo: PedidosRepository, private val pedidoId: Int) : ViewModel() {
    private val _estado = MutableStateFlow(LinhaDoTempoUiState())
    val estado: StateFlow<LinhaDoTempoUiState> = _estado.asStateFlow()

    init {
        carregar()
    }

    fun carregar() {
        viewModelScope.launch {
            _estado.value = LinhaDoTempoUiState(carregando = true)
            _estado.value = when (val r = repo.detalhe(pedidoId)) {
                is Resultado.Ok -> LinhaDoTempoUiState(carregando = false, pedido = r.valor)
                is Resultado.Falha -> LinhaDoTempoUiState(
                    carregando = false,
                    erro = if (r.tipo == TipoErro.SEM_REDE) {
                        "Sem conexão. A linha do tempo precisa de internet."
                    } else {
                        r.mensagem
                    },
                )
            }
        }
    }

    /** Pagamento simulado (só na demonstração): representa o retorno do meio de pagamento. */
    fun aprovarPagamento() {
        viewModelScope.launch {
            _estado.value = _estado.value.copy(pagando = true, aviso = null)
            when (val r = repo.aprovarPagamento(pedidoId)) {
                is Resultado.Ok -> _estado.value = LinhaDoTempoUiState(carregando = false, pedido = r.valor)
                is Resultado.Falha -> _estado.value = _estado.value.copy(pagando = false, aviso = r.mensagem)
            }
        }
    }

    companion object {
        fun factory(c: AppContainer, pedidoId: Int) =
            viewModelFactory { initializer { LinhaDoTempoViewModel(c.pedidosRepo, pedidoId) } }
    }
}
