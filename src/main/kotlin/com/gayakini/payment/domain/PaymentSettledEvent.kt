package com.gayakini.payment.domain

import java.util.UUID

data class PaymentSettledEvent(
    val orderId: UUID,
    val paymentId: UUID,
    val transactionNumber: String,
    val amount: Long,
    val provider: String,
)
