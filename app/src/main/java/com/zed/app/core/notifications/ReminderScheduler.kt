package com.zed.app.core.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalTime
import java.util.concurrent.TimeUnit

// Планировщик ежедневных напоминаний: привычки в 20:00, кредиты в 09:00
object ReminderScheduler {

    private const val WORK_HABITS = "zed_habits_reminder"
    private const val WORK_CREDITS = "zed_credits_reminder"

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
}
