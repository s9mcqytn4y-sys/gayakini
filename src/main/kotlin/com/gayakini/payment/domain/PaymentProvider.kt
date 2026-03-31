package com.gayakini.payment.domain

import java.util.UUID

interface PaymentProvider {
    fun createPaymentSession(orderId: UUID, amount: Long, customerDetails: CustomerPaymentDetails): PaymentSession
    fun verifyWebhook(payload: Map<String, Any>, signature: String): Boolean
    fun getPaymentStatus(externalId: String): PaymentStatus
}

data class CustomerPaymentDetails(
    val email: String,
    val fullName: String,
    val phone: String?
)

data class PaymentSession(
    val token: String,
    val redirectUrl: String,
    val externalId: String
)

enum class PaymentStatus {
    PENDING, SETTLED, CANCELLED, EXPIRED, DENIED, REFUNDED
}
