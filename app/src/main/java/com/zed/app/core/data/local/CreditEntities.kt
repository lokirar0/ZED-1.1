package com.zed.app.core.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

// Кредит: название, ежемесячный платёж, день платежа (1–31)
@Entity(tableName = "credits")
data class CreditEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val monthlyPaymentMinor: Long,
    val paymentDay: Int,
    val note: String = ""
)

// Отметка «оплачено» за месяц (yearMonth = "2026-10")
@Entity(
    tableName = "credit_payments",
    foreignKeys = [ForeignKey(
        entity = CreditEntity::class,
        parentColumns = ["id"],
        childColumns = ["creditId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["creditId", "yearMonth"], unique = true)]
)
data class CreditPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val creditId: Int,
    val yearMonth: String
)

@Dao
interface CreditDao {
    @Query("SELECT * FROM credits ORDER BY title ASC")
    fun observeCredits(): Flow<List<CreditEntity>>

    @Query("SELECT * FROM credits ORDER BY title ASC")
    suspend fun getCreditsOnce(): List<CreditEntity>

    @Query("SELECT * FROM credits WHERE id = :id")
    suspend fun getCredit(id: Int): CreditEntity?

    @Upsert
    suspend fun upsert(credit: CreditEntity): Long

    @Query("DELETE FROM credits WHERE id = :id")
    suspend fun delete(id: Int)

    // Бэкап
    @Query("SELECT * FROM credits")
    suspend fun getAll(): List<CreditEntity>

    @Query("DELETE FROM credits")
    suspend fun clear()

    @Insert
    suspend fun insertAll(list: List<CreditEntity>)
}

@Dao
interface CreditPaymentDao {
    @Query("SELECT * FROM credit_payments")
    fun observePayments(): Flow<List<CreditPaymentEntity>>

    @Query("SELECT * FROM credit_payments")
    suspend fun getPaymentsOnce(): List<CreditPaymentEntity>

    @Query("SELECT * FROM credit_payments WHERE creditId = :creditId AND yearMonth = :yearMonth LIMIT 1")
    suspend fun find(creditId: Int, yearMonth: String): CreditPaymentEntity?

    @Query("INSERT INTO credit_payments (creditId, yearMonth) VALUES (:creditId, :yearMonth)")
    suspend fun insert(creditId: Int, yearMonth: String)

    @Query("DELETE FROM credit_payments WHERE creditId = :creditId AND yearMonth = :yearMonth")
    suspend fun delete(creditId: Int, yearMonth: String)

    // Бэкап
    @Query("DELETE FROM credit_payments")
    suspend fun clear()

    @Insert
    suspend fun insertAll(list: List<CreditPaymentEntity>)
}
