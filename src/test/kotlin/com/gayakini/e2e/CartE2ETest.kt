package com.gayakini.e2e

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import com.gayakini.catalog.domain.*
import java.util.UUID

class CartE2ETest : BaseE2ETest() {
    @Autowired
    lateinit var categoryRepository: CategoryRepository

    @Autowired
    lateinit var productRepository: ProductRepository

    private lateinit var testVariant: ProductVariant

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

        val createBody = createResponse.body ?: throw AssertionError("Create body is null")
        val cartData = createBody["data"] as Map<*, *>
        val cartId = cartData["id"] as String
        val cartToken = cartData["accessToken"] as String

        val headers = HttpHeaders()
        headers.set("X-Cart-Token", cartToken)

        // 2. Add Item
        val addRequest = E2EDataFactory.createAddCartItemRequest(UUID.fromString(testVariant.id.toString()), 2)
        val addResponse =
            restTemplate.exchange(
                "/v1/carts/$cartId/items",
                HttpMethod.POST,
                HttpEntity(addRequest, headers),
                Map::class.java,
            )

        assertEquals(HttpStatus.OK, addResponse.statusCode)
        val addBody = addResponse.body ?: throw AssertionError("Add body is null")
        val addData = addBody["data"] as Map<*, *>
        val items = addData["items"] as List<*>
        assertEquals(1, items.size)
        val item = items[0] as Map<*, *>
        assertEquals(2, item["quantity"])
        val itemId = item["id"] as String

        // 3. Update Item
        val updateRequest = E2EDataFactory.createUpdateCartItemRequest(5)
        val updateResponse =
            restTemplate.exchange(
                "/v1/carts/$cartId/items/$itemId",
                HttpMethod.PATCH,
                HttpEntity(updateRequest, headers),
                Map::class.java,
            )

        assertEquals(HttpStatus.OK, updateResponse.statusCode)
        val updateBody = updateResponse.body ?: throw AssertionError("Update body is null")
        val updateData = updateBody["data"] as Map<*, *>
        val updatedItems = updateData["items"] as List<*>
        val updatedItem = updatedItems[0] as Map<*, *>
        assertEquals(5, updatedItem["quantity"])

        // 4. Remove Item
        val removeResponse =
            restTemplate.exchange(
                "/v1/carts/$cartId/items/$itemId",
                HttpMethod.DELETE,
                HttpEntity<Any>(headers),
                Map::class.java,
            )

        assertEquals(HttpStatus.OK, removeResponse.statusCode)
        val removeBody = removeResponse.body ?: throw AssertionError("Remove body is null")
        val removeData = removeBody["data"] as Map<*, *>
        val finalItems = removeData["items"] as List<*>
        assertTrue(finalItems.isEmpty())
    }
}
