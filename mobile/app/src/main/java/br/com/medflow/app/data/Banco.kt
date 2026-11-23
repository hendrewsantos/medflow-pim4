package br.com.medflow.app.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

const val LISTA_DISPONIVEIS = "disponiveis"
const val LISTA_MINHAS = "minhas"

@Entity(tableName = "entregas")
data class EntregaEntity(
    @PrimaryKey val pedidoId: Int,
    val codigo: String,
    val paciente: String,
    val endereco: String,
    val telefone: String?,
    val totalSaches: Int,
    val status: String,
    val tentativa: Int,
    val lista: String,
)

@Entity(tableName = "pedidos")
data class PedidoEntity(
    @PrimaryKey val id: Int,
    val paciente: String,
    val status: String,
    val criadoEm: String,
    val atualizadoEm: String,
    val totalItens: Int,
)

@Entity(tableName = "fila_acoes")
data class AcaoPendenteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pedidoId: Int,
    val tipo: String,
    val texto: String,
    val criadoEm: Long,
)

fun EntregaResumoDto.paraEntidade(lista: String) = EntregaEntity(
    pedidoId = pedidoId, codigo = codigo, paciente = paciente, endereco = endereco, telefone = telefone,
    totalSaches = totalSaches, status = status, tentativa = tentativa, lista = lista,
)

fun PedidoResumoDto.paraEntidade() = PedidoEntity(
    id = id, paciente = paciente, status = status, criadoEm = criadoEm, atualizadoEm = atualizadoEm, totalItens = totalItens,
)

@Dao
abstract class EntregaDao {
    @Query("SELECT * FROM entregas WHERE lista = :lista ORDER BY pedidoId")
    abstract fun observarLista(lista: String): Flow<List<EntregaEntity>>

    @Query("SELECT * FROM entregas WHERE pedidoId = :id")
    abstract fun observar(id: Int): Flow<EntregaEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun inserir(entrega: EntregaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun inserirVarias(entregas: List<EntregaEntity>)

    @Query("DELETE FROM entregas")
    abstract suspend fun limparTudo()

    @Transaction
    open suspend fun substituirTudo(entregas: List<EntregaEntity>) {
        limparTudo()
        inserirVarias(entregas)
    }
}

@Dao
abstract class PedidoDao {
    @Query("SELECT * FROM pedidos ORDER BY id DESC")
    abstract fun observarTodos(): Flow<List<PedidoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun inserirVarios(pedidos: List<PedidoEntity>)

    @Query("DELETE FROM pedidos")
    abstract suspend fun limparTudo()

    @Transaction
    open suspend fun substituirTudo(pedidos: List<PedidoEntity>) {
        limparTudo()
        inserirVarios(pedidos)
    }
}

@Dao
interface AcaoDao {
    @Insert
    suspend fun inserir(acao: AcaoPendenteEntity): Long

    @Query("SELECT * FROM fila_acoes ORDER BY id")
    suspend fun listar(): List<AcaoPendenteEntity>

    @Query("SELECT * FROM fila_acoes ORDER BY id")
    fun observarTodas(): Flow<List<AcaoPendenteEntity>>

    @Query("SELECT COUNT(*) FROM fila_acoes WHERE pedidoId = :pedidoId")
    suspend fun contarDoPedido(pedidoId: Int): Int

    @Query("SELECT COUNT(*) FROM fila_acoes")
    suspend fun contar(): Int

    @Query("DELETE FROM fila_acoes WHERE id = :id")
    suspend fun remover(id: Long)
}

@Database(
    entities = [EntregaEntity::class, PedidoEntity::class, AcaoPendenteEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class MedFlowDb : RoomDatabase() {
    abstract fun entregaDao(): EntregaDao
    abstract fun pedidoDao(): PedidoDao
    abstract fun acaoDao(): AcaoDao
}
