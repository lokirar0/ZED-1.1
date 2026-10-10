package com.zed.app.core.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorProvider
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.zed.app.MainActivity
import com.zed.app.core.domain.model.TransactionType
import com.zed.app.core.domain.repository.HabitRepository
import com.zed.app.core.domain.repository.TransactionRepository
import com.zed.app.core.domain.util.MoneyFormatter
import com.zed.app.core.domain.util.StreakCalculator
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Токены виджетов:.surface-подложка и акцент (True Black для AMOLED)
private val WSurface = ColorProvider(Color(0xFF111111), Color(0xFF111111))
private val WAccent = ColorProvider(Color(0xFFD71921), Color(0xFFD71921))
private val WText = ColorProvider(Color(0xFFE8E8E8), Color(0xFFE8E8E8))
private val WDim = ColorProvider(Color(0xFF333333), Color(0xFF333333))
private val WGray = ColorProvider(Color(0xFF999999), Color(0xFF999999))

// Hilt-доступ к репозиториям из виджетов (вне Compose-дерева приложения)
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetDataEntryPoint {
    fun habitRepository(): HabitRepository
    fun transactionRepository(): TransactionRepository
}

// ================= ВИДЖЕТ «TODAY» =================
// Прогресс привычек за день + точечная неделя. Тап = открыть приложение.
class TodayWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val ep = EntryPointAccessors.fromApplication(context, WidgetDataEntryPoint::class.java)
        val habits = ep.habitRepository().getHabitsOnce()
        val completions = ep.habitRepository().observeCompletions().first()
        val today = LocalDate.now().toEpochDay()
        val doneIds = completions.filter { it.day == today }.map { it.habitId }.toSet()
        val done = habits.count { it.id in doneIds }
        val total = habits.size
        val week = (6 downTo 0).map { off ->
            val day = today - off
            completions.any { it.day == day }
        }

        content {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(WSurface)
                    .padding(12.dp)
                    .clickable(actionStartActivity<MainActivity>())
            ) {
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    Text(
                        text = "ZED · TODAY",
                        style = TextStyle(color = WGray, textSize = 10.sp, fontWeight = FontWeight.Bold)
                    )
                }
                Row(modifier = GlanceModifier.fillMaxWidth().padding(top = 4.dp)) {
                    Text(
                        text = "$done/$total",
                        style = TextStyle(color = WText, textSize = 28.sp, fontWeight = FontWeight.Bold)
                    )
                }
                Row(modifier = GlanceModifier.fillMaxWidth().padding(top = 8.dp)) {
                    week.forEach { flag ->
                        Box(
                            modifier = GlanceModifier
                                .size(8.dp)
                                .padding(1.dp)
                                .background(if (flag) WAccent else WDim)
                        )
                    }
                }
            }
        }
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}

// ================= ВИДЖЕТ «BUDGET» =================
// Баланс месяца + прогноз + полоса расхода. Тап = открыть приложение.
class BudgetWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val ep = EntryPointAccessors.fromApplication(context, WidgetDataEntryPoint::class.java)
        val txs = ep.transactionRepository().observeTransactions().first()
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

        content {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(WSurface)
                    .padding(12.dp)
                    .clickable(actionStartActivity<MainActivity>())
            ) {
                Text(
                    text = "ZED · BUDGET",
                    style = TextStyle(color = WGray, textSize = 10.sp, fontWeight = FontWeight.Bold)
                )
                Row(modifier = GlanceModifier.fillMaxWidth().padding(top = 4.dp)) {
                    Text(
                        text = (if (balance < 0) "-" else "") + MoneyFormatter.format(kotlin.math.abs(balance)),
                        style = TextStyle(
                            color = if (balance < 0) WAccent else WText,
                            textSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Text(
                    text = "FORECAST " + (if (forecast < 0) "-" else "") + MoneyFormatter.format(kotlin.math.abs(forecast)),
                    style = TextStyle(
                        color = if (forecast < 0) WAccent else WGray,
                        textSize = 10.sp
                    ),
                    modifier = GlanceModifier.padding(top = 2.dp)
                )
                // Полоса расхода: 16 квадратных сегментов
                Row(modifier = GlanceModifier.fillMaxWidth().padding(top = 8.dp)) {
                    for (i in 0 until 16) {
                        Box(
                            modifier = GlanceModifier
                                .width(8.dp)
                                .height(6.dp)
                                .padding(1.dp)
                                .background(if (i < filled) WAccent else WDim)
                        )
                    }
                }
            }
        }
    }
}

class BudgetWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BudgetWidget()
}

// ================= РЕФРЕШЕР =================
// Вызывается при уходе приложения в фон (MainActivity.onPause):
// виджеты показывают свежие данные сразу, как только пользователь вышел на домашний экран.
object ZedWidgetRefresher {
    fun update(context: Context) {
        CoroutineScope(Dispatchers.Default).launch {
            runCatching { TodayWidget().updateAll(context) }
            runCatching { BudgetWidget().updateAll(context) }
        }
    }
}
