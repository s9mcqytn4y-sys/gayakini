package com.gayakini.catalog.api

import com.gayakini.catalog.domain.ProductStatus
import com.gayakini.catalog.domain.VariantStatus
import java.util.UUID

data class AdminCreateProductRequest(
    val title: String,
    val slug: String,
    val brandName: String,
    val categorySlug: String,
    val description: String,
    val collections: List<String> = listOf(),
    val status: ProductStatus = ProductStatus.DRAFT
)

data class AdminUpdateProductRequest(
    val title: String? = null,
    val slug: String? = null,
    val brandName: String? = null,
    val categorySlug: String? = null,
    val description: String? = null,
    val collections: List<String>? = null,
    val status: ProductStatus? = null
)

data class AdminProductResponse(
    val id: UUID,
    val slug: String,
    val title: String,
    val subtitle: String?,
    val brandName: String,
    val categorySlug: String?,
    val description: String,
    val collections: List<String>,
    val status: ProductStatus
)

data class AdminCreateVariantRequest(
    val sku: String,
    val price: MoneyResponse,
    val compareAtPrice: MoneyResponse? = null,
    val attributes: List<ProductVariantAttributeResponse>,
    val stockOnHand: Int,
    val weightGrams: Int = 0,
    val status: VariantStatus = VariantStatus.ACTIVE
)

data class AdminUpdateVariantRequest(
    val price: MoneyResponse? = null,
    val compareAtPrice: MoneyResponse? = null,
    val weightGrams: Int? = null,
    val status: VariantStatus? = null
)

data class StockAdjustmentRequest(
    val quantityDelta: Int,
    val reasonCode: String,
    val note: String? = null
)

data class StockAdjustmentResponse(
    val variantId: UUID,
    val stockOnHand: Int,
    val stockReserved: Int,
    val stockAvailable: Int,
    val lastAdjustmentId: UUID
)
