package com.zed.app.feature.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zed.app.core.domain.model.Habit
import com.zed.app.core.domain.model.HabitCompletion
import com.zed.app.core.domain.repository.HabitRepository
import com.zed.app.core.domain.util.StreakCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

// Ячейка heatmap: доля выполненных привычек за день (0..1)
data class DayHeat(val day: Long, val ratio: Float)

// Столбик «по дням недели»: средняя доля выполнения
data class WeekDayStat(val label: String, val ratio: Float)

// Строка «по привычкам»
data class HabitStatRow(
    val id: Int,
    val title: String,
    val streak: Int,
    val best: Int,
    val ratio30: Float
)

data class HabitStatsUi(
    val todayDone: Int = 0,
    val todayTotal: Int = 0,
    val bestStreak: Int = 0,
    val totalMarks: Int = 0,
    val activeHabits: Int = 0,
    val heat: List<DayHeat> = emptyList(),
    val weekDays: List<WeekDayStat> = emptyList(),
    val perHabit: List<HabitStatRow> = emptyList()
)

@HiltViewModel
class HabitStatsViewModel @Inject constructor(
    repository: HabitRepository
) : ViewModel() {

    val state: StateFlow<HabitStatsUi> =
        combine(repository.observeHabits(), repository.observeCompletions()) { habits, completions ->
            build(habits, completions)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HabitStatsUi())

    // --- приватное: вся математика чистая, без Android-API ---

    private fun build(habits: List<Habit>, completions: List<HabitCompletion>): HabitStatsUi {
        val today = LocalDate.now().toEpochDay()
        val byHabit = completions.groupBy({ it.habitId }, { it.day })

        // Сводка
        val doneToday = habits.count { h -> byHabit[h.id]?.contains(today) == true }
        val best = habits.maxOfOrNull { h ->
            StreakCalculator.bestStreak(byHabit[h.id]?.toSet() ?: emptySet())
        } ?: 0

        // Heatmap: последние 30 дней, знаменатель = привычки, существовавшие в тот день
        val heat = (29 downTo 0).map { offset ->
            val day = today - offset
            val denom = habits.count { it.createdDay() <= day }
            val done = completions.count { it.day == day }
            DayHeat(day, if (denom == 0) 0f else (done.toFloat() / denom).coerceIn(0f, 1f))
        }

        // Профиль по дням недели: среднее за 8 недель
        val weekDays = (1..7).map { dow ->
            val days = (55 downTo 0)
                .map { today - it }
                .filter { LocalDate.ofEpochDay(it).dayOfWeek.value == dow }
            val ratios = days.map { d ->
                val denom = habits.count { it.createdDay() <= d }
                val done = completions.count { it.day == d }
                if (denom == 0) 0f else done.toFloat() / denom
            }
            WeekDayStat(
                label = DayOfWeek.of(dow).getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                ratio = if (ratios.isEmpty()) 0f else ratios.average().toFloat()
            )
        }

        // По привычкам: текущая/рекордная серия + доля за 30 дней
        val perHabit = habits.map { h ->
            val days = byHabit[h.id]?.toSet() ?: emptySet()
            val window = minOf(30L, today - h.createdDay() + 1).toInt().coerceAtLeast(1)
            val done30 = (0 until window).count { off -> days.contains(today - off) }
            HabitStatRow(
                id = h.id,
                title = h.title,
                streak = StreakCalculator.currentStreak(days, today),
                best = StreakCalculator.bestStreak(days),
                ratio30 = done30.toFloat() / window
            )
        }

        return HabitStatsUi(
            todayDone = doneToday,
            todayTotal = habits.size,
            bestStreak = best,
            totalMarks = completions.size,
            activeHabits = habits.size,
            heat = heat,
            weekDays = weekDays,
            perHabit = perHabit
        )
    }

    private fun Habit.createdDay(): Long =
        Instant.ofEpochMilli(createdAt).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
}
