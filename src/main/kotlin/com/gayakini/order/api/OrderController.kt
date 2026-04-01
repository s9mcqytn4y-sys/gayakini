package com.gayakini.order.api

import com.gayakini.common.api.ApiResponse
import com.gayakini.common.util.HashUtils
import com.gayakini.infrastructure.security.SecurityUtils
import com.gayakini.order.application.OrderService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Manajemen pesanan")
class OrderController(
    private val orderService: OrderService,
) {
    @PostMapping("/place")
    @Operation(summary = "Membuat pesanan baru dari keranjang")
    fun placeOrder(
        @Valid @RequestBody request: PlaceOrderRequest,
        @RequestHeader(value = "X-Guest-Token", required = false) guestToken: String?,
    ): ApiResponse<OrderResponse> {
        val currentUser = SecurityUtils.getCurrentUser()

        val customerId = currentUser?.id
        // Hashing guest token to ensure we don't store/leak raw token
        val guestTokenHash = if (customerId == null && guestToken != null) HashUtils.sha256(guestToken) else null

        if (customerId == null && guestTokenHash == null) {
            return ApiResponse.error("Identitas pembeli tidak ditemukan (User atau Guest Token).")
        }

        val order = orderService.placeOrder(customerId, guestTokenHash, request)

        return ApiResponse.success(
            data =
                OrderResponse(
                    id = order.id,
                    orderNumber = order.orderNumber,
                    status = order.status,
                    totalAmount = order.totalAmount,
                    shippingCost = order.shippingCost,
                    grandTotal = order.grandTotal,
                    items = emptyList(),
                ),
            message = "Pesanan berhasil dibuat. Silakan selesaikan pembayaran.",
        )
    }
}
