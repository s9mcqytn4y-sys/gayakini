package com.gayakini.catalog.api

import com.gayakini.catalog.domain.VariantStatus
import com.gayakini.common.api.ApiMeta
import com.gayakini.common.api.MoneyDto
import com.gayakini.common.api.PageMeta
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "Respons halaman daftar produk.")
data class ProductPageResponse(
    val success: Boolean = true,
    @Schema(description = "Pesan status", example = "Daftar produk berhasil diambil.")
    val message: String,
    val data: List<ProductSummaryDto>,
    val meta: PageMeta,
)

@Schema(description = "Ringkasan informasi produk untuk tampilan daftar.")
data class ProductSummaryDto(
    @Schema(description = "ID unik produk", example = "550e8400-e29b-41d4-a716-446655440000")
    val id: UUID,
    @Schema(description = "Slug produk untuk URL", example = "kaos-polos-hitam")
    val slug: String,
    @Schema(description = "Nama produk", example = "Kaos Polos Hitam")
    val title: String,
    @Schema(description = "Subjudul atau deskripsi singkat", example = "Bahan katun premium")
    val subtitle: String?,
    @Schema(description = "Nama brand", example = "Gayakini")
    val brandName: String,
    @Schema(description = "Slug kategori", example = "pakaian-pria")
    val categorySlug: String,
    @Schema(description = "URL gambar utama", example = "https://cdn.gayakini.com/products/kaos-hitam-1.jpg")
    val primaryImageUrl: String?,
    val priceRange: PriceRangeDto,
    @Schema(description = "Status ketersediaan stok", example = "true")
    val inStock: Boolean,
)

@Schema(description = "Rentang harga produk.")
data class PriceRangeDto(
    @Schema(description = "Harga terendah")
    val min: MoneyDto,
    @Schema(description = "Harga tertinggi")
    val max: MoneyDto,
)

@Schema(description = "Ringkasan informasi koleksi.")
data class CollectionDto(
    @Schema(description = "ID unik koleksi", example = "550e8400-e29b-41d4-a716-446655440003")
    val id: UUID,
    @Schema(description = "Slug koleksi untuk URL", example = "summer-sale")
    val slug: String,
    @Schema(description = "Nama koleksi", example = "Summer Sale")
    val name: String,
)

@Schema(description = "Respons detail produk.")
data class ProductDetailResponse(
    val success: Boolean = true,
    @Schema(description = "Pesan status", example = "Detail produk berhasil diambil.")
    val message: String,
    val data: ProductDetailDto,
    val meta: ApiMeta? = null,
)

@Schema(description = "Informasi lengkap detail produk.")
data class ProductDetailDto(
    @Schema(description = "ID unik produk", example = "550e8400-e29b-41d4-a716-446655440000")
    val id: UUID,
    @Schema(description = "Slug produk untuk URL", example = "kaos-polos-hitam")
    val slug: String,
    @Schema(description = "Nama produk", example = "Kaos Polos Hitam")
    val title: String,
    @Schema(description = "Subjudul atau deskripsi singkat", example = "Bahan katun premium")
    val subtitle: String?,
    @Schema(description = "Nama brand", example = "Gayakini")
    val brandName: String,
    @Schema(description = "Slug kategori", example = "pakaian-pria")
    val categorySlug: String,
    @Schema(description = "URL gambar utama", example = "https://cdn.gayakini.com/products/kaos-hitam-1.jpg")
    val primaryImageUrl: String?,
    val priceRange: PriceRangeDto,
    @Schema(description = "Status ketersediaan stok", example = "true")
    val inStock: Boolean,
    @Schema(description = "Deskripsi lengkap produk", example = "Kaos polos hitam dengan bahan katun 30s...")
    val description: String,
    @Schema(description = "Daftar koleksi terkait")
    val collections: List<CollectionDto>,
    @Schema(description = "Daftar media (gambar/video) produk")
    val media: List<ProductMediaDto>,
    @Schema(description = "Daftar varian produk")
    val variants: List<ProductVariantDto>,
)

@Schema(description = "Informasi media produk.")
data class ProductMediaDto(
    @Schema(description = "ID unik media", example = "550e8400-e29b-41d4-a716-446655440001")
    val id: UUID,
    @Schema(description = "URL file media", example = "https://cdn.gayakini.com/products/kaos-hitam-1.jpg")
    val url: String,
    @Schema(description = "Teks alternatif gambar", example = "Tampak depan kaos polos hitam")
    val altText: String,
    @Schema(description = "Urutan tampilan", example = "1")
    val sortOrder: Int,
    @Schema(description = "Apakah ini gambar utama", example = "true")
    val isPrimary: Boolean = false,
)

@Schema(description = "Informasi varian produk.")
data class ProductVariantDto(
    @Schema(description = "ID unik varian", example = "550e8400-e29b-41d4-a716-446655440002")
    val id: UUID,
    @Schema(description = "SKU varian", example = "GK-KPS-BLK-M")
    val sku: String,
    @Schema(description = "Status ketersediaan varian", example = "AVAILABLE")
    val status: VariantStatus,
    @Schema(description = "Harga varian")
    val price: MoneyDto,
    @Schema(description = "Harga perbandingan (coret)", example = "100000")
    val compareAtPrice: MoneyDto?,
    val inventory: InventorySummaryDto,
    @Schema(description = "Atribut varian (misal: Warna, Ukuran)")
    val attributes: List<ProductVariantAttributeDto>,
    @Schema(description = "Berat varian dalam gram", example = "200")
    val weightGrams: Int,
    @Schema(description = "URL gambar spesifik varian", example = "https://cdn.gayakini.com/products/kaos-hitam-m.jpg")
    val primaryImageUrl: String?,
)

@Schema(description = "Ringkasan inventaris varian.")
data class InventorySummaryDto(
    @Schema(description = "Stok fisik yang ada", example = "50")
    val stockOnHand: Int,
    @Schema(description = "Stok yang sudah dipesan", example = "5")
    val stockReserved: Int,
    @Schema(description = "Stok yang tersedia untuk dijual", example = "45")
    val stockAvailable: Int,
)

@Schema(description = "Atribut varian produk.")
data class ProductVariantAttributeDto(
    @Schema(description = "Nama atribut", example = "Ukuran")
    val name: String,
    @Schema(description = "Nilai atribut", example = "M")
    val value: String,
)

@Schema(description = "Respons daftar varian produk.")
data class ProductVariantListResponse(
    val success: Boolean = true,
    @Schema(description = "Pesan status", example = "Daftar varian berhasil diambil.")
    val message: String,
    val data: List<ProductVariantDto>,
    val meta: ApiMeta? = null,
)
