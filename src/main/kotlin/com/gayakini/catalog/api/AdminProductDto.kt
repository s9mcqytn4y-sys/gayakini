package com.gayakini.catalog.api

import com.gayakini.catalog.domain.ProductStatus
import com.gayakini.catalog.domain.VariantStatus
import com.gayakini.common.api.ApiMeta
import com.gayakini.common.api.MoneyDto
import java.util.UUID

data class AdminCreateProductRequest(
    val title: String,
    val slug: String,
    val subtitle: String? = null,
    val brandName: String,
    val categorySlug: String,
    val description: String,
    val collections: List<String> = listOf(),
    val media: List<ProductMediaDto> = listOf(),
    val status: ProductStatus = ProductStatus.DRAFT,
)

data class AdminUpdateProductRequest(
    val title: String? = null,
    val slug: String? = null,
    val subtitle: String? = null,
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
    val sku: String,
    val price: MoneyDto,
    val compareAtPrice: MoneyDto? = null,
    val attributes: List<ProductVariantAttributeDto>,
    val stockOnHand: Int,
    val weightGrams: Int = 0,
    val status: VariantStatus = VariantStatus.ACTIVE,
)

data class AdminUpdateVariantRequest(
    val price: MoneyDto? = null,
    val compareAtPrice: MoneyDto? = null,
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
