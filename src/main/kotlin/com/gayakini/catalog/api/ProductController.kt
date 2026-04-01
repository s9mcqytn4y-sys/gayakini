package com.gayakini.catalog.api

import com.gayakini.catalog.application.ProductService
import com.gayakini.catalog.domain.Product
import com.gayakini.catalog.domain.ProductVariant
import com.gayakini.common.api.ApiMeta
import com.gayakini.common.api.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/products")
class ProductController(private val productService: ProductService) {
    @GetMapping
    fun listProducts(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "newest") sort: String,
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false) categorySlug: String?,
        @RequestParam(required = false) collectionSlug: String?,
        @RequestParam(required = false) color: String?,
        @RequestParam(required = false) sizeCode: String?,
        @RequestParam(required = false) minPrice: Long?,
        @RequestParam(required = false) maxPrice: Long?,
        @RequestParam(required = false) inStock: Boolean?,
    ): ApiResponse<List<ProductSummaryResponse>> {
        val products = productService.listProducts()

        return ApiResponse.success(
            message = "Daftar produk berhasil diambil.",
            data = products.map { mapToSummary(it) },
            meta = ApiMeta(page = page, size = size, totalElements = products.size.toLong(), totalPages = 1),
        )
    }

    @GetMapping("/{productId}")
    fun getProductById(
        @PathVariable productId: UUID,
    ): ApiResponse<ProductDetailData> {
        val product = productService.getProduct(productId)

        return ApiResponse.success(
            message = "Detail produk berhasil diambil.",
            data = mapToDetail(product),
        )
    }

    private fun mapToSummary(product: Product): ProductSummaryResponse {
        return ProductSummaryResponse(
            id = product.id,
            slug = product.slug,
            title = product.title,
            subtitle = product.subtitle,
            brandName = product.brandName,
            categorySlug = "todo", // Logic to fetch slug from category entity
            primaryImageUrl = null, // TODO: Fetch from ProductMedia
            priceRange =
                PriceRangeResponse(
                    min = MoneyResponse(amount = 0), // TODO: Calculate from variants
                    max = MoneyResponse(amount = 0),
                ),
            inStock = true, // TODO: Check variants available stock
        )
    }

    private fun mapToDetail(product: Product): ProductDetailData {
        return ProductDetailData(
            id = product.id,
            slug = product.slug,
            title = product.title,
            subtitle = product.subtitle,
            brandName = product.brandName,
            categorySlug = "todo",
            primaryImageUrl = null,
            priceRange =
                PriceRangeResponse(
                    min = MoneyResponse(amount = 0),
                    max = MoneyResponse(amount = 0),
                ),
            inStock = true,
            description = product.description,
            collections = listOf(),
            media = listOf(),
            variants = listOf(), // TODO: Map variants using mapToVariantResponse
        )
    }

    private fun mapToVariantResponse(variant: ProductVariant): ProductVariantResponse {
        return ProductVariantResponse(
            id = variant.id,
            sku = variant.sku,
            status = variant.status,
            price = MoneyResponse(amount = variant.priceAmount),
            compareAtPrice = variant.compareAtAmount?.let { MoneyResponse(amount = it) },
            inventory =
                InventorySummaryResponse(
                    stockOnHand = variant.stockOnHand,
                    stockReserved = variant.stockReserved,
                    stockAvailable = variant.stockAvailable,
                ),
            attributes =
                listOf(
                    ProductVariantAttributeResponse(name = "color", value = variant.color),
                    ProductVariantAttributeResponse(name = "size", value = variant.sizeCode),
                ),
            weightGrams = variant.weightGrams,
            primaryImageUrl = null,
        )
    }
}
