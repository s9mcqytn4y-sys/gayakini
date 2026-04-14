package com.gayakini.checkout.api

import com.gayakini.BaseWebMvcTest
import com.gayakini.cart.domain.Cart
import com.gayakini.checkout.application.CheckoutService
import com.gayakini.checkout.domain.Checkout
import com.gayakini.checkout.domain.CheckoutStatus
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.util.*

@WebMvcTest(CheckoutController::class)
@Import(BaseWebMvcTest.SecurityTestConfig::class)
class CheckoutControllerTest : BaseWebMvcTest() {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var checkoutService: CheckoutService

    @org.springframework.boot.test.context.TestConfiguration
    class ControllerTestConfig {
        @org.springframework.context.annotation.Bean
        fun checkoutService(): CheckoutService = mockk()
    }

    @Test
    fun `createCheckout should return 201`() {
        val cartId = UUID.randomUUID()
        val checkoutId = UUID.randomUUID()
        val mockCart =
            mockk<Cart> {
                every { id } returns cartId
            }
        val checkout =
            mockk<Checkout> {
                every { id } returns checkoutId
                every { cart } returns mockCart
                every { customerId } returns null
                every { status } returns CheckoutStatus.ACTIVE
                every { currencyCode } returns "IDR"
                every { items } returns mutableListOf()
                every { subtotalAmount } returns 0L
                every { shippingCostAmount } returns 0L
                every { discountAmount } returns 0L
                every { totalAmount } returns 0L
                every { promoCode } returns null
                every { expiresAt } returns null
                every { shippingAddress } returns null
                every { availableShippingQuotes } returns mutableListOf()
                every { selectedShippingQuoteId } returns null
            }

        every { checkoutService.createCheckout(cartId, any(), any()) } returns checkout

        mockMvc.post("/v1/checkouts") {
            contentType = MediaType.APPLICATION_JSON
            content = """{ "cartId": "$cartId" }"""
        }.andExpectStandardResponse(201)
            .andExpect {
                jsonPath("$.data.id") { value(checkoutId.toString()) }
            }
    }

    @Test
    fun `getCheckout should return 200`() {
        val checkoutId = UUID.randomUUID()
        val mockCart =
            mockk<Cart> {
                every { id } returns UUID.randomUUID()
            }
        val checkout =
            mockk<Checkout> {
                every { id } returns checkoutId
                every { cart } returns mockCart
                every { customerId } returns null
                every { status } returns CheckoutStatus.ACTIVE
                every { currencyCode } returns "IDR"
                every { items } returns mutableListOf()
                every { subtotalAmount } returns 0L
                every { shippingCostAmount } returns 0L
                every { discountAmount } returns 0L
                every { totalAmount } returns 0L
                every { promoCode } returns null
                every { expiresAt } returns null
                every { shippingAddress } returns null
                every { availableShippingQuotes } returns mutableListOf()
                every { selectedShippingQuoteId } returns null
            }

        every { checkoutService.getValidatedCheckout(checkoutId, any(), any()) } returns checkout

        mockMvc.get("/v1/checkouts/$checkoutId")
            .andExpectStandardResponse(200)
            .andExpect {
                jsonPath("$.data.id") { value(checkoutId.toString()) }
            }
    }
}
