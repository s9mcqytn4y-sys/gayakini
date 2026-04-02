package com.gayakini.cart.api

import com.gayakini.cart.application.CartService
import com.gayakini.cart.domain.Cart
import com.gayakini.common.api.ApiMeta
import com.gayakini.common.api.MoneyDto
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/v1/carts")
class CartController(private val cartService: CartService) {

    @PostMapping
    fun createCart(
        @RequestParam(required = false) currency: String?,
    ): CartResponse {
        val (cart, rawToken) = cartService.createCart(null, currency ?: "IDR")
        return mapToResponse(cart, "Keranjang berhasil dibuat.", rawToken)
    }

    @GetMapping("/{cartId}")
    fun getCart(
        @PathVariable cartId: UUID,
        @RequestHeader(value = "X-Cart-Token", required = false) cartToken: String?,
    ): CartResponse {
        val cart = cartService.getValidatedCart(cartId, null, cartToken)
        return mapToResponse(cart, "Keranjang berhasil diambil.")
    }

    @PostMapping("/{cartId}/items")
    fun addCartItem(
        @PathVariable cartId: UUID,
        @RequestHeader(value = "X-Cart-Token", required = false) cartToken: String?,
        @RequestBody request: AddCartItemRequest,
    ): CartResponse {
        val cart = cartService.addItem(cartId, request.variantId, request.quantity, null, cartToken)
        return mapToResponse(cart, "Produk berhasil dimasukkan ke keranjang.")
    }

    private fun mapToResponse(cart: Cart, message: String, rawToken: String? = null): CartResponse {
        return CartResponse(
            message = message,
            data = CartDto(
                id = cart.id,
                customerId = cart.customerId,
                status = cart.status,
                currency = cart.currencyCode,
                accessToken = rawToken,
                expiresAt = cart.expiresAt,
                items = cart.items.map { item ->
                    CartItemDto(
                        id = item.id,
                        productId = item.product?.id ?: UUID.randomUUID(),
                        productTitle = item.productTitleSnapshot ?: "",
                        variantId = item.variant.id,
                        sku = item.skuSnapshot ?: "",
                        attributes = listOf(
                            ProductVariantAttributeDto(name = "color", value = item.color ?: ""),
                            ProductVariantAttributeDto(name = "size", value = item.sizeCode ?: "")
                        ),
                        quantity = item.quantity,
                        unitPrice = MoneyDto(amount = item.unitPriceAmount),
                        compareAtPrice = item.compareAtAmount?.let { MoneyDto(amount = it) },
                        lineTotal = MoneyDto(amount = item.lineTotalAmount),
                        primaryImageUrl = item.primaryImageUrl
                    )
                },
                summary = CartSummaryDto(
                    subtotal = MoneyDto(amount = cart.subtotalAmount),
                    itemCount = cart.itemCount
                )
            ),
            meta = ApiMeta(requestId = UUID.randomUUID().toString())
        )
    }
}
