package com.gayakini.cart.api

import com.gayakini.cart.domain.CartStatus
import com.gayakini.common.api.ApiMeta
import com.gayakini.common.api.MoneyDto
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import java.time.Instant
import java.util.UUID

data class CreateCartRequest(
    @field:NotBlank
    @field:Pattern(regexp = "^[A-Z]{3}$")
    val currency: String = "IDR",
)

data class AddCartItemRequest(
    val variantId: UUID,
    @field:Min(1)
    @field:Max(99)
    val quantity: Int,
)

data class UpdateCartItemRequest(
    @field:Min(1)
    @field:Max(99)
    val quantity: Int,
)

data class CartDto(
    val id: UUID,
    val customerId: UUID?,
    val status: CartStatus,
    val currency: String,
    val accessToken: String? = null,
    val expiresAt: Instant?,
    val items: List<CartItemDto>,
    val summary: CartSummaryDto,
)

data class CartItemDto(
    val id: UUID,
    val productId: UUID,
    val productTitle: String,
    val variantId: UUID,
    val sku: String,
    val attributes: List<ProductVariantAttributeDto>,
    val quantity: Int,
    val unitPrice: MoneyDto,
    val compareAtPrice: MoneyDto? = null,
    val lineTotal: MoneyDto,
    val primaryImageUrl: String?,
)

data class CartSummaryDto(
    val subtotal: MoneyDto,
    val itemCount: Int,
)

data class ProductVariantAttributeDto(
    val name: String,
    val value: String,
)

data class CartResponse(
    val message: String,
    val data: CartDto,
    val meta: ApiMeta? = null,
)
