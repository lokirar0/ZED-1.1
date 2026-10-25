package com.zed.app.feature.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zed.app.core.data.local.CategoryEntity
import com.zed.app.core.domain.model.Transaction
import com.zed.app.core.domain.model.TransactionType
import com.zed.app.core.domain.repository.CategoryRepository
import com.zed.app.core.domain.repository.TransactionRepository
import com.zed.app.core.domain.util.MoneyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Столбик графика расходов за месяц
data class MonthBarUi(
    val label: String,
    val fraction: Float,
    val sumText: String
)

// Категория с суммой расходов за текущий месяц
data class CategorySumUi(
    val id: Int,
    val name: String,
    val sumText: String,
    val fraction: Float
)

// Строка транзакции в списке
data class TransactionUi(
    val id: Int,
    val title: String,
    val subtitle: String,
    val sumText: String,
    val isExpense: Boolean
)

data class FinanceUiState(
    val balanceText: String = "0",
    val incomeText: String = "0",
    val expenseText: String = "0",
    val forecastText: String = "0",
    val monthBars: List<MonthBarUi> = emptyList(),
    val categorySums: List<CategorySumUi> = emptyList(),
    val transactions: List<TransactionUi> = emptyList(),
    val quickCategories: List<CategorySumUi> = emptyList(),
    val loaded: Boolean = false
)

@HiltViewModel
class FinanceViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    // Свежий снимок транзакций для undo
    private var transactionsNow: List<Transaction> = emptyList()
    private var lastTransaction: Transaction? = null

    val state: StateFlow<FinanceUiState> = combine(
        transactionRepository.observeTransactions(),
        categoryRepository.observeCategories()
    ) { txs, categories ->
        transactionsNow = txs
        build(txs, categories)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinanceUiState())

    // Свайп-удаление со снимком для отмены
    fun deleteWithSnapshot(id: Int) {
        viewModelScope.launch {
            lastTransaction = transactionsNow.firstOrNull { it.id == id } ?: return@launch
            transactionRepository.delete(id)
        }
    }

    // ОТМЕНА: возвращаем удалённую транзакцию с прежним id
    fun restoreLast() {
        viewModelScope.launch {
            val tx = lastTransaction ?: return@launch
            transactionRepository.insert(tx)
            lastTransaction = null
        }
    }

    // Quick-Add: тап по плитке категории = повторить её последнюю сумму сегодня
    fun quickAdd(categoryId: Int) {
        viewModelScope.launch {
            val txs = transactionRepository.observeTransactions().first()
            // Сразу разбираем nullable: нет последней траты — выходим
            val last = txs.firstOrNull {
                it.categoryId == categoryId && it.type == TransactionType.EXPENSE
            } ?: return@launch
            val amount = last.amountMinor
            if (amount <= 0L) return@launch
            transactionRepository.insert(
                Transaction(
                    id = 0,
                    type = TransactionType.EXPENSE,
                    amountMinor = amount,
                    categoryId = categoryId,
                    note = last.note,
                    dateMillis = System.currentTimeMillis()
                )
            )
        }
    }

    // --- приватное ---

    private fun build(
        txs: List<Transaction>,
        categories: List<CategoryEntity>
    ): FinanceUiState {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val month = YearMonth.from(today)
        val catName = categories.associate { it.id to it.name }

        val inMonth = txs.filter {
            YearMonth.from(Instant.ofEpochMilli(it.dateMillis).atZone(zone)) == month
        }
        val income = inMonth.filter { it.type == TransactionType.INCOME }.sumOf { it.amountMinor }
        val expense = inMonth.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountMinor }
        val balance = income - expense

        // Прогноз конца месяца: доход минус экстраполированный расход
        val day = today.dayOfMonth
        val projected = if (day > 0) expense * month.lengthOfMonth() / day else expense
        val forecast = income - projected

        // График расходов за 6 месяцев
        val months = (5 downTo 0).map { month.minusMonths(it.toLong()) }
        val sums = months.map { m ->
            txs.filter {
                it.type == TransactionType.EXPENSE &&
                    YearMonth.from(Instant.ofEpochMilli(it.dateMillis).atZone(zone)) == m
            }.sumOf { it.amountMinor }
        }
        val maxSum = sums.maxOrNull()?.coerceAtLeast(1L) ?: 1L
        val bars = months.mapIndexed { i, m ->
            MonthBarUi(
                label = m.monthValue.toString(),
                fraction = sums[i].toFloat() / maxSum,
                sumText = MoneyFormatter.format(sums[i])
            )
        }

        // Категории текущего месяца (только расходы)
        val byCat = inMonth.filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.categoryId }
            .mapValues { entry -> entry.value.sumOf { it.amountMinor } }
        val maxCat = byCat.values.maxOrNull()?.coerceAtLeast(1L) ?: 1L
        val catSums = byCat.map { (id, sum) ->
            CategorySumUi(
                id = id,
                name = catName[id] ?: "#$id",
                sumText = MoneyFormatter.format(sum),
                fraction = sum.toFloat() / maxCat
            )
        }.sortedByDescending { it.fraction }

        // Плитки Quick-Add: категории, по которым уже есть траты
        val quick = catSums.take(6)

        // Список транзакций месяца: свежие сверху
        val dayFmt = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
        val list = inMonth.sortedByDescending { it.dateMillis }.map { tx ->
            TransactionUi(
                id = tx.id,
                title = if (tx.note.isBlank()) (catName[tx.categoryId] ?: "—") else tx.note,
                subtitle = Instant.ofEpochMilli(tx.dateMillis).atZone(zone).toLocalDate().format(dayFmt),
                sumText = (if (tx.type == TransactionType.EXPENSE) "-" else "+") +
                    MoneyFormatter.format(tx.amountMinor),
                isExpense = tx.type == TransactionType.EXPENSE
            )
        }

        return FinanceUiState(
            balanceText = (if (balance < 0) "-" else "") + MoneyFormatter.format(abs(balance)),
            incomeText = MoneyFormatter.format(income),
            expenseText = MoneyFormatter.format(expense),
            forecastText = (if (forecast < 0) "-" else "") + MoneyFormatter.format(abs(forecast)),
            monthBars = bars,
            categorySums = catSums,
            transactions = list,
            quickCategories = quick,
            loaded = true
        )
    }
}
