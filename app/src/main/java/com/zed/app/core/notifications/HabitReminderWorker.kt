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
import kotlinx.coroutines.flow.first

// Ежедневная проверка в 20:00: есть неотмеченные привычки → уведомление
@HiltWorker
class HabitReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val repository: HabitRepository,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Уведомления выключены в настройках — ничего не показываем
        if (!settingsRepository.settings.first().notificationsEnabled) return Result.success()

        val pending = repository.todayPendingCount()
        if (pending > 0) {
            NotificationHelper.show(
                context = context,
                notificationId = NotificationHelper.ID_HABITS,
                title = context.getString(R.string.notif_title),
                text = context.getString(R.string.notif_body, pending)
            )
        }
        return Result.success()
    }
}
