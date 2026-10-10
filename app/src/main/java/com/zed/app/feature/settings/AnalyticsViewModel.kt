package com.zed.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zed.app.core.data.repository.ActivityRepository
import com.zed.app.core.domain.model.TransactionType
import com.zed.app.core.domain.repository.HabitRepository
import com.zed.app.core.domain.repository.TransactionRepository
import com.zed.app.core.domain.util.MoneyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Строка цели по привычке
data class GoalRow(
    val habitId: Int,
    val title: String,
    val target: Int,      // 0 = цель не задана
    val done30: Int       // отмечено за последние 30 дней
)

data class AnalyticsUi(
    val monthLabel: String = "",
    val daysUsed: Int = 0,
    val daysInMonth: Int = 30,
    val dayFlags: List<Boolean> = emptyList(),
    val totalSeconds: Long = 0,
    val avgSeconds: Long = 0,
    val launches: Int = 0,
    val timeDeltaPct: Int? = null,     // изменение времени к прошлому месяцу
    val expenseCurText: String = "0",
    val expensePrevText: String = "0",
    val expenseDeltaPct: Int? = null,  // >0 = потратили больше
    val goals: List<GoalRow> = emptyList()
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    activityRepository: ActivityRepository,
    private val activityRepo: ActivityRepository,
    habitRepository: HabitRepository,
    transactionRepository: TransactionRepository
) : ViewModel() {

    val state: StateFlow<AnalyticsUi> = combine(
        activityRepository.observeSessions(),
        activityRepository.observeGoals(),
        habitRepository.observeHabits(),
        habitRepository.observeCompletions(),
        transactionRepository.observeTransactions()
    ) { sessions, goals, habits, completions, txs ->
        build(sessions, goals, habits, completions, txs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsUi())

    fun changeGoal(habitId: Int, target: Int) {
        viewModelScope.launch { activityRepo.setGoal(habitId, target) }
    }

    // --- приватное ---

    private fun build(
        sessions: List<com.zed.app.core.data.local.SessionEntity>,
        goals: List<com.zed.app.core.data.local.HabitGoalEntity>,
        habits: List<com.zed.app.core.domain.model.Habit>,
        completions: List<com.zed.app.core.domain.model.HabitCompletion>,
        txs: List<com.zed.app.core.domain.model.Transaction>
    ): AnalyticsUi {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val month = YearMonth.from(today)
        val prevMonth = month.minusMonths(1)

        // Активность за текущий месяц
        val monthSessions = sessions.filter { YearMonth.from(LocalDate.ofEpochDay(it.day)) == month }
        val active = monthSessions.filter { it.seconds > 0 || it.launches > 0 }
        val totalSeconds = monthSessions.sumOf { it.seconds }
        val launches = monthSessions.sumOf { it.launches }
        val avgSeconds = if (launches > 0) totalSeconds / launches else 0L
        val dayFlags = (1..month.lengthOfMonth()).map { d ->
            active.any { LocalDate.ofEpochDay(it.day).dayOfMonth == d }
        }

        // Дельта времени к прошлому месяцу
        val prevSeconds = sessions
            .filter { YearMonth.from(LocalDate.ofEpochDay(it.day)) == prevMonth }
            .sumOf { it.seconds }
        val timeDelta = if (prevSeconds > 0) ((totalSeconds - prevSeconds) * 100 / prevSeconds).toInt() else null

        // Финансы: сравнение расходов месяц к месяцу
        fun expenseOf(m: YearMonth): Long = txs
            .filter { it.type == TransactionType.EXPENSE && YearMonth.from(Instant.ofEpochMilli(it.dateMillis).atZone(zone)) == m }
            .sumOf { it.amountMinor }
        val expCur = expenseOf(month)
        val expPrev = expenseOf(prevMonth)
        val expDelta = if (expPrev > 0) ((expCur - expPrev) * 100 / expPrev).toInt() else null

        // Цели привычек: прогресс за последние 30 дней
        val goalMap = goals.associate { it.habitId to it.targetDays }
        val todayEpoch = today.toEpochDay()
        val goalRows = habits.map { h ->
            val done30 = completions.count { c ->
                c.habitId == h.id && c.day in (todayEpoch - 29)..todayEpoch
            }
            GoalRow(
                habitId = h.id,
                title = h.title,
                target = goalMap[h.id] ?: 0,
                done30 = done30
            )
        }

        return AnalyticsUi(
            monthLabel = month.toString(),
            daysUsed = active.size,
            daysInMonth = month.lengthOfMonth(),
            dayFlags = dayFlags,
            totalSeconds = totalSeconds,
            avgSeconds = avgSeconds,
            launches = launches,
            timeDeltaPct = timeDelta,
            expenseCurText = MoneyFormatter.format(expCur),
            expensePrevText = MoneyFormatter.format(expPrev),
            expenseDeltaPct = expDelta,
            goals = goalRows
        )
    }
}
