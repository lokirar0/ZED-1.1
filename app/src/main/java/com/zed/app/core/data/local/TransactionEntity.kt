package com.zed.app.core.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// Операция: доход или расход.
// recurring = шаблон «повторять ежемесячно» (подписки, аренда, ЗП).
// sourceId = id шаблона у авто-созданных копий (копии не плодят дубли).
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String,          // TransactionType.name
    val amountMinor: Long,
    val categoryId: Int,
    val note: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val creditId: Int? = null,
    val yearMonth: String? = null,
    val recurring: Boolean = false,
    val sourceId: Int? = null
)

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC, id DESC")
    fun observeTransactions(): Flow<List<TransactionEntity>>

    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: Int)

    // Авто-списание платежа по кредиту
    @Query("SELECT * FROM transactions WHERE creditId = :creditId AND yearMonth = :yearMonth LIMIT 1")
    suspend fun findCreditPayment(creditId: Int, yearMonth: String): TransactionEntity?

    @Query("DELETE FROM transactions WHERE creditId = :creditId AND yearMonth = :yearMonth")
    suspend fun deleteCreditPayment(creditId: Int, yearMonth: String)

    // Бэкап + материализация повторений
    @Query("SELECT * FROM transactions")
    suspend fun getAll(): List<TransactionEntity>

    @Query("DELETE FROM transactions")
    suspend fun clear()

    @Insert
    suspend fun insertAll(list: List<TransactionEntity>)
}
