package com.gayakini.cart.api

import com.gayakini.cart.application.CartService
import com.gayakini.cart.domain.Cart
import com.gayakini.common.api.ApiResponse
import com.gayakini.common.api.MoneyDto
import com.gayakini.infrastructure.security.SecurityUtils
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/v1/carts")
@Tag(name = "Cart", description = "Manajemen keranjang belanja untuk pelanggan dan tamu.")
class CartController(private val cartService: CartService) {
    @PostMapping
    @Operation(
        summary = "Buat keranjang baru",
        description = "Membuat keranjang belanja baru. Bisa digunakan oleh tamu maupun pengguna terautentikasi.",
    )
    @SecurityRequirements
    fun createCart(
        @Parameter(description = "Kode mata uang (misal: IDR, USD)")
        @RequestParam(required = false) currency: String?,
    ): ApiResponse<CartDto> {
        val (cart, rawToken) = cartService.createCart(SecurityUtils.getCurrentUserId(), currency ?: "IDR")
        return mapToResponse(cart, "Keranjang berhasil dibuat.", rawToken)
    }

    @GetMapping("/{cartId}")
    @Operation(
        summary = "Ambil data keranjang",
        description =
            "Mengambil detail keranjang berdasarkan ID. " +
                "Jika keranjang milik tamu, X-Cart-Token harus disertakan.",
    )
    @SecurityRequirements
    fun getCart(
        @Parameter(description = "ID Keranjang (UUID)")
        @PathVariable cartId: UUID,
        @Parameter(description = "Token akses keranjang untuk tamu")
        @RequestHeader(value = "X-Cart-Token", required = false) cartToken: String?,
    ): ApiResponse<CartDto> {
        val cart = cartService.getValidatedCart(cartId, SecurityUtils.getCurrentUserId(), cartToken)
        return mapToResponse(cart, "Keranjang berhasil diambil.")
    }

    @PostMapping("/{cartId}/items")
    @Operation(
        summary = "Tambah item ke keranjang",
        description = "Menambahkan varian produk ke dalam keranjang.",
    )
    @SecurityRequirements
    fun addCartItem(
        @Parameter(description = "ID Keranjang (UUID)")
        @PathVariable cartId: UUID,
        @Parameter(description = "Token akses keranjang untuk tamu")
        @RequestHeader(value = "X-Cart-Token", required = false) cartToken: String?,
        @Valid @RequestBody request: AddCartItemRequest,
    ): ApiResponse<CartDto> {
        val cart =
            cartService.addItem(
                cartId,
                request.variantId,
                request.quantity,
                SecurityUtils.getCurrentUserId(),
                cartToken,
            )
        return mapToResponse(cart, "Produk berhasil dimasukkan ke keranjang.")
    }

    @PatchMapping("/{cartId}/items/{itemId}")
    @Operation(
        summary = "Update kuantitas item",
        description = "Mengubah jumlah item yang ada di dalam keranjang.",
    )
    @SecurityRequirements
    fun updateCartItem(
        @Parameter(description = "ID Keranjang (UUID)")
        @PathVariable cartId: UUID,
        @Parameter(description = "ID Item Keranjang (UUID)")
        @PathVariable itemId: UUID,
        @Parameter(description = "Token akses keranjang untuk tamu")
        @RequestHeader(value = "X-Cart-Token", required = false) cartToken: String?,
        @Valid @RequestBody request: UpdateCartItemRequest,
    ): ApiResponse<CartDto> {
        val cart = cartService.updateItem(cartId, itemId, request.quantity, SecurityUtils.getCurrentUserId(), cartToken)
        return mapToResponse(cart, "Item keranjang berhasil diperbarui.")
    }

    @DeleteMapping("/{cartId}/items/{itemId}")
    @Operation(
        summary = "Hapus item dari keranjang",
        description = "Menghapus item tertentu dari keranjang belanja.",
    )
    @SecurityRequirements
    fun deleteCartItem(
        @Parameter(description = "ID Keranjang (UUID)")
        @PathVariable cartId: UUID,
        @Parameter(description = "ID Item Keranjang (UUID)")
        @PathVariable itemId: UUID,
        @Parameter(description = "Token akses keranjang untuk tamu")
        @RequestHeader(value = "X-Cart-Token", required = false) cartToken: String?,
    ): ApiResponse<CartDto> {
        val cart = cartService.removeItem(cartId, itemId, SecurityUtils.getCurrentUserId(), cartToken)
        return mapToResponse(cart, "Item keranjang berhasil dihapus.")
    }

    private fun mapToResponse(
        cart: Cart,
        message: String,
        rawToken: String? = null,
    ): ApiResponse<CartDto> {
        return ApiResponse.success(
            message = message,
            data =
                CartDto(
                    id = cart.id,
                    customerId = cart.customerId,
                    status = cart.status,
                    currency = cart.currencyCode,
                    accessToken = rawToken,
                    expiresAt = cart.expiresAt,
                    items =
                        cart.items.map { item ->
                            CartItemDto(
                                id = item.id,
                                productId = item.product?.id ?: UUID.randomUUID(),
                                productTitle = item.productTitleSnapshot.orEmpty(),
                                variantId = item.variant.id,
                                sku = item.skuSnapshot.orEmpty(),
                                attributes =
                                    listOf(
                                        ProductVariantAttributeDto(name = "color", value = item.color.orEmpty()),
                                        ProductVariantAttributeDto(name = "size", value = item.sizeCode.orEmpty()),
                                    ),
                                quantity = item.quantity,
                                unitPrice = MoneyDto(amount = item.unitPriceAmount),
                                compareAtPrice = item.compareAtAmount?.let { MoneyDto(amount = it) },
                                lineTotal = MoneyDto(amount = item.lineTotalAmount),
                                primaryImageUrl = item.primaryImageUrl,
                            )
                        },
                    summary =
                        CartSummaryDto(
                            subtotal = MoneyDto(amount = cart.subtotalAmount),
                            itemCount = cart.itemCount,
                        ),
                ),
        )
    }
}
