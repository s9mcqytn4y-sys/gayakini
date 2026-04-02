package com.gayakini.cart.api

import com.gayakini.cart.domain.CartStatus
import com.gayakini.common.api.ApiMeta
import com.gayakini.common.api.MoneyDto
import java.time.Instant
import java.util.UUID

data class CreateCartRequest(
    val currency: String = "IDR",
)

data class AddCartItemRequest(
    val variantId: UUID,
    val quantity: Int,
)

data class UpdateCartItemRequest(
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
