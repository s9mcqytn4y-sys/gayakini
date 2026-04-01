package com.gayakini.cart.api

import com.gayakini.cart.domain.CartStatus
import java.time.Instant
import java.util.UUID

data class CreateCartRequest(
    val currency: String = "IDR"
)

data class AddCartItemRequest(
    val variantId: UUID,
    val quantity: Int
)

data class UpdateCartItemRequest(
    val quantity: Int
)

data class CartResponse(
    val id: UUID,
    val customerId: UUID?,
    val status: CartStatus,
    val currency: String,
    val accessToken: String? = null,
    val expiresAt: Instant?,
    val items: List<CartItemResponse>,
    val summary: CartSummaryResponse
)

data class CartItemResponse(
    val id: UUID,
    val productId: UUID,
    val productTitle: String,
    val variantId: UUID,
    val sku: String,
    val attributes: List<ProductVariantAttributeResponse>,
    val quantity: Int,
    val unitPrice: MoneyResponse,
    val compareAtPrice: MoneyResponse? = null,
    val lineTotal: MoneyResponse,
    val primaryImageUrl: String?
)

data class CartSummaryResponse(
    val subtotal: MoneyResponse,
    val itemCount: Int
)

data class MoneyResponse(
    val currency: String = "IDR",
    val amount: Long
)

data class ProductVariantAttributeResponse(
    val name: String,
    val value: String
)
