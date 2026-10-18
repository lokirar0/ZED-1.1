package com.zed.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zed.app.core.data.repository.ActivityRepository
import com.zed.app.core.domain.model.Course
import com.zed.app.core.domain.model.DoseMark
import com.zed.app.core.domain.model.TransactionType
import com.zed.app.core.domain.repository.CourseRepository
import com.zed.app.core.domain.repository.HabitRepository
import com.zed.app.core.domain.repository.TransactionRepository
import com.zed.app.core.domain.util.DoseScheduleCalculator
import com.zed.app.core.domain.util.MoneyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
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
    val target: Int,
    val done30: Int
)

data class AnalyticsUi(
    val monthLabel: String = "",
    val daysUsed: Int = 0,
    val daysInMonth: Int = 30,
    val dayFlags: List<Boolean> = emptyList(),
    val totalSeconds: Long = 0,
    val avgSeconds: Long = 0,
    val launches: Int = 0,
    val timeDeltaPct: Int? = null,
    val expenseCurText: String = "0",
    val expensePrevText: String = "0",
    val expenseDeltaPct: Int? = null,
    val goals: List<GoalRow> = emptyList()
)

// Статистика пропусков по одному курсу
data class CourseStatRow(
    val id: Int,
    val title: String,
    val intervalLabel: String,   // «2 ч 30 м»
    val missedTotal: Int,        // пропущено приёмов всего
    val missedDays: Int,         // дней с хотя бы одним пропуском
    val lastMissLabel: String?   // «18 окт, 14:00»
)

data class CourseStatsUi(
    val totalMissed: Int = 0,
    val totalMissedDays: Int = 0,
    val rows: List<CourseStatRow> = emptyList()
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    activityRepository: ActivityRepository,
    private val activityRepo: ActivityRepository,
    habitRepository: HabitRepository,
    transactionRepository: TransactionRepository,
    courseRepository: CourseRepository
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

    // Статистика курсов: отдельный поток, чтобы не раздувать основной combine
    val courseStats: StateFlow<CourseStatsUi> = combine(
        courseRepository.observeCourses(),
        courseRepository.observeDoses()
    ) { courses, doses ->
        buildCourseStats(courses, doses)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CourseStatsUi())

    fun changeGoal(habitId: Int, target: Int) {
        viewModelScope.launch { activityRepo.setGoal(habitId, target) }
    }

    // --- приватное: активность, финансы, цели ---

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

        val monthSessions = sessions.filter { YearMonth.from(LocalDate.ofEpochDay(it.day)) == month }
        val active = monthSessions.filter { it.seconds > 0 || it.launches > 0 }
        val totalSeconds = monthSessions.sumOf { it.seconds }
        val launches = monthSessions.sumOf { it.launches }
        val avgSeconds = if (launches > 0) totalSeconds / launches else 0L
        val dayFlags = (1..month.lengthOfMonth()).map { d ->
            active.any { LocalDate.ofEpochDay(it.day).dayOfMonth == d }
        }

        val prevSeconds = sessions
            .filter { YearMonth.from(LocalDate.ofEpochDay(it.day)) == prevMonth }
            .sumOf { it.seconds }
        val timeDelta = if (prevSeconds > 0) ((totalSeconds - prevSeconds) * 100 / prevSeconds).toInt() else null

        fun expenseOf(m: YearMonth): Long = txs
            .filter { it.type == TransactionType.EXPENSE && YearMonth.from(Instant.ofEpochMilli(it.dateMillis).atZone(zone)) == m }
            .sumOf { it.amountMinor }
        val expCur = expenseOf(month)
        val expPrev = expenseOf(prevMonth)
        val expDelta = if (expPrev > 0) ((expCur - expPrev) * 100 / expPrev).toInt() else null

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

    // --- приватное: пропуски курсов ---

    private fun buildCourseStats(courses: List<Course>, doses: List<DoseMark>): CourseStatsUi {
        val today = LocalDate.now()
        val nowMin = LocalTime.now().hour * 60 + LocalTime.now().minute
        val dayFmt = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

        var totalMissed = 0
        var totalMissedDays = 0
        val rows = courses.map { course ->
            val start = course.startLocalDate()
            var missed = 0
            var daysWithMiss = 0
            var lastDate: LocalDate? = null
            var lastMin: Int? = null

            for (d in 0 until course.totalDays) {
                val date = start.plusDays(d.toLong())
                if (date.isAfter(today)) break
                val taken = doses
                    .filter { it.courseId == course.id && it.day == date.toEpochDay() }
                    .map { it.doseMinutes }
                    .toSet()
                val dayMisses = DoseScheduleCalculator
                    .dosesFor(course)
                    .filter { min ->
                        val past = date.isBefore(today) || (date.isEqual(today) && min < nowMin)
                        past && !taken.contains(min)
                    }
                if (dayMisses.isNotEmpty()) {
                    daysWithMiss++
                    missed += dayMisses.size
                    lastDate = date
                    lastMin = dayMisses.last()
                }
            }

            totalMissed += missed
            totalMissedDays += daysWithMiss
            CourseStatRow(
                id = course.id,
                title = course.title,
                intervalLabel = DoseScheduleCalculator.formatInterval(course.intervalTotalMinutes()),
                missedTotal = missed,
                missedDays = daysWithMiss,
                lastMissLabel = if (lastDate != null && lastMin != null) {
                    lastDate!!.format(dayFmt) + ", " + DoseScheduleCalculator.formatMinutes(lastMin!!)
                } else null
            )
        }

        return CourseStatsUi(
            totalMissed = totalMissed,
            totalMissedDays = totalMissedDays,
            rows = rows
        )
    }
}
