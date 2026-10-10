package com.zed.app.core.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// Активность пользователя за день: секунды в приложении + количество запусков
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val day: Long,      // LocalDate.toEpochDay()
    val seconds: Long = 0,
    val launches: Int = 0
)

// Цель по привычке: «отмечать N дней из 30»
@Entity(tableName = "habit_goals")
data class HabitGoalEntity(
    @PrimaryKey val habitId: Int,
    val targetDays: Int = 0         // 0 = цель не задана
)

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE day = :day LIMIT 1")
    suspend fun find(day: Long): SessionEntity?

    @Insert
    suspend fun insert(entity: SessionEntity)

    @Query("UPDATE sessions SET seconds = :seconds, launches = :launches WHERE day = :day")
    suspend fun update(day: Long, seconds: Long, launches: Int)

    @Query("SELECT * FROM sessions")
    fun observe(): Flow<List<SessionEntity>>
}

@Dao
interface HabitGoalDao {
    @Query("SELECT * FROM habit_goals")
    fun observe(): Flow<List<HabitGoalEntity>>

    @Query("SELECT * FROM habit_goals WHERE habitId = :id LIMIT 1")
    suspend fun find(id: Int): HabitGoalEntity?

    @Insert
    suspend fun insert(entity: HabitGoalEntity)

    @Query("UPDATE habit_goals SET targetDays = :target WHERE habitId = :id")
    suspend fun update(id: Int, target: Int)
}
