package com.zed.app.feature.courses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zed.app.core.domain.model.Course
import com.zed.app.core.domain.model.DoseMark
import com.zed.app.core.domain.repository.CourseRepository
import com.zed.app.core.domain.util.DoseScheduleCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Приём на сегодня: плановое время + отметка
data class DoseUi(
    val minutes: Int,
    val label: String,
    val taken: Boolean
)

// Карточка курса на экране
data class CourseUiItem(
    val id: Int,
    val title: String,
    val dayIndex: Int,
    val totalDays: Int,
    val status: Int,            // 0 = не начался, 1 = идёт, 2 = завершён
    val startLabel: String,     // «16 октября 2026»
    val intervalHours: Int,
    val doses: List<DoseUi>,    // приёмы на сегодня (пусто, если курс неактивен)
    val takenCount: Int,
    val doseCount: Int
)

data class CoursesUiState(
    val courses: List<CourseUiItem> = emptyList(),
    val loaded: Boolean = false
)

@HiltViewModel
class CoursesViewModel @Inject constructor(
    private val repository: CourseRepository
) : ViewModel() {

    val state: StateFlow<CoursesUiState> =
        combine(repository.observeCourses(), repository.observeDoses()) { courses, doses ->
            CoursesUiState(
                courses = courses.map { mapItem(it, doses) },
                loaded = true
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CoursesUiState())

    fun toggleDose(courseId: Int, doseMinutes: Int) {
        viewModelScope.launch {
            repository.toggleDose(courseId, LocalDate.now().toEpochDay(), doseMinutes)
        }
    }

    fun delete(courseId: Int) {
        viewModelScope.launch { repository.delete(courseId) }
    }

    // --- приватное ---

    private fun mapItem(course: Course, doses: List<DoseMark>): CourseUiItem {
        val today = LocalDate.now()
        val day = DoseScheduleCalculator.dayIndex(course.startLocalDate(), today)
        val status = when {
            day < 1 -> 0
            day > course.totalDays -> 2
            else -> 1
        }
        val takenToday = doses
            .filter { it.courseId == course.id && it.day == today.toEpochDay() }
            .map { it.doseMinutes }
            .toSet()

        val doseList = if (status == 1) {
            DoseScheduleCalculator.dosesForDay(course.firstDoseMinutes, course.intervalHours).map { min ->
                DoseUi(
                    minutes = min,
                    label = DoseScheduleCalculator.formatMinutes(min),
                    taken = takenToday.contains(min)
                )
            }
        } else {
            emptyList()
        }

        return CourseUiItem(
            id = course.id,
            title = course.title,
            dayIndex = day.coerceIn(1, course.totalDays),
            totalDays = course.totalDays,
            status = status,
            startLabel = course.startLocalDate().format(
                DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault())
            ),
            intervalHours = course.intervalHours,
            doses = doseList,
            takenCount = doseList.count { it.taken },
            doseCount = doseList.size
        )
    }
}
