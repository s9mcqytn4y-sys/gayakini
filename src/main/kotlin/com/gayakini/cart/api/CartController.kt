package com.gayakini.cart.api

import com.gayakini.cart.application.CartService
import com.gayakini.common.api.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/carts")
class CartController(private val cartService: CartService) {
    @PostMapping
    fun createCart(
        @RequestParam(required = false) customerId: UUID?,
        @RequestParam(defaultValue = "IDR") currency: String,
    ): ApiResponse<CartResponse> {
        val cart = cartService.createCart(customerId, currency)
        return ApiResponse.success(
            message = "Keranjang berhasil dibuat.",
            data = mapToResponse(cart),
        )
    }

    @PostMapping("/{cartId}/items")
    fun addItem(
        @PathVariable cartId: UUID,
        @RequestBody request: AddCartItemRequest,
    ): ApiResponse<CartResponse> {
        val cart = cartService.addItem(cartId, request.variantId, request.quantity)
        return ApiResponse.success(
            message = "Produk berhasil ditambahkan ke keranjang.",
            data = mapToResponse(cart),
        )
    }

    @GetMapping("/{cartId}")
    fun getCart(
        @PathVariable cartId: UUID,
    ): ApiResponse<CartResponse> {
        val cart = cartService.getCart(cartId)
        return ApiResponse.success(
            message = "Keranjang berhasil diambil.",
            data = mapToResponse(cart),
        )
    }

    private fun mapToResponse(cart: com.gayakini.cart.domain.Cart): CartResponse {
        return CartResponse(
            id = cart.id,
            customerId = cart.customerId,
            status = cart.status,
            currency = cart.currencyCode,
            accessToken = null, // Logic to expose token only on creation if needed
            items =
                cart.items.map { item ->
                    CartItemResponse(
                        id = item.id,
                        productId = item.product?.id ?: UUID.randomUUID(),
                        productTitle = item.productTitleSnapshot ?: "",
                        variantId = item.variant.id,
                        sku = item.skuSnapshot ?: "",
                        attributes = listOf(),
                        quantity = item.quantity,
                        unitPrice = MoneyResponse(amount = item.unitPriceAmount),
                        compareAtPrice = item.compareAtAmount?.let { MoneyResponse(amount = it) },
                        lineTotal = MoneyResponse(amount = item.lineTotalAmount),
                        primaryImageUrl = item.primaryImageUrl,
                    )
                },
            summary =
                CartSummaryResponse(
                    subtotal = MoneyResponse(amount = cart.subtotalAmount),
                    itemCount = cart.itemCount,
                ),
            expiresAt = cart.expiresAt,
        )
    }
}
