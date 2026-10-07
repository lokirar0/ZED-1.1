package com.zed.app.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.zed.app.R

// Канал и показ локальных уведомлений (без облака, без FCM)
object NotificationHelper {

    const val CHANNEL_ID = "zed_reminders"
    const val ID_HABITS = 1001
    const val ID_CREDITS = 1002

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notif_channel),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }
    }

    // Универсальный показ: id позволяет держать несколько независимых уведомлений
    fun show(context: Context, notificationId: Int, title: String, text: String) {
        ensureChannel(context)
        val compat = NotificationManagerCompat.from(context)
        if (!compat.areNotificationsEnabled()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setSilent(false)
            .build()
        compat.notify(notificationId, notification)
    }
}
