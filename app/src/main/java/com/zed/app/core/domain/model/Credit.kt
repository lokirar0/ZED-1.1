package com.zed.app.core.domain.model

data class Credit(
    val id: Int = 0,
    val title: String,
    val monthlyPaymentMinor: Long,
    val paymentDay: Int,
    val note: String = ""
)

data class CreditPayment(
    val id: Int = 0,
    val creditId: Int,
    val yearMonth: String
)
