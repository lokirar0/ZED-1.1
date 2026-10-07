package com.zed.app.core.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.time.Duration
import java.time.LocalTime
import java.util.concurrent.TimeUnit

// Планировщик напоминаний:
// — глобальные: привычки 20:00 (сводка), кредиты 09:00
// — персональные: каждая привычка со своим временем (unique work "zed_habit_<id>")
object ReminderScheduler {

    private const val WORK_HABITS = "zed_habits_reminder"
    private const val WORK_CREDITS = "zed_credits_reminder"
    private const val WORK_HABIT_PREFIX = "zed_habit_"

    fun scheduleHabits(context: Context) {
        enqueue<HabitReminderWorker>(context, WORK_HABITS, LocalTime.of(20, 0))
    }

    fun scheduleCredits(context: Context) {
        enqueue<CreditReminderWorker>(context, WORK_CREDITS, LocalTime.of(9, 0))
    }

    fun scheduleAll(context: Context) {
        scheduleHabits(context)
        scheduleCredits(context)
    }

    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_HABITS)
        WorkManager.getInstance(context).cancelUniqueWork(WORK_CREDITS)
    }

    // Персональное напоминание привычки: ежедневно в timeMinutes от полуночи
    fun scheduleHabitReminder(context: Context, habitId: Int, timeMinutes: Int) {
        val request = PeriodicWorkRequestBuilder<HabitTimeWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(minutesUntilTime(timeMinutes), TimeUnit.MINUTES)
            .setInputData(workDataOf(HabitTimeWorker.KEY_HABIT_ID to habitId))
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_HABIT_PREFIX + habitId,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelHabitReminder(context: Context, habitId: Int) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_HABIT_PREFIX + habitId)
    }

    // --- приватное ---

    private inline fun <reified W : ListenableWorker> enqueue(
        context: Context,
        workName: String,
        time: LocalTime
    ) {
        val request = PeriodicWorkRequestBuilder<W>(1, TimeUnit.DAYS)
            .setInitialDelay(minutesUntil(time), TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            workName, ExistingPeriodicWorkPolicy.UPDATE, request
        )
    }

    private fun minutesUntil(target: LocalTime): Long {
        var minutes = Duration.between(LocalTime.now(), target).toMinutes()
        if (minutes < 0) minutes += 24 * 60
        return minutes
    }

    private fun minutesUntilTime(timeMinutes: Int): Long {
        val target = LocalTime.of(timeMinutes / 60, timeMinutes % 60)
        return minutesUntil(target)
    }
}
