package br.com.medflow.app.data

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/** Anexa `Authorization: Bearer <jwt>`; se a API responder 401 com token, encerra a sessão local. */
class AuthInterceptor(private val sessao: SessaoStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = sessao.token()
        val requisicao = if (token != null) {
            chain.request().newBuilder().header("Authorization", "Bearer $token").build()
        } else {
            chain.request()
        }
        val resposta = chain.proceed(requisicao)
        if (resposta.code == 401 && token != null && !requisicao.url.encodedPath.endsWith("/auth/login")) {
            sessao.limpar()
        }
        return resposta
    }
}

fun criarApi(urlBase: String, sessao: SessaoStore): MedFlowApi {
    val cliente = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(sessao))
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()
    return Retrofit.Builder()
        .baseUrl(urlBase)
        .client(cliente)
        .addConverterFactory(ApiJson.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(MedFlowApi::class.java)
}
