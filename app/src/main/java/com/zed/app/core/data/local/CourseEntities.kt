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

// Курс приёма. Интервал = intervalHours часов + intervalMinutes минут
// (колонка intervalMinutes добавлена миграцией 5→6, дефолт 0)
@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val note: String = "",
    val startDateMillis: Long,
    val totalDays: Int,
    val intervalHours: Int,
    val intervalMinutes: Int = 0,
    val firstDoseMinutes: Int,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "dose_marks",
    foreignKeys = [ForeignKey(
        entity = CourseEntity::class,
        parentColumns = ["id"],
        childColumns = ["courseId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["courseId", "day", "doseMinutes"], unique = true)]
)
data class DoseMarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val courseId: Int,
    val day: Long,
    val doseMinutes: Int
)

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses ORDER BY startDateMillis ASC")
    fun observe(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses ORDER BY startDateMillis ASC")
    suspend fun getAll(): List<CourseEntity>

    @Query("SELECT * FROM courses WHERE id = :id")
    suspend fun get(id: Int): CourseEntity?

    @Upsert
    suspend fun upsert(course: CourseEntity): Long

    @Query("DELETE FROM courses WHERE id = :id")
    suspend fun delete(id: Int)
}

@Dao
interface DoseDao {
    @Query("SELECT * FROM dose_marks")
    fun observe(): Flow<List<DoseMarkEntity>>

    @Query("SELECT * FROM dose_marks")
    suspend fun getAll(): List<DoseMarkEntity>

    @Query("SELECT * FROM dose_marks WHERE courseId = :courseId AND day = :day AND doseMinutes = :doseMinutes LIMIT 1")
    suspend fun find(courseId: Int, day: Long, doseMinutes: Int): DoseMarkEntity?

    @Insert
    suspend fun insert(entity: DoseMarkEntity)

    @Query("DELETE FROM dose_marks WHERE courseId = :courseId AND day = :day AND doseMinutes = :doseMinutes")
    suspend fun delete(courseId: Int, day: Long, doseMinutes: Int)
}
