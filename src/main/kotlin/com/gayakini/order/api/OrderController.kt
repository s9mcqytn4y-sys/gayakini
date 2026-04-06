package com.gayakini.order.api

import com.gayakini.common.api.ApiMeta
import com.gayakini.common.api.MoneyDto
import com.gayakini.common.api.PageMeta
import com.gayakini.order.application.OrderService
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import com.gayakini.infrastructure.security.UserPrincipal
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
        return mapToResponse(order, "Pesanan berhasil dibuat.")
    }

    @GetMapping("/orders/{orderId}")
    fun getOrderById(
        @PathVariable orderId: UUID,
        @RequestHeader(value = "X-Order-Token", required = false) orderToken: String?,
    ): OrderResponse {
        val order = orderService.getAuthorizedOrder(orderId, orderToken)
        return mapToResponse(order, "Detail pesanan berhasil diambil.")
    }

    @GetMapping("/me/orders")
    fun listMyOrders(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): OrderPageResponse {
        val currentUser = SecurityContextHolder.getContext().authentication?.principal as? UserPrincipal
        val orders = orderService.listOrders(currentUser?.id)

        return OrderPageResponse(
            message = "Daftar pesanan berhasil diambil.",
            data = orders.map { mapToDto(it) },
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
        return mapToResponse(order, "Pesanan berhasil dibatalkan.")
    }

    private fun mapToResponse(
        order: com.gayakini.order.domain.Order,
        message: String,
    ): OrderResponse {
        return OrderResponse(
            message = message,
            data = mapToDto(order),
            meta = ApiMeta(requestId = UUID.randomUUID().toString()),
        )
    }

    private fun mapToDto(order: com.gayakini.order.domain.Order): OrderDto {
        return OrderDto(
            id = order.id,
            orderNumber = order.orderNumber,
            customerId = order.customerId,
            status = order.status,
            fulfillmentStatus = order.fulfillmentStatus,
            paymentSummary =
                OrderPaymentSummaryDto(
                    provider = "MIDTRANS",
                    status = order.paymentStatus,
                ),
            shippingAddress =
                order.shippingAddress!!.let { addr ->
                    OrderAddressDto(
                        recipientName = addr.recipientName,
                        phone = addr.phone,
                        line1 = addr.line1,
                        line2 = addr.line2,
                        notes = addr.notes,
                        areaId = addr.areaId,
                        district = addr.district,
                        city = addr.city,
                        province = addr.province,
                        postalCode = addr.postalCode,
                        countryCode = addr.countryCode,
                    )
                },
            items =
                order.items.map { item ->
                    OrderItemDto(
                        id = item.id,
                        productId = item.product.id,
                        variantId = item.variant.id,
                        skuSnapshot = item.skuSnapshot,
                        titleSnapshot = item.titleSnapshot,
                        quantity = item.quantity,
                        unitPrice = MoneyDto(amount = item.unitPriceAmount),
                        lineTotal = MoneyDto(amount = item.lineTotalAmount),
                    )
                },
            subtotal = MoneyDto(amount = order.subtotalAmount),
            shippingCost = MoneyDto(amount = order.shippingCostAmount),
            total = MoneyDto(amount = order.totalAmount),
            currency = order.currencyCode,
            customerNotes = order.customerNotes,
            createdAt = order.createdAt,
            paidAt = order.paidAt,
            cancelledAt = order.cancelledAt,
        )
    }
}
