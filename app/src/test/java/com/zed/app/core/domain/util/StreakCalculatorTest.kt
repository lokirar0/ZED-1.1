package com.zed.app.core.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test

class StreakCalculatorTest {

    @Test
    fun `current streak counts consecutive days including today`() {
        assertEquals(3, StreakCalculator.currentStreak(setOf(100, 99, 98), today = 100))
    }

    @Test
    fun `current streak stays alive if today not marked yet`() {
        assertEquals(3, StreakCalculator.currentStreak(setOf(99, 98, 97), today = 100))
    }

    @Test
    fun `current streak is zero after a gap`() {
        assertEquals(0, StreakCalculator.currentStreak(setOf(95, 94), today = 100))
    }

    @Test
    fun `best streak finds the longest run`() {
        assertEquals(4, StreakCalculator.bestStreak(setOf(1, 2, 3, 4, 10, 11)))
    }

    @Test
    fun `best streak of empty set is zero`() {
        assertEquals(0, StreakCalculator.bestStreak(emptySet()))
    }
}
