package com.zed.app.core.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zed.app.R
import com.zed.app.core.domain.repository.HabitRepository
import com.zed.app.core.settings.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import kotlinx.coroutines.flow.first

// Персональное напоминание привычки в заданное пользователем время.
// Не приходит, если привычка уже отмечена сегодня.
@HiltWorker
class HabitTimeWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val habitRepository: HabitRepository,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_HABIT_ID = "habit_id"
        // Уведомления привычек: 2000 + habitId (не пересекается с 1001/1002)
        const val NOTIF_ID_BASE = 2000
    }

    override suspend fun doWork(): Result {
        // Уведомления выключены глобально — молчим
        if (!settingsRepository.settings.first().notificationsEnabled) return Result.success()

        val habitId = inputData.getInt(KEY_HABIT_ID, -1)
        if (habitId == -1) return Result.success()

        val habit = habitRepository.getHabit(habitId) ?: return Result.success()

        // Уже отмечено сегодня — напоминание не нужно
        val today = LocalDate.now().toEpochDay()
        if (habitRepository.isCompleted(habitId, today)) return Result.success()

        NotificationHelper.show(
            context = context,
            notificationId = NOTIF_ID_BASE + habitId,
            title = context.getString(R.string.notif_habit_title),
            text = context.getString(R.string.notif_habit_body, habit.title)
        )
        return Result.success()
    }
}
