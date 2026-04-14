package com.gayakini.catalog.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.gayakini.BaseWebMvcTest
import com.gayakini.catalog.application.ProductService
import com.gayakini.catalog.domain.ProductStatus
import com.gayakini.inventory.application.InventoryService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.util.*

@WebMvcTest(AdminProductController::class)
@Import(BaseWebMvcTest.SecurityTestConfig::class)
class AdminProductControllerTest : BaseWebMvcTest() {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var productService: ProductService

    @TestConfiguration
    class Config {
        @Bean
        fun productService(): ProductService = mockk()

        @Bean
        fun inventoryService(): InventoryService = mockk()
    }

    @Test
    fun `createProduct returns 401 when no token provided`() {
        val request =
            AdminCreateProductRequest(
                title = "New Product",
                subtitle = "Subtitle",
                brandName = "Brand",
                categorySlug = "category",
                description = "Description",
                slug = "new-product",
                status = ProductStatus.DRAFT,
                collections = emptyList(),
            )

        mockMvc.post("/v1/admin/products") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }
            .andExpectStandardResponse(expectedStatus = 401, success = false)
    }

    @Test
    fun `createProduct returns 403 when customer token provided`() {
        val request =
            AdminCreateProductRequest(
                title = "New Product",
                subtitle = "Subtitle",
                brandName = "Brand",
                categorySlug = "category",
                description = "Description",
                slug = "new-product",
                status = ProductStatus.DRAFT,
                collections = emptyList(),
            )

        mockMvc.post("/v1/admin/products") {
            header("Authorization", "Bearer valid-customer-token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }
            .andExpectStandardResponse(expectedStatus = 403, success = false)
    }

    @Test
    fun `createProduct returns 201 when admin token provided`() {
        val request =
            AdminCreateProductRequest(
                title = "New Product",
                subtitle = "Subtitle",
                brandName = "Brand",
                categorySlug = "category",
                description = "Description",
                slug = "new-product",
                status = ProductStatus.DRAFT,
                collections = emptyList(),
            )

        val product =
            mockk<com.gayakini.catalog.domain.Product>(relaxed = true) {
                every { id } returns UUID.randomUUID()
                every { title } returns "New Product"
                every { slug } returns "new-product"
                every { status } returns ProductStatus.DRAFT
                every { category?.slug } returns "category"
            }

        every { productService.createProduct(any()) } returns product

        mockMvc.post("/v1/admin/products") {
            header("Authorization", "Bearer valid-admin-token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }
            .andExpect {
                status { isCreated() }
                jsonPath("$.message") { value("Product created successfully.") }
                jsonPath("$.data.title") { value("New Product") }
            }
    }
}
