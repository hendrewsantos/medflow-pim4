package br.com.medflow.app.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface MedFlowApi {
    @POST("api/auth/login")
    suspend fun login(@Body corpo: LoginRequest): AuthResponse

    @GET("api/entregas/disponiveis")
    suspend fun entregasDisponiveis(): List<EntregaResumoDto>

    @GET("api/entregas/minhas")
    suspend fun entregasMinhas(): List<EntregaResumoDto>

    @POST("api/entregas/{pedidoId}/iniciar")
    suspend fun iniciar(@Path("pedidoId") pedidoId: Int): EntregaResumoDto

    @POST("api/entregas/{pedidoId}/confirmar")
    suspend fun confirmar(@Path("pedidoId") pedidoId: Int, @Body corpo: ConfirmarEntregaRequest): EntregaResumoDto

    @POST("api/entregas/{pedidoId}/ocorrencia")
    suspend fun ocorrencia(@Path("pedidoId") pedidoId: Int, @Body corpo: OcorrenciaRequest): EntregaResumoDto

    @GET("api/pedidos/meus")
    suspend fun meusPedidos(): List<PedidoResumoDto>

    @GET("api/pedidos/proxima-renovacao")
    suspend fun proximaRenovacao(): ProximaRenovacaoDto

    @GET("api/pedidos/{id}")
    suspend fun pedido(@Path("id") id: Int): PedidoDto

    /** Só existe na demonstração (a API responde 404 fora dela). */
    @POST("api/pedidos/{id}/pagamento/aprovar")
    suspend fun aprovarPagamento(@Path("id") id: Int): PedidoDto
}
