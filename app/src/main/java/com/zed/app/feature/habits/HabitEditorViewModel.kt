package com.zed.app.feature.habits

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zed.app.core.domain.model.Habit
import com.zed.app.core.domain.repository.HabitRepository
import com.zed.app.core.notifications.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EditorState(
    val title: String = "",
    val note: String = "",
    val isEditing: Boolean = false,
    val titleError: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val finished: Boolean = false
)

@HiltViewModel
class HabitEditorViewModel @Inject constructor(
    private val repository: HabitRepository,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val habitId: Int = savedStateHandle.get<Int>("habitId") ?: -1

    private val _state = MutableStateFlow(EditorState(isEditing = habitId != -1))
    val state: StateFlow<EditorState> = _state.asStateFlow()

    private var createdAt: Long = System.currentTimeMillis()

    init {
        if (habitId != -1) {
            viewModelScope.launch {
                repository.getHabit(habitId)?.let { habit ->
                    createdAt = habit.createdAt
                    val minutes = habit.reminderTimeMinutes
                    _state.value = _state.value.copy(
                        title = habit.title,
                        note = habit.note,
                        reminderEnabled = minutes != null,
                        reminderHour = (minutes ?: 1200) / 60,
                        reminderMinute = (minutes ?: 1200) % 60
                    )
                }
            }
        }
    }

    fun onTitleChange(value: String) {
        _state.value = _state.value.copy(title = value, titleError = false)
    }

    fun onNoteChange(value: String) {
        _state.value = _state.value.copy(note = value)
    }

    fun setReminderEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(reminderEnabled = enabled)
    }

    fun setReminderTime(hour: Int, minute: Int) {
        _state.value = _state.value.copy(reminderHour = hour, reminderMinute = minute)
    }

    fun save() {
        val current = _state.value
        if (current.title.isBlank()) {
            _state.value = current.copy(titleError = true)
            return
        }
        val reminderMinutes = if (current.reminderEnabled) {
            current.reminderHour * 60 + current.reminderMinute
        } else {
            null
        }
        viewModelScope.launch {
            val id = repository.upsert(
                Habit(
                    id = if (current.isEditing) habitId else 0,
                    title = current.title.trim(),
                    note = current.note.trim(),
                    createdAt = createdAt,
                    reminderTimeMinutes = reminderMinutes
                )
            ).toInt()

            // Персональное расписание: ставим или снимаем
            val realId = if (current.isEditing) habitId else id
            if (reminderMinutes != null) {
                ReminderScheduler.scheduleHabitReminder(context, realId, reminderMinutes)
            } else {
                ReminderScheduler.cancelHabitReminder(context, realId)
            }
            _state.value = _state.value.copy(finished = true)
        }
    }

    fun delete() {
        viewModelScope.launch {
            ReminderScheduler.cancelHabitReminder(context, habitId)
            repository.delete(habitId)
            _state.value = _state.value.copy(finished = true)
        }
    }
}
