package com.zed.app.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zed.app.core.data.local.displayName
import com.zed.app.core.domain.model.TransactionType
import com.zed.app.core.domain.repository.CategoryRepository
import com.zed.app.core.domain.repository.CreditRepository
import com.zed.app.core.domain.repository.HabitRepository
import com.zed.app.core.domain.repository.PlayerRepository
import com.zed.app.core.domain.repository.TransactionRepository
import com.zed.app.core.domain.util.MoneyFormatter
import com.zed.app.core.domain.util.StreakCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class YearReportUi(
    val year: Int = LocalDate.now().year,
    val marksTotal: Int = 0,
    val bestStreak: Int = 0,
    val bestStreakHabit: String = "—",
    val bestWeekday: String = "—",
    val expenseTotal: String = "0",
    val incomeTotal: String = "0",
    val topCategory: String = "—",
    val topCategorySum: String = "0",
    val priceyMonth: String = "—",
    val creditsPaid: Int = 0,
    val likedTracks: Int = 0,
    val playlists: Int = 0
)

// Годовой отчёт в духе Wrapped: привычки + финансы + кредиты + плеер
@HiltViewModel
class YearReportViewModel @Inject constructor(
    habitRepository: HabitRepository,
    transactionRepository: TransactionRepository,
    creditRepository: CreditRepository,
    playerRepository: PlayerRepository,
    categoryRepository: CategoryRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(YearReportUi())
    val state: StateFlow<YearReportUi> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val year = LocalDate.now().year
            val zone = ZoneId.systemDefault()
            val locale = Locale.getDefault()

            // Привычки
            val habits = habitRepository.getHabitsOnce()
            val completions = habitRepository.observeCompletions().first()
            val yearMarks = completions.filter { LocalDate.ofEpochDay(it.day).year == year }
            val byHabit = yearMarks.groupBy({ it.habitId }, { it.day })
            var bestStreak = 0
            var bestHabit = "—"
            habits.forEach { h ->
                val days = byHabit[h.id]?.toSet() ?: emptySet()
                val b = StreakCalculator.bestStreak(days)
                if (b > bestStreak) { bestStreak = b; bestHabit = h.title }
            }
            val byWeekday = yearMarks.groupBy { LocalDate.ofEpochDay(it.day).dayOfWeek }
            val bestDay = byWeekday.maxByOrNull { it.value.size }?.key ?: DayOfWeek.MONDAY

            // Финансы
            val txs = transactionRepository.observeTransactions().first()
                .filter { YearMonth.from(Instant.ofEpochMilli(it.dateMillis).atZone(zone)).year == year }
            val expense = txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountMinor }
            val income = txs.filter { it.type == TransactionType.INCOME }.sumOf { it.amountMinor }
            val cats = categoryRepository.observeCategories().first()
            val byCat = txs.filter { it.type == TransactionType.EXPENSE }.groupBy { it.categoryId }
            val topCatId = byCat.maxByOrNull { e -> e.value.sumOf { it.amountMinor } }?.key
            val topCatSum = byCat[topCatId]?.sumOf { it.amountMinor } ?: 0L
            val topCat = cats.firstOrNull { it.id == topCatId }?.displayName(context) ?: "—"
            val byMonth = txs.filter { it.type == TransactionType.EXPENSE }
                .groupBy { YearMonth.from(Instant.ofEpochMilli(it.dateMillis).atZone(zone)) }
            val pricey = byMonth.maxByOrNull { e -> e.value.sumOf { it.amountMinor } }?.key

            // Кредиты и плеер
            val payments = creditRepository.getPaymentsOnce()
                .count { YearMonth.parse(it.yearMonth).year == year }
            val liked = playerRepository.observeFavorites().first().size
            val playlists = playerRepository.observePlaylistsWithTracks().first().size

            _state.value = YearReportUi(
                year = year,
                marksTotal = yearMarks.size,
                bestStreak = bestStreak,
                bestStreakHabit = bestHabit,
                bestWeekday = bestDay.getDisplayName(TextStyle.FULL, locale),
                expenseTotal = MoneyFormatter.format(expense),
                incomeTotal = MoneyFormatter.format(income),
                topCategory = topCat,
                topCategorySum = MoneyFormatter.format(topCatSum),
                priceyMonth = pricey?.format(
                    java.time.format.DateTimeFormatter.ofPattern("LLLL", locale)
                ) ?: "—",
                creditsPaid = payments,
                likedTracks = liked,
                playlists = playlists
            )
        }
    }
}
