package com.gayakini.order.api

import com.gayakini.common.api.PageMeta
import com.gayakini.common.api.UnauthorizedException
import com.gayakini.infrastructure.security.UserPrincipal
import com.gayakini.order.application.OrderService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/v1")
@Tag(name = "Orders", description = "Pengelolaan pesanan untuk customer.")
class OrderController(
    private val orderService: OrderService,
) {
    @PostMapping("/checkouts/{checkoutId}/orders")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Membuat pesanan baru dari sesi checkout yang sudah diverifikasi")
    @SecurityRequirements // Diperbolehkan untuk Guest jika ada checkoutToken
    fun placeOrder(
        @Parameter(description = "ID sesi checkout")
        @PathVariable
        checkoutId: UUID,
        @Parameter(description = "Token idempotensi untuk mencegah duplikasi (misal: UUID)")
        @RequestHeader("Idempotency-Key")
        idempotencyKey: String,
        @Parameter(description = "Token otorisasi checkout (wajib untuk guest)")
        @RequestHeader(value = "X-Checkout-Token", required = false)
        checkoutToken: String?,
        @RequestBody
        request: PlaceOrderRequest,
    ): OrderResponse {
        val order = orderService.placeOrderFromCheckout(checkoutId, idempotencyKey, checkoutToken, request)
        return OrderResponseMapper.toResponse(order, "Pesanan berhasil dibuat.", accessToken = checkoutToken)
    }

    @GetMapping("/orders/{orderId}")
    @Operation(summary = "Mengambil detail pesanan berdasarkan ID")
    @SecurityRequirements // Diperbolehkan jika memiliki X-Order-Token valid
    fun getOrderById(
        @Parameter(description = "UUID unik pesanan")
        @PathVariable
        orderId: UUID,
        @Parameter(description = "Token akses pesanan (untuk guest atau via email link)")
        @RequestHeader(value = "X-Order-Token", required = false)
        orderToken: String?,
    ): OrderResponse {
        val order = orderService.getAuthorizedOrder(orderId, orderToken)
        return OrderResponseMapper.toResponse(order, "Detail pesanan berhasil diambil.", accessToken = orderToken)
    }

    @GetMapping("/me/orders")
    @Operation(summary = "Daftar pesanan milik user yang sedang login (Role: CUSTOMER)")
    fun listMyOrders(
        @Parameter(description = "Halaman") @RequestParam(defaultValue = "1") page: Int,
        @Parameter(description = "Ukuran") @RequestParam(defaultValue = "20") size: Int,
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
                ),
        )
    }

    @PostMapping("/orders/{orderId}/cancellations")
    @Operation(summary = "Membatalkan pesanan")
    @SecurityRequirements // Diperbolehkan jika memiliki X-Order-Token valid
    fun cancelOrderByCustomer(
        @Parameter(description = "UUID pesanan")
        @PathVariable
        orderId: UUID,
        @Parameter(description = "Token idempotensi")
        @RequestHeader("Idempotency-Key")
        idempotencyKey: String,
        @Parameter(description = "Token akses pesanan")
        @RequestHeader(value = "X-Order-Token", required = false)
        orderToken: String?,
        @RequestBody
        request: CancelOrderRequest,
    ): OrderResponse {
        val order = orderService.cancelOrder(orderId, request.reason, idempotencyKey, orderToken)
        return OrderResponseMapper.toResponse(order, "Pesanan berhasil dibatalkan.", accessToken = orderToken)
    }
}
