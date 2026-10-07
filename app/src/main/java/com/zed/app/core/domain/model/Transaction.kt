package com.zed.app.core.domain.model

// Тип операции
enum class TransactionType { INCOME, EXPENSE }

// Категории живут в БД (builtIn + кастомные), здесь только ссылка categoryId
data class Transaction(
    val id: Int = 0,
    val type: TransactionType,
    val amountMinor: Long,
    val categoryId: Int,
    val note: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val creditId: Int? = null,
    val yearMonth: String? = null
)
