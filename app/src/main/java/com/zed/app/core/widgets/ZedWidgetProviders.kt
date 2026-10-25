package com.zed.app.core.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.zed.app.MainActivity
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
// Виджеты TODAY и BUDGET на чистых RemoteViews (БЕЗ Glance):
// никаких content-лямбд, которые ломал автоперевод при вставке.
// Стиль тот же: True Black подложка, точечные цифры, красные точки.
// Обновление: рефрешер при выходе из приложения + системный цикл.
// ============================================================

private const val COLOR_BG = 0xFF111111.toInt()
private const val COLOR_TEXT = 0xFFE8E8E8.toInt()
private const val COLOR_GRAY = 0xFF999999.toInt()
private const val COLOR_ACCENT = 0xFFD71921.toInt()
private const val COLOR_DIM = 0xFF333333.toInt()

// ================= ВИДЖЕТ «TODAY» =================
@AndroidEntryPoint
class TodayWidgetProvider : android.appwidget.AppWidgetProvider() {

    @Inject lateinit var habitRepository: HabitRepository

    override fun onUpdate(
        context: Context,
        mgr: AppWidgetManager,
        ids: IntArray
    ) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                render(context, mgr, ids)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun render(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        val habits = habitRepository.getHabitsOnce()
        val completions = habitRepository.observeCompletions().first()
        val today = LocalDate.now().toEpochDay()
        val doneIds = completions.filter { it.day == today }
            .map { it.habitId }
            .toSet()
        val done = habits.count { it.id in doneIds }
        val total = habits.size
        val week = (6 downTo 0).map { off -> completions.any { it.day == today - off } }

        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_today)
            views.setTextViewText(R.id.widget_today_count, "$done/$total")
            views.removeAllViews(R.id.widget_today_week)
            week.forEach { flag ->
                val dot = RemoteViews(context.packageName, R.layout.widget_dot)
                dot.setInt(
                    R.id.widget_dot_view,
                    "setBackgroundColor",
                    if (flag) COLOR_ACCENT else COLOR_DIM
                )
                views.addView(R.id.widget_today_week, dot)
            }
            views.setOnClickPendingIntent(
                R.id.widget_today_root,
                openAppPi(context, 31)
            )
            mgr.updateAppWidget(id, views)
        }
    }
}

// ================= ВИДЖЕТ «BUDGET» =================
@AndroidEntryPoint
class BudgetWidgetProvider : android.appwidget.AppWidgetProvider() {

    @Inject lateinit var transactionRepository: TransactionRepository

    override fun onUpdate(
        context: Context,
        mgr: AppWidgetManager,
        ids: IntArray
    ) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                render(context, mgr, ids)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun render(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        val txs = transactionRepository.observeTransactions().first()
        val zone = ZoneId.systemDefault()
        val month = YearMonth.now()
        val today = LocalDate.now()
        val inMonth = txs.filter {
            YearMonth.from(Instant.ofEpochMilli(it.dateMillis).atZone(zone)) == month
        }
        val income = inMonth.filter { it.type == TransactionType.INCOME }
            .sumOf { it.amountMinor }
        val expense = inMonth.filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amountMinor }
        val balance = income - expense
        val day = today.dayOfMonth
        val projected = if (day > 0) expense * month.lengthOfMonth() / day else expense
        val forecast = income - projected
        val ratio = if (income > 0) (expense.toFloat() / income).coerceIn(0f, 1f) else 0f
        val filled = (ratio * 16).toInt()

        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_budget)
            views.setTextViewText(
                R.id.widget_budget_balance,
                (if (balance < 0) "-" else "") + MoneyFormatter.format(abs(balance))
            )
            views.setTextColor(
                R.id.widget_budget_balance,
                if (balance < 0) COLOR_ACCENT else COLOR_TEXT
            )
            views.setTextViewText(
                R.id.widget_budget_forecast,
                "FORECAST " + (if (forecast < 0) "-" else "") + MoneyFormatter.format(abs(forecast))
            )
            views.removeAllViews(R.id.widget_budget_dots)
            for (i in 0 until 16) {
                val dot = RemoteViews(context.packageName, R.layout.widget_dot)
                dot.setInt(
                    R.id.widget_dot_view,
                    "setBackgroundColor",
                    if (i < filled) COLOR_ACCENT else COLOR_DIM
                )
                views.addView(R.id.widget_budget_dots, dot)
            }
            views.setOnClickPendingIntent(
                R.id.widget_budget_root,
                openAppPi(context, 32)
            )
            mgr.updateAppWidget(id, views)
        }
    }
}

// ================= ОБЩИЙ ХЕЛПЕР =================
private fun openAppPi(context: Context, req: Int): android.app.PendingIntent =
    android.app.PendingIntent.getActivity(
        context,
        req,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
    )

// ================= РЕФРЕШЕР ВСЕХ ВИДЖЕТОВ =================
object ZedWidgetRefresher {
    fun update(context: Context) {
        refreshRemote(context, TodayWidgetProvider::class.java)
        refreshRemote(context, BudgetWidgetProvider::class.java)
        refreshRemote(context, PlayerWidgetProvider::class.java)
        refreshRemote(context, CreditsWidgetProvider::class.java)
    }

    private fun refreshRemote(
        context: Context,
        cls: Class<out android.appwidget.AppWidgetProvider>
    ) {
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
