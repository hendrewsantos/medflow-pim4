package br.com.medflow.app.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import br.com.medflow.app.MedFlowApp
import br.com.medflow.app.data.AgendadorSync
import java.util.concurrent.TimeUnit

/** Reenvia a fila offline (confirmar entrega / ocorrência). Repete com espera crescente se ainda sem rede. */
class SyncWorker(contexto: Context, params: WorkerParameters) : CoroutineWorker(contexto, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as MedFlowApp).container
        val resumo = container.entregasRepo.sincronizarFila()
        return if (resumo.completo) Result.success() else Result.retry()
    }
}

class AgendadorWorkManager(private val contexto: Context) : AgendadorSync {
    override fun agendar() {
        val pedido = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(contexto)
            .enqueueUniqueWork(NOME_TRABALHO, ExistingWorkPolicy.APPEND_OR_REPLACE, pedido)
    }

    private companion object {
        const val NOME_TRABALHO = "medflow-sync-fila"
    }
}
