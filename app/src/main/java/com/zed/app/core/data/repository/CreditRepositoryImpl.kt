package com.zed.app.core.data.repository

import com.zed.app.core.data.local.CreditDao
import com.zed.app.core.data.local.CreditEntity
import com.zed.app.core.data.local.CreditPaymentDao
import com.zed.app.core.data.local.CreditPaymentEntity
import com.zed.app.core.domain.model.Credit
import com.zed.app.core.domain.model.CreditPayment
import com.zed.app.core.domain.repository.CreditRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class CreditRepositoryImpl @Inject constructor(
    private val creditDao: CreditDao,
    private val paymentDao: CreditPaymentDao
) : CreditRepository {

    override fun observeCredits(): Flow<List<Credit>> =
        creditDao.observeCredits().map { list -> list.map { it.toDomain() } }

    override fun observePayments(): Flow<List<CreditPayment>> =
        paymentDao.observePayments().map { list -> list.map { it.toDomain() } }

    override suspend fun getCredit(id: Int): Credit? = creditDao.getCredit(id)?.toDomain()

    override suspend fun upsert(credit: Credit): Long =
        creditDao.upsert(
            CreditEntity(
                id = credit.id,
                title = credit.title,
                monthlyPaymentMinor = credit.monthlyPaymentMinor,
                paymentDay = credit.paymentDay,
                note = credit.note
            )
        )

    override suspend fun delete(id: Int) = creditDao.delete(id)

    override suspend fun setPaid(creditId: Int, yearMonth: String, paid: Boolean) {
        val exists = paymentDao.find(creditId, yearMonth) != null
        if (paid && !exists) paymentDao.insert(creditId, yearMonth)
        if (!paid && exists) paymentDao.delete(creditId, yearMonth)
    }

    override suspend fun getCreditsOnce(): List<Credit> =
        creditDao.getCreditsOnce().map { it.toDomain() }

    override suspend fun getPaymentsOnce(): List<CreditPayment> =
        paymentDao.getPaymentsOnce().map { it.toDomain() }

    private fun CreditEntity.toDomain() = Credit(id, title, monthlyPaymentMinor, paymentDay, note)
    private fun CreditPaymentEntity.toDomain() = CreditPayment(id, creditId, yearMonth)
}
