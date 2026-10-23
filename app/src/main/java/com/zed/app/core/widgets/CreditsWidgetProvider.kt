package com.zed.app.core.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.zed.app.MainActivity
import com.zed.app.R
import com.zed.app.core.domain.repository.CreditRepository
import com.zed.app.core.domain.util.MoneyFormatter
import com.zed.app.core.domain.util.PaymentCalculator
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Виджет «ZED · CREDITS»: список ближайших платежей по кредитам.
// Строка: название | статус (Д-N / ОПЛАЧЕНО / ПРОСРОЧЕНО) | сумма.
// Красным — просрочка и платежи ближе 3 дней, серым — оплачено.
@AndroidEntryPoint
class CreditsWidgetProvider : AppWidgetProvider() {

    @Inject lateinit var creditRepository: CreditRepository

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                render(context, appWidgetManager, appWidgetIds)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun render(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        val credits = creditRepository.getCreditsOnce()
        val payments = creditRepository.getPaymentsOnce().groupBy { it.creditId }
        val today = LocalDate.now()

        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_credits)
            views.removeAllViews(R.id.widget_credits_rows)

            if (credits.isEmpty()) {
                val row = RemoteViews(context.packageName, R.layout.widget_credit_row)
                row.setTextViewText(R.id.row_title, context.getString(R.string.widget_credits_empty))
                row.setTextViewText(R.id.row_status, "")
                row.setTextViewText(R.id.row_sum, "")
                views.addView(R.id.widget_credits_rows, row)
            } else {
                val sorted = credits.sortedBy { credit ->
                    val paidMonths = payments[credit.id]?.map { it.yearMonth }?.toSet() ?: emptySet()
                    val (date, paidThisMonth) =
                        PaymentCalculator.nextUnpaid(credit.paymentDay, today, paidMonths)
                    if (paidThisMonth) Long.MAX_VALUE else PaymentCalculator.daysUntil(date, today)
                }
                sorted.take(4).forEach { credit ->
                    val paidMonths = payments[credit.id]?.map { it.yearMonth }?.toSet() ?: emptySet()
                    val (date, paidThisMonth) =
                        PaymentCalculator.nextUnpaid(credit.paymentDay, today, paidMonths)
                    val days = PaymentCalculator.daysUntil(date, today)

                    val status = when {
                        paidThisMonth -> context.getString(R.string.widget_paid)
                        days < 0 -> context.getString(R.string.widget_overdue)
                        else -> context.getString(R.string.widget_day, days.toInt())
                    }
                    val statusColor = when {
                        paidThisMonth -> 0xFF999999.toInt()
                        days <= 3 -> 0xFFD71921.toInt()
                        else -> 0xFFE8E8E8.toInt()
                    }

                    val row = RemoteViews(context.packageName, R.layout.widget_credit_row)
                    row.setTextViewText(R.id.row_title, credit.title)
                    row.setTextViewText(R.id.row_status, status)
                    row.setTextColor(R.id.row_status, statusColor)
                    row.setTextViewText(R.id.row_sum, MoneyFormatter.format(credit.monthlyPaymentMinor))
                    views.addView(R.id.widget_credits_rows, row)
                }
            }

            views.setOnClickPendingIntent(
                R.id.widget_credits_root,
                PendingIntent.getActivity(
                    context,
                    21,
                    Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            mgr.updateAppWidget(id, views)
        }
    }
}
