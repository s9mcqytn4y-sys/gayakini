package com.gayakini.cart.api

import com.gayakini.BaseWebMvcTest
import com.gayakini.cart.application.CartService
import com.gayakini.cart.domain.Cart
import com.gayakini.cart.domain.CartStatus
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import java.util.*

@WebMvcTest(CartController::class)
@Import(BaseWebMvcTest.SecurityTestConfig::class)
class CartControllerTest : BaseWebMvcTest() {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var cartService: CartService

    @org.springframework.boot.test.context.TestConfiguration
    class ControllerTestConfig {
        @org.springframework.context.annotation.Bean
        fun cartService(): CartService = mockk()
    }

    @Test
    fun `createCart should return 200 and cart response`() {
        val cartId = UUID.randomUUID()
        val cart =
            mockk<Cart> {
                every { id } returns cartId
                every { customerId } returns null
                every { status } returns CartStatus.ACTIVE
                every { currencyCode } returns "IDR"
                every { expiresAt } returns null
                every { items } returns mutableListOf()
                every { subtotalAmount } returns 0L
                every { itemCount } returns 0
            }

        every { cartService.createCart(any(), "IDR") } returns (cart to "some-token")

        mockMvc.post("/v1/carts") {
            param("currency", "IDR")
        }.andExpectStandardResponse(200)
            .andExpect {
                jsonPath("$.data.id") { value(cartId.toString()) }
                jsonPath("$.data.accessToken") { value("some-token") }
            }
    }

    @Test
    fun `getCart should return 200 for valid cart`() {
        val cartId = UUID.randomUUID()
        val cart =
            mockk<Cart> {
                every { id } returns cartId
                every { customerId } returns null
                every { status } returns CartStatus.ACTIVE
                every { currencyCode } returns "IDR"
                every { expiresAt } returns null
                every { items } returns mutableListOf()
                every { subtotalAmount } returns 0L
                every { itemCount } returns 0
            }

        every { cartService.getValidatedCart(cartId, any(), "some-token") } returns cart

        mockMvc.get("/v1/carts/$cartId") {
            header("X-Cart-Token", "some-token")
        }.andExpectStandardResponse(200)
            .andExpect {
                jsonPath("$.data.id") { value(cartId.toString()) }
            }
    }

    @Test
    fun `addCartItem should return 200`() {
        val cartId = UUID.randomUUID()
        val variantId = UUID.randomUUID()
        val cart =
            mockk<Cart> {
                every { id } returns cartId
                every { customerId } returns null
                every { status } returns CartStatus.ACTIVE
                every { currencyCode } returns "IDR"
                every { expiresAt } returns null
                every { items } returns mutableListOf()
                every { subtotalAmount } returns 0L
                every { itemCount } returns 1
            }

        every { cartService.addItem(cartId, variantId, 2, any(), "some-token") } returns cart

        mockMvc.post("/v1/carts/$cartId/items") {
            header("X-Cart-Token", "some-token")
            contentType = MediaType.APPLICATION_JSON
            content =
                """
                {
                    "variantId": "$variantId",
                    "quantity": 2
                }
                """.trimIndent()
        }.andExpectStandardResponse(200)
    }

    @Test
    fun `updateCartItem should return 200`() {
        val cartId = UUID.randomUUID()
        val itemId = UUID.randomUUID()
        val cart =
            mockk<Cart> {
                every { id } returns cartId
                every { customerId } returns null
                every { status } returns CartStatus.ACTIVE
                every { currencyCode } returns "IDR"
                every { expiresAt } returns null
                every { items } returns mutableListOf()
                every { subtotalAmount } returns 10000L
                every { itemCount } returns 1
            }

        every { cartService.updateItem(cartId, itemId, 5, any(), "some-token") } returns cart

        mockMvc.patch("/v1/carts/$cartId/items/$itemId") {
            header("X-Cart-Token", "some-token")
            contentType = MediaType.APPLICATION_JSON
            content =
                """
                {
                    "quantity": 5
                }
                """.trimIndent()
        }.andExpectStandardResponse(200)
    }

    @Test
    fun `deleteCartItem should return 200`() {
        val cartId = UUID.randomUUID()
        val itemId = UUID.randomUUID()
        val cart =
            mockk<Cart> {
                every { id } returns cartId
                every { customerId } returns null
                every { status } returns CartStatus.ACTIVE
                every { currencyCode } returns "IDR"
                every { expiresAt } returns null
                every { items } returns mutableListOf()
                every { subtotalAmount } returns 0L
                every { itemCount } returns 0
            }

        every { cartService.removeItem(cartId, itemId, any(), "some-token") } returns cart

        mockMvc.delete("/v1/carts/$cartId/items/$itemId") {
            header("X-Cart-Token", "some-token")
        }.andExpectStandardResponse(200)
    }
}
