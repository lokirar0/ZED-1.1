package com.zed.app.core.domain.repository

import com.zed.app.core.domain.model.Habit
import com.zed.app.core.domain.model.HabitCompletion
import kotlinx.coroutines.flow.Flow

interface HabitRepository {
    fun observeHabits(): Flow<List<Habit>>
    fun observeCompletions(): Flow<List<HabitCompletion>>
    suspend fun getHabit(id: Int): Habit?
    suspend fun getHabitsOnce(): List<Habit>
    suspend fun isCompleted(habitId: Int, day: Long): Boolean
    suspend fun upsert(habit: Habit): Long
    suspend fun delete(habitId: Int)
    // Переключить отметку за день (нет → поставить, есть → убрать)
    suspend fun toggle(habitId: Int, day: Long)
    // ZED 2.0: только поставить отметку (идемпотентно, для виджета/быстрой отметки)
    suspend fun complete(habitId: Int, day: Long)
    // ZED 2.0: архивирование без удаления истории
    suspend fun setArchived(habitId: Int, archived: Boolean)
    // Сколько привычек сегодня не отмечено (для уведомления)
    suspend fun todayPendingCount(): Int
}
