package com.gayakini.catalog.application

import com.gayakini.catalog.api.AdminCreateProductRequest
import com.gayakini.catalog.api.AdminUpdateProductRequest
import com.gayakini.catalog.domain.*
import com.gayakini.common.util.UuidV7Generator
import com.gayakini.infrastructure.storage.StorageCategory
import com.gayakini.infrastructure.storage.StorageService
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
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
    private val publicProductSummaryRepository: PublicProductSummaryRepository,
    private val categoryRepository: CategoryRepository,
    private val collectionRepository: CollectionRepository,
    private val storageService: StorageService,
) {
    @Transactional
    @CacheEvict(cacheNames = ["products", "product_summaries"], allEntries = true)
    fun createProduct(request: AdminCreateProductRequest): Product {
        val category =
            categoryRepository.findBySlug(request.categorySlug)
                .orElseThrow { NoSuchElementException("Kategori dengan slug ${request.categorySlug} tidak ditemukan.") }

        val collections =
            request.collections.map { slug ->
                collectionRepository.findBySlug(slug)
                    .orElseThrow { NoSuchElementException("Koleksi dengan slug $slug tidak ditemukan.") }
            }

        val product =
            Product(
                id = UuidV7Generator.generate(),
                slug = request.slug,
                title = request.title,
                subtitle = request.subtitle,
                brandName = request.brandName,
                category = category,
                description = request.description,
                status = request.status,
            ).apply {
                this.collections.addAll(collections)
            }

        return productRepository.save(product)
    }

    @Transactional
    @CacheEvict(cacheNames = ["products", "product_summaries"], allEntries = true)
    fun updateProduct(
        id: UUID,
        request: AdminUpdateProductRequest,
    ): Product {
        val product = getProduct(id)

        request.title?.let { product.title = it }
        request.slug?.let { product.slug = it }
        request.subtitle?.let { product.subtitle = it }
        request.brandName?.let { product.brandName = it }
        request.description?.let { product.description = it }
        request.status?.let { product.status = it }

        request.categorySlug?.let { slug ->
            val category =
                categoryRepository.findBySlug(slug)
                    .orElseThrow { NoSuchElementException("Kategori dengan slug $slug tidak ditemukan.") }
            product.category = category
        }

        request.collections?.let { slugs ->
            val collections =
                slugs.map { slug ->
                    collectionRepository.findBySlug(slug)
                        .orElseThrow { NoSuchElementException("Koleksi dengan slug $slug tidak ditemukan.") }
                }
            product.collections.clear()
            product.collections.addAll(collections)
        }

        product.updatedAt = Instant.now()
        return productRepository.save(product)
    }

    @Cacheable(
        cacheNames = ["product_summaries"],
        key =
            "{#page, #size, #sort, #q, #categorySlug, #collectionSlug, #color, #sizeCode, #minPrice, " +
                "#maxPrice, #inStock}",
    )
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
    @Cacheable(cacheNames = ["products"], key = "#id")
    fun getProduct(id: UUID): Product {
        return productRepository.findById(id)
            .orElseThrow { NoSuchElementException("Produk tidak ditemukan.") }
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
