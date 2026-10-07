package com.zed.app.core.export

import android.content.Context
import android.net.Uri
import com.zed.app.core.domain.model.Transaction
import com.zed.app.core.domain.util.MoneyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Экспорт операций в CSV через системный диалог сохранения (SAF).
// Имя категории передаёт вызывающий (резолвер из БД категорий).
object CsvExporter {

    // Разделитель ';' + BOM: файл сразу открывается в Excel корректно
    fun buildCsv(transactions: List<Transaction>, categoryName: (Transaction) -> String): String {
        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val sb = StringBuilder("\uFEFF")
        sb.appendLine("date;type;category;note;amount")
        transactions.forEach { t ->
            sb.appendLine(
                listOf(
                    dateFmt.format(Date(t.dateMillis)),
                    t.type.name,
                    escape(categoryName(t)),
                    escape(t.note),
                    MoneyFormatter.format(t.amountMinor).replace(",", ".")
                ).joinToString(";")
            )
        }
        return sb.toString()
    }

    fun write(context: Context, uri: Uri, csv: String): Boolean = runCatching {
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(csv.toByteArray(Charsets.UTF_8))
        } != null
    }.getOrDefault(false)

    // Экранирование полей с ';' или кавычками
    private fun escape(value: String): String =
        if (value.contains(';') || value.contains('"')) "\"${value.replace("\"", "\"\"")}\""
        else value
}
