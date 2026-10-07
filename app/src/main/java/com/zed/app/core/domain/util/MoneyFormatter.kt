package com.zed.app.core.domain.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

// Форматирование и разбор сумм без float-ошибок (храним копейки в Long)
object MoneyFormatter {

    // 123456 -> "1 234,56" (зависит от локали устройства)
    fun format(minor: Long): String {
        val value = BigDecimal(minor).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
        val nf = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
            isGroupingUsed = true
        }
        return nf.format(value)
    }

    // "1 234,56" / "1234.56" -> 123456; null если мусор или <= 0
    fun parseToMinor(input: String): Long? = runCatching {
        val normalized = input.replace(" ", "").replace(",", ".")
        val bd = BigDecimal(normalized).setScale(2, RoundingMode.HALF_UP)
        if (bd.signum() <= 0) null else bd.multiply(BigDecimal(100)).longValueExact()
    }.getOrNull()
}
