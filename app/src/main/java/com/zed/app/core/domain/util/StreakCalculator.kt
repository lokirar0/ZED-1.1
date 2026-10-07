package com.zed.app.core.domain.util

// Чистая логика серий. Без Android-зависимостей — покрыта unit-тестами.
object StreakCalculator {

    // Текущая серия: считаем от сегодня; если сегодня ещё не отмечено —
    // серия не сгорает, считаем от вчера.
    fun currentStreak(days: Set<Long>, today: Long): Int {
        var day = if (days.contains(today)) today else today - 1
        var count = 0
        while (days.contains(day)) {
            count++
            day--
        }
        return count
    }

    // Рекордная серия за всё время
    fun bestStreak(days: Set<Long>): Int {
        if (days.isEmpty()) return 0
        val sorted = days.sorted()
        var best = 1
        var run = 1
        for (i in 1 until sorted.size) {
            run = if (sorted[i] == sorted[i - 1] + 1) run + 1 else 1
            if (run > best) best = run
        }
        return best
    }
}
