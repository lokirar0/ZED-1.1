package com.zed.app.core.domain.model

// Доменные модели без Room-аннотаций
data class Habit(
    val id: Int = 0,
    val title: String,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val reminderTimeMinutes: Int? = null, // минуты от полуночи, null = выключено
    val archived: Boolean = false         // ZED 2.0: архив скрыт с главного экрана
)

data class HabitCompletion(
    val id: Int = 0,
    val habitId: Int,
    val day: Long
)
