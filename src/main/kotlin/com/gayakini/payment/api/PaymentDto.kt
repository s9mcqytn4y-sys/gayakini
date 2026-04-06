package com.gayakini.payment.api

import com.gayakini.order.domain.PaymentStatus
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class CreatePaymentRequest(
    @field:Pattern(regexp = "^MIDTRANS$")
    val provider: String? = "MIDTRANS",
    @field:Pattern(regexp = "^SNAP$")
    val flow: String? = "SNAP",
    @field:Size(max = 30)
    val preferredChannel: String? = null,
    val enabledChannels: List<String>? = null,
)

data class PaymentSessionDto(
    val paymentId: UUID,
    val provider: String,
    val flow: String,
    val status: PaymentStatus,
    val providerOrderId: String,
    val providerTransactionId: String? = null,
    val preferredChannel: String? = null,
    val snapToken: String? = null,
    val snapRedirectUrl: String? = null,
    val expiresAt: Instant? = null,
)
