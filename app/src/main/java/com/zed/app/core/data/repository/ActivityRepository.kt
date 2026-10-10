package com.zed.app.core.data.repository

import com.zed.app.core.data.local.HabitGoalDao
import com.zed.app.core.data.local.HabitGoalEntity
import com.zed.app.core.data.local.SessionDao
import com.zed.app.core.data.local.SessionEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

// Учёт активности: секунды/запуски по дням + цели привычек.
// Без upsert-SQL (ON CONFLICT недоступен на SQLite API 28) — find + insert/update.
@Singleton
class ActivityRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val goalDao: HabitGoalDao
) {

    fun observeSessions(): Flow<List<SessionEntity>> = sessionDao.observe()
    fun observeGoals(): Flow<List<HabitGoalEntity>> = goalDao.observe()

    // Запуск приложения (вызывается один раз в onCreate)
    suspend fun addLaunch(day: Long) {
        val existing = sessionDao.find(day)
        if (existing == null) sessionDao.insert(SessionEntity(day = day, seconds = 0, launches = 1))
        else sessionDao.update(day, existing.seconds, existing.launches + 1)
    }

    // Секунды сессии (вызывается в onPause)
    suspend fun addSeconds(day: Long, seconds: Long) {
        if (seconds <= 0) return
        val existing = sessionDao.find(day)
        if (existing == null) sessionDao.insert(SessionEntity(day = day, seconds = seconds, launches = 0))
        else sessionDao.update(day, existing.seconds + seconds, existing.launches)
    }

    // Цель привычки: 0..30 дней из 30
    suspend fun setGoal(habitId: Int, targetDays: Int) {
        val target = targetDays.coerceIn(0, 30)
        val existing = goalDao.find(habitId)
        if (existing == null) goalDao.insert(HabitGoalEntity(habitId, target))
        else goalDao.update(habitId, target)
    }
}
