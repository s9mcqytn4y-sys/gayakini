package com.gayakini.order.api

import com.gayakini.order.domain.FulfillmentStatus
import com.gayakini.order.domain.OrderStatus
import com.gayakini.order.domain.PaymentStatus
import java.time.Instant
import java.util.UUID

data class PlaceOrderRequest(
    val customerNotes: String? = null,
)

data class OrderResponse(
    val id: UUID,
    val orderNumber: String,
    val accessToken: String? = null,
    val customerId: UUID?,
    val status: OrderStatus,
    val fulfillmentStatus: FulfillmentStatus,
    val paymentSummary: OrderPaymentSummaryResponse,
    val shippingAddress: OrderAddressResponse,
    val items: List<OrderItemResponse>,
    val subtotal: MoneyResponse,
    val shippingCost: MoneyResponse,
    val total: MoneyResponse,
    val currency: String,
    val customerNotes: String?,
    val createdAt: Instant,
    val paidAt: Instant?,
    val cancelledAt: Instant?,
)

data class OrderPaymentSummaryResponse(
    val provider: String,
    val status: PaymentStatus,
    val flow: String? = null,
    val providerOrderId: String? = null,
    val providerTransactionId: String? = null,
    val paidAt: Instant? = null,
    val grossAmount: MoneyResponse? = null,
)

data class OrderAddressResponse(
    val id: UUID?,
    val recipientName: String,
    val phone: String,
    val line1: String,
    val district: String,
    val city: String,
    val province: String,
    val postalCode: String,
    val countryCode: String,
)

data class OrderItemResponse(
    val id: UUID,
    val productId: UUID,
    val variantId: UUID,
    val skuSnapshot: String,
    val titleSnapshot: String,
    val quantity: Int,
    val unitPrice: MoneyResponse,
    val lineTotal: MoneyResponse,
)

data class MoneyResponse(
    val currency: String = "IDR",
    val amount: Long,
)
