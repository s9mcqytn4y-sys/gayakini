package com.gayakini.order.api

import com.gayakini.common.api.PageMeta
import com.gayakini.order.application.OrderService
import com.gayakini.order.domain.FulfillmentStatus
import com.gayakini.order.domain.OrderStatus
import com.gayakini.order.domain.PaymentStatus
import com.gayakini.shipping.application.ShippingService
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/v1/admin/orders")
class AdminOrderController(
    private val orderService: OrderService,
    private val shippingService: ShippingService,
) {
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun listOrders(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) status: OrderStatus?,
        @RequestParam(required = false) paymentStatus: PaymentStatus?,
        @RequestParam(required = false) fulfillmentStatus: FulfillmentStatus?,
        @RequestParam(required = false) orderNumber: String?,
    ): OrderPageResponse {
        val filtered =
            orderService.listOrdersForAdmin(status, paymentStatus, fulfillmentStatus, orderNumber)
        val fromIndex = ((page - 1).coerceAtLeast(0) * size).coerceAtMost(filtered.size)
        val toIndex = (fromIndex + size).coerceAtMost(filtered.size)
        val slice = filtered.subList(fromIndex, toIndex)

        return OrderPageResponse(
            message = "Daftar pesanan admin berhasil diambil.",
            data = slice.map { OrderResponseMapper.toDto(it, shippingService.findShipmentByOrderId(it.id)) },
            meta =
                PageMeta(
                    page = page,
                    size = size,
                    totalElements = filtered.size.toLong(),
                    totalPages = if (filtered.isEmpty()) 0 else ((filtered.size + size - 1) / size),
                ),
        )
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    fun getOrder(
        @PathVariable orderId: UUID,
    ): OrderResponse {
        val order = orderService.getOrder(orderId)
        return OrderResponseMapper.toResponse(
            order = order,
            message = "Detail pesanan admin berhasil diambil.",
            shipment = shippingService.findShipmentByOrderId(order.id),
        )
    }

    @PostMapping("/{orderId}/shipments")
    @PreAuthorize("hasRole('ADMIN')")
    fun createShipment(
        @PathVariable orderId: UUID,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @Valid @RequestBody(required = false) request: AdminCreateShipmentRequest?,
    ): OrderResponse {
        val shipment = shippingService.bookShipment(orderId, idempotencyKey, request)
        val order = orderService.getOrder(orderId)
        return OrderResponseMapper.toResponse(
            order = order,
            message = "Pengiriman berhasil dibuat.",
            shipment = shipment,
        )
    }

    @PostMapping("/{orderId}/cancellations")
    @PreAuthorize("hasRole('ADMIN')")
    fun cancelOrder(
        @PathVariable orderId: UUID,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @Valid @RequestBody(required = false) request: AdminCancelOrderRequest?,
    ): OrderResponse {
        val order = orderService.cancelOrderAsAdmin(orderId, request?.reason, idempotencyKey)
        return OrderResponseMapper.toResponse(order, "Pesanan berhasil dibatalkan oleh admin.")
    }
}
