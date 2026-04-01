package com.gayakini.cart.api

import com.gayakini.cart.application.CartService
import com.gayakini.common.api.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/v1/carts")
class CartController(private val cartService: CartService) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createCart(
        @RequestBody(required = false) request: CreateCartRequest?,
        @RequestHeader(value = "Authorization", required = false) authHeader: String?
    ): ApiResponse<CartResponse> {
        // TODO: Extract customerId from authHeader if present
        val customerId: UUID? = null 
        val currency = request?.currency ?: "IDR"
        
        val cart = cartService.createCart(customerId, currency)
        
        return ApiResponse.success(
            message = "Keranjang berhasil dibuat.",
            data = mapToResponse(cart)
        )
    }

    @GetMapping("/{cartId}")
    fun getCart(
        @PathVariable cartId: UUID,
        @RequestHeader(value = "X-Cart-Token", required = false) cartToken: String?
    ): ApiResponse<CartResponse> {
        val cart = cartService.getCart(cartId)
        // TODO: Validate cartToken if guest cart
        
        return ApiResponse.success(
            message = "Keranjang berhasil diambil.",
            data = mapToResponse(cart)
        )
    }

    @PostMapping("/{cartId}/items")
    fun addCartItem(
        @PathVariable cartId: UUID,
        @Valid @RequestBody request: AddCartItemRequest,
        @RequestHeader(value = "X-Cart-Token", required = false) cartToken: String?
    ): ApiResponse<CartResponse> {
        val cart = cartService.addItem(cartId, request.variantId, request.quantity)
        
        return ApiResponse.success(
            message = "Produk berhasil dimasukkan ke keranjang.",
            data = mapToResponse(cart)
        )
    }

    private fun mapToResponse(cart: com.gayakini.cart.domain.Cart): CartResponse {
        return CartResponse(
            id = cart.id,
            customerId = cart.customerId,
            status = cart.status,
            currency = cart.currencyCode,
            accessToken = null, // Only return raw token on creation for guests
            expiresAt = cart.expiresAt,
            items = cart.items.map { item ->
                CartItemResponse(
                    id = item.id,
                    productId = item.product?.id ?: UUID.randomUUID(),
                    productTitle = item.productTitleSnapshot ?: "",
                    variantId = item.variant.id,
                    sku = item.skuSnapshot ?: "",
                    attributes = listOf(), // TODO
                    quantity = item.quantity,
                    unitPrice = MoneyResponse(amount = item.unitPriceAmount),
                    compareAtPrice = item.compareAtAmount?.let { MoneyResponse(amount = it) },
                    lineTotal = MoneyResponse(amount = item.lineTotalAmount),
                    primaryImageUrl = item.primaryImageUrl
                )
            },
            summary = CartSummaryResponse(
                subtotal = MoneyResponse(amount = cart.subtotalAmount),
                itemCount = cart.itemCount
            )
        )
    }
}
