package com.gayakini.order.api

import com.gayakini.common.api.ApiResponse
import com.gayakini.order.application.OrderService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/v1/checkouts")
class OrderController(
    private val orderService: OrderService,
) {
    @PostMapping("/{checkoutId}/orders")
    @ResponseStatus(HttpStatus.CREATED)
    fun placeOrder(
        @PathVariable checkoutId: UUID,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestHeader(value = "X-Checkout-Token", required = false) checkoutToken: String?,
        @RequestBody request: PlaceOrderRequest
    ): ApiResponse<OrderResponse> {
        val response = orderService.placeOrderFromCheckout(checkoutId, idempotencyKey, checkoutToken, request)
        
        return ApiResponse.success(
            data = response,
            message = "Pesanan berhasil dibuat. Silakan selesaikan pembayaran.",
        )
    }
}
