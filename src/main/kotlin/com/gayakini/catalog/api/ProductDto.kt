package com.gayakini.catalog.api

import com.gayakini.catalog.domain.VariantStatus
import com.gayakini.common.api.ApiMeta
import java.util.UUID

data class ProductPageResponse(
    val message: String,
    val data: List<ProductSummaryResponse>,
    val meta: ApiMeta,
)

data class ProductSummaryResponse(
    val id: UUID,
    val slug: String,
    val title: String,
    val subtitle: String?,
    val brandName: String,
    val categorySlug: String,
    val primaryImageUrl: String?,
    val priceRange: PriceRangeResponse,
    val inStock: Boolean,
)

data class PriceRangeResponse(
    val min: MoneyResponse,
    val max: MoneyResponse,
)

data class MoneyResponse(
    val currency: String = "IDR",
    val amount: Long,
)

data class ProductDetailResponse(
    val message: String,
    val data: ProductDetailData,
)

data class ProductDetailData(
    val id: UUID,
    val slug: String,
    val title: String,
    val subtitle: String?,
    val brandName: String,
    val categorySlug: String,
    val primaryImageUrl: String?,
    val priceRange: PriceRangeResponse,
    val inStock: Boolean,
    val description: String,
    val collections: List<String>,
    val media: List<ProductMediaResponse>,
    val variants: List<ProductVariantResponse>,
)

data class ProductMediaResponse(
    val id: UUID,
    val url: String,
    val altText: String,
    val sortOrder: Int,
    val isPrimary: Boolean,
)

data class ProductVariantResponse(
    val id: UUID,
    val sku: String,
    val status: VariantStatus,
    val price: MoneyResponse,
    val compareAtPrice: MoneyResponse?,
    val inventory: InventorySummaryResponse,
    val attributes: List<ProductVariantAttributeResponse>,
    val weightGrams: Int,
    val primaryImageUrl: String?,
)

data class InventorySummaryResponse(
    val stockOnHand: Int,
    val stockReserved: Int,
    val stockAvailable: Int,
)

data class ProductVariantAttributeResponse(
    val name: String,
    val value: String,
)
