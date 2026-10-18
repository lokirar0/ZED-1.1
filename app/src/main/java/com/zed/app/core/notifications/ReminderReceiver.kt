package com.zed.app.core.notifications

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.zed.app.R
import com.zed.app.core.domain.repository.CourseRepository
import com.zed.app.core.domain.repository.CreditRepository
import com.zed.app.core.domain.repository.HabitRepository
import com.zed.app.core.domain.util.DoseScheduleCalculator
import com.zed.app.core.domain.util.PaymentCalculator
import com.zed.app.core.settings.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Точка входа системных будильников: уведомления, полноэкранные алерты курсов,
// snooze/dismiss, перестановка цепочек. Работает при убитом процессе.
@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ZED_REMINDER"
        private const val NOTIF_COURSE_BASE = 4000
        private const val SNOOZE_MINUTES = 5L
    }

    @Inject lateinit var habitRepository: HabitRepository
    @Inject lateinit var creditRepository: CreditRepository
    @Inject lateinit var courseRepository: CourseRepository
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
                    ReminderScheduler.ACTION_COURSE_DOSE -> {
                        val courseId = intent.getIntExtra(ReminderScheduler.KEY_COURSE_ID, -1)
                        val doseMin = intent.getIntExtra(ReminderScheduler.KEY_DOSE_MIN, -1)
                        val dayEpoch = intent.getIntExtra(ReminderScheduler.KEY_DOSE_DAY, -1)
                        // Цепочка: сразу ставим следующий приём
                        if (courseId != -1) courseRepository.rescheduleReminders(courseId)
                        handleCourseDose(context, courseId, doseMin, dayEpoch.toLong())
                    }
                    ReminderScheduler.ACTION_SNOOZE -> {
                        val notifId = intent.getIntExtra(ReminderScheduler.KEY_NOTIF_ID, -1)
                        NotificationHelper.cancel(context, notifId)
                        ReminderScheduler.scheduleSnoozeFire(
                            context = context,
                            notifId = notifId,
                            triggerAt = System.currentTimeMillis() + SNOOZE_MINUTES * 60_000L,
                            courseId = intent.getIntExtra(ReminderScheduler.KEY_COURSE_ID, -1),
                            doseMin = intent.getIntExtra(ReminderScheduler.KEY_DOSE_MIN, -1),
                            dayEpoch = intent.getIntExtra(ReminderScheduler.KEY_DOSE_DAY, -1),
                            title = intent.getStringExtra(ReminderScheduler.KEY_TITLE).orEmpty(),
                            timeLabel = intent.getStringExtra(ReminderScheduler.KEY_TIME_LABEL).orEmpty()
                        )
                    }
                    ReminderScheduler.ACTION_SNOOZE_FIRE -> {
                        // Повторный показ будильника после snooze
                        showCourseAlarm(
                            context = context,
                            notifId = intent.getIntExtra(ReminderScheduler.KEY_NOTIF_ID, -1),
                            courseId = intent.getIntExtra(ReminderScheduler.KEY_COURSE_ID, -1),
                            doseMin = intent.getIntExtra(ReminderScheduler.KEY_DOSE_MIN, -1),
                            dayEpoch = intent.getIntExtra(ReminderScheduler.KEY_DOSE_DAY, -1).toLong(),
                            title = intent.getStringExtra(ReminderScheduler.KEY_TITLE).orEmpty(),
                            timeLabel = intent.getStringExtra(ReminderScheduler.KEY_TIME_LABEL).orEmpty()
                        )
                    }
                    ReminderScheduler.ACTION_DISMISS -> {
                        val notifId = intent.getIntExtra(ReminderScheduler.KEY_NOTIF_ID, -1)
                        NotificationHelper.cancel(context, notifId)
                        ReminderScheduler.cancelSnoozeFire(context, notifId)
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

    private suspend fun soundUri(): String =
        settingsRepository.settings.first().notificationSoundUri

    private suspend fun notificationsEnabled(): Boolean =
        settingsRepository.settings.first().notificationsEnabled

    private suspend fun handleHabitTime(context: Context, habitId: Int) {
        if (habitId == -1 || !notificationsEnabled()) return
        val habit = habitRepository.getHabit(habitId) ?: return
        val today = LocalDate.now().toEpochDay()
        if (habitRepository.isCompleted(habitId, today)) {
            Log.d(TAG, "habit $habitId already done today — silent")
            return
        }
        NotificationHelper.show(
            context = context,
            notificationId = 2000 + habitId,
            title = context.getString(R.string.notif_habit_title),
            text = context.getString(R.string.notif_habit_body, habit.title),
            soundUri = soundUri()
        )
    }

    private suspend fun handleHabitsSummary(context: Context) {
        if (!notificationsEnabled()) return
        val pendingCount = habitRepository.todayPendingCount()
        if (pendingCount > 0) {
            NotificationHelper.show(
                context = context,
                notificationId = NotificationHelper.ID_HABITS,
                title = context.getString(R.string.notif_title),
                text = context.getString(R.string.notif_body, pendingCount),
                soundUri = soundUri()
            )
        }
    }

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
            text = text,
            soundUri = soundUri()
        )
    }

    // Приём курса: полноэкранный будильник, если день актуален и доза не принята
    private suspend fun handleCourseDose(context: Context, courseId: Int, doseMin: Int, dayEpoch: Long) {
        if (courseId == -1 || doseMin == -1 || !notificationsEnabled()) return
        if (dayEpoch != LocalDate.now().toEpochDay()) {
            Log.d(TAG, "course $courseId dose stale — silent")
            return
        }
        val course = courseRepository.getCourse(courseId) ?: return
        if (courseRepository.isDoseTaken(courseId, dayEpoch, doseMin)) {
            Log.d(TAG, "course $courseId dose $doseMin taken — silent")
            return
        }
        val time = DoseScheduleCalculator.formatMinutes(doseMin)
        showCourseAlarm(
            context = context,
            notifId = NOTIF_COURSE_BASE + courseId * 100 + doseMin / 60,
            courseId = courseId,
            doseMin = doseMin,
            dayEpoch = dayEpoch,
            title = course.title,
            timeLabel = time
        )
    }

    // Сборка и показ алерта: fullScreenIntent на ZedAlarmActivity + кнопки snooze/dismiss
    private suspend fun showCourseAlarm(
        context: Context,
        notifId: Int,
        courseId: Int,
        doseMin: Int,
        dayEpoch: Long,
        title: String,
        timeLabel: String
    ) {
        if (notifId == -1) return
        val text = context.getString(R.string.notif_course_body, title, timeLabel)

        val payload = Intent(context, ReminderReceiver::class.java)
        payload.putExtra(ReminderScheduler.KEY_NOTIF_ID, notifId)
        payload.putExtra(ReminderScheduler.KEY_COURSE_ID, courseId)
        payload.putExtra(ReminderScheduler.KEY_DOSE_MIN, doseMin)
        payload.putExtra(ReminderScheduler.KEY_DOSE_DAY, dayEpoch.toInt())
        payload.putExtra(ReminderScheduler.KEY_TITLE, title)
        payload.putExtra(ReminderScheduler.KEY_TIME_LABEL, timeLabel)

        val fullScreen = PendingIntent.getActivity(
            context,
            notifId,
            Intent(context, ZedAlarmActivity::class.java).putExtras(payload.extras!!),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val snoozePi = PendingIntent.getBroadcast(
            context,
            notifId + 1,
            payload.setAction(ReminderScheduler.ACTION_SNOOZE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val dismissPi = PendingIntent.getBroadcast(
            context,
            notifId + 2,
            payload.setAction(ReminderScheduler.ACTION_DISMISS),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        NotificationHelper.showAlarm(
            context = context,
            notificationId = notifId,
            title = context.getString(R.string.notif_course_title),
            text = text,
            soundUri = soundUri(),
            fullScreenIntent = fullScreen,
            actions = listOf(
                context.getString(R.string.alarm_snooze) to snoozePi,
                context.getString(R.string.alarm_dismiss) to dismissPi
            )
        )
        Log.d(TAG, "course alarm shown: $title $timeLabel")
    }
}
