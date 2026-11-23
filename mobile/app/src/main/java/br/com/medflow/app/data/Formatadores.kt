package br.com.medflow.app.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object Formatadores {
    private val dataBr = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val dataHoraBr = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

    private val rotulos = mapOf(
        "AguardandoPagamento" to "Aguardando pagamento",
        "Pendente" to "Receita em análise",
        "EmSeparacao" to "Em separação",
        "AguardandoEntregador" to "Aguardando entregador",
        "EmRota" to "Em rota",
        "Entregue" to "Entregue",
        "Cancelado" to "Cancelado",
        "Recusado" to "Receita recusada",
    )

    /** "EmSeparacao" -> "Em separação" (mesmos rótulos do site); status desconhecido vira texto separado por espaços. */
    fun rotuloStatus(status: String): String {
        if (status.isBlank()) return "Sem informação"
        rotulos[status]?.let { return it }
        val separado = status.replace(Regex("(?<=[a-z])(?=[A-Z])"), " ").lowercase()
        return separado.replaceFirstChar { it.uppercase() }
    }

    /** "2026-12-20" ou "2026-12-20T10:00:00" -> "20/12/2026". */
    fun data(iso: String?): String {
        if (iso.isNullOrBlank()) return "-"
        return try {
            LocalDate.parse(iso.take(10)).format(dataBr)
        } catch (_: DateTimeParseException) {
            iso
        }
    }

    /**
     * Data e hora. Com fuso (Z/+hh:mm) converte para o fuso do aparelho;
     * sem fuso, exibe como veio da API.
     */
    fun dataHora(iso: String?): String {
        if (iso.isNullOrBlank()) return "-"
        return try {
            OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime().format(dataHoraBr)
        } catch (_: DateTimeParseException) {
            try {
                LocalDateTime.parse(iso).format(dataHoraBr)
            } catch (_: DateTimeParseException) {
                iso
            }
        }
    }

    /** 60.0 -> "R$ 60,00". */
    fun reais(valor: Double): String = "R$ " + String.format(java.util.Locale("pt", "BR"), "%,.2f", valor)

    /** Milissegundos que faltam até o instante informado (0 se já passou ou se o texto não for uma data). */
    fun milissegundosAte(iso: String?, agora: Long = System.currentTimeMillis()): Long {
        if (iso.isNullOrBlank()) return 0L
        return try {
            (OffsetDateTime.parse(iso).toInstant().toEpochMilli() - agora).coerceAtLeast(0L)
        } catch (_: DateTimeParseException) {
            0L
        }
    }

    /** 143000 -> "02:23". */
    fun contagem(milissegundos: Long): String {
        val total = (milissegundos.coerceAtLeast(0L) + 999L) / 1000L
        return "%02d:%02d".format(total / 60, total % 60)
    }

    /** Mantém só dígitos e "+" para o esquema tel:. */
    fun telefoneParaDiscagem(telefone: String): String = telefone.filter { it.isDigit() || it == '+' }
}

object Validacoes {
    fun recebedor(texto: String): String? =
        if (texto.isBlank()) "Informe o nome de quem recebeu a box." else null

    fun motivo(texto: String): String? =
        if (texto.isBlank()) "Descreva o motivo da ocorrência." else null
}

/** Status usados pelo app (nomes do contrato). */
object StatusPedido {
    const val AGUARDANDO_ENTREGADOR = "AguardandoEntregador"
    const val EM_ROTA = "EmRota"
    const val AGUARDANDO_PAGAMENTO = "AguardandoPagamento"
}
