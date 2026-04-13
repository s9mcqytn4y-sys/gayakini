package com.gayakini.catalog.api

import com.gayakini.catalog.domain.ProductStatus
import com.gayakini.common.api.ApiMeta
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.util.UUID

@Schema(description = "Permintaan pembuatan produk baru oleh admin.")
data class AdminCreateProductRequest(
    @field:Schema(description = "Nama produk", example = "Kaos Polos Premium")
    @field:NotBlank
    @field:Size(max = 200)
    val title: String,
    @field:Schema(description = "Slug URL unik", example = "kaos-polos-premium")
    @field:NotBlank
    @field:Pattern(regexp = "^[a-z0-9-]+$")
    @field:Size(max = 200)
    val slug: String,
    @field:Schema(description = "Subjudul produk", example = "Bahan Katun 100%")
    @field:Size(max = 180)
    val subtitle: String? = null,
    @field:Schema(description = "Nama Brand", example = "Gayakini")
    @field:NotBlank
    @field:Size(max = 120)
    val brandName: String,
    @field:Schema(description = "Slug kategori", example = "pakaian-pria")
    @field:NotBlank
    val categorySlug: String,
    @field:Schema(description = "Deskripsi produk", example = "Kaos polos kualitas tinggi...")
    @field:NotBlank
    val description: String,
    @field:Schema(description = "Daftar slug koleksi")
    val collections: List<String> = listOf(),
    @field:Schema(description = "Daftar media awal")
    val media: List<ProductMediaDto> = listOf(),
    @field:Schema(description = "Status publikasi produk", example = "DRAFT")
    @field:NotNull
    val status: ProductStatus = ProductStatus.DRAFT,
)

@Schema(description = "Permintaan pembaruan data produk oleh admin.")
data class AdminUpdateProductRequest(
    @field:Schema(description = "Nama produk", example = "Kaos Polos Premium V2")
    @field:Size(max = 200)
    val title: String? = null,
    @field:Schema(description = "Slug URL unik", example = "kaos-polos-premium-v2")
    @field:Pattern(regexp = "^[a-z0-9-]+$")
    @field:Size(max = 200)
    val slug: String? = null,
    @field:Schema(description = "Subjudul produk", example = "Bahan Katun 100% Lembut")
    @field:Size(max = 180)
    val subtitle: String? = null,
    @field:Schema(description = "Nama Brand", example = "Gayakini")
    @field:Size(max = 120)
    val brandName: String? = null,
    @field:Schema(description = "Slug kategori", example = "pakaian-pria")
    val categorySlug: String? = null,
    @field:Schema(description = "Deskripsi produk", example = "Kaos polos kualitas tinggi dengan jahitan rapi...")
    val description: String? = null,
    @field:Schema(description = "Status publikasi produk", example = "ACTIVE")
    val status: ProductStatus? = null,
)

@Schema(description = "Informasi produk untuk admin.")
data class AdminProductData(
    @Schema(description = "ID unik produk", example = "550e8400-e29b-41d4-a716-446655440300")
    val id: UUID,
    @Schema(description = "Slug URL", example = "kaos-polos-premium")
    val slug: String,
    @Schema(description = "Nama produk", example = "Kaos Polos Premium")
    val title: String,
    @Schema(description = "Subjudul", example = "Bahan Katun 100%")
    val subtitle: String?,
    @Schema(description = "Brand", example = "Gayakini")
    val brandName: String,
    @Schema(description = "Kategori", example = "pakaian-pria")
    val categorySlug: String?,
    @Schema(description = "Deskripsi", example = "Kaos polos kualitas tinggi...")
    val description: String,
    @Schema(description = "Daftar koleksi")
    val collections: List<String>,
    @Schema(description = "Daftar media")
    val media: List<ProductMediaDto>,
    @Schema(description = "Status produk", example = "ACTIVE")
    val status: ProductStatus,
)

@Schema(description = "Respons detail produk untuk admin.")
data class AdminProductResponse(
    @Schema(description = "Pesan status", example = "Produk berhasil dibuat.")
    val message: String,
    val data: AdminProductData,
    val meta: ApiMeta? = null,
)

@Schema(description = "Permintaan penyesuaian stok.")
data class StockAdjustmentRequest(
    @Schema(description = "Perubahan jumlah stok (positif/negatif)", example = "10")
    val quantityDelta: Int,
    @Schema(description = "Kode alasan penyesuaian", example = "RESTOCK")
    val reasonCode: String,
    @Schema(description = "Catatan tambahan", example = "Barang baru datang dari supplier")
    val note: String? = null,
)

@Schema(description = "Data hasil penyesuaian stok.")
data class StockAdjustmentData(
    @Schema(description = "ID varian yang disesuaikan", example = "550e8400-e29b-41d4-a716-446655440301")
    val variantId: UUID,
    @Schema(description = "Stok fisik terbaru", example = "100")
    val stockOnHand: Int,
    @Schema(description = "Stok reservasi terbaru", example = "5")
    val stockReserved: Int,
    @Schema(description = "Stok tersedia terbaru", example = "95")
    val stockAvailable: Int,
    @Schema(description = "ID log penyesuaian terakhir", example = "550e8400-e29b-41d4-a716-446655440302")
    val lastAdjustmentId: UUID,
)

@Schema(description = "Respons hasil penyesuaian stok.")
data class StockAdjustmentResponse(
    @Schema(description = "Pesan status", example = "Stok berhasil diperbarui.")
    val message: String,
    val data: StockAdjustmentData,
    val meta: ApiMeta? = null,
)
