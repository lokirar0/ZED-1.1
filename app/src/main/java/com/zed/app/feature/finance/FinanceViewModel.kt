package com.zed.app.feature.finance

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zed.app.core.data.local.CategoryEntity
import com.zed.app.core.data.local.displayName
import com.zed.app.core.domain.model.Transaction
import com.zed.app.core.domain.model.TransactionType
import com.zed.app.core.domain.repository.CategoryRepository
import com.zed.app.core.domain.repository.TransactionRepository
import com.zed.app.core.domain.util.MoneyFormatter
import com.zed.app.core.export.CsvExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TransactionUiItem(
    val id: Int,
    val isExpense: Boolean,
    val amountText: String,
    val categoryName: String,
    val dateLabel: String,
    val note: String
)

data class MonthSummaryUi(
    val incomeText: String,
    val expenseText: String,
    val balanceText: String,
    val balanceNegative: Boolean
)

data class MonthPointUi(
    val label: String,
    val income: Long,
    val expense: Long
)

data class FinanceUiState(
    val transactions: List<TransactionUiItem> = emptyList(),
    val summary: MonthSummaryUi = MonthSummaryUi("0", "0", "0", false),
    val chart: List<MonthPointUi> = emptyList(),
    val monthLabel: String = "",
    val calendarWeeks: List<List<CalendarDayUi?>> = emptyList(),
    val loaded: Boolean = false
)

@HiltViewModel
class FinanceViewModel @Inject constructor(
    private val repository: TransactionRepository,
    categoryRepository: CategoryRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val allTransactions: StateFlow<List<Transaction>> = repository.observeTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val categoriesNow = MutableStateFlow<List<CategoryEntity>>(emptyList())

    val state: StateFlow<FinanceUiState> =
        combine(repository.observeTransactions(), categoryRepository.observeCategories()) { list, cats ->
            categoriesNow.value = cats
            mapToState(list, cats)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinanceUiState())

    init {
        viewModelScope.launch { categoryRepository.ensureSeeded() }
    }

    fun delete(id: Int) {
        viewModelScope.launch { repository.delete(id) }
    }

    suspend fun exportTo(uri: Uri): Boolean =
        CsvExporter.write(
            context,
            uri,
            CsvExporter.buildCsv(allTransactions.value) { t -> categoryNameFor(t, categoriesNow.value) }
        )

    // --- приватное ---

    private fun categoryNameFor(t: Transaction, cats: List<CategoryEntity>): String =
        cats.firstOrNull { it.id == t.categoryId }?.displayName(context) ?: "—"

    private fun mapToState(list: List<Transaction>, cats: List<CategoryEntity>): FinanceUiState {
        val locale = Locale.getDefault()
        val zone = ZoneId.systemDefault()
        val dayFmt = DateTimeFormatter.ofPattern("d MMM", locale)
        val currentMonth = YearMonth.now()
        val today = LocalDate.now()

        val inMonth = list.filter { YearMonth.from(Instant.ofEpochMilli(it.dateMillis).atZone(zone)) == currentMonth }
        val income = inMonth.filter { it.type == TransactionType.INCOME }.sumOf { it.amountMinor }
        val expense = inMonth.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountMinor }
        val balance = income - expense

        val chart = (5 downTo 0).map { offset ->
            val month = currentMonth.minusMonths(offset.toLong())
            val monthTx = list.filter { YearMonth.from(Instant.ofEpochMilli(it.dateMillis).atZone(zone)) == month }
            MonthPointUi(
                label = month.month.getDisplayName(TextStyle.SHORT, locale).take(3),
                income = monthTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amountMinor },
                expense = monthTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountMinor }
            )
        }

        val items = list.map { t ->
            TransactionUiItem(
                id = t.id,
                isExpense = t.type == TransactionType.EXPENSE,
                amountText = MoneyFormatter.format(t.amountMinor),
                categoryName = categoryNameFor(t, cats),
                dateLabel = dayFmt.format(Instant.ofEpochMilli(t.dateMillis).atZone(zone)),
                note = t.note
            )
        }

        // Календарь текущего месяца: точки по дням
        val byDay = list.groupBy { Instant.ofEpochMilli(it.dateMillis).atZone(zone).toLocalDate() }
        val lead = currentMonth.atDay(1).dayOfWeek.value - 1
        val weeks = mutableListOf<List<CalendarDayUi?>>()
        val row = MutableList<CalendarDayUi?>(lead) { null }
        for (d in 1..currentMonth.lengthOfMonth()) {
            val date = currentMonth.atDay(d)
            val dayTx = byDay[date].orEmpty()
            row.add(
                CalendarDayUi(
                    day = d,
                    dateMillis = date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli(),
                    hasExpense = dayTx.any { it.type == TransactionType.EXPENSE },
                    hasIncome = dayTx.any { it.type == TransactionType.INCOME },
                    isToday = date == today
                )
            )
            if (row.size == 7) {
                weeks.add(row.toList())
                row.clear()
            }
        }
        if (row.isNotEmpty()) {
            while (row.size < 7) row.add(null)
            weeks.add(row.toList())
        }

        return FinanceUiState(
            transactions = items,
            summary = MonthSummaryUi(
                incomeText = MoneyFormatter.format(income),
                expenseText = MoneyFormatter.format(expense),
                balanceText = (if (balance < 0) "-" else "") + MoneyFormatter.format(abs(balance)),
                balanceNegative = balance < 0
            ),
            chart = chart,
            monthLabel = currentMonth.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)),
            calendarWeeks = weeks,
            loaded = true
        )
    }
}
