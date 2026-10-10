package com.zed.app.core.domain.repository

import com.zed.app.core.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun observeTransactions(): Flow<List<Transaction>>
    suspend fun insert(transaction: Transaction): Long
    suspend fun delete(id: Int)

    // Синхронизация авто-списания платежа по кредиту
    suspend fun syncCreditPayment(
        creditId: Int,
        creditTitle: String,
        amountMinor: Long,
        dateMillis: Long,
        yearMonth: String,
        paid: Boolean
    )

    // Создаёт недостающие месячные копии recurring-шаблонов (по текущий месяц)
    suspend fun materializeRecurring()
}
