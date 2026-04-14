package com.gayakini.catalog.application

import com.gayakini.catalog.api.AdminCreateProductRequest
import com.gayakini.catalog.domain.*
import com.gayakini.infrastructure.storage.StorageService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

class ProductServiceUnitTest {

    private val productRepository = mockk<ProductRepository>()
    private val publicProductSummaryRepository = mockk<PublicProductSummaryRepository>()
    private val categoryRepository = mockk<CategoryRepository>()
    private val collectionRepository = mockk<CollectionRepository>()
    private val storageService = mockk<StorageService>()

    private val productService = ProductService(
        productRepository,
        publicProductSummaryRepository,
        categoryRepository,
        collectionRepository,
        storageService
    )

    @Test
    fun `createProduct should correctly map request and save`() {
        val category = Category(id = UUID.randomUUID(), name = "Shoes", slug = "shoes", description = "Shoes category")
        val request = AdminCreateProductRequest(
            slug = "cool-shoes",
            title = "Cool Shoes",
            subtitle = "Very cool",
            brandName = "Gaya",
            categorySlug = "shoes",
            collections = emptyList(),
            description = "Best shoes",
            status = ProductStatus.DRAFT
        )

        every { categoryRepository.findBySlug("shoes") } returns Optional.of(category)
        every { productRepository.save(any()) } answers { it.invocation.args[0] as Product }

        val created = productService.createProduct(request)

        assertEquals("cool-shoes", created.slug)
        assertEquals("Cool Shoes", created.title)
        assertEquals(category, created.category)
        verify { productRepository.save(any()) }
    }

    @Test
    fun `updateProduct should update fields correctly`() {
        val productId = UUID.randomUUID()
        val existingProduct = Product(
            id = productId,
            slug = "old-slug",
            title = "Old Title",
            subtitle = "Old Sub",
            brandName = "Old Brand",
            description = "Old Desc",
            status = ProductStatus.DRAFT
        )

        val request = com.gayakini.catalog.api.AdminUpdateProductRequest(
            title = "New Title",
            slug = "new-slug",
            status = ProductStatus.PUBLISHED
        )

        every { productRepository.findById(productId) } returns Optional.of(existingProduct)
        every { productRepository.save(any()) } answers { it.invocation.args[0] as Product }

        val updated = productService.updateProduct(productId, request)

        assertEquals("New Title", updated.title)
        assertEquals("new-slug", updated.slug)
        assertEquals(ProductStatus.PUBLISHED, updated.status)
        assertEquals("Old Brand", updated.brandName) // Unchanged
    }
}
