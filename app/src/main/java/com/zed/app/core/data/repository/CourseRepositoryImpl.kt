package com.zed.app.core.data.repository

import android.content.Context
import com.zed.app.core.data.local.CourseDao
import com.zed.app.core.data.local.CourseEntity
import com.zed.app.core.data.local.DoseDao
import com.zed.app.core.data.local.DoseMarkEntity
import com.zed.app.core.domain.model.Course
import com.zed.app.core.domain.model.DoseMark
import com.zed.app.core.domain.repository.CourseRepository
import com.zed.app.core.domain.util.DoseScheduleCalculator
import com.zed.app.core.notifications.ReminderScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class CourseRepositoryImpl @Inject constructor(
    private val courseDao: CourseDao,
    private val doseDao: DoseDao,
    @ApplicationContext private val context: Context
) : CourseRepository {

    override fun observeCourses(): Flow<List<Course>> =
        courseDao.observe().map { list -> list.map { it.toDomain() } }

    override fun observeDoses(): Flow<List<DoseMark>> =
        doseDao.observe().map { list -> list.map { it.toDomain() } }

    override suspend fun getCoursesOnce(): List<Course> =
        courseDao.getAll().map { it.toDomain() }

    override suspend fun getCourse(id: Int): Course? = courseDao.get(id)?.toDomain()

    override suspend fun upsert(course: Course): Long =
        courseDao.upsert(
            CourseEntity(
                id = course.id,
                title = course.title,
                note = course.note,
                startDateMillis = course.startDateMillis,
                totalDays = course.totalDays,
                intervalHours = course.intervalHours,
                intervalMinutes = course.intervalMinutes,
                firstDoseMinutes = course.firstDoseMinutes,
                createdAt = course.createdAt
            )
        )

    override suspend fun delete(id: Int) {
        ReminderScheduler.cancelCourseDose(context, id)
        courseDao.delete(id)
    }

    override suspend fun toggleDose(courseId: Int, day: Long, doseMinutes: Int) {
        val existing = doseDao.find(courseId, day, doseMinutes)
        if (existing != null) {
            doseDao.delete(courseId, day, doseMinutes)
        } else {
            doseDao.insert(DoseMarkEntity(courseId = courseId, day = day, doseMinutes = doseMinutes))
        }
        rescheduleReminders(courseId)
    }

    override suspend fun isDoseTaken(courseId: Int, day: Long, doseMinutes: Int): Boolean =
        doseDao.find(courseId, day, doseMinutes) != null

    override suspend fun rescheduleReminders(courseId: Int) {
        val course = getCourse(courseId)
        if (course == null) {
            ReminderScheduler.cancelCourseDose(context, courseId)
            return
        }
        val today = LocalDate.now()
        val day = DoseScheduleCalculator.dayIndex(course.startLocalDate(), today)
        val takenToday = if (DoseScheduleCalculator.isActive(day, course.totalDays)) {
            doseDao.getAll()
                .filter { it.courseId == courseId && it.day == today.toEpochDay() }
                .map { it.doseMinutes }
                .toSet()
        } else {
            emptySet()
        }
        val next = DoseScheduleCalculator.nextDose(course, ZonedDateTime.now(), takenToday)
        if (next == null) {
            ReminderScheduler.cancelCourseDose(context, courseId)
        } else {
            val (moment, doseMinutes) = next
            ReminderScheduler.scheduleCourseDose(
                context = context,
                courseId = courseId,
                triggerAt = moment.toInstant().toEpochMilli(),
                doseMinutes = doseMinutes,
                dayEpoch = moment.toLocalDate().toEpochDay()
            )
        }
    }

    override suspend fun rescheduleAllReminders() {
        getCoursesOnce().forEach { rescheduleReminders(it.id) }
    }

    private fun CourseEntity.toDomain() = Course(
        id, title, note, startDateMillis, totalDays,
        intervalHours, intervalMinutes, firstDoseMinutes, createdAt
    )

    private fun DoseMarkEntity.toDomain() = DoseMark(id, courseId, day, doseMinutes)
}
