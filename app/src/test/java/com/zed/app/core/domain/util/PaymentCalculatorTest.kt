package com.zed.app.core.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class PaymentCalculatorTest {

    @Test
    fun `day 31 clamps to the last day of a short month`() {
        assertEquals(
            LocalDate.of(2026, 2, 28),
            PaymentCalculator.paymentDate(31, YearMonth.of(2026, 2))
        )
    }

    @Test
    fun `next unpaid date is in the current month`() {
        val (date, paid) = PaymentCalculator.nextUnpaid(15, LocalDate.of(2026, 10, 6), emptySet())
        assertEquals(LocalDate.of(2026, 10, 15), date)
        assertEquals(false, paid)
    }

    @Test
    fun `paid current month shifts the date to next month`() {
        val (date, paid) = PaymentCalculator.nextUnpaid(15, LocalDate.of(2026, 10, 6), setOf("2026-10"))
        assertEquals(LocalDate.of(2026, 11, 15), date)
        assertEquals(true, paid)
    }

    @Test
    fun `days until is negative when overdue`() {
        assertEquals(
            -5L,
            PaymentCalculator.daysUntil(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 6))
        )
    }
}
