package com.gayakini.order.api

import com.gayakini.common.api.ApiMeta
import com.gayakini.common.api.MoneyDto
import com.gayakini.order.domain.Order
import com.gayakini.shipping.application.Shipment
import java.nio.charset.StandardCharsets
import java.util.UUID

object OrderResponseMapper {
    fun toResponse(
        order: Order,
        message: String,
        shipment: Shipment? = null,
        accessToken: String? = null,
    ): OrderResponse {
        return OrderResponse(
            message = message,
            data = toDto(order, shipment, accessToken),
            meta = ApiMeta(),
        )
    }

    fun toDto(
        order: Order,
        shipment: Shipment? = null,
        accessToken: String? = null,
    ): OrderDto {
        return OrderDto(
            id = order.id,
            orderNumber = order.orderNumber,
            accessToken = accessToken,
            customerId = order.customerId,
            status = order.status,
            fulfillmentStatus = order.fulfillmentStatus,
            paymentSummary =
                OrderPaymentSummaryDto(
                    provider = "MIDTRANS",
                    status = order.paymentStatus,
                    paidAt = order.paidAt,
                    grossAmount = MoneyDto(amount = order.totalAmount),
                ),
            shippingAddress =
                order.shippingAddress?.let { addr ->
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
                } ?: error("Order shipping address missing"),
            selectedShippingQuote =
                order.shippingSelection?.let { selection ->
                    OrderShippingQuoteDto(
                        quoteId =
                            UUID.nameUUIDFromBytes(
                                (selection.providerReference ?: "${selection.courierCode}:${selection.serviceCode}")
                                    .toByteArray(StandardCharsets.UTF_8),
                            ),
                        provider = selection.provider,
                        courierCode = selection.courierCode,
                        courierName = selection.courierName,
                        serviceCode = selection.serviceCode,
                        serviceName = selection.serviceName,
                        cost = MoneyDto(amount = selection.costAmount),
                    )
                },
            shipment =
                shipment?.let {
                    OrderShipmentDto(
                        shipmentId = it.id,
                        provider = it.provider,
                        providerShipmentId = it.providerOrderId,
                        courierCode = it.courierCode,
                        courierName = it.courierName,
                        serviceCode = it.serviceCode,
                        serviceName = it.serviceName,
                        trackingNumber = it.trackingNumber,
                        trackingUrl = it.trackingUrl,
                        status = it.status,
                        bookedAt = it.bookedAt,
                        shippedAt = it.shippedAt,
                        deliveredAt = it.deliveredAt,
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
                        attributesSnapshot =
                            listOf(
                                com.gayakini.cart.api.ProductVariantAttributeDto(name = "color", value = item.color),
                                com.gayakini.cart.api.ProductVariantAttributeDto(name = "size", value = item.sizeCode),
                            ),
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
