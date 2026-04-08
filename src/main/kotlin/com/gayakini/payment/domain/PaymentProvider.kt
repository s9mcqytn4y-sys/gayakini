package com.gayakini.payment.domain

import com.gayakini.order.domain.PaymentStatus
import java.util.UUID

interface PaymentProvider {
    fun createPaymentSession(
        orderId: UUID,
        providerOrderId: String,
        amount: Long,
        customerDetails: CustomerPaymentDetails,
        itemDetails: List<PaymentItemDetail> = emptyList(),
    ): PaymentSession

    fun verifyWebhook(
        payload: Map<String, Any>,
        signature: String,
    ): Boolean

    fun getPaymentStatus(providerOrderId: String): PaymentStatus
}

data class CustomerPaymentDetails(
    val email: String,
    val fullName: String,
    val phone: String?,
)

data class PaymentItemDetail(
    val id: String,
    val price: Long,
    val quantity: Int,
    val name: String,
)

data class PaymentSession(
    val token: String,
    val redirectUrl: String,
    val providerOrderId: String,
    val requestPayload: String? = null,
    val responsePayload: String? = null,
)
