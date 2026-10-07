package com.zed.app.core.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

// Привычка
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

// Отметка выполнения за день (day = LocalDate.toEpochDay())
@Entity(
    tableName = "habit_completions",
    foreignKeys = [ForeignKey(
        entity = HabitEntity::class,
        parentColumns = ["id"],
        childColumns = ["habitId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("habitId")]
)
data class HabitCompletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val habitId: Int,
    val day: Long
)

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY createdAt ASC")
    fun observeHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabit(id: Int): HabitEntity?

    @Query("SELECT * FROM habits ORDER BY createdAt ASC")
    suspend fun getHabitsOnce(): List<HabitEntity>

    @Upsert
    suspend fun upsert(habit: HabitEntity): Long

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun delete(id: Int)

    // Бэкап
    @Query("SELECT * FROM habits")
    suspend fun getAll(): List<HabitEntity>

    @Query("DELETE FROM habits")
    suspend fun clear()

    @Insert
    suspend fun insertAll(list: List<HabitEntity>)
}

@Dao
interface HabitCompletionDao {
    @Query("SELECT * FROM habit_completions")
    fun observeCompletions(): Flow<List<HabitCompletionEntity>>

    @Query("SELECT * FROM habit_completions")
    suspend fun getCompletionsOnce(): List<HabitCompletionEntity>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND day = :day LIMIT 1")
    suspend fun find(habitId: Int, day: Long): HabitCompletionEntity?

    @Insert
    suspend fun insert(completion: HabitCompletionEntity)

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId AND day = :day")
    suspend fun delete(habitId: Int, day: Long)

    // Бэкап
    @Query("DELETE FROM habit_completions")
    suspend fun clear()

    @Insert
    suspend fun insertAll(list: List<HabitCompletionEntity>)
}
