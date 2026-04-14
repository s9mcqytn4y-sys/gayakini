package com.gayakini.payment.api

import com.gayakini.order.domain.PaymentStatus
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Pattern
import java.time.Instant
import java.util.UUID

@Schema(description = "Metode/Kanal pembayaran yang didukung.")
enum class PaymentChannel(val midtransCodes: List<String>) {
    @Schema(description = "Kartu Kredit")
    CREDIT_CARD(listOf("credit_card")),

    @Schema(description = "Transfer Bank (VA)")
    BANK_TRANSFER(listOf("bca_va", "bni_va", "bri_va", "echannel", "permata_va")),

    @Schema(description = "GoPay")
    GOPAY(listOf("gopay")),

    @Schema(description = "ShopeePay")
    SHOPEEPAY(listOf("shopeepay")),

    @Schema(description = "QRIS")
    QRIS(listOf("qris")),

    @Schema(description = "Convenience Store")
    CSTORE(listOf("indomaret", "alfamart")),
}

@Schema(description = "Permintaan pembuatan sesi pembayaran.")
data class CreatePaymentRequest(
    @field:Schema(description = "Penyedia layanan pembayaran", example = "MIDTRANS")
    @field:Pattern(regexp = "^MIDTRANS$")
    val provider: String? = "MIDTRANS",
    @field:Schema(description = "Alur pembayaran (misal: SNAP)", example = "SNAP")
    @field:Pattern(regexp = "^SNAP$")
    val flow: String? = "SNAP",
    @field:Schema(description = "Kanal pembayaran yang diinginkan")
    val preferredChannel: PaymentChannel? = null,
    @field:Schema(description = "Daftar kanal pembayaran yang diaktifkan")
    val enabledChannels: List<PaymentChannel>? = null,
)

@Schema(description = "Detail sesi pembayaran yang berhasil dibuat.")
data class PaymentSessionDto(
    @Schema(description = "ID unik pembayaran", example = "550e8400-e29b-41d4-a716-446655440100")
    val paymentId: UUID,
    @Schema(description = "Nomor transaksi internal", example = "TX-123456")
    val transactionNumber: String,
    @Schema(description = "Penyedia", example = "MIDTRANS")
    val provider: String,
    @Schema(description = "Alur", example = "SNAP")
    val flow: String,
    @Schema(description = "Status pembayaran", example = "PENDING")
    val status: PaymentStatus,
    @Schema(description = "ID pesanan di sisi penyedia", example = "ORD-12345")
    val providerOrderId: String,
    @Schema(description = "ID transaksi di sisi penyedia", example = "mid-tx-789")
    val providerTransactionId: String? = null,
    @Schema(description = "Kanal yang dipilih", example = "GOPAY")
    val preferredChannel: String? = null,
    @Schema(description = "Token SNAP Midtrans", example = "abc-123-def")
    val snapToken: String? = null,
    @Schema(description = "URL redirect SNAP Midtrans", example = "https://app.midtrans.com/snap/v2/vtweb/123")
    val snapRedirectUrl: String? = null,
    @Schema(description = "Waktu kadaluarsa sesi", example = "2024-04-12T11:00:00Z")
    val expiresAt: Instant? = null,
)

@Schema(description = "Konfigurasi client-side untuk payment gateway.")
data class PaymentConfigResponse(
    val success: Boolean = true,
    @Schema(description = "Client Key dari penyedia", example = "SB-Mid-client-123")
    val clientKey: String,
    @Schema(description = "Apakah mode produksi", example = "false")
    val isProduction: Boolean,
)
