package com.gayakini.checkout.api

import com.gayakini.cart.api.CartItemDto
import com.gayakini.checkout.domain.CheckoutStatus
import com.gayakini.common.api.ApiMeta
import com.gayakini.common.api.MoneyDto
import java.time.Instant
import java.util.UUID

data class CreateCheckoutRequest(
    val cartId: UUID,
)

data class CheckoutDto(
    val id: UUID,
    val cartId: UUID,
    val customerId: UUID?,
    val accessToken: String? = null,
    val status: CheckoutStatus,
    val currency: String,
    val shippingAddress: CheckoutAddressDto? = null,
    val selectedShippingQuote: ShippingQuoteDto? = null,
    val availableShippingQuotes: List<ShippingQuoteDto> = listOf(),
    val items: List<CartItemDto>,
    val subtotal: MoneyDto,
    val shippingCost: MoneyDto,
    val total: MoneyDto,
    val expiresAt: Instant?,
)

data class CheckoutAddressDto(
    val id: UUID?,
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

data class ShippingQuoteDto(
    val quoteId: UUID,
    val provider: String,
    val providerReference: String? = null,
    val courierCode: String,
    val courierName: String,
    val serviceCode: String,
    val serviceName: String,
    val description: String? = null,
    val cost: MoneyDto,
    val estimatedDaysMin: Int? = null,
    val estimatedDaysMax: Int? = null,
    val isRecommended: Boolean = false,
)

data class CheckoutResponse(
    val message: String,
    val data: CheckoutDto,
    val meta: ApiMeta? = null,
)

data class CheckoutShippingAddressRequest(
    val addressId: UUID? = null,
    val guestAddress: GuestAddressRequest? = null,
)

data class GuestAddressRequest(
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
    val countryCode: String = "ID",
)

data class SelectShippingQuoteRequest(
    val quoteId: UUID,
)
