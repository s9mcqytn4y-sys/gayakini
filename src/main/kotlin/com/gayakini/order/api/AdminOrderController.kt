package com.gayakini.order.api

import com.gayakini.common.api.ApiResponse
import com.gayakini.order.application.OrderService
import com.gayakini.order.domain.FulfillmentStatus
import com.gayakini.order.domain.OrderStatus
import com.gayakini.order.domain.PaymentStatus
import com.gayakini.shipping.application.ShippingService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/v1/admin/orders")
@Tag(name = "Admin Orders", description = "Order management for administrators (Internal/English).")
class AdminOrderController(
    private val orderService: OrderService,
    private val shippingService: ShippingService,
) {
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all orders with filters")
    fun listOrders(
        @Parameter(description = "Filter by order status")
        @RequestParam(required = false)
        status: OrderStatus?,
        @Parameter(description = "Filter by payment status")
        @RequestParam(required = false)
        paymentStatus: PaymentStatus?,
        @Parameter(description = "Filter by fulfillment status")
        @RequestParam(required = false)
        fulfillmentStatus: FulfillmentStatus?,
        @Parameter(description = "Search by order number")
        @RequestParam(required = false)
        orderNumber: String?,
        @Parameter(hidden = true)
        pageable: Pageable,
    ): Page<OrderDto> {
        return orderService.listOrdersForAdmin(status, paymentStatus, fulfillmentStatus, orderNumber, pageable)
            .map { OrderResponseMapper.toDto(it, shippingService.findShipmentByOrderId(it.id)) }
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get order detail by ID")
    fun getOrder(
        @Parameter(description = "Order UUID")
        @PathVariable
        orderId: UUID,
    ): ApiResponse<OrderDto> {
        val order = orderService.getOrder(orderId)
        return ApiResponse.success(
            data = OrderResponseMapper.toDto(order, shippingService.findShipmentByOrderId(order.id)),
            message = "Detail pesanan admin berhasil diambil.",
        )
    }

    @PostMapping("/{orderId}/shipments")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create shipment for an order")
    fun createShipment(
        @Parameter(description = "Order UUID")
        @PathVariable
        orderId: UUID,
        @Parameter(description = "Idempotency token")
        @RequestHeader("Idempotency-Key")
        idempotencyKey: String,
        @Valid
        @RequestBody(required = false)
        request: AdminCreateShipmentRequest?,
    ): ApiResponse<OrderDto> {
        val shipment = shippingService.bookShipment(orderId, idempotencyKey, request)
        val order = orderService.getOrder(orderId)
        return ApiResponse.success(
            data = OrderResponseMapper.toDto(order, shipment),
            message = "Pengiriman berhasil dibuat.",
        )
    }

    @PostMapping("/{orderId}/cancellations")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cancel order as administrator")
    fun cancelOrder(
        @Parameter(description = "Order UUID")
        @PathVariable
        orderId: UUID,
        @Parameter(description = "Idempotency token")
        @RequestHeader("Idempotency-Key")
        idempotencyKey: String,
        @Valid
        @RequestBody(required = false)
        request: AdminCancelOrderRequest?,
    ): ApiResponse<OrderDto> {
        val order = orderService.cancelOrderAsAdmin(orderId, request?.reason, idempotencyKey)
        return ApiResponse.success(
            data = OrderResponseMapper.toDto(order),
            message = "Pesanan berhasil dibatalkan oleh admin.",
        )
    }
}
