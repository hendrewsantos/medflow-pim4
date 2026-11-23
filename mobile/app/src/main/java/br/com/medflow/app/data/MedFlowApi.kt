package br.com.medflow.app.data

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
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

    @POST("api/pedidos/{id}/cancelar")
    suspend fun cancelarPedido(@Path("id") id: Int): PedidoDto

    @POST("api/auth/cadastro")
    suspend fun cadastro(@Body corpo: CadastroRequest): AuthResponse

    @GET("api/medicamentos/catalogo")
    suspend fun catalogo(): List<MedicamentoCatalogoDto>

    @GET("api/medicamentos")
    suspend fun medicamentos(): List<MedicamentoDto>

    @GET("api/receitas/minhas")
    suspend fun minhasReceitas(): List<ReceitaDto>

    @Multipart
    @POST("api/receitas")
    suspend fun enviarReceita(@Part arquivo: MultipartBody.Part, @Part("consentimentoSaude") consentimento: RequestBody): ReceitaDto

    @POST("api/pedidos/simular")
    suspend fun simular(@Body itens: List<ItemBoxRequest>): SimulacaoDto

    @POST("api/pedidos")
    suspend fun confirmarPedido(@Body corpo: ConfirmarPedidoRequest): PedidoDto

    @GET("api/assinatura")
    suspend fun assinatura(): AssinaturaDto

    @POST("api/assinatura/ativar")
    suspend fun ativarAssinatura(): AssinaturaDto

    @POST("api/assinatura/cancelar")
    suspend fun cancelarAssinatura(): AssinaturaDto

    /** Só existe na demonstração (a API responde 404 fora dela). */
    @POST("api/assinatura/renovar-agora")
    suspend fun renovarAgora(): RenovacaoDto

    @GET("api/privacidade/meus-dados")
    suspend fun meusDados(): MeusDadosDto

    @GET("api/privacidade/meus-dados/exportar")
    suspend fun exportarMeusDados(): ResponseBody

    @PUT("api/privacidade/meus-dados")
    suspend fun atualizarMeusDados(@Body corpo: AtualizarMeusDadosRequest): MeusDadosDto

    @POST("api/privacidade/revogar")
    suspend fun revogarConsentimento(@Body corpo: RevogarConsentimentoRequest): MeusDadosDto

    @DELETE("api/privacidade/minha-conta")
    suspend fun excluirConta()

    @POST("api/auth/alterar-senha")
    suspend fun alterarSenha(@Body corpo: AlterarSenhaRequest)
}
