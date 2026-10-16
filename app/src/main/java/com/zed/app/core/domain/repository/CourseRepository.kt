package com.zed.app.core.domain.repository

import com.zed.app.core.domain.model.Course
import com.zed.app.core.domain.model.DoseMark
import kotlinx.coroutines.flow.Flow

interface CourseRepository {
    fun observeCourses(): Flow<List<Course>>
    fun observeDoses(): Flow<List<DoseMark>>
    suspend fun getCoursesOnce(): List<Course>
    suspend fun getCourse(id: Int): Course?
    suspend fun upsert(course: Course): Long
    suspend fun delete(id: Int)

    // Переключить отметку приёма за день (нет → принято, есть → снять)
    suspend fun toggleDose(courseId: Int, day: Long, doseMinutes: Int)
    suspend fun isDoseTaken(courseId: Int, day: Long, doseMinutes: Int): Boolean

    // Перестановка будильника ближайшего приёма курса / всех курсов
    suspend fun rescheduleReminders(courseId: Int)
    suspend fun rescheduleAllReminders()
}
