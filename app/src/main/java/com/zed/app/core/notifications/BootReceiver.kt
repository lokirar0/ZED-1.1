package com.zed.app.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zed.app.core.settings.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// После перезагрузки WorkManager-задачи слетают — восстанавливаем оба напоминания
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (settingsRepository.settings.first().notificationsEnabled) {
                    ReminderScheduler.scheduleAll(context)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
