package br.com.medflow.app.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException

enum class TipoErro { SEM_REDE, CONFLITO, NAO_AUTORIZADO, SEM_PERMISSAO, DADOS_INVALIDOS, SERVIDOR, OUTRO }

sealed interface Resultado<out T> {
    data class Ok<T>(val valor: T) : Resultado<T>
    data class Falha(val tipo: TipoErro, val mensagem: String) : Resultado<Nothing>
}

/** Executa uma chamada de rede e converte exceções em [Resultado.Falha] com mensagem em linguagem simples. */
suspend fun <T> chamar(bloco: suspend () -> T): Resultado<T> = try {
    Resultado.Ok(withContext(Dispatchers.IO) { bloco() })
} catch (e: CancellationException) {
    throw e
} catch (e: HttpException) {
    mapearHttp(e.code(), lerCorpoDeErro(e))
} catch (e: IOException) {
    Resultado.Falha(TipoErro.SEM_REDE, "Sem conexão com a internet ou com o servidor.")
} catch (e: SerializationException) {
    Resultado.Falha(TipoErro.OUTRO, "Resposta inesperada do servidor.")
}

private fun lerCorpoDeErro(e: HttpException): ProblemaDto? {
    val texto = try {
        e.response()?.errorBody()?.string()
    } catch (_: Exception) {
        null
    }
    if (texto.isNullOrBlank()) return null
    return try {
        ApiJson.decodeFromString(ProblemaDto.serializer(), texto)
    } catch (_: Exception) {
        null
    }
}

/** Separado para ser testável sem rede. */
fun mapearHttp(codigo: Int, problema: ProblemaDto?): Resultado.Falha {
    val texto = problema?.detail?.takeIf { it.isNotBlank() } ?: problema?.title?.takeIf { it.isNotBlank() }
    return when (codigo) {
        401 -> Resultado.Falha(TipoErro.NAO_AUTORIZADO, "Sua sessão expirou. Entre novamente.")
        403 -> Resultado.Falha(TipoErro.SEM_PERMISSAO, "Você não tem permissão para esta ação.")
        404 -> Resultado.Falha(TipoErro.OUTRO, "Não encontramos este registro.")
        409 -> Resultado.Falha(TipoErro.CONFLITO, texto ?: "Esta ação não é mais possível. Os dados foram atualizados.")
        400, 422 -> Resultado.Falha(TipoErro.DADOS_INVALIDOS, texto ?: "Os dados informados não foram aceitos.")
        423 -> Resultado.Falha(TipoErro.OUTRO, texto ?: "Conta bloqueada. Procure a farmácia.")
        429 -> Resultado.Falha(TipoErro.OUTRO, "Muitas tentativas. Aguarde alguns minutos e tente de novo.")
        in 500..599 -> Resultado.Falha(TipoErro.SERVIDOR, "O servidor está com problemas. Tente novamente em instantes.")
        else -> Resultado.Falha(TipoErro.OUTRO, texto ?: "Não foi possível concluir. Tente novamente.")
    }
}
