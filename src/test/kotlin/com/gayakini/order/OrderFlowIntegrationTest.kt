package com.gayakini.order

import com.fasterxml.jackson.databind.ObjectMapper
import com.gayakini.cart.domain.Cart
import com.gayakini.cart.domain.CartItem
import com.gayakini.cart.domain.CartRepository
import com.gayakini.cart.domain.CartStatus
import com.gayakini.catalog.domain.Product
import com.gayakini.catalog.domain.ProductRepository
import com.gayakini.catalog.domain.ProductVariant
import com.gayakini.catalog.domain.ProductVariantRepository
import com.gayakini.checkout.domain.*
import com.gayakini.common.util.HashUtils
import com.gayakini.common.util.UuidV7Generator
import com.gayakini.order.api.PlaceOrderRequest
import com.gayakini.order.domain.OrderRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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
import java.util.UUID

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
    private lateinit var checkoutRepository: CheckoutRepository

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var quoteRepository: CheckoutShippingQuoteRepository

    private lateinit var testProduct: Product
    private lateinit var testVariant: ProductVariant
    private lateinit var testCart: Cart
    private lateinit var testCheckout: Checkout

    private val guestTokenRaw = "test-guest-raw"
    private val guestTokenHash = HashUtils.sha256(guestTokenRaw)

    @BeforeEach
    fun setup() {
        testProduct = Product(
            id = UuidV7Generator.generate(),
            slug = "test-product-${UUID.randomUUID()}",
            title = "Test Product",
            subtitle = "Subtitle",
            brandName = "Brand",
            category = null,
            description = "Desc",
        )
        productRepository.save(testProduct)

        testVariant = ProductVariant(
            id = UuidV7Generator.generate(),
            product = testProduct,
            sku = "SKU-${UUID.randomUUID()}",
            color = "Black",
            sizeCode = "M",
            priceAmount = 50000,
            weightGrams = 100,
            stockOnHand = 10,
        )
        variantRepository.save(testVariant)

        testCart = Cart(
            id = UuidV7Generator.generate(),
            customerId = null,
            accessTokenHash = guestTokenHash,
            status = CartStatus.ACTIVE,
        )
        val cartItem = CartItem(
            id = UuidV7Generator.generate(),
            cart = testCart,
            product = testProduct,
            variant = testVariant,
            productTitleSnapshot = testProduct.title,
            skuSnapshot = testVariant.sku,
            color = testVariant.color,
            sizeCode = testVariant.sizeCode,
            quantity = 2,
            unitPriceAmount = testVariant.priceAmount,
        )
        testCart.items.add(cartItem)
        cartRepository.save(testCart)

        testCheckout = Checkout(
            id = UuidV7Generator.generate(),
            cart = testCart,
            customerId = null,
            status = CheckoutStatus.READY_FOR_ORDER,
            accessTokenHash = guestTokenHash,
            subtotalAmount = 100000,
            shipping_cost_amount = 10000,
            expiresAt = null,
        )
        val address = CheckoutShippingAddress(
            checkoutId = testCheckout.id,
            checkout = testCheckout,
            recipientName = "John Doe",
            phone = "08123456789",
            line1 = "Jl. Sudirman No. 1",
            line2 = null,
            notes = null,
            areaId = "AREA1",
            district = "Jakarta Selatan",
            city = "Jakarta",
            province = "DKI Jakarta",
            postalCode = "12345",
        )
        testCheckout.shippingAddress = address
        testCheckout.items.add(
            CheckoutItem(
                id = UuidV7Generator.generate(),
                checkout = testCheckout,
                product = testProduct,
                variant = testVariant,
                productTitleSnapshot = testProduct.title,
                skuSnapshot = testVariant.sku,
                color = testVariant.color,
                sizeCode = testVariant.sizeCode,
                quantity = 2,
                unitPriceAmount = testVariant.priceAmount,
            ),
        )

        val quote = CheckoutShippingQuote(
            id = UuidV7Generator.generate(),
            checkout = testCheckout,
            provider = "BITESHIP",
            providerReference = "REF1",
            courierCode = "jne",
            courierName = "JNE",
            serviceCode = "reg",
            serviceName = "Reguler",
            description = null,
            costAmount = 10000,
            estimatedDaysMin = 1,
            estimatedDaysMax = 2,
            rawPayload = null,
            expiresAt = null
        )
        testCheckout.availableShippingQuotes.add(quote)
        testCheckout.selectedShippingQuoteId = quote.id

        checkoutRepository.save(testCheckout)
    }

    @Test
    fun `should place order successfully`() {
        val request = PlaceOrderRequest(customerNotes = "Please handle with care")
        val idempotencyKey = UUID.randomUUID().toString()

        mockMvc.perform(
            post("/v1/checkouts/{checkoutId}/orders", testCheckout.id)
                .header("Idempotency-Key", idempotencyKey)
                .header("X-Checkout-Token", guestTokenRaw)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.message").value("Pesanan berhasil dibuat."))
            .andExpect(jsonPath("$.data.subtotal.amount").value(100000))
            .andExpect(jsonPath("$.data.total.amount").value(110000))
            .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"))

        val variantAfter = variantRepository.findById(testVariant.id).get()
        assert(variantAfter.stockAvailable == 8)
        assert(variantAfter.stockReserved == 2)
    }

    @Test
    fun `should reject order when stock is insufficient`() {
        testVariant.stockOnHand = 1
        variantRepository.save(testVariant)

        val request = PlaceOrderRequest()
        val idempotencyKey = UUID.randomUUID().toString()

        mockMvc.perform(
            post("/v1/checkouts/{checkoutId}/orders", testCheckout.id)
                .header("Idempotency-Key", idempotencyKey)
                .header("X-Checkout-Token", guestTokenRaw)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.userMessage").exists())
    }

    @Test
    fun `should handle idempotency on place order`() {
        val key = UUID.randomUUID().toString()
        val request = PlaceOrderRequest()

        mockMvc.perform(
            post("/v1/checkouts/{checkoutId}/orders", testCheckout.id)
                .header("Idempotency-Key", key)
                .header("X-Checkout-Token", guestTokenRaw)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)

        mockMvc.perform(
            post("/v1/checkouts/{checkoutId}/orders", testCheckout.id)
                .header("Idempotency-Key", key)
                .header("X-Checkout-Token", guestTokenRaw)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)

        val count = orderRepository.findAll().filter { it.checkoutId == testCheckout.id }.size
        assert(count == 1)
    }
}
