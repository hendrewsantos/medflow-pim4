package br.com.medflow.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Sessao(val token: String, val usuarioId: Int, val nome: String, val perfil: String)

interface SessaoStore {
    val sessao: StateFlow<Sessao?>
    fun token(): String?
    fun salvar(sessao: Sessao)
    fun limpar()
}

/**
 * Guarda o token JWT em EncryptedSharedPreferences (chave mestra no Android Keystore).
 * O token nunca é gravado em texto claro.
 */
class SessaoStoreCriptografada(private val contexto: Context) : SessaoStore {

    private val prefs: SharedPreferences = criarPrefs()
    private val estado = MutableStateFlow(ler())

    override val sessao: StateFlow<Sessao?> = estado.asStateFlow()

    override fun token(): String? = estado.value?.token

    override fun salvar(sessao: Sessao) {
        prefs.edit()
            .putString(CHAVE_TOKEN, sessao.token)
            .putInt(CHAVE_ID, sessao.usuarioId)
            .putString(CHAVE_NOME, sessao.nome)
            .putString(CHAVE_PERFIL, sessao.perfil)
            .apply()
        estado.value = sessao
    }

    override fun limpar() {
        prefs.edit().clear().apply()
        estado.value = null
    }

    private fun ler(): Sessao? {
        val token = prefs.getString(CHAVE_TOKEN, null) ?: return null
        val perfil = prefs.getString(CHAVE_PERFIL, null) ?: return null
        return Sessao(token, prefs.getInt(CHAVE_ID, 0), prefs.getString(CHAVE_NOME, "") ?: "", perfil)
    }

    private fun criarPrefs(): SharedPreferences {
        fun abrir(): SharedPreferences {
            val chave = MasterKey.Builder(contexto)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            return EncryptedSharedPreferences.create(
                contexto,
                ARQUIVO,
                chave,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }
        return try {
            abrir()
        } catch (e: Exception) {
            // Keystore corrompido/restaurado: descarta o arquivo e o usuário entra de novo.
            contexto.deleteSharedPreferences(ARQUIVO)
            abrir()
        }
    }

    private companion object {
        const val ARQUIVO = "medflow_sessao"
        const val CHAVE_TOKEN = "token"
        const val CHAVE_ID = "usuario_id"
        const val CHAVE_NOME = "nome"
        const val CHAVE_PERFIL = "perfil"
    }
}
