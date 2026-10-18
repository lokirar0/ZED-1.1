package com.zed.app

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zed.app.core.data.repository.ActivityRepository
import com.zed.app.core.domain.repository.CourseRepository
import com.zed.app.core.domain.repository.HabitRepository
import com.zed.app.core.domain.repository.TransactionRepository
import com.zed.app.core.notifications.ReminderScheduler
import com.zed.app.core.settings.Settings
import com.zed.app.core.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val habitRepository: HabitRepository,
    private val transactionRepository: TransactionRepository,
    private val courseRepository: CourseRepository,
    private val activityRepository: ActivityRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // null = настройки ещё не прочитаны (чёрный экран без вспышки онбординга)
    val settings: StateFlow<Settings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        ensureSchedules()
        viewModelScope.launch { transactionRepository.materializeRecurring() }
        viewModelScope.launch { activityRepository.addLaunch(LocalDate.now().toEpochDay()) }
        viewModelScope.launch { courseRepository.rescheduleAllReminders() }
    }

    fun addSession(seconds: Long) {
        viewModelScope.launch {
            activityRepository.addSeconds(LocalDate.now().toEpochDay(), seconds)
        }
    }

    // Экран первичных разрешений пройден (или пропущен) — больше не показывать
    fun markPermissionsAsked() {
        viewModelScope.launch { settingsRepository.setPermissionsAsked(true) }
    }

    fun ensureSchedules() {
        viewModelScope.launch {
            val s = settingsRepository.settings.first()
            if (s.notificationsEnabled) {
                ReminderScheduler.scheduleAll(context)
                habitRepository.getHabitsOnce().forEach { habit ->
                    habit.reminderTimeMinutes?.let { minutes ->
                        ReminderScheduler.scheduleHabitReminder(context, habit.id, minutes)
                    }
                }
                courseRepository.rescheduleAllReminders()
            }
        }
    }
}
