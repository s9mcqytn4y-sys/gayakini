package com.gayakini.checkout.api

import com.gayakini.cart.api.CartItemDto
import com.gayakini.checkout.domain.CheckoutStatus
import com.gayakini.common.api.ApiMeta
import com.gayakini.common.api.MoneyDto
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
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
    @field:Valid
    val guestAddress: GuestAddressRequest? = null,
)

data class GuestAddressRequest(
    @field:NotBlank
    @field:Size(max = 120)
    val recipientName: String,
    @field:NotBlank
    @field:Pattern(regexp = "^[0-9+]{8,20}$")
    val phone: String,
    @field:NotBlank
    @field:Size(max = 200)
    val line1: String,
    @field:Size(max = 200)
    val line2: String? = null,
    @field:Size(max = 200)
    val notes: String? = null,
    @field:NotBlank
    @field:Size(max = 100)
    val areaId: String,
    @field:NotBlank
    @field:Size(max = 120)
    val district: String,
    @field:NotBlank
    @field:Size(max = 120)
    val city: String,
    @field:NotBlank
    @field:Size(max = 120)
    val province: String,
    @field:NotBlank
    @field:Size(max = 20)
    val postalCode: String,
    @field:Pattern(regexp = "^[A-Z]{2}$")
    val countryCode: String = "ID",
)

data class SelectShippingQuoteRequest(
    val quoteId: UUID,
)
