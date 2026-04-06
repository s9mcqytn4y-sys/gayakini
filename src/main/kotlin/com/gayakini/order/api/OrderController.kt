package com.gayakini.order.api

import com.gayakini.common.api.PageMeta
import com.gayakini.common.api.UnauthorizedException
import org.springframework.http.HttpStatus
import com.gayakini.infrastructure.security.UserPrincipal
import com.gayakini.order.application.OrderService
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/v1")
class OrderController(
    private val orderService: OrderService,
) {
    @PostMapping("/checkouts/{checkoutId}/orders")
    @ResponseStatus(HttpStatus.CREATED)
    fun placeOrder(
        @PathVariable checkoutId: UUID,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestHeader(value = "X-Checkout-Token", required = false) checkoutToken: String?,
        @RequestBody request: PlaceOrderRequest,
    ): OrderResponse {
        val order = orderService.placeOrderFromCheckout(checkoutId, idempotencyKey, checkoutToken, request)
        return OrderResponseMapper.toResponse(order, "Pesanan berhasil dibuat.", accessToken = checkoutToken)
    }

    @GetMapping("/orders/{orderId}")
    fun getOrderById(
        @PathVariable orderId: UUID,
        @RequestHeader(value = "X-Order-Token", required = false) orderToken: String?,
    ): OrderResponse {
        val order = orderService.getAuthorizedOrder(orderId, orderToken)
        return OrderResponseMapper.toResponse(order, "Detail pesanan berhasil diambil.", accessToken = orderToken)
    }

    @GetMapping("/me/orders")
    fun listMyOrders(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): OrderPageResponse {
        val currentUser =
            SecurityContextHolder.getContext().authentication?.principal as? UserPrincipal
                ?: throw UnauthorizedException()
        val orders = orderService.listOrders(currentUser.id)

        return OrderPageResponse(
            message = "Daftar pesanan berhasil diambil.",
            data = orders.map { OrderResponseMapper.toDto(it) },
            meta =
                PageMeta(
                    page = page,
                    size = size,
                    totalElements = orders.size.toLong(),
                    totalPages = 1,
                    requestId = UUID.randomUUID().toString(),
                ),
        )
    }

    @PostMapping("/orders/{orderId}/cancellations")
    fun cancelOrderByCustomer(
        @PathVariable orderId: UUID,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestHeader(value = "X-Order-Token", required = false) orderToken: String?,
        @RequestBody request: CancelOrderRequest,
    ): OrderResponse {
        val order = orderService.cancelOrder(orderId, request.reason, idempotencyKey, orderToken)
        return OrderResponseMapper.toResponse(order, "Pesanan berhasil dibatalkan.", accessToken = orderToken)
    }
}
