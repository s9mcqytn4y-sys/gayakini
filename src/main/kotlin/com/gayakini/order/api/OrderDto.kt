package com.gayakini.order.api

import com.gayakini.cart.api.ProductVariantAttributeDto
import com.gayakini.common.api.ApiMeta
import com.gayakini.common.api.MoneyDto
import com.gayakini.common.api.PageMeta
import com.gayakini.order.domain.FulfillmentStatus
import com.gayakini.order.domain.OrderStatus
import com.gayakini.order.domain.PaymentStatus
import java.time.Instant
import java.util.UUID

data class PlaceOrderRequest(
    val customerNotes: String? = null,
)

data class OrderDto(
    val id: UUID,
    val orderNumber: String,
    val accessToken: String? = null,
    val customerId: UUID?,
    val status: OrderStatus,
    val fulfillmentStatus: FulfillmentStatus,
    val paymentSummary: OrderPaymentSummaryDto,
    val shippingAddress: OrderAddressDto,
    val selectedShippingQuote: OrderShippingQuoteDto? = null,
    val shipment: OrderShipmentDto? = null,
    val items: List<OrderItemDto>,
    val subtotal: MoneyDto,
    val shippingCost: MoneyDto,
    val total: MoneyDto,
    val currency: String,
    val customerNotes: String? = null,
    val createdAt: Instant,
    val paidAt: Instant? = null,
    val cancelledAt: Instant? = null,
)

data class OrderPaymentSummaryDto(
    val provider: String,
    val status: PaymentStatus,
    val flow: String? = null,
    val providerOrderId: String? = null,
    val providerTransactionId: String? = null,
    val paidAt: Instant? = null,
    val grossAmount: MoneyDto? = null,
    val providerStatus: String? = null,
)

data class OrderAddressDto(
    val id: UUID? = null,
    val recipientName: String,
    val phone: String,
    val line1: String,
    val line2: String? = null,
    val notes: String? = null,
    val areaId: String,
    val district: String,
    val city: String,
    val province: String,
    val postalCode: String,
    val countryCode: String,
)

data class OrderShippingQuoteDto(
    val quoteId: UUID,
    val provider: String,
    val courierCode: String,
    val courierName: String,
    val serviceCode: String,
    val serviceName: String,
    val cost: MoneyDto,
)

data class OrderShipmentDto(
    val shipmentId: UUID,
    val provider: String,
    val providerShipmentId: String? = null,
    val courierCode: String? = null,
    val courierName: String? = null,
    val serviceCode: String? = null,
    val serviceName: String? = null,
    val trackingNumber: String? = null,
    val trackingUrl: String? = null,
    val status: FulfillmentStatus,
    val bookedAt: Instant? = null,
    val shippedAt: Instant? = null,
    val deliveredAt: Instant? = null,
)

data class OrderItemDto(
    val id: UUID,
    val productId: UUID,
    val variantId: UUID,
    val skuSnapshot: String,
    val titleSnapshot: String,
    val attributesSnapshot: List<ProductVariantAttributeDto> = listOf(),
    val quantity: Int,
    val unitPrice: MoneyDto,
    val lineTotal: MoneyDto,
)

data class OrderResponse(
    val message: String,
    val data: OrderDto,
    val meta: ApiMeta? = null,
)

data class OrderPageResponse(
    val message: String,
    val data: List<OrderDto>,
    val meta: PageMeta,
)

data class CancelOrderRequest(
    val reason: String? = null,
)
