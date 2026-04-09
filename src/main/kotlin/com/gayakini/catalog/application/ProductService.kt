package com.gayakini.catalog.application

import com.gayakini.catalog.domain.Product
import com.gayakini.catalog.domain.ProductMedia
import com.gayakini.catalog.domain.ProductRepository
import com.gayakini.catalog.domain.ProductVariant
import com.gayakini.catalog.domain.ProductVariantRepository
import com.gayakini.catalog.domain.PublicProductSummary
import com.gayakini.catalog.domain.PublicProductSummaryRepository
import com.gayakini.infrastructure.storage.StorageCategory
import com.gayakini.infrastructure.storage.StorageService
import org.hibernate.Hibernate
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.InputStream
import java.time.Instant
import java.util.UUID

@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val productVariantRepository: ProductVariantRepository,
    private val publicProductSummaryRepository: PublicProductSummaryRepository,
    private val storageService: StorageService,
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

    @Transactional(readOnly = true)
    fun getProduct(id: UUID): Product {
        val product =
            productRepository.findById(id)
                .orElseThrow { NoSuchElementException("Produk tidak ditemukan.") }

        // Eagerly initialize lazy collections to avoid LazyInitializationException in controller mapping
        Hibernate.initialize(product.variants)
        Hibernate.initialize(product.media)

        return product
    }

    @Transactional
    fun reserveStock(
        variantId: UUID,
        quantity: Int,
    ): ProductVariant {
        val variant =
            productVariantRepository.findWithLockById(variantId)
                .orElseThrow { NoSuchElementException("Varian produk tidak ditemukan.") }

        check(variant.stockAvailable >= quantity) {
            "Stok tidak mencukupi untuk varian ${variant.sku}. Tersedia: ${variant.stockAvailable}"
        }

        variant.stockReserved += quantity
        return productVariantRepository.save(variant)
    }

    @Transactional
    fun uploadProductMedia(
        productId: UUID,
        inputStream: InputStream,
        originalFilename: String,
        altText: String,
        isPrimary: Boolean,
    ): ProductMedia {
        val product = getProduct(productId)

        val relativePath =
            storageService.store(
                inputStream = inputStream,
                filename = originalFilename,
                category = StorageCategory.PRODUCTS,
            )

        // If this is set as primary, unset other primary media
        if (isPrimary) {
            product.media.forEach { it.isPrimary = false }
            product.imageUrl = relativePath
        }

        val media =
            ProductMedia(
                product = product,
                url = relativePath,
                altText = altText,
                isPrimary = isPrimary,
                sortOrder = product.media.size,
            )

        product.media.add(media)
        product.updatedAt = Instant.now()
        productRepository.save(product)

        return media
    }

    @Transactional
    fun deleteProductMedia(
        productId: UUID,
        mediaId: UUID,
    ) {
        val product = getProduct(productId)
        val media =
            product.media.find { it.id == mediaId }
                ?: throw NoSuchElementException("Media tidak ditemukan.")

        storageService.delete(media.url, StorageCategory.PRODUCTS)
        product.media.remove(media)

        if (media.isPrimary) {
            product.imageUrl = product.media.find { it.isPrimary }?.url ?: product.media.firstOrNull()?.url
            product.media.find { it.url == product.imageUrl }?.isPrimary = true
        }

        product.updatedAt = Instant.now()
        productRepository.save(product)
    }
}
