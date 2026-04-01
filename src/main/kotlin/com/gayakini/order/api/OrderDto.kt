package com.gayakini.order.api

import com.gayakini.order.domain.OrderStatus
import java.util.UUID

data class PlaceOrderRequest(
    val cartId: UUID,
    val shippingAddress: ShippingAddressDto,
    val shippingService: String,
    val shippingCost: Long,
    val paymentMethod: String,
    val idempotencyKey: String,
)

data class ShippingAddressDto(
    val fullName: String,
    val phoneNumber: String,
    val email: String,
    val address: String,
    val city: String,
    val province: String,
    val zipCode: String,
    val areaId: String? = null,
)

data class OrderResponse(
    val id: UUID,
    val orderNumber: String,
    val status: OrderStatus,
    val totalAmount: Long,
    val shippingCost: Long,
    val grandTotal: Long,
    val items: List<OrderItemResponse>,
)

data class OrderItemResponse(
    val variantId: UUID,
    val name: String,
    val sku: String,
    val price: Long,
    val quantity: Int,
    val subtotal: Long,
)
