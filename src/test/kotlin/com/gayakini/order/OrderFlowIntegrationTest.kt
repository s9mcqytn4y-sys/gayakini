package com.gayakini.order

import com.fasterxml.jackson.databind.ObjectMapper
import com.gayakini.cart.domain.*
import com.gayakini.catalog.domain.*
import com.gayakini.common.util.HashUtils
import com.gayakini.common.util.UuidV7Generator
import com.gayakini.order.api.*
import com.gayakini.order.domain.*
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.util.*

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrderFlowIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var productRepository: ProductRepository

    @Autowired
    private lateinit var variantRepository: ProductVariantRepository

    @Autowired
    private lateinit var cartRepository: CartRepository

    @Autowired
    private lateinit var orderRepository: OrderRepository

    private lateinit var testProduct: Product
    private lateinit var testVariant: ProductVariant
    private lateinit var testCart: Cart

    private val guestTokenRaw = "test-guest-raw"
    private val guestTokenHash = HashUtils.sha256(guestTokenRaw)

    @BeforeEach
    fun setup() {
        testProduct =
            Product(
                id = UuidV7Generator.generate(),
                slug = "test-product-${UUID.randomUUID()}",
                name = "Test Product",
                description = "Desc",
                basePrice = 50000,
            )
        productRepository.save(testProduct)

        testVariant =
            ProductVariant(
                id = UuidV7Generator.generate(),
                product = testProduct,
                sku = "SKU-${UUID.randomUUID()}",
                name = "Test Variant",
                price = 50000,
                stock = 10,
                weightGrams = 100,
            )
        variantRepository.save(testVariant)

        testCart =
            Cart(
                id = UuidV7Generator.generate(),
                customerId = null,
                guestTokenHash = guestTokenHash,
                isActive = true,
            )
        testCart.items.add(
            CartItem(
                id = UuidV7Generator.generate(),
                cart = testCart,
                variantId = testVariant.id,
                quantity = 2,
            ),
        )
        cartRepository.save(testCart)
    }

    @Test
    fun `should place order successfully`() {
        val request =
            PlaceOrderRequest(
                cartId = testCart.id,
                shippingAddress =
                    ShippingAddressDto(
                        fullName = "John Doe",
                        phoneNumber = "08123456789",
                        email = "john@example.com",
                        address = "Jl. Sudirman No. 1",
                        city = "Jakarta",
                        province = "DKI Jakarta",
                        zipCode = "12345",
                    ),
                shippingService = "jne_reg",
                shippingCost = 10000,
                paymentMethod = "midtrans_snap",
                idempotencyKey = UUID.randomUUID().toString(),
            )

        mockMvc.perform(
            post("/api/v1/orders/place")
                .header("X-Guest-Token", guestTokenRaw)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.totalAmount").value(100000))
            .andExpect(jsonPath("$.data.grandTotal").value(110000))

        val variantAfter = variantRepository.findById(testVariant.id).get()
        assert(variantAfter.stock == 8)
    }

    @Test
    fun `should reject order when stock is insufficient`() {
        testVariant.stock = 1
        variantRepository.save(testVariant)

        val request =
            PlaceOrderRequest(
                cartId = testCart.id,
                shippingAddress =
                    ShippingAddressDto(
                        fullName = "John Doe",
                        phoneNumber = "08123456789",
                        email = "john@example.com",
                        address = "Jl. Sudirman No. 1",
                        city = "Jakarta",
                        province = "DKI Jakarta",
                        zipCode = "12345",
                    ),
                shippingService = "jne_reg",
                shippingCost = 10000,
                paymentMethod = "midtrans_snap",
                idempotencyKey = UUID.randomUUID().toString(),
            )

        mockMvc.perform(
            post("/api/v1/orders/place")
                .header("X-Guest-Token", guestTokenRaw)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `should handle idempotency on place order`() {
        val key = UUID.randomUUID().toString()
        val request =
            PlaceOrderRequest(
                cartId = testCart.id,
                shippingAddress =
                    ShippingAddressDto(
                        fullName = "John Doe",
                        phoneNumber = "08123456789",
                        email = "john@example.com",
                        address = "Jl. Sudirman No. 1",
                        city = "Jakarta",
                        province = "DKI Jakarta",
                        zipCode = "12345",
                    ),
                shippingService = "jne_reg",
                shippingCost = 10000,
                paymentMethod = "midtrans_snap",
                idempotencyKey = key,
            )

        // First attempt
        mockMvc.perform(
            post("/api/v1/orders/place")
                .header("X-Guest-Token", guestTokenRaw)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isOk)

        // Second attempt with same key
        mockMvc.perform(
            post("/api/v1/orders/place")
                .header("X-Guest-Token", guestTokenRaw)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isOk)

        val count = orderRepository.findAll().filter { it.idempotencyKey == key }.size
        assert(count == 1)
    }
}
