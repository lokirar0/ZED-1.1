package com.zed.app.core.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// Операция: доход или расход.
// categoryId ссылается на categories; creditId + yearMonth — только у авто-списаний.
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String,          // TransactionType.name
    val amountMinor: Long,     // сумма в копейках (без float-ошибок)
    val categoryId: Int,
    val note: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val creditId: Int? = null,
    val yearMonth: String? = null
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

    // Бэкап
    @Query("SELECT * FROM transactions")
    suspend fun getAll(): List<TransactionEntity>

    @Query("DELETE FROM transactions")
    suspend fun clear()

    @Insert
    suspend fun insertAll(list: List<TransactionEntity>)
}
