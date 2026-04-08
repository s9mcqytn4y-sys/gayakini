package com.gayakini.catalog.api

import com.gayakini.catalog.domain.ProductStatus
import com.gayakini.catalog.domain.VariantStatus
import com.gayakini.common.api.ApiMeta
import com.gayakini.common.api.MoneyDto
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.util.UUID

data class AdminCreateProductRequest(
    @field:NotBlank
    @field:Size(max = 200)
    val title: String,
    @field:NotBlank
    @field:Pattern(regexp = "^[a-z0-9-]+$")
    @field:Size(max = 200)
    val slug: String,
    @field:Size(max = 180)
    val subtitle: String? = null,
    @field:NotBlank
    @field:Size(max = 120)
    val brandName: String,
    @field:NotBlank
    val categorySlug: String,
    @field:NotBlank
    val description: String,
    val collections: List<String> = listOf(),
    val media: List<ProductMediaDto> = listOf(),
    @field:NotNull
    val status: ProductStatus = ProductStatus.DRAFT,
)

data class AdminUpdateProductRequest(
    @field:Size(max = 200)
    val title: String? = null,
    @field:Pattern(regexp = "^[a-z0-9-]+$")
    @field:Size(max = 200)
    val slug: String? = null,
    @field:Size(max = 180)
    val subtitle: String? = null,
    @field:Size(max = 120)
    val brandName: String? = null,
    val categorySlug: String? = null,
    val description: String? = null,
    val collections: List<String>? = null,
    val media: List<ProductMediaDto>? = null,
    val status: ProductStatus? = null,
)

data class AdminProductData(
    val id: UUID,
    val slug: String,
    val title: String,
    val subtitle: String?,
    val brandName: String,
    val categorySlug: String?,
    val description: String,
    val collections: List<String>,
    val media: List<ProductMediaDto>,
    val status: ProductStatus,
)

data class AdminProductResponse(
    val message: String,
    val data: AdminProductData,
    val meta: ApiMeta? = null,
)

data class AdminCreateVariantRequest(
    @field:NotBlank
    @field:Pattern(regexp = "^[A-Z0-9-]{3,64}$")
    val sku: String,
    @field:Valid
    @field:NotNull
    val price: MoneyDto,
    @field:Valid
    val compareAtPrice: MoneyDto? = null,
    @field:NotEmpty
    val attributes: List<ProductVariantAttributeDto>,
    @field:Min(0)
    val stockOnHand: Int,
    @field:Min(0)
    val weightGrams: Int = 0,
    @field:NotNull
    val status: VariantStatus = VariantStatus.ACTIVE,
)

data class AdminUpdateVariantRequest(
    @field:Valid
    val price: MoneyDto? = null,
    @field:Valid
    val compareAtPrice: MoneyDto? = null,
    @field:Min(0)
    val weightGrams: Int? = null,
    val status: VariantStatus? = null,
)

data class AdminVariantResponse(
    val message: String,
    val data: ProductVariantDto,
    val meta: ApiMeta? = null,
)

data class StockAdjustmentRequest(
    val quantityDelta: Int,
    val reasonCode: String,
    val note: String? = null,
)

data class StockAdjustmentData(
    val variantId: UUID,
    val stockOnHand: Int,
    val stockReserved: Int,
    val stockAvailable: Int,
    val lastAdjustmentId: UUID,
)

data class StockAdjustmentResponse(
    val message: String,
    val data: StockAdjustmentData,
    val meta: ApiMeta? = null,
)
