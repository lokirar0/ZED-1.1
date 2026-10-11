package com.zed.app.core.domain.util

import com.zed.app.core.domain.model.Transaction
import com.zed.app.core.domain.model.TransactionType

// ZED 2.0: остатки по счетам. Перевод (TRANSFER) уменьшает accountId и увеличивает linkedAccountId;
// доход/расход меняют только accountId. Операции без счёта ни на что не влияют.
object AccountBalanceCalculator {

    // accountStartMinor: id → стартовый остаток счёта
    fun balances(accountStartMinor: Map<Int, Long>, transactions: List<Transaction>): Map<Int, Long> {
        val result = accountStartMinor.toMutableMap()
        for (t in transactions) {
            when (t.type) {
                TransactionType.INCOME -> t.accountId?.let { result[it] = (result[it] ?: 0L) + t.amountMinor }
                TransactionType.EXPENSE -> t.accountId?.let { result[it] = (result[it] ?: 0L) - t.amountMinor }
                TransactionType.TRANSFER -> {
                    t.accountId?.let { result[it] = (result[it] ?: 0L) - t.amountMinor }
                    t.linkedAccountId?.let { result[it] = (result[it] ?: 0L) + t.amountMinor }
                }
            }
        }
        return result
    }

    // Суммарный капитал по всем счетам
    fun total(balances: Map<Int, Long>): Long = balances.values.sum()
}
