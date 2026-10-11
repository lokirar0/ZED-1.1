package com.zed.app.core.domain.model

// Тип операции
enum class TransactionType { INCOME, EXPENSE, TRANSFER }

// Категории живут в БД (builtIn + кастомные), здесь только ссылка categoryId
// ZED 2.0: accountId — счёт операции; linkedAccountId — второй счёт для перевода
data class Transaction(
    val id: Int = 0,
    val type: TransactionType,
    val amountMinor: Long,
    val categoryId: Int,
    val note: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val creditId: Int? = null,
    val yearMonth: String? = null,
    val recurring: Boolean = false,
    val sourceId: Int? = null,
    val accountId: Int? = null,
    val linkedAccountId: Int? = null
)
