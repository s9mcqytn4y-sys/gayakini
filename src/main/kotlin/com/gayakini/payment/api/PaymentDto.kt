package com.gayakini.payment.api

import com.gayakini.order.domain.PaymentStatus
import java.time.Instant
import java.util.UUID

data class CreatePaymentRequest(
    val provider: String? = "MIDTRANS",
    val flow: String? = "SNAP",
    val preferredChannel: String? = null,
    val enabledChannels: List<String>? = null
)

data class PaymentSessionResponse(
    val paymentId: UUID,
    val provider: String,
    val flow: String,
    val status: PaymentStatus,
    val providerOrderId: String,
    val providerTransactionId: String? = null,
    val preferredChannel: String? = null,
    val snapToken: String? = null,
    val snapRedirectUrl: String? = null,
    val expiresAt: Instant? = null
)
