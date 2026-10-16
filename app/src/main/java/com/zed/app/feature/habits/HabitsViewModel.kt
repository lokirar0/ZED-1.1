package com.zed.app.feature.habits

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zed.app.core.domain.model.Habit
import com.zed.app.core.domain.model.HabitCompletion
import com.zed.app.core.domain.repository.CourseRepository
import com.zed.app.core.domain.repository.HabitRepository
import com.zed.app.core.domain.util.DoseScheduleCalculator
import com.zed.app.core.domain.util.StreakCalculator
import com.zed.app.core.notifications.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HabitUiItem(
    val id: Int,
    val title: String,
    val note: String,
    val streak: Int,
    val weekFlags: List<Boolean>,
    val doneToday: Boolean,
    val reminderLabel: String?
)

data class HabitsUiState(
    val habits: List<HabitUiItem> = emptyList(),
    val todayDone: Int = 0,
    val todayTotal: Int = 0,
    val bestStreak: Int = 0,
    val coursesLabel: String? = null, // «ДЕНЬ 3/25 · СЛЕД. 14:00» или null
    val loaded: Boolean = false
)

@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val repository: HabitRepository,
    private val courseRepository: CourseRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private var completionsNow: List<HabitCompletion> = emptyList()
    private var lastHabit: Habit? = null
    private var lastCompletions: List<HabitCompletion> = emptyList()

    val state: StateFlow<HabitsUiState> = combine(
        repository.observeHabits(),
        repository.observeCompletions(),
        courseRepository.observeCourses(),
        courseRepository.observeDoses()
    ) { habits, completions, courses, doses ->
        completionsNow = completions
        val today = LocalDate.now().toEpochDay()
        val byHabit = completions.groupBy({ it.habitId }, { it.day })

        val items = habits.map { habit ->
            val days = byHabit[habit.id]?.toSet() ?: emptySet()
            HabitUiItem(
                id = habit.id,
                title = habit.title,
                note = habit.note,
                streak = StreakCalculator.currentStreak(days, today),
                weekFlags = (6 downTo 0).map { offset -> days.contains(today - offset) },
                doneToday = days.contains(today),
                reminderLabel = habit.reminderTimeMinutes?.let { m ->
                    "%02d:%02d".format(m / 60, m % 60)
                }
            )
        }

        HabitsUiState(
            habits = items,
            todayDone = items.count { it.doneToday },
            todayTotal = items.size,
            bestStreak = habits.maxOfOrNull { h ->
                StreakCalculator.bestStreak(byHabit[h.id]?.toSet() ?: emptySet())
            } ?: 0,
            coursesLabel = buildCoursesLabel(courses, doses),
            loaded = true
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HabitsUiState())

    fun toggle(habitId: Int) {
        viewModelScope.launch { repository.toggle(habitId, LocalDate.now().toEpochDay()) }
    }

    fun deleteWithSnapshot(habitId: Int) {
        viewModelScope.launch {
            val habit = repository.getHabit(habitId) ?: return@launch
            lastHabit = habit
            lastCompletions = completionsNow.filter { it.habitId == habitId }
            ReminderScheduler.cancelHabitReminder(context, habitId)
            repository.delete(habitId)
        }
    }

    fun restoreLast() {
        viewModelScope.launch {
            val habit = lastHabit ?: return@launch
            repository.upsert(habit)
            lastCompletions.forEach { c -> repository.toggle(habit.id, c.day) }
            habit.reminderTimeMinutes?.let { minutes ->
                ReminderScheduler.scheduleHabitReminder(context, habit.id, minutes)
            }
            lastHabit = null
            lastCompletions = emptyList()
        }
    }

    // --- приватное ---

    // Метка карточки курсов: первый активный курс + ближайший неприёманный приём
    private fun buildCoursesLabel(
        courses: List<com.zed.app.core.domain.model.Course>,
        doses: List<com.zed.app.core.domain.model.DoseMark>
    ): String? {
        val today = LocalDate.now()
        val nowMin = LocalTime.now().hour * 60 + LocalTime.now().minute
        val active = courses.firstOrNull { c ->
            DoseScheduleCalculator.isActive(
                DoseScheduleCalculator.dayIndex(c.startLocalDate(), today),
                c.totalDays
            )
        } ?: return null
        val day = DoseScheduleCalculator.dayIndex(active.startLocalDate(), today)
        val taken = doses
            .filter { it.courseId == active.id && it.day == today.toEpochDay() }
            .map { it.doseMinutes }
            .toSet()
        val next = DoseScheduleCalculator
            .dosesForDay(active.firstDoseMinutes, active.intervalHours)
            .firstOrNull { it >= nowMin && it !in taken }
        return if (next == null) {
            "DAY $day/${active.totalDays} · DONE"
        } else {
            "DAY $day/${active.totalDays} · ${DoseScheduleCalculator.formatMinutes(next)}"
        }
    }
}
