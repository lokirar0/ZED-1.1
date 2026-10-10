package com.zed.app

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zed.app.core.domain.repository.HabitRepository
import com.zed.app.core.domain.repository.TransactionRepository
import com.zed.app.core.notifications.ReminderScheduler
import com.zed.app.core.settings.Settings
import com.zed.app.core.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    private val habitRepository: HabitRepository,
    private val transactionRepository: TransactionRepository,
    private val settingsRepositoryForSchedule: SettingsRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // null = настройки ещё не прочитаны (чёрный экран без вспышки онбординга)
    val settings: StateFlow<Settings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        ensureSchedules()
        // Повторяющиеся операции: копии за пропущенные месяцы при каждом старте
        viewModelScope.launch { transactionRepository.materializeRecurring() }
    }

    // Будильники напоминаний: глобальные + персональные (идемпотентно)
    fun ensureSchedules() {
        viewModelScope.launch {
            val s = settingsRepositoryForSchedule.settings.first()
            if (s.notificationsEnabled) {
                ReminderScheduler.scheduleAll(context)
                habitRepository.getHabitsOnce().forEach { habit ->
                    habit.reminderTimeMinutes?.let { minutes ->
                        ReminderScheduler.scheduleHabitReminder(context, habit.id, minutes)
                    }
                }
            }
        }
    }
}
