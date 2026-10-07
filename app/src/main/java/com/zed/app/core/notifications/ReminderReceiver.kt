package com.zed.app.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.zed.app.R
import com.zed.app.core.domain.repository.CreditRepository
import com.zed.app.core.domain.repository.HabitRepository
import com.zed.app.core.domain.util.PaymentCalculator
import com.zed.app.core.settings.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Точка входа системных будильников: показывает уведомление и переставляет
// себя на следующие сутки. Работает даже при убитом процессе приложения.
@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ZED_REMINDER"
    }

    @Inject lateinit var habitRepository: HabitRepository
    @Inject lateinit var creditRepository: CreditRepository
    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    ReminderScheduler.ACTION_HABIT_TIME -> {
                        val id = intent.getIntExtra(ReminderScheduler.KEY_HABIT_ID, -1)
                        val minutes = intent.getIntExtra(ReminderScheduler.KEY_TIME, -1)
                        // Сначала переставляем на завтра — цепочка не рвётся даже при сбое
                        if (id != -1 && minutes != -1) {
                            ReminderScheduler.scheduleHabitReminder(context, id, minutes)
                        }
                        handleHabitTime(context, id)
                    }
                    ReminderScheduler.ACTION_HABITS_SUMMARY -> {
                        ReminderScheduler.scheduleHabits(context)
                        handleHabitsSummary(context)
                    }
                    ReminderScheduler.ACTION_CREDITS -> {
                        ReminderScheduler.scheduleCredits(context)
                        handleCredits(context)
                    }
                }
            } catch (t: Throwable) {
                Log.w(TAG, "handle failed: $t")
            } finally {
                pending.finish()
            }
        }
    }

    // --- обработчики ---

    private suspend fun notificationsEnabled(): Boolean =
        settingsRepository.settings.first().notificationsEnabled

    // Персональное напоминание привычки
    private suspend fun handleHabitTime(context: Context, habitId: Int) {
        if (habitId == -1 || !notificationsEnabled()) return
        val habit = habitRepository.getHabit(habitId) ?: return
        val today = LocalDate.now().toEpochDay()
        // Уже отмечено сегодня — молчим (так задумано)
        if (habitRepository.isCompleted(habitId, today)) {
            Log.d(TAG, "habit $habitId already done today — silent")
            return
        }
        NotificationHelper.show(
            context = context,
            notificationId = 2000 + habitId,
            title = context.getString(R.string.notif_habit_title),
            text = context.getString(R.string.notif_habit_body, habit.title)
        )
        Log.d(TAG, "habit $habitId notified")
    }

    // Сводка по привычкам в 20:00
    private suspend fun handleHabitsSummary(context: Context) {
        if (!notificationsEnabled()) return
        val pendingCount = habitRepository.todayPendingCount()
        if (pendingCount > 0) {
            NotificationHelper.show(
                context = context,
                notificationId = NotificationHelper.ID_HABITS,
                title = context.getString(R.string.notif_title),
                text = context.getString(R.string.notif_body, pendingCount)
            )
            Log.d(TAG, "habits summary notified: $pendingCount pending")
        }
    }

    // Кредиты в 09:00: платёж ≤ 3 дней или просрочка
    private suspend fun handleCredits(context: Context) {
        if (!notificationsEnabled()) return
        val today = LocalDate.now()
        val payments = creditRepository.getPaymentsOnce().groupBy { it.creditId }
        val due = creditRepository.getCreditsOnce().mapNotNull { credit ->
            val paidMonths = payments[credit.id]?.map { it.yearMonth }?.toSet() ?: emptySet()
            val (date, paidThisMonth) = PaymentCalculator.nextUnpaid(credit.paymentDay, today, paidMonths)
            if (paidThisMonth) return@mapNotNull null
            val days = PaymentCalculator.daysUntil(date, today)
            if (days <= 3) credit.title to days else null
        }
        if (due.isEmpty()) return
        val overdue = due.any { it.second < 0 }
        val first = due.minByOrNull { it.second } ?: return
        val text = if (overdue) {
            context.getString(R.string.notif_credits_overdue, first.first)
        } else {
            context.getString(R.string.notif_credits_body, first.second.toInt(), first.first)
        }
        NotificationHelper.show(
            context = context,
            notificationId = NotificationHelper.ID_CREDITS,
            title = context.getString(R.string.notif_credits_title),
            text = text
        )
        Log.d(TAG, "credits notified: ${first.first}")
    }
}
