package br.com.medflow.app.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Configuração JSON compartilhada. Enums e datas chegam como texto (ver CONTRATO_API.md). */
val ApiJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
}

object Perfis {
    const val ENTREGADOR = "Entregador"
    const val PACIENTE = "Paciente"
}

@Serializable
data class LoginRequest(val email: String, val senha: String)

@Serializable
data class UsuarioResumo(
    val id: Int,
    val nome: String,
    val email: String = "",
    val cpfMascarado: String = "",
    val perfil: String,
    val ativo: Boolean = true,
)

@Serializable
data class AuthResponse(val token: String, val expiraEm: String = "", val usuario: UsuarioResumo)

@Serializable
data class EntregaResumoDto(
    val pedidoId: Int,
    val codigo: String,
    val paciente: String,
    val endereco: String,
    val telefone: String? = null,
    val totalSaches: Int = 0,
    val status: String,
    val tentativa: Int = 0,
)

@Serializable
data class ConfirmarEntregaRequest(val recebedor: String)

@Serializable
data class OcorrenciaRequest(val motivo: String)

@Serializable
data class PedidoResumoDto(
    val id: Int,
    val paciente: String = "",
    val status: String,
    val criadoEm: String = "",
    val atualizadoEm: String = "",
    val totalItens: Int = 0,
    val valorTotal: Double = 0.0,
    val pagamentoPrazoEm: String? = null,
)

@Serializable
data class PedidoItemDto(
    val medicamentoId: Int = 0,
    val medicamento: String,
    val quantidade: Int = 0,
)

@Serializable
data class HistoricoDto(
    val de: String? = null,
    val para: String,
    val em: String = "",
    val observacao: String? = null,
)

@Serializable
data class PedidoDto(
    val id: Int,
    val status: String,
    val enderecoEntrega: String = "",
    val criadoEm: String = "",
    val atualizadoEm: String = "",
    val itens: List<PedidoItemDto> = emptyList(),
    val historico: List<HistoricoDto> = emptyList(),
    val valorMedicamentos: Double = 0.0,
    val valorEntrega: Double = 0.0,
    val pagamentoPrazoEm: String? = null,
    val pagoEm: String? = null,
)

@Serializable
data class ProximaRenovacaoDto(
    val temBox: Boolean = false,
    val dataEstimadaFimDaBox: String? = null,
    val diasRestantes: Int? = null,
    val receitaValidaAte: String? = null,
    val receitaVenceAntes: Boolean = false,
)

/** Corpo de erro ProblemDetails. */
@Serializable
data class ProblemaDto(val title: String? = null, val detail: String? = null, val status: Int? = null)
