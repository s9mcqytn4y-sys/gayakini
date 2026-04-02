package com.gayakini.catalog.application

import com.gayakini.catalog.domain.Product
import com.gayakini.catalog.domain.ProductRepository
import com.gayakini.catalog.domain.ProductVariant
import com.gayakini.catalog.domain.ProductVariantRepository
import com.gayakini.catalog.domain.PublicProductSummary
import com.gayakini.catalog.domain.PublicProductSummaryRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val productVariantRepository: ProductVariantRepository,
    private val publicProductSummaryRepository: PublicProductSummaryRepository,
) {
    fun searchProducts(
        page: Int,
        size: Int,
        sort: String,
        q: String?,
        categorySlug: String?,
        collectionSlug: String?,
        color: String?,
        sizeCode: String?,
        minPrice: Long?,
        maxPrice: Long?,
        inStock: Boolean?,
    ): Page<PublicProductSummary> {
        val sortOrder =
            when (sort) {
                "price_asc" -> Sort.by(Sort.Direction.ASC, "minPriceAmount")
                "price_desc" -> Sort.by(Sort.Direction.DESC, "minPriceAmount")
                else -> Sort.by(Sort.Direction.DESC, "createdAt")
            }
        val pageable = PageRequest.of(page - 1, size, sortOrder)
        return publicProductSummaryRepository.search(
            q = q,
            categorySlug = categorySlug,
            collectionSlug = collectionSlug,
            color = color,
            sizeCode = sizeCode,
            minPrice = minPrice,
            maxPrice = maxPrice,
            inStock = inStock,
            pageable = pageable,
        )
    }

    fun getProduct(id: UUID): Product {
        return productRepository.findById(id)
            .orElseThrow { NoSuchElementException("Produk tidak ditemukan.") }
    }

    @Transactional
    fun reserveStock(
        variantId: UUID,
        quantity: Int,
    ): ProductVariant {
        val variant =
            productVariantRepository.findWithLockById(variantId)
                .orElseThrow { NoSuchElementException("Varian produk tidak ditemukan.") }

        if (variant.stockAvailable < quantity) {
            throw IllegalStateException("Stok tidak mencukupi untuk produk: ${variant.sku}")
        }

        variant.stockReserved += quantity
        return productVariantRepository.save(variant)
    }
}
