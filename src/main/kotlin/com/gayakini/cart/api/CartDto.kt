package com.gayakini.cart.api

import com.gayakini.cart.domain.CartStatus
import com.gayakini.common.api.ApiMeta
import com.gayakini.common.api.MoneyDto
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import java.time.Instant
import java.util.UUID

object CartConstants {
    const val MIN_QUANTITY = 1L
    const val MAX_QUANTITY = 99L
}

@Schema(description = "Permintaan untuk membuat keranjang belanja baru.")
data class CreateCartRequest(
    @field:NotBlank
    @field:Pattern(regexp = "^[A-Z]{3}$")
    @Schema(description = "Kode mata uang ISO 4217", example = "IDR")
    val currency: String = "IDR",
)

@Schema(description = "Permintaan untuk menambahkan item ke keranjang.")
data class AddCartItemRequest(
    @Schema(description = "ID unik varian produk", example = "550e8400-e29b-41d4-a716-446655440002")
    val variantId: UUID,
    @field:Min(CartConstants.MIN_QUANTITY)
    @field:Max(CartConstants.MAX_QUANTITY)
    @Schema(description = "Jumlah item", example = "1", minimum = "1", maximum = "99")
    val quantity: Int,
)

@Schema(description = "Permintaan untuk memperbarui jumlah item di keranjang.")
data class UpdateCartItemRequest(
    @field:Min(CartConstants.MIN_QUANTITY)
    @field:Max(CartConstants.MAX_QUANTITY)
    @Schema(description = "Jumlah item baru", example = "2", minimum = "1", maximum = "99")
    val quantity: Int,
)

@Schema(description = "Representasi keranjang belanja.")
data class CartDto(
    @Schema(description = "ID unik keranjang", example = "550e8400-e29b-41d4-a716-446655440005")
    val id: UUID,
    @Schema(description = "ID pelanggan (null untuk guest)", example = "550e8400-e29b-41d4-a716-446655440006")
    val customerId: UUID?,
    @Schema(description = "Status keranjang", example = "ACTIVE")
    val status: CartStatus,
    @Schema(description = "Kode mata uang", example = "IDR")
    val currency: String,
    @Schema(description = "Token akses untuk keranjang guest", example = "cart_access_token_xyz")
    val accessToken: String? = null,
    @Schema(description = "Waktu kedaluwarsa keranjang guest")
    val expiresAt: Instant?,
    @Schema(description = "Daftar item dalam keranjang")
    val items: List<CartItemDto>,
    val summary: CartSummaryDto,
)

@Schema(description = "Item dalam keranjang belanja.")
data class CartItemDto(
    @Schema(description = "ID unik item keranjang", example = "550e8400-e29b-41d4-a716-446655440007")
    val id: UUID,
    @Schema(description = "ID produk", example = "550e8400-e29b-41d4-a716-446655440000")
    val productId: UUID,
    @Schema(description = "Nama produk", example = "Kaos Polos Hitam")
    val productTitle: String,
    @Schema(description = "ID varian produk", example = "550e8400-e29b-41d4-a716-446655440002")
    val variantId: UUID,
    @Schema(description = "SKU produk", example = "GK-KPS-BLK-M")
    val sku: String,
    @Schema(description = "Atribut varian (misal: Warna, Ukuran)")
    val attributes: List<ProductVariantAttributeDto>,
    @Schema(description = "Jumlah item", example = "1")
    val quantity: Int,
    @Schema(description = "Harga satuan")
    val unitPrice: MoneyDto,
    @Schema(description = "Harga perbandingan (coret)")
    val compareAtPrice: MoneyDto? = null,
    @Schema(description = "Total harga untuk baris item ini")
    val lineTotal: MoneyDto,
    @Schema(description = "URL gambar utama produk", example = "https://cdn.gayakini.com/products/kaos-hitam-1.jpg")
    val primaryImageUrl: String?,
)

@Schema(description = "Ringkasan total keranjang belanja.")
data class CartSummaryDto(
    @Schema(description = "Subtotal harga")
    val subtotal: MoneyDto,
    @Schema(description = "Total jumlah seluruh item", example = "3")
    val itemCount: Int,
)

@Schema(description = "Atribut varian produk dalam keranjang.")
data class ProductVariantAttributeDto(
    @Schema(description = "Nama atribut", example = "Ukuran")
    val name: String,
    @Schema(description = "Nilai atribut", example = "M")
    val value: String,
)

@Schema(description = "Respons operasi keranjang belanja.")
data class CartResponse(
    @Schema(description = "Pesan status", example = "Item berhasil ditambahkan ke keranjang.")
    val message: String,
    val data: CartDto,
    val meta: ApiMeta? = null,
)
