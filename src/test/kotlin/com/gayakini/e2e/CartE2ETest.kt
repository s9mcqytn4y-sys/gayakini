package com.gayakini.e2e

import com.gayakini.catalog.domain.CategoryRepository
import com.gayakini.catalog.domain.ProductRepository
import com.gayakini.catalog.domain.ProductVariant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.util.UUID

class CartE2ETest : BaseE2ETest() {
    @Autowired
    lateinit var categoryRepository: CategoryRepository

    @Autowired
    lateinit var productRepository: ProductRepository

    private lateinit var testVariant: ProductVariant

    companion object {
        private const val INITIAL_QUANTITY = 2
        private const val UPDATED_QUANTITY = 5
    }

    @BeforeEach
    fun setup() {
        cleanupDatabase()
        seedCatalog()
    }

    private fun seedCatalog() {
        val category = categoryRepository.save(E2EDataFactory.createCategory())
        val product = productRepository.save(E2EDataFactory.createProduct(category))
        testVariant = E2EDataFactory.createVariant(product)
        product.variants.add(testVariant)
        productRepository.save(product)
    }

    @Test
    fun `should perform full cart lifecycle successfully`() {
        // 1. Create Cart
        val createResponse = restTemplate.postForEntity("/v1/carts", null, Map::class.java)
        assertEquals(HttpStatus.OK, createResponse.statusCode)

        val cartId = extractIdFromResponse(createResponse)
        val cartToken = extractTokenFromResponse(createResponse)

        val headers = HttpHeaders()
        headers.set("X-Cart-Token", cartToken)

        // 2. Add Item
        val itemId = addItemToCart(cartId, headers)

        // 3. Update Item
        updateCartItem(cartId, itemId, headers)

        // 4. Remove Item
        removeCartItem(cartId, itemId, headers)
    }

    private fun extractIdFromResponse(response: org.springframework.http.ResponseEntity<Map<*, *>>): String {
        val body = response.body
        checkNotNull(body) { "Response body is null" }
        val data = body["data"] as Map<*, *>
        return data["id"] as String
    }

    private fun extractTokenFromResponse(response: org.springframework.http.ResponseEntity<Map<*, *>>): String {
        val body = response.body
        checkNotNull(body) { "Response body is null" }
        val data = body["data"] as Map<*, *>
        return data["accessToken"] as String
    }

    private fun addItemToCart(
        cartId: String,
        headers: HttpHeaders,
    ): String {
        val addRequest =
            E2EDataFactory.createAddCartItemRequest(
                UUID.fromString(testVariant.id.toString()),
                INITIAL_QUANTITY,
            )
        val response =
            restTemplate.exchange(
                "/v1/carts/$cartId/items",
                HttpMethod.POST,
                HttpEntity(addRequest, headers),
                Map::class.java,
            )

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = response.body
        checkNotNull(body) { "Add body is null" }
        val data = body["data"] as Map<*, *>
        val items = data["items"] as List<*>
        assertEquals(1, items.size)
        val item = items[0] as Map<*, *>
        assertEquals(INITIAL_QUANTITY, item["quantity"])
        return item["id"] as String
    }

    private fun updateCartItem(
        cartId: String,
        itemId: String,
        headers: HttpHeaders,
    ) {
        val updateRequest = E2EDataFactory.createUpdateCartItemRequest(UPDATED_QUANTITY)
        val response =
            restTemplate.exchange(
                "/v1/carts/$cartId/items/$itemId",
                HttpMethod.PATCH,
                HttpEntity(updateRequest, headers),
                Map::class.java,
            )

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = response.body
        checkNotNull(body) { "Update body is null" }
        val data = body["data"] as Map<*, *>
        val updatedItems = data["items"] as List<*>
        val updatedItem = updatedItems[0] as Map<*, *>
        assertEquals(UPDATED_QUANTITY, updatedItem["quantity"])
    }

    private fun removeCartItem(
        cartId: String,
        itemId: String,
        headers: HttpHeaders,
    ) {
        val response =
            restTemplate.exchange(
                "/v1/carts/$cartId/items/$itemId",
                HttpMethod.DELETE,
                HttpEntity<Any>(headers),
                Map::class.java,
            )

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = response.body
        checkNotNull(body) { "Remove body is null" }
        val data = body["data"] as Map<*, *>
        val finalItems = data["items"] as List<*>
        assertTrue(finalItems.isEmpty())
    }
}
