package com.zed.app.core.data.repository

import com.zed.app.core.data.local.BUILT_IN_CATEGORIES
import com.zed.app.core.data.local.CategoryDao
import com.zed.app.core.data.local.TransactionDao
import com.zed.app.core.data.local.TransactionEntity
import com.zed.app.core.domain.model.Transaction
import com.zed.app.core.domain.model.TransactionType
import com.zed.app.core.domain.repository.TransactionRepository
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
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
                yearMonth = transaction.yearMonth,
                recurring = transaction.recurring,
                sourceId = transaction.sourceId
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
            var creditCat = categoryDao.findByKey("CREDIT")
            if (creditCat == null) {
                categoryDao.insertAll(BUILT_IN_CATEGORIES)
                creditCat = categoryDao.findByKey("CREDIT") ?: return
            }
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

    // Повторения: для каждого шаблона создаём копии за месяцы после его месяца
    // по текущий включительно, если копии ещё нет (защита от дублей)
    override suspend fun materializeRecurring() {
        val all = dao.getAll()
        val templates = all.filter { it.recurring && it.sourceId == null }
        if (templates.isEmpty()) return

        val zone = ZoneId.systemDefault()
        val current = YearMonth.now()
        val toInsert = mutableListOf<TransactionEntity>()

        for (t in templates) {
            val start = YearMonth.from(Instant.ofEpochMilli(t.dateMillis).atZone(zone))
            val dayOfMonth = Instant.ofEpochMilli(t.dateMillis).atZone(zone).dayOfMonth
            var m = start.plusMonths(1)
            while (!m.isAfter(current)) {
                val ym = m.toString()
                val exists = all.any { it.sourceId == t.id && it.yearMonth == ym } ||
                    toInsert.any { it.sourceId == t.id && it.yearMonth == ym }
                if (!exists) {
                    val date = m.atDay(minOf(dayOfMonth, m.lengthOfMonth()))
                    toInsert += TransactionEntity(
                        type = t.type,
                        amountMinor = t.amountMinor,
                        categoryId = t.categoryId,
                        note = t.note,
                        dateMillis = date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli(),
                        creditId = null,
                        yearMonth = ym,
                        recurring = false,
                        sourceId = t.id
                    )
                }
                m = m.plusMonths(1)
            }
        }
        if (toInsert.isNotEmpty()) dao.insertAll(toInsert)
    }

    private fun TransactionEntity.toDomain() = Transaction(
        id = id,
        type = runCatching { TransactionType.valueOf(type) }.getOrDefault(TransactionType.EXPENSE),
        amountMinor = amountMinor,
        categoryId = categoryId,
        note = note,
        dateMillis = dateMillis,
        creditId = creditId,
        yearMonth = yearMonth,
        recurring = recurring,
        sourceId = sourceId
    )
}
