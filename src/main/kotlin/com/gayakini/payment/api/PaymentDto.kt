package com.gayakini.payment.api

import com.gayakini.order.domain.PaymentStatus
import jakarta.validation.constraints.Pattern
import java.time.Instant
import java.util.UUID

enum class PaymentChannel(val midtransCode: String) {
    CREDIT_CARD("credit_card"),
    BCA_VA("bca_va"),
    BNI_VA("bni_va"),
    BRI_VA("bri_va"),
    MANDIRI_BILL("echannel"),
    PERMATA_VA("permata_va"),
    GOPAY("gopay"),
    SHOPEEPAY("shopeepay"),
    QRIS("qris"),
}

data class CreatePaymentRequest(
    @field:Pattern(regexp = "^MIDTRANS$")
    val provider: String? = "MIDTRANS",
    @field:Pattern(regexp = "^SNAP$")
    val flow: String? = "SNAP",
    val preferredChannel: PaymentChannel? = null,
    val enabledChannels: List<PaymentChannel>? = null,
)

data class PaymentSessionDto(
    val paymentId: UUID,
    val transactionNumber: String,
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

data class PaymentConfigResponse(
    val clientKey: String,
    val isProduction: Boolean,
)
