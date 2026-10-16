package com.zed.app.core.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// Доменная модель курса приёма
data class Course(
    val id: Int = 0,
    val title: String,
    val note: String = "",
    val startDateMillis: Long,
    val totalDays: Int,
    val intervalHours: Int,
    val firstDoseMinutes: Int,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun startLocalDate(): LocalDate =
        Instant.ofEpochMilli(startDateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
}

// Отметка приёма
data class DoseMark(
    val id: Int = 0,
    val courseId: Int,
    val day: Long,
    val doseMinutes: Int
)
