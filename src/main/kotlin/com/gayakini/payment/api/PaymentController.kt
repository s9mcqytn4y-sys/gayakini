package com.gayakini.payment.api

import com.gayakini.common.api.ApiMeta
import com.gayakini.common.api.StandardResponse
import com.gayakini.payment.application.PaymentService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/v1/orders")
class PaymentController(private val paymentService: PaymentService) {

    @PostMapping("/{orderId}/payments")
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
            data = PaymentSessionDto(
                paymentId = payment.id,
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
            meta = ApiMeta(requestId = UUID.randomUUID().toString())
        )
    }
}
