package br.com.medflow.app.data

import kotlinx.serialization.Serializable

@Serializable
data class CadastroRequest(
    val nome: String,
    val email: String,
    val cpf: String,
    val senha: String,
    val endereco: String,
    val telefone: String? = null,
    val aceiteTermos: Boolean,
)

/** Ficha de consulta (sem estoque). Itens fora da Box aparecem só como informação, com o motivo. */
@Serializable
data class MedicamentoCatalogoDto(
    val id: Int,
    val nome: String,
    val principioAtivo: String = "",
    val concentracao: String = "",
    val formaFarmaceutica: String = "",
    val tarja: String = "",
    val categoria: String = "Comum",
    val termolabil: Boolean = false,
    val precoUnitario: Double = 0.0,
    val permitidoNaBox: Boolean = false,
    val disponivel: Boolean = false,
    val motivoForaDaBox: String? = null,
)

/** Medicamento que o paciente pode pôr na Box (com saldo para o ciclo). */
@Serializable
data class MedicamentoDto(
    val id: Int,
    val nome: String,
    val principioAtivo: String = "",
    val concentracao: String = "",
    val formaFarmaceutica: String = "",
    val tarja: String = "",
    val disponivelParaCiclo: Int = 0,
    val precoUnitario: Double = 0.0,
)

@Serializable
data class ReceitaDto(
    val id: Int,
    val nomeOriginal: String = "",
    val enviadaEm: String = "",
    val status: String = "",
    val validaAte: String? = null,
    val motivoRecusa: String? = null,
    val prescritorNome: String? = null,
)

@Serializable
data class ItemBoxRequest(val medicamentoId: Int, val horarios: List<String>, val dosePorHorario: Int)

@Serializable
data class ConfirmarPedidoRequest(val receitaId: Int, val itens: List<ItemBoxRequest>, val enderecoEntrega: String)

@Serializable
data class DisponibilidadeItemDto(
    val medicamentoId: Int = 0,
    val medicamento: String = "",
    val necessario: Int = 0,
    val disponivel: Int = 0,
    val atende: Boolean = true,
)

@Serializable
data class SimulacaoDto(
    val podeConfirmar: Boolean = false,
    val itens: List<DisponibilidadeItemDto> = emptyList(),
    val valorMedicamentos: Double = 0.0,
    val taxaEntrega: Double = 0.0,
    val prazoPagamentoMinutos: Int = 0,
)

@Serializable
data class AssinaturaDto(
    val ativa: Boolean = false,
    val ativadaEm: String? = null,
    val proximaRenovacaoEm: String? = null,
    val diasAntecedencia: Int = 5,
    val ultimaRenovacaoEm: String? = null,
)

@Serializable
data class RenovacaoDto(val renovou: Boolean = false, val motivo: String? = null, val assinatura: AssinaturaDto = AssinaturaDto())

@Serializable
data class ConsentimentoDto(
    val tipo: String = "",
    val versaoTermo: String = "",
    val concedidoEm: String = "",
    val revogadoEm: String? = null,
)

@Serializable
data class MeusDadosDto(
    val id: Int = 0,
    val nome: String = "",
    val email: String = "",
    val cpf: String = "",
    val endereco: String? = null,
    val telefone: String? = null,
    val criadoEm: String = "",
    val consentimentos: List<ConsentimentoDto> = emptyList(),
)

@Serializable
data class AtualizarMeusDadosRequest(val nome: String, val email: String, val endereco: String? = null, val telefone: String? = null)

@Serializable
data class AlterarSenhaRequest(val senhaAtual: String, val novaSenha: String)

@Serializable
data class RevogarConsentimentoRequest(val tipo: String)
