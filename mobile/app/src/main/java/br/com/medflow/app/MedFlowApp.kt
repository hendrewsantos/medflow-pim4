package br.com.medflow.app

import android.app.Application
import android.content.Context
import androidx.room.Room
import br.com.medflow.app.data.AuthRepository
import br.com.medflow.app.data.EntregasRepository
import br.com.medflow.app.data.MedFlowDb
import br.com.medflow.app.data.PedidosRepository
import br.com.medflow.app.data.SessaoStore
import br.com.medflow.app.data.SessaoStoreCriptografada
import br.com.medflow.app.data.criarApi
import br.com.medflow.app.work.AgendadorWorkManager

/** Injeção manual de dependências (suficiente para o escopo do projeto). */
class AppContainer(contexto: Context) {
    private val app = contexto.applicationContext

    val sessao: SessaoStore = SessaoStoreCriptografada(app)
    private val db: MedFlowDb = Room.databaseBuilder(app, MedFlowDb::class.java, "medflow.db")
        .fallbackToDestructiveMigration()
        .build()
    private val api = criarApi(BuildConfig.API_BASE_URL, sessao)

    val authRepo = AuthRepository(api, sessao, db)
    val entregasRepo = EntregasRepository(api, db, AgendadorWorkManager(app))
    val pedidosRepo = PedidosRepository(api, db)
}

class MedFlowApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
