package com.zed.app.feature.courses

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zed.app.core.domain.model.Course
import com.zed.app.core.domain.repository.CourseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CourseEditorState(
    val title: String = "",
    val note: String = "",
    val startDateMillis: Long = System.currentTimeMillis(),
    val totalDaysText: String = "7",
    val intervalHoursText: String = "2",
    val intervalMinutesText: String = "0",
    val firstDoseMinutes: Int = 8 * 60,
    val titleError: Boolean = false,
    val daysError: Boolean = false,
    val intervalError: Boolean = false,
    val isEditing: Boolean = false,
    val finished: Boolean = false
)

@HiltViewModel
class CourseEditorViewModel @Inject constructor(
    private val repository: CourseRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val courseId: Int = savedStateHandle.get<Int>("courseId") ?: -1

    private val _state = MutableStateFlow(CourseEditorState(isEditing = courseId != -1))
    val state: StateFlow<CourseEditorState> = _state.asStateFlow()

    init {
        if (courseId != -1) {
            viewModelScope.launch {
                repository.getCourse(courseId)?.let { c ->
                    _state.value = _state.value.copy(
                        title = c.title,
                        note = c.note,
                        startDateMillis = c.startDateMillis,
                        totalDaysText = c.totalDays.toString(),
                        intervalHoursText = c.intervalHours.toString(),
                        intervalMinutesText = c.intervalMinutes.toString(),
                        firstDoseMinutes = c.firstDoseMinutes
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

    fun setStartDate(millis: Long) {
        _state.value = _state.value.copy(startDateMillis = millis)
    }

    fun onDaysChange(value: String) {
        _state.value = _state.value.copy(totalDaysText = value, daysError = false)
    }

    fun onIntervalHoursChange(value: String) {
        _state.value = _state.value.copy(intervalHoursText = value, intervalError = false)
    }

    fun onIntervalMinutesChange(value: String) {
        _state.value = _state.value.copy(intervalMinutesText = value, intervalError = false)
    }

    fun setFirstDoseMinutes(minutes: Int) {
        _state.value = _state.value.copy(firstDoseMinutes = minutes)
    }

    fun save() {
        val current = _state.value
        val days = current.totalDaysText.trim().toIntOrNull()
        val hours = current.intervalHoursText.trim().toIntOrNull() ?: 0
        val minutes = current.intervalMinutesText.trim().toIntOrNull() ?: 0
        val totalInterval = hours * 60 + minutes

        val titleBad = current.title.isBlank()
        val daysBad = days == null || days !in 1..365
        // Интервал: от 10 минут до 24 часов; часы 0..23, минуты 0..59
        val intervalBad = hours !in 0..23 || minutes !in 0..59 || totalInterval !in 10..1440
        if (titleBad || daysBad || intervalBad) {
            _state.value = current.copy(
                titleError = titleBad,
                daysError = daysBad,
                intervalError = intervalBad
            )
            return
        }
        viewModelScope.launch {
            val id = repository.upsert(
                Course(
                    id = if (current.isEditing) courseId else 0,
                    title = current.title.trim(),
                    note = current.note.trim(),
                    startDateMillis = current.startDateMillis,
                    totalDays = days!!,
                    intervalHours = hours,
                    intervalMinutes = minutes,
                    firstDoseMinutes = current.firstDoseMinutes
                )
            ).toInt()
            repository.rescheduleReminders(if (current.isEditing) courseId else id)
            _state.value = _state.value.copy(finished = true)
        }
    }

    fun delete() {
        viewModelScope.launch {
            repository.delete(courseId)
            _state.value = _state.value.copy(finished = true)
        }
    }
}
