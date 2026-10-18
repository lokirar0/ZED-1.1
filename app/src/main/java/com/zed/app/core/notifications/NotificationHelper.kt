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
//
// ЗВУК: хранится и применяется в устойчивой форме
// «android.resource://пакет/raw/имя_файла». Числовая форма
// («.../2131756032») нормализуется автоматически — OxygenOS и ряд оболочек
// молча откатывают числовой Uri на системный звук.
// Канал будильников использует USAGE_ALARM (поток будильников, надёжнее и громче).
//
// ВИБРАЦИЯ: ритм-паттерн VIBRO_RHYTHM (пульс-пульс-пульс… пауза) —
// единый для каналов и полноэкранного будильника.
object NotificationHelper {

    const val CHANNEL_REMINDERS = "zed_reminders"
    const val CHANNEL_ALARMS = "zed_alarms"
    const val ID_HABITS = 1001
    const val ID_CREDITS = 1002

    // Ритм вибрации «в такт»: три коротких пульса, пауза, повтор
    val VIBRO_RHYTHM: LongArray = longArrayOf(0, 100, 100, 100, 100, 100, 300)

    // Приводит Uri звука к форме raw/имя. Числовой id → имя ресурса.
    fun normalizeSoundUri(context: Context, uri: String): String {
        if (uri.isBlank()) return uri
        return runCatching {
            val parsed = Uri.parse(uri)
            val last = parsed.lastPathSegment ?: return@runCatching uri
            // Уже именованная форма (raw/pace_np) — не трогаем
            if (last.toIntOrNull() == null) return@runCatching uri
            val id = last.toInt()
            val name = context.resources.getResourceEntryName(id)
            "android.resource://${context.packageName}/raw/$name"
        }.getOrDefault(uri)
    }

    fun ensureChannels(context: Context, soundUri: String?) {
        val manager = context.getSystemService(NotificationManager::class.java)
        ensureChannel(
            context, manager, CHANNEL_REMINDERS,
            context.getString(R.string.notif_channel),
            NotificationManager.IMPORTANCE_HIGH,
            soundUri,
            bypassDnd = false,
            usage = AudioAttributes.USAGE_NOTIFICATION
        )
        ensureChannel(
            context, manager, CHANNEL_ALARMS,
            context.getString(R.string.notif_alarm_channel),
            NotificationManager.IMPORTANCE_HIGH,
            soundUri,
            bypassDnd = true,
            usage = AudioAttributes.USAGE_ALARM
        )
    }

    private fun ensureChannel(
        context: Context,
        manager: NotificationManager,
        id: String,
        name: String,
        importance: Int,
        soundUri: String?,
        bypassDnd: Boolean,
        usage: Int
    ) {
        val normalized = normalizeSoundUri(context, soundUri.orEmpty())
        val desired: Uri? = normalized.takeIf { it.isNotBlank() }
            ?.let { runCatching { Uri.parse(it) }.getOrNull() }
        val existing = manager.getNotificationChannel(id)
        if (existing != null) {
            val sameSound = when {
                desired == null && existing.sound == null -> true
                desired != null && desired == existing.sound -> true
                else -> false
            }
            val sameVib = existing.vibrationPattern?.contentEquals(VIBRO_RHYTHM) ?: false
            if (sameSound && sameVib) return
            // Звук или ритм изменились — пересоздаём канал
            manager.deleteNotificationChannel(id)
        }
        val channel = NotificationChannel(id, name, importance)
        if (desired != null) {
            channel.setSound(
                desired,
                AudioAttributes.Builder()
                    .setUsage(usage)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }
        // Вибрация в такт: паттерн канала = ритм ZED
        channel.vibrationPattern = VIBRO_RHYTHM
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
            .setContentContentSafe(text)
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

// Внутренний хелпер: setContentText без риска null
private fun NotificationCompat.Builder.setContentContentSafe(text: String): NotificationCompat.Builder =
    this.setContentText(text)
