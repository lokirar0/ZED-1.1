package com.zed.app.core.data.repository

import com.zed.app.core.data.local.BUILT_IN_CATEGORIES
import com.zed.app.core.data.local.CategoryDao
import com.zed.app.core.data.local.TransactionDao
import com.zed.app.core.data.local.TransactionEntity
import com.zed.app.core.domain.model.Transaction
import com.zed.app.core.domain.model.TransactionType
import com.zed.app.core.domain.repository.TransactionRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val dao: TransactionDao,
    private val categoryDao: CategoryDao
) : TransactionRepository {

    override fun observeTransactions(): Flow<List<Transaction>> =
        dao.observeTransactions().map { list -> list.map { it.toDomain() } }

    override suspend fun insert(transaction: Transaction): Long =
        dao.insert(
            TransactionEntity(
                id = transaction.id,
                type = transaction.type.name,
                amountMinor = transaction.amountMinor,
                categoryId = transaction.categoryId,
                note = transaction.note,
                dateMillis = transaction.dateMillis,
                creditId = transaction.creditId,
                yearMonth = transaction.yearMonth
            )
        )

    override suspend fun delete(id: Int) = dao.delete(id)

    override suspend fun syncCreditPayment(
        creditId: Int,
        creditTitle: String,
        amountMinor: Long,
        dateMillis: Long,
        yearMonth: String,
        paid: Boolean
    ) {
        if (paid) {
            // Служебная категория CREDIT: находим, при отсутствии сейдим сами
            var creditCat = categoryDao.findByKey("CREDIT")
            if (creditCat == null) {
                categoryDao.insertAll(BUILT_IN_CATEGORIES)
                creditCat = categoryDao.findByKey("CREDIT") ?: return
            }
            // Защита от дублей: одна авто-транзакция на кредит+месяц
            if (dao.findCreditPayment(creditId, yearMonth) == null) {
                dao.insert(
                    TransactionEntity(
                        type = TransactionType.EXPENSE.name,
                        amountMinor = amountMinor,
                        categoryId = creditCat.id,
                        note = creditTitle,
                        dateMillis = dateMillis,
                        creditId = creditId,
                        yearMonth = yearMonth
                    )
                )
            }
        } else {
            dao.deleteCreditPayment(creditId, yearMonth)
        }
    }

    private fun TransactionEntity.toDomain() = Transaction(
        id = id,
        type = runCatching { TransactionType.valueOf(type) }.getOrDefault(TransactionType.EXPENSE),
        amountMinor = amountMinor,
        categoryId = categoryId,
        note = note,
        dateMillis = dateMillis,
        creditId = creditId,
        yearMonth = yearMonth
    )
}
