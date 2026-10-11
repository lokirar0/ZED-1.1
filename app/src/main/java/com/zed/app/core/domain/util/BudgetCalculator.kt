package com.zed.app.core.domain.util

import kotlin.math.roundToLong

// ZED 2.0: расчёт состояния месячного бюджета. Чистая логика, без Android — покрыта тестами.
enum class BudgetStatus { DISABLED, OK, WARNING, EXCEEDED }

data class BudgetInfo(
    val status: BudgetStatus,
    val spentMinor: Long,      // расходы за месяц (TRANSFER не учитывается)
    val limitMinor: Long,      // 0 = бюджет выключен
    val remainingMinor: Long,  // остаток до лимита (может быть < 0)
    val progress: Float,       // 0..1+ доля лимита
    val daysLeft: Int          // дней до конца месяца включительно
) {
    companion object {
        const val WARNING_RATIO = 0.8f
    }
}

object BudgetCalculator {

    fun info(spentMinor: Long, limitMinor: Long, dayOfMonth: Int, daysInMonth: Int): BudgetInfo {
        if (limitMinor <= 0L) {
            return BudgetInfo(BudgetStatus.DISABLED, spentMinor, 0L, 0L, 0f, daysLeft(dayOfMonth, daysInMonth))
        }
        val remaining = limitMinor - spentMinor
        val progress = (spentMinor.toFloat() / limitMinor.toFloat()).coerceAtLeast(0f)
        val status = when {
            spentMinor >= limitMinor -> BudgetStatus.EXCEEDED
            progress >= BudgetInfo.WARNING_RATIO -> BudgetStatus.WARNING
            else -> BudgetStatus.OK
        }
        return BudgetInfo(status, spentMinor, limitMinor, remaining, progress, daysLeft(dayOfMonth, daysInMonth))
    }

    private fun daysLeft(dayOfMonth: Int, daysInMonth: Int): Int =
        (daysInMonth - dayOfMonth + 1).coerceAtLeast(1)

    // Прогноз расходов на конец месяца по среднему темпу (линейная экстраполяция)
    fun projectedExpense(spentMinor: Long, dayOfMonth: Int, daysInMonth: Int): Long {
        if (dayOfMonth <= 0) return spentMinor
        return (spentMinor.toDouble() * daysInMonth / dayOfMonth).roundToLong()
    }
}
