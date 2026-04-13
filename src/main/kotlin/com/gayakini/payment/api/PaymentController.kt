package com.gayakini.payment.api

import com.gayakini.common.api.ApiMeta
import com.gayakini.common.api.StandardResponse
import com.gayakini.payment.application.PaymentService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/v1/payments")
@Tag(name = "Payment", description = "Layanan inisialisasi dan konfigurasi pembayaran.")
class PaymentController(
    private val paymentService: PaymentService,
    private val properties: com.gayakini.infrastructure.config.GayakiniProperties,
) {
    @PostMapping("/orders/{orderId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Inisialisasi pembayaran pesanan",
        description = "Membuat sesi pembayaran baru untuk pesanan tertentu.",
    )
    @SecurityRequirements
    fun createOrderPayment(
        @Parameter(description = "ID unik pesanan") @PathVariable orderId: UUID,
        @Parameter(description = "Kunci idempotensi untuk mencegah duplikasi pembayaran")
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @Parameter(description = "Token akses pesanan untuk tamu")
        @RequestHeader(value = "X-Order-Token", required = false) orderToken: String?,
        @Valid @RequestBody(required = false) request: CreatePaymentRequest?,
    ): StandardResponse<PaymentSessionDto> {
        val payment = paymentService.createPaymentSession(orderId, idempotencyKey, orderToken, request)

        return StandardResponse(
            message = "Pembayaran siap dilanjutkan.",
            data =
                PaymentSessionDto(
                    paymentId = payment.id,
                    transactionNumber = payment.transactionNumber,
                    provider = payment.provider,
                    flow = payment.flow,
                    status = payment.status,
                    providerOrderId = payment.providerOrderId,
                    providerTransactionId = payment.providerTransactionId,
                    preferredChannel = payment.preferredChannel,
                    snapToken = payment.snapToken,
                    snapRedirectUrl = payment.snapRedirectUrl,
                    expiresAt = payment.expiresAt,
                ),
            meta = ApiMeta(),
        )
    }

    @GetMapping("/config")
    @Operation(
        summary = "Ambil konfigurasi pembayaran",
        description = "Mendapatkan kunci publik provider (misal: Midtrans Client Key).",
    )
    @SecurityRequirements
    fun getPaymentConfig(): StandardResponse<PaymentConfigResponse> {
        return StandardResponse(
            message = "Konfigurasi pembayaran berhasil diambil.",
            data =
                PaymentConfigResponse(
                    clientKey = properties.midtrans.clientKey,
                    isProduction = properties.midtrans.isProduction,
                ),
        )
    }
}
