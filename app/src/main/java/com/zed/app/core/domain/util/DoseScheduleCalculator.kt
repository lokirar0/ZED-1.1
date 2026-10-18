package com.zed.app.core.domain.util

import com.zed.app.core.domain.model.Course
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

// Чистая логика расписания приёмов: day_of_course → schedule_array на день.
// Интервал задаётся в МИНУТАХ (часы*60 + минуты), минимум 10 минут.
object DoseScheduleCalculator {

    fun dayIndex(start: LocalDate, today: LocalDate): Int =
        ChronoUnit.DAYS.between(start, today).toInt() + 1

    fun isActive(day: Int, totalDays: Int): Boolean = day in 1..totalDays

    // Времена приёма на день (минуты от полуночи): первый приём + шаг интервала
    fun dosesForDay(firstDoseMinutes: Int, intervalHours: Int, intervalMinutes: Int = 0): List<Int> {
        val step = (intervalHours * 60 + intervalMinutes).coerceAtLeast(10)
        val list = mutableListOf<Int>()
        var t = firstDoseMinutes.coerceIn(0, 23 * 60 + 59)
        while (t < 24 * 60) {
            list.add(t)
            t += step
        }
        return list
    }

    // Удобная обёртка для курса
    fun dosesFor(course: Course): List<Int> =
        dosesForDay(course.firstDoseMinutes, course.intervalHours, course.intervalMinutes)

    // Ближайший будущий приём: сегодня (непрошедшее и неотмеченное),
    // иначе первый приём завтра, если курс ещё идёт
    fun nextDose(
        course: Course,
        now: ZonedDateTime,
        takenTodayMinutes: Set<Int>
    ): Pair<ZonedDateTime, Int>? {
        val today = now.toLocalDate()
        val day = dayIndex(course.startLocalDate(), today)
        val doses = dosesFor(course)
        val nowMin = now.hour * 60 + now.minute

        if (isActive(day, course.totalDays)) {
            val dose = doses.firstOrNull { it >= nowMin && it !in takenTodayMinutes }
            if (dose != null) {
                return today.atTime(dose / 60, dose % 60).atZone(now.zone) to dose
            }
        }
        val tomorrow = day + 1
        if (isActive(tomorrow, course.totalDays)) {
            val first = doses.firstOrNull()
            if (first != null) {
                return today.plusDays(1).atTime(first / 60, first % 60).atZone(now.zone) to first
            }
        }
        return null
    }

    // 480 → "08:00"
    fun formatMinutes(minutes: Int): String =
        "%02d:%02d".format(minutes / 60, minutes % 60)

    // 150 → "2 ч 30 м"; 120 → "2 ч"; 45 → "45 м"
    fun formatInterval(totalMinutes: Int): String =
        when {
            totalMinutes >= 60 && totalMinutes % 60 == 0 -> "${totalMinutes / 60} ч"
            totalMinutes >= 60 -> "${totalMinutes / 60} ч ${totalMinutes % 60} м"
            else -> "$totalMinutes м"
        }
}
