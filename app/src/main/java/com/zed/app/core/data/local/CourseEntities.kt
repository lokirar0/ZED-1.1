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

// Курс приёма: препарат/добавка с датой старта, длительностью и интервалом приёмов
@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val note: String = "",
    val startDateMillis: Long,     // день старта (локальный)
    val totalDays: Int,            // длительность курса в днях (day_of_course 1..N)
    val intervalHours: Int,        // интервал приёмов, часов (настраиваемо: 2, 4, 8...)
    val firstDoseMinutes: Int,     // первый приём дня, минут от полуночи (08:00 = 480)
    val createdAt: Long = System.currentTimeMillis()
)

// Отметка «принято»: курс + день + плановое время приёма
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
    val day: Long,          // LocalDate.toEpochDay()
    val doseMinutes: Int    // плановое время приёма в минутах от полуночи
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
