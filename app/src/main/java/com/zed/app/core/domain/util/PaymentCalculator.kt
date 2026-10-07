package com.zed.app.core.domain.util

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.min

// Чистая логика дат платежей (покрывается unit-тестами)
object PaymentCalculator {

    // Дата платежа в месяце: день 31 в коротком месяце → последний день
    fun paymentDate(paymentDay: Int, month: YearMonth): LocalDate =
        month.atDay(min(paymentDay, month.lengthOfMonth()))

    // Следующая неоплаченная дата + флаг «текущий месяц оплачен»
    fun nextUnpaid(paymentDay: Int, today: LocalDate, paidMonths: Set<String>): Pair<LocalDate, Boolean> {
        val current = YearMonth.from(today)
        val paidThisMonth = paidMonths.contains(current.toString())
        val date = if (paidThisMonth) paymentDate(paymentDay, current.plusMonths(1))
                   else paymentDate(paymentDay, current)
        return date to paidThisMonth
    }

    // Дней до даты (отрицательно = просрочка)
    fun daysUntil(date: LocalDate, today: LocalDate): Long =
        ChronoUnit.DAYS.between(today, date)
}
