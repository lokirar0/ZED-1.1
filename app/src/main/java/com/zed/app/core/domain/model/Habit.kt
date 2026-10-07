package com.zed.app.core.domain.model

// Доменные модели без Room-аннотаций
data class Habit(
    val id: Int = 0,
    val title: String,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class HabitCompletion(
    val id: Int = 0,
    val habitId: Int,
    val day: Long
)
