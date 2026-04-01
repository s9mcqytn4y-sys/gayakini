package com.gayakini.checkout.api

import com.gayakini.cart.api.CartItemResponse
import com.gayakini.cart.api.MoneyResponse
import com.gayakini.checkout.domain.CheckoutStatus
import java.time.Instant
import java.util.UUID

data class CreateCheckoutRequest(
    val cartId: UUID
)

data class CheckoutResponse(
    val id: UUID,
    val cartId: UUID,
    val customerId: UUID?,
    val accessToken: String? = null,
    val status: CheckoutStatus,
    val currency: String,
    val shippingAddress: CheckoutAddressResponse? = null,
    val selectedShippingQuote: ShippingQuoteResponse? = null,
    val availableShippingQuotes: List<ShippingQuoteResponse> = listOf(),
    val items: List<CartItemResponse>,
    val subtotal: MoneyResponse,
    val shippingCost: MoneyResponse,
    val total: MoneyResponse,
    val expiresAt: Instant?
)

data class CheckoutAddressResponse(
    val id: UUID?,
    val recipientName: String,
    val phone: String,
    val line1: String,
    val district: String,
    val city: String,
    val province: String,
    val postalCode: String,
    val countryCode: String
)

data class ShippingQuoteResponse(
    val quoteId: UUID,
    val provider: String,
    val courierCode: String,
    val courierName: String,
    val serviceCode: String,
    val serviceName: String,
    val cost: MoneyResponse
)

data class CheckoutShippingAddressRequest(
    val addressId: UUID? = null,
    val guestAddress: GuestAddressRequest? = null
)

data class GuestAddressRequest(
    val recipientName: String,
    val phone: String,
    val line1: String,
    val areaId: String,
    val district: String,
    val city: String,
    val province: String,
    val postalCode: String,
    val countryCode: String = "ID"
)

data class SelectShippingQuoteRequest(
    val quoteId: UUID
)
