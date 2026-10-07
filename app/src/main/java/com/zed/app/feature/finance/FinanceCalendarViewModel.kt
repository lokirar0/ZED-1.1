package com.zed.app.feature.finance

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zed.app.core.data.local.displayName
import com.zed.app.core.domain.model.TransactionType
import com.zed.app.core.domain.repository.CategoryRepository
import com.zed.app.core.domain.repository.TransactionRepository
import com.zed.app.core.domain.util.MoneyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

// День месяца: точки = были расходы / доходы
data class CalendarDayUi(
    val day: Int,
    val dateMillis: Long,
    val hasExpense: Boolean,
    val hasIncome: Boolean,
    val isToday: Boolean
)

// Операция выбранного дня
data class DayTxUi(
    val id: Int,
    val title: String,
    val subtitle: String,
    val amountText: String,
    val isExpense: Boolean
)

data class FinanceCalendarUiState(
    val monthLabel: String = "",
    val weekDays: List<String> = emptyList(),
    val weeks: List<List<CalendarDayUi?>> = emptyList(),
    val selectedDate: Long? = null,
    val selectedLabel: String = "",
    val dayItems: List<DayTxUi> = emptyList(),
    val dayIncomeText: String = "",
    val dayExpenseText: String = ""
)

@HiltViewModel
class FinanceCalendarViewModel @Inject constructor(
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val initialDate: Long = savedStateHandle.get<Long>("date") ?: -1L

    private val month = MutableStateFlow(yearMonthOf(initialDate))
    private val selected = MutableStateFlow<Long?>(
        if (initialDate > 0) middayMillis(initialDate) else null
    )

    val state: StateFlow<FinanceCalendarUiState> = combine(
        transactionRepository.observeTransactions(),
        categoryRepository.observeCategories(),
        month,
        selected
    ) { txs, cats, m, sel ->
        buildState(txs, cats, m, sel)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinanceCalendarUiState())

    fun prevMonth() { month.value = month.value.minusMonths(1) }
    fun nextMonth() { month.value = month.value.plusMonths(1) }

    fun selectDay(millis: Long) {
        selected.value = if (selected.value == millis) null else millis
    }

    // --- приватное ---

    private fun buildState(
        txs: List<com.zed.app.core.domain.model.Transaction>,
        cats: List<com.zed.app.core.data.local.CategoryEntity>,
        m: YearMonth,
        sel: Long?
    ): FinanceCalendarUiState {
        val zone = ZoneId.systemDefault()
        val locale = Locale.getDefault()
        val today = LocalDate.now()

        // Группируем операции по локальной дате
        val byDay = txs.groupBy { Instant.ofEpochMilli(it.dateMillis).atZone(zone).toLocalDate() }

        // Сетка недель: понедельник первый, ведущие пустые ячейки
        val lead = m.atDay(1).dayOfWeek.value - 1
        val weeks = mutableListOf<List<CalendarDayUi?>>()
        val row = MutableList<CalendarDayUi?>(lead) { null }
        for (d in 1..m.lengthOfMonth()) {
            val date = m.atDay(d)
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

        // Выбранный день
        val selDate = sel?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
        val selTx = selDate?.let { byDay[it].orEmpty() } ?: emptyList()
        val dayItems = selTx.map { t ->
            DayTxUi(
                id = t.id,
                title = cats.firstOrNull { it.id == t.categoryId }?.displayName(context) ?: "—",
                subtitle = t.note,
                amountText = MoneyFormatter.format(t.amountMinor),
                isExpense = t.type == TransactionType.EXPENSE
            )
        }

        return FinanceCalendarUiState(
            monthLabel = m.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)),
            weekDays = (1..7).map { dow ->
                java.time.DayOfWeek.of(dow).getDisplayName(java.time.format.TextStyle.SHORT, locale)
            },
            weeks = weeks,
            selectedDate = sel,
            selectedLabel = selDate?.format(
                DateTimeFormatter.ofPattern("d MMMM, EEEE", locale)
            ) ?: "",
            dayItems = dayItems,
            dayIncomeText = MoneyFormatter.format(
                selTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amountMinor }
            ),
            dayExpenseText = MoneyFormatter.format(
                selTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountMinor }
            )
        )
    }

    companion object {
        fun yearMonthOf(millis: Long): YearMonth =
            if (millis > 0) YearMonth.from(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))
            else YearMonth.now()

        fun middayMillis(millis: Long): Long =
            Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                .atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
