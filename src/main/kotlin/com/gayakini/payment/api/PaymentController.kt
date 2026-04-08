package com.gayakini.payment.api

import com.gayakini.common.api.ApiMeta
import com.gayakini.common.api.StandardResponse
import com.gayakini.payment.application.PaymentService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/v1/payments")
class PaymentController(
    private val paymentService: PaymentService,
    private val properties: com.gayakini.infrastructure.config.GayakiniProperties,
) {
    @PostMapping("/orders/{orderId}")
    @ResponseStatus(HttpStatus.CREATED)
    fun createOrderPayment(
        @PathVariable orderId: UUID,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
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
            meta = ApiMeta(requestId = UUID.randomUUID().toString()),
        )
    }

    @GetMapping("/config")
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
