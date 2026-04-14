package com.gayakini.catalog.api

import com.gayakini.BaseWebMvcTest
import com.gayakini.catalog.application.ProductService
import com.gayakini.catalog.domain.PublicProductSummary
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageImpl
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.time.Instant
import java.util.*

@WebMvcTest(ProductController::class)
@Import(BaseWebMvcTest.SecurityTestConfig::class)
class ProductControllerTest : BaseWebMvcTest() {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var productService: ProductService

    @TestConfiguration
    class Config {
        @Bean
        fun productService(): ProductService = mockk()
    }

    @Test
    fun `listProducts should return paginated products`() {
        val summary =
            PublicProductSummary(
                id = UUID.randomUUID(),
                slug = "test-product",
                title = "Test Product",
                subtitle = "Subtitle",
                brandName = "Brand",
                categorySlug = "category",
                collectionSlug = null,
                color = null,
                sizeCode = null,
                primaryImageUrl = "http://image.url",
                minPriceAmount = 10000,
                maxPriceAmount = 20000,
                inStock = true,
                createdAt = Instant.now(),
            )

        every {
            productService.searchProducts(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns PageImpl(listOf(summary))

        mockMvc.get("/v1/products")
            .andExpectStandardResponse(message = "Daftar produk berhasil diambil.")
            .andExpect {
                jsonPath("$.data[0].slug") { value("test-product") }
                jsonPath("$.meta.page") { value(1) }
            }
    }
}
