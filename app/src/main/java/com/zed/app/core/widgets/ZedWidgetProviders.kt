package com.zed.app.core.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.zed.app.R
import com.zed.app.core.domain.model.TransactionType
import com.zed.app.core.domain.repository.HabitRepository
import com.zed.app.core.domain.repository.TransactionRepository
import com.zed.app.core.domain.util.MoneyFormatter
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// ============================================================
// Виджеты ZED на классических RemoteViews (стабильный системный API,
// без Glance и его версионных сюрпризов).
// Today: прогресс за день + точечная неделя.
// Budget: баланс, прогноз, полоса расхода.
// Данные читаются в фоне (goAsync), UI обновляется через updateAppWidget.
// ============================================================

@AndroidEntryPoint
class TodayWidgetProvider : AppWidgetProvider() {

    @Inject lateinit var habitRepository: HabitRepository

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val habits = habitRepository.getHabitsOnce()
                val completions = habitRepository.observeCompletions().first()
                val today = LocalDate.now().toEpochDay()
                val doneIds = completions.filter { it.day == today }.map { it.habitId }.toSet()
                val done = habits.count { it.id in doneIds }
                val week = (6 downTo 0).map { off -> completions.any { it.day == today - off } }

                val views = RemoteViews(context.packageName, R.layout.widget_today)
                views.setTextViewText(R.id.widget_today_count, "$done/${habits.size}")
                week.forEach { flag ->
                    views.addView(
                        R.id.widget_today_dots,
                        RemoteViews(
                            context.packageName,
                            if (flag) R.layout.widget_dot_on else R.layout.widget_dot_off
                        )
                    )
                }
                ids.forEach { mgr.updateAppWidget(it, views) }
            } finally {
                pending.finish()
            }
        }
    }
}

@AndroidEntryPoint
class BudgetWidgetProvider : AppWidgetProvider() {

    @Inject lateinit var transactionRepository: TransactionRepository

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val txs = transactionRepository.observeTransactions().first()
                val zone = ZoneId.systemDefault()
                val month = YearMonth.now()
                val today = LocalDate.now()
                val inMonth = txs.filter { YearMonth.from(Instant.ofEpochMilli(it.dateMillis).atZone(zone)) == month }
                val income = inMonth.filter { it.type == TransactionType.INCOME }.sumOf { it.amountMinor }
                val expense = inMonth.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountMinor }
                val balance = income - expense
                val dayOfMonth = today.dayOfMonth
                val projected = if (dayOfMonth > 0) expense * month.lengthOfMonth() / dayOfMonth else expense
                val forecast = income - projected
                val ratio = if (income > 0) (expense.toFloat() / income).coerceIn(0f, 1f) else 0f
                val filled = (ratio * 16).toInt()

                val views = RemoteViews(context.packageName, R.layout.widget_budget)
                views.setTextViewText(
                    R.id.widget_budget_balance,
                    (if (balance < 0) "-" else "") + MoneyFormatter.format(abs(balance))
                )
                views.setTextColor(
                    R.id.widget_budget_balance,
                    if (balance < 0) 0xFFD71921.toInt() else 0xFFE8E8E8.toInt()
                )
                views.setTextViewText(
                    R.id.widget_budget_forecast,
                    "FORECAST " + (if (forecast < 0) "-" else "") + MoneyFormatter.format(abs(forecast))
                )
                views.setTextColor(
                    R.id.widget_budget_forecast,
                    if (forecast < 0) 0xFFD71921.toInt() else 0xFF999999.toInt()
                )
                for (i in 0 until 16) {
                    views.addView(
                        R.id.widget_budget_bar,
                        RemoteViews(
                            context.packageName,
                            if (i < filled) R.layout.widget_seg_on else R.layout.widget_seg_off
                        )
                    )
                }
                ids.forEach { mgr.updateAppWidget(it, views) }
            } finally {
                pending.finish()
            }
        }
    }
}

// Рефрешер: дёргает onUpdate обоих виджетов при уходе приложения в фон
object ZedWidgetRefresher {
    fun update(context: Context) {
        refresh(context, TodayWidgetProvider::class.java)
        refresh(context, BudgetWidgetProvider::class.java)
    }

    private fun refresh(context: Context, cls: Class<out AppWidgetProvider>) {
        val mgr = AppWidgetManager.getInstance(context)
        val ids = mgr.getAppWidgetIds(ComponentName(context, cls))
        if (ids.isEmpty()) return
        val intent = Intent(context, cls).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        context.sendBroadcast(intent)
    }
}
