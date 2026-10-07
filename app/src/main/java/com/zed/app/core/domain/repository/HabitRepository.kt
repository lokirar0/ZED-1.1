package com.zed.app.core.domain.repository

import com.zed.app.core.domain.model.Habit
import com.zed.app.core.domain.model.HabitCompletion
import kotlinx.coroutines.flow.Flow

interface HabitRepository {
    fun observeHabits(): Flow<List<Habit>>
    fun observeCompletions(): Flow<List<HabitCompletion>>
    suspend fun getHabit(id: Int): Habit?
    suspend fun upsert(habit: Habit): Long
    suspend fun delete(habitId: Int)
    // Переключить отметку за день (нет → поставить, есть → убрать)
    suspend fun toggle(habitId: Int, day: Long)
    // Сколько привычек сегодня не отмечено (для уведомления)
    suspend fun todayPendingCount(): Int
}
