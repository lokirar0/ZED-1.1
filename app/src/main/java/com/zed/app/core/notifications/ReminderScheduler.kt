package com.zed.app.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.time.ZonedDateTime

// ВСЕ напоминания ZED — на системном AlarmManager (exact RTC_WAKEUP):
// срабатывают в настенное время даже при убитом процессе приложения.
// WorkManager для будильников не годится: OxygenOS откладывает фоновые задачи.
object ReminderScheduler {

    private const val TAG = "ZED_REMINDER"

    const val ACTION_HABIT_TIME = "zed.action.HABIT_TIME"
    const val ACTION_HABITS_SUMMARY = "zed.action.HABITS_SUMMARY"
    const val ACTION_CREDITS = "zed.action.CREDITS"

    const val KEY_HABIT_ID = "habit_id"
    const val KEY_TIME = "time_minutes"

    private const val CODE_SUMMARY = 1001
    private const val CODE_CREDITS = 1002
    private const val CODE_HABIT_BASE = 2000

    // Глобально: сводка привычек в 20:00
    fun scheduleHabits(context: Context) {
        setAlarm(context, ACTION_HABITS_SUMMARY, CODE_SUMMARY, nextTriggerAt(20, 0))
    }

    // Глобально: кредиты в 09:00
    fun scheduleCredits(context: Context) {
        setAlarm(context, ACTION_CREDITS, CODE_CREDITS, nextTriggerAt(9, 0))
    }

    fun scheduleAll(context: Context) {
        scheduleHabits(context)
        scheduleCredits(context)
    }

    fun cancelAll(context: Context) {
        cancelAlarm(context, ACTION_HABITS_SUMMARY, CODE_SUMMARY)
        cancelAlarm(context, ACTION_CREDITS, CODE_CREDITS)
    }

    // Персональное напоминание привычки: ежедневно в timeMinutes от полуночи
    fun scheduleHabitReminder(context: Context, habitId: Int, timeMinutes: Int) {
        setAlarm(
            context = context,
            action = ACTION_HABIT_TIME,
            requestCode = CODE_HABIT_BASE + habitId,
            triggerAt = nextTriggerAt(timeMinutes / 60, timeMinutes % 60),
            extras = mapOf(KEY_HABIT_ID to habitId, KEY_TIME to timeMinutes)
        )
    }

    fun cancelHabitReminder(context: Context, habitId: Int) {
        cancelAlarm(context, ACTION_HABIT_TIME, CODE_HABIT_BASE + habitId)
    }

    // --- приватное ---

    // Ближайшее будущее наступление HH:mm (сегодня или завтра)
    private fun nextTriggerAt(hour: Int, minute: Int): Long {
        val now = ZonedDateTime.now()
        var t = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!t.isAfter(now)) t = t.plusDays(1)
        return t.toInstant().toEpochMilli()
    }

    private fun canExact(am: AlarmManager): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()

    private fun setAlarm(
        context: Context,
        action: String,
        requestCode: Int,
        triggerAt: Long,
        extras: Map<String, Int> = emptyMap()
    ) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).setAction(action)
        extras.forEach { (k, v) -> intent.putExtra(k, v) }
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // Точный будильник; если точные запрещены системой — мягкий fallback
        if (canExact(am)) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
        Log.d(TAG, "set $action code=$requestCode at=$triggerAt exact=${canExact(am)}")
    }

    private fun cancelAlarm(context: Context, action: String, requestCode: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, ReminderReceiver::class.java).setAction(action),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) am.cancel(pi)
        Log.d(TAG, "cancel $action code=$requestCode")
    }
}
