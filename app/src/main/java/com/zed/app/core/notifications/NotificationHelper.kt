package com.zed.app.core.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.zed.app.R

// Каналы и показ уведомлений ZED.
// Два канала: обычные напоминания и «будильники» курсов (полноэкранные, DND-обход).
// Звук задаётся Uri из настроек; при смене звука канал пересоздаётся
// (Android фиксирует звук канала на момент создания).
object NotificationHelper {

    const val CHANNEL_REMINDERS = "zed_reminders"
    const val CHANNEL_ALARMS = "zed_alarms"
    const val ID_HABITS = 1001
    const val ID_CREDITS = 1002

    fun ensureChannels(context: Context, soundUri: String?) {
        val manager = context.getSystemService(NotificationManager::class.java)
        ensureChannel(
            context, manager, CHANNEL_REMINDERS,
            context.getString(R.string.notif_channel),
            NotificationManager.IMPORTANCE_HIGH,
            soundUri, bypassDnd = false
        )
        ensureChannel(
            context, manager, CHANNEL_ALARMS,
            context.getString(R.string.notif_alarm_channel),
            NotificationManager.IMPORTANCE_HIGH,
            soundUri, bypassDnd = true
        )
    }

    private fun ensureChannel(
        context: Context,
        manager: NotificationManager,
        id: String,
        name: String,
        importance: Int,
        soundUri: String?,
        bypassDnd: Boolean
    ) {
        val desired: Uri? = soundUri?.takeIf { it.isNotBlank() }
            ?.let { runCatching { Uri.parse(it) }.getOrNull() }
        val existing = manager.getNotificationChannel(id)
        if (existing != null) {
            val same = when {
                desired == null && existing.sound == null -> true
                desired != null && desired == existing.sound -> true
                else -> false
            }
            if (same) return
            // Звук изменился — пересоздаём канал с новым звуком
            manager.deleteNotificationChannel(id)
        }
        val channel = NotificationChannel(id, name, importance)
        if (desired != null) {
            channel.setSound(
                desired,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }
        channel.enableVibration(true)
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        if (bypassDnd) channel.setBypassDnd(true)
        manager.createNotificationChannel(channel)
    }

    // Обычное напоминание (привычки, кредиты, сводки)
    fun show(context: Context, notificationId: Int, title: String, text: String, soundUri: String? = null) {
        ensureChannels(context, soundUri)
        val compat = NotificationManagerCompat.from(context)
        if (!compat.areNotificationsEnabled()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .build()
        compat.notify(notificationId, notification)
    }

    // Будильник курса: полноэкранный алерт поверх блокировки + кнопки в шторке
    fun showAlarm(
        context: Context,
        notificationId: Int,
        title: String,
        text: String,
        soundUri: String?,
        fullScreenIntent: PendingIntent,
        actions: List<Pair<String, PendingIntent>>
    ) {
        ensureChannels(context, soundUri)
        val compat = NotificationManagerCompat.from(context)
        if (!compat.areNotificationsEnabled()) return
        val builder = NotificationCompat.Builder(context, CHANNEL_ALARMS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(fullScreenIntent)
            .setFullScreenIntent(fullScreenIntent, true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
        actions.forEach { (label, pi) -> builder.addAction(0, label, pi) }
        compat.notify(notificationId, builder.build())
    }

    fun cancel(context: Context, notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }
}
