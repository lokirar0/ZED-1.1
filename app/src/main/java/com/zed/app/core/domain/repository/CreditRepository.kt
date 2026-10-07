package com.zed.app.core.domain.repository

import com.zed.app.core.domain.model.Credit
import com.zed.app.core.domain.model.CreditPayment
import kotlinx.coroutines.flow.Flow

interface CreditRepository {
    fun observeCredits(): Flow<List<Credit>>
    fun observePayments(): Flow<List<CreditPayment>>
    suspend fun getCredit(id: Int): Credit?
    suspend fun upsert(credit: Credit): Long
    suspend fun delete(id: Int)
    suspend fun setPaid(creditId: Int, yearMonth: String, paid: Boolean)
    // Разовые снимки для воркера уведомлений
    suspend fun getCreditsOnce(): List<Credit>
    suspend fun getPaymentsOnce(): List<CreditPayment>
}
