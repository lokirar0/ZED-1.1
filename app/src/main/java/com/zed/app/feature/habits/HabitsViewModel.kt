package com.zed.app.feature.habits

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zed.app.core.domain.repository.HabitRepository
import com.zed.app.core.domain.util.StreakCalculator
import com.zed.app.core.notifications.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Элемент карточки на экране
data class HabitUiItem(
    val id: Int,
    val title: String,
    val note: String,
    val streak: Int,
    val weekFlags: List<Boolean>,
    val doneToday: Boolean,
    val reminderLabel: String? // "20:00" или null
)

data class HabitsUiState(
    val habits: List<HabitUiItem> = emptyList(),
    val todayDone: Int = 0,
    val todayTotal: Int = 0,
    val bestStreak: Int = 0,
    val loaded: Boolean = false
)

@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val repository: HabitRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val state: StateFlow<HabitsUiState> =
        combine(repository.observeHabits(), repository.observeCompletions()) { habits, completions ->
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
                loaded = true
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HabitsUiState())

    fun toggle(habitId: Int) {
        viewModelScope.launch { repository.toggle(habitId, LocalDate.now().toEpochDay()) }
    }

    // Удаление: чистим и персональное расписание напоминаний
    fun delete(habitId: Int) {
        viewModelScope.launch {
            ReminderScheduler.cancelHabitReminder(context, habitId)
            repository.delete(habitId)
        }
    }
}
