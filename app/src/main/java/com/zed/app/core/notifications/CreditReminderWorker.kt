package com.zed.app.core.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zed.app.R
import com.zed.app.core.domain.repository.CreditRepository
import com.zed.app.core.domain.util.PaymentCalculator
import com.zed.app.core.settings.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import kotlinx.coroutines.flow.first

// Ежедневная проверка кредитов в 09:00: платёж в течение 3 дней или просрочен → уведомление
@HiltWorker
class CreditReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val creditRepository: CreditRepository,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!settingsRepository.settings.first().notificationsEnabled) return Result.success()

        val today = LocalDate.now()
        val payments = creditRepository.getPaymentsOnce().groupBy { it.creditId }
        val due = creditRepository.getCreditsOnce().mapNotNull { credit ->
            val paidMonths = payments[credit.id]?.map { it.yearMonth }?.toSet() ?: emptySet()
            val (date, paidThisMonth) = PaymentCalculator.nextUnpaid(credit.paymentDay, today, paidMonths)
            if (paidThisMonth) return@mapNotNull null
            val days = PaymentCalculator.daysUntil(date, today)
            if (days <= 3) credit.title to days else null
        }
        if (due.isEmpty()) return Result.success()

        val overdue = due.any { it.second < 0 }
        val first = due.minByOrNull { it.second } ?: return Result.success()
        val text = if (overdue) {
            context.getString(R.string.notif_credits_overdue, first.first)
        } else {
            context.getString(R.string.notif_credits_body, first.second.toInt(), first.first)
        }
        NotificationHelper.show(
            context,
            NotificationHelper.ID_CREDITS,
            context.getString(R.string.notif_credits_title),
            text
        )
        return Result.success()
    }
}
