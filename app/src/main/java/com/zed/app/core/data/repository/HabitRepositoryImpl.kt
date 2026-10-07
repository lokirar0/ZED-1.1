package com.zed.app.core.data.repository

import com.zed.app.core.data.local.HabitCompletionDao
import com.zed.app.core.data.local.HabitCompletionEntity
import com.zed.app.core.data.local.HabitDao
import com.zed.app.core.data.local.HabitEntity
import com.zed.app.core.domain.model.Habit
import com.zed.app.core.domain.model.HabitCompletion
import com.zed.app.core.domain.repository.HabitRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class HabitRepositoryImpl @Inject constructor(
    private val habitDao: HabitDao,
    private val completionDao: HabitCompletionDao
) : HabitRepository {

    override fun observeHabits(): Flow<List<Habit>> =
        habitDao.observeHabits().map { list -> list.map { it.toDomain() } }

    override fun observeCompletions(): Flow<List<HabitCompletion>> =
        completionDao.observeCompletions().map { list -> list.map { it.toDomain() } }

    override suspend fun getHabit(id: Int): Habit? = habitDao.getHabit(id)?.toDomain()

    override suspend fun getHabitsOnce(): List<Habit> =
        habitDao.getHabitsOnce().map { it.toDomain() }

    override suspend fun isCompleted(habitId: Int, day: Long): Boolean =
        completionDao.find(habitId, day) != null

    override suspend fun upsert(habit: Habit): Long =
        habitDao.upsert(
            HabitEntity(
                id = habit.id,
                title = habit.title,
                note = habit.note,
                createdAt = habit.createdAt,
                reminderTimeMinutes = habit.reminderTimeMinutes
            )
        )

    override suspend fun delete(habitId: Int) = habitDao.delete(habitId)

    override suspend fun toggle(habitId: Int, day: Long) {
        val existing = completionDao.find(habitId, day)
        if (existing != null) completionDao.delete(habitId, day)
        else completionDao.insert(HabitCompletionEntity(habitId = habitId, day = day))
    }

    override suspend fun todayPendingCount(): Int {
        val today = LocalDate.now().toEpochDay()
        val doneIds = completionDao.getCompletionsOnce()
            .filter { it.day == today }
            .map { it.habitId }
            .toSet()
        return habitDao.getHabitsOnce().count { it.id !in doneIds }
    }

    private fun HabitEntity.toDomain() =
        Habit(id, title, note, createdAt, reminderTimeMinutes)

    private fun HabitCompletionEntity.toDomain() = HabitCompletion(id, habitId, day)
}
