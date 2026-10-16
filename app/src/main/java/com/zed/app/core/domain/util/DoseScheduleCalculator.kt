package com.zed.app.core.domain.util

import com.zed.app.core.domain.model.Course
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

// Чистая логика расписания приёмов (покрывается unit-тестами, без Android-API).
// Аналог схемы из ТЗ: day_of_course → schedule_array на текущий день.
object DoseScheduleCalculator {

    // День курса: 1..N; <=0 = ещё не начался; >N = завершён
    fun dayIndex(start: LocalDate, today: LocalDate): Int =
        ChronoUnit.DAYS.between(start, today).toInt() + 1

    fun isActive(day: Int, totalDays: Int): Boolean = day in 1..totalDays

    // Массив времён приёма на день (минуты от полуночи):
    // первый приём + шаг intervalHours, пока не выйдет за пределы суток
    fun dosesForDay(firstDoseMinutes: Int, intervalHours: Int): List<Int> {
        val step = (intervalHours * 60).coerceAtLeast(60)
        val list = mutableListOf<Int>()
        var t = firstDoseMinutes.coerceIn(0, 23 * 60 + 59)
        while (t < 24 * 60) {
            list.add(t)
            t += step
        }
        return list
    }

    // Ближайший будущий приём: сегодня (непройденное и неотмеченное время),
    // иначе первый приём завтрашнего дня, если курс ещё идёт.
    // Возвращает момент + минуты приёма, либо null (курс неактивен).
    fun nextDose(
        course: Course,
        now: ZonedDateTime,
        takenTodayMinutes: Set<Int>
    ): Pair<ZonedDateTime, Int>? {
        val today = now.toLocalDate()
        val day = dayIndex(course.startLocalDate(), today)
        val doses = dosesForDay(course.firstDoseMinutes, course.intervalHours)
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

    // Человекочитаемое время: 480 → "08:00"
    fun formatMinutes(minutes: Int): String =
        "%02d:%02d".format(minutes / 60, minutes % 60)
}
