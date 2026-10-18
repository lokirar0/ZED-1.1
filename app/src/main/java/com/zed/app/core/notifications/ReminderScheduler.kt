package com.zed.app.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.time.ZonedDateTime

// ВСЕ напоминания ZED — на системном AlarmManager (exact RTC_WAKEUP).
// Цепочки: привычки, кредиты, приёмы курсов, snooze-повторы будильника.
object ReminderScheduler {

    private const val TAG = "ZED_REMINDER"

    const val ACTION_HABIT_TIME = "zed.action.HABIT_TIME"
    const val ACTION_HABITS_SUMMARY = "zed.action.HABITS_SUMMARY"
    const val ACTION_CREDITS = "zed.action.CREDITS"
    const val ACTION_COURSE_DOSE = "zed.action.COURSE_DOSE"
    const val ACTION_SNOOZE = "zed.action.SNOOZE"
    const val ACTION_SNOOZE_FIRE = "zed.action.SNOOZE_FIRE"
    const val ACTION_DISMISS = "zed.action.DISMISS"

    const val KEY_HABIT_ID = "habit_id"
    const val KEY_TIME = "time_minutes"
    const val KEY_COURSE_ID = "course_id"
    const val KEY_DOSE_MIN = "dose_min"
    const val KEY_DOSE_DAY = "dose_day"
    const val KEY_NOTIF_ID = "notif_id"
    const val KEY_TITLE = "payload_title"
    const val KEY_TIME_LABEL = "payload_time"

    private const val CODE_SUMMARY = 1001
    private const val CODE_CREDITS = 1002
    private const val CODE_HABIT_BASE = 2000
    private const val CODE_COURSE_BASE = 3000
    private const val CODE_SNOOZE_BASE = 5000

    fun scheduleHabits(context: Context) {
        setAlarm(context, ACTION_HABITS_SUMMARY, CODE_SUMMARY, nextTriggerAt(20, 0))
    }

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

    fun scheduleCourseDose(
        context: Context,
        courseId: Int,
        triggerAt: Long,
        doseMinutes: Int,
        dayEpoch: Long
    ) {
        setAlarm(
            context = context,
            action = ACTION_COURSE_DOSE,
            requestCode = CODE_COURSE_BASE + courseId,
            triggerAt = triggerAt,
            extras = mapOf(
                KEY_COURSE_ID to courseId,
                KEY_DOSE_MIN to doseMinutes,
                KEY_DOSE_DAY to dayEpoch.toInt()
            )
        )
    }

    fun cancelCourseDose(context: Context, courseId: Int) {
        cancelAlarm(context, ACTION_COURSE_DOSE, CODE_COURSE_BASE + courseId)
    }

    // Snooze: повторный показ будильника через N минут (кнопка «ПОДОЖДАТЬ»)
    fun scheduleSnoozeFire(
        context: Context,
        notifId: Int,
        triggerAt: Long,
        courseId: Int,
        doseMin: Int,
        dayEpoch: Int,
        title: String,
        timeLabel: String
    ) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ACTION_SNOOZE_FIRE)
            .putExtra(KEY_NOTIF_ID, notifId)
            .putExtra(KEY_COURSE_ID, courseId)
            .putExtra(KEY_DOSE_MIN, doseMin)
            .putExtra(KEY_DOSE_DAY, dayEpoch)
            .putExtra(KEY_TITLE, title)
            .putExtra(KEY_TIME_LABEL, timeLabel)
        val pi = PendingIntent.getBroadcast(
            context,
            CODE_SNOOZE_BASE + notifId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (canExact(am)) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        Log.d(TAG, "snooze scheduled notif=$notifId at=$triggerAt")
    }

    fun cancelSnoozeFire(context: Context, notifId: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(
            context,
            CODE_SNOOZE_BASE + notifId,
            Intent(context, ReminderReceiver::class.java).setAction(ACTION_SNOOZE_FIRE),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) am.cancel(pi)
    }

    // --- приватное ---

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
