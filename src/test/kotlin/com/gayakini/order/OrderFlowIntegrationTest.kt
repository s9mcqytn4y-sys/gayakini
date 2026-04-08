package com.gayakini.order

import com.gayakini.cart.domain.Cart
import com.gayakini.cart.domain.CartItem
import com.gayakini.cart.domain.CartRepository
import com.gayakini.catalog.domain.*
import com.gayakini.checkout.domain.*
import com.gayakini.common.api.UnauthorizedException
import com.gayakini.common.util.HashUtils
import com.gayakini.common.util.UuidV7Generator
import com.gayakini.customer.domain.Customer
import com.gayakini.customer.domain.CustomerRepository
import com.gayakini.infrastructure.security.UserPrincipal
import com.gayakini.order.api.PlaceOrderRequest
import com.gayakini.order.application.OrderService
import com.gayakini.order.domain.OrderRepository
import com.gayakini.order.domain.OrderStatus
import com.gayakini.payment.application.PaymentService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderFlowIntegrationTest {
    @Autowired
    lateinit var orderService: OrderService

    @Autowired
    lateinit var orderRepository: OrderRepository

    @Autowired
    lateinit var cartRepository: CartRepository

    @Autowired
    lateinit var customerRepository: CustomerRepository

    @Autowired
    lateinit var productRepository: ProductRepository

    @Autowired
    lateinit var categoryRepository: CategoryRepository

    @Autowired
    lateinit var checkoutRepository: CheckoutRepository

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    lateinit var paymentService: PaymentService

    private lateinit var testCustomer: Customer
    private lateinit var testProduct: Product
    private lateinit var testVariant: ProductVariant
    private lateinit var testCart: Cart
    private lateinit var testCheckout: Checkout
    private val guestToken = "test-guest-token"
    private val guestTokenHash = HashUtils.sha256(guestToken)

    @BeforeEach
    fun setup() {
        // Clear security context to simulate guest by default
        SecurityContextHolder.clearContext()

        val category =
            categoryRepository.save(
                Category(
                    id = UuidV7Generator.generate(),
                    slug = "test-category",
                    name = "Test Category",
                    description = "Test Description",
                ),
            )

        testProduct =
            productRepository.save(
                Product(
                    id = UuidV7Generator.generate(),
                    slug = "test-product",
                    title = "Test Product",
                    subtitle = "Test Subtitle",
                    brandName = "GAYAKINI",
                    category = category,
                    description = "Test Product Description",
                    status = ProductStatus.PUBLISHED,
                ),
            )

        // Increased to avoid insufficient stock errors
        testVariant =
            ProductVariant(
                id = UuidV7Generator.generate(),
                product = testProduct,
                sku = "TEST-SKU-1",
                sizeCode = "M",
                color = "Black",
                priceAmount = 50000,
                stockOnHand = 100,
            )
        testProduct.variants.add(testVariant)
        productRepository.save(testProduct)

        testCustomer =
            customerRepository.save(
                Customer(
                    id = UuidV7Generator.generate(),
                    email = "test@example.com",
                    passwordHash = passwordEncoder.encode("password"),
                    fullName = "Test User",
                    phone = "08123456789",
                ),
            )

        testCart =
            cartRepository.save(
                Cart(
                    id = UuidV7Generator.generate(),
                    customerId = null,
                    accessTokenHash = guestTokenHash,
                ),
            )

        val cartItem =
            CartItem(
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

        testCheckout =
            Checkout(
                id = UuidV7Generator.generate(),
                cart = testCart,
                customerId = null,
                status = CheckoutStatus.READY_FOR_ORDER,
                accessTokenHash = guestTokenHash,
                subtotalAmount = 100000,
                shippingCostAmount = 10000,
                expiresAt = null,
            )
        val address =
            CheckoutShippingAddress(
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

        val quote =
            CheckoutShippingQuote(
                id = UuidV7Generator.generate(),
                checkout = testCheckout,
                provider = "BITESHIP",
                providerReference = "REF123",
                courierCode = "jne",
                courierName = "JNE",
                serviceCode = "reg",
                serviceName = "Regular",
                description = "Reguler Service",
                costAmount = 10000,
                estimatedDaysMin = 1,
                estimatedDaysMax = 3,
                isRecommended = true,
                rawPayload = null,
                expiresAt = null,
            )
        testCheckout.availableShippingQuotes.add(quote)
        testCheckout.selectedShippingQuoteId = quote.id

        checkoutRepository.save(testCheckout)
    }

    @Suppress("unused")
    private fun authenticateAs(customer: Customer) {
        val principal = UserPrincipal(id = customer.id, email = customer.email, role = "CUSTOMER")
        val auth = UsernamePasswordAuthenticationToken(principal, null, principal.toAuthorities())
        SecurityContextHolder.getContext().authentication = auth
    }

    @Test
    fun `should place order from checkout successfully`() {
        // Given
        val idempotencyKey = "test-idempotency-key"
        val request = PlaceOrderRequest(customerNotes = "Test notes")

        // When
        val order =
            orderService.placeOrderFromCheckout(
                checkoutId = testCheckout.id,
                idempotencyKey = idempotencyKey,
                checkoutToken = guestToken,
                request = request,
            )

        // Then
        assertNotNull(order)
        assertEquals(testCheckout.id, order.checkoutId)
        assertEquals(OrderStatus.PENDING_PAYMENT, order.status)
        assertEquals(110000, order.totalAmount)

        val savedOrder = orderRepository.findById(order.id).orElseThrow()
        assertEquals(order.orderNumber, savedOrder.orderNumber)
    }

    @Test
    fun `should reject guest order access without token`() {
        val order =
            orderService.placeOrderFromCheckout(
                checkoutId = testCheckout.id,
                idempotencyKey = "guest-order-access-key",
                checkoutToken = guestToken,
                request = PlaceOrderRequest(customerNotes = "Test notes"),
            )

        val error =
            assertThrows(UnauthorizedException::class.java) {
                orderService.getAuthorizedOrder(order.id, null)
            }

        assertEquals("Token pesanan diperlukan.", error.message)
    }

    @Test
    fun `should reject order if stock is insufficient`() {
        // Given
        testVariant.stockOnHand = 1
        testVariant.stockReserved = 0
        productRepository.save(testProduct)

        val request = PlaceOrderRequest(customerNotes = "Test notes")

        // When & Then
        val exception = assertThrows(IllegalStateException::class.java) {
            orderService.placeOrderFromCheckout(
                checkoutId = testCheckout.id,
                idempotencyKey = "insufficient-stock-key",
                checkoutToken = guestToken,
                request = request,
            )
        }
        assertTrue(exception.message!!.contains("Stok tidak mencukupi"))
    }

    @Test
    fun `should reject order if product is not published`() {
        // Given
        testProduct.status = ProductStatus.DRAFT
        productRepository.save(testProduct)

        val request = PlaceOrderRequest(customerNotes = "Test notes")

        // When & Then
        val exception = assertThrows(IllegalStateException::class.java) {
            orderService.placeOrderFromCheckout(
                checkoutId = testCheckout.id,
                idempotencyKey = "unpublished-product-key",
                checkoutToken = guestToken,
                request = request,
            )
        }
        assertTrue(exception.message!!.contains("tidak tersedia"))
    }

    @Test
    fun `should reject order if variant is inactive`() {
        // Given
        testVariant.status = VariantStatus.INACTIVE
        productRepository.save(testProduct)

        val request = PlaceOrderRequest(customerNotes = "Test notes")

        // When & Then
        val exception = assertThrows(IllegalStateException::class.java) {
            orderService.placeOrderFromCheckout(
                checkoutId = testCheckout.id,
                idempotencyKey = "inactive-variant-key",
                checkoutToken = guestToken,
                request = request,
            )
        }
        assertTrue(exception.message!!.contains("tidak tersedia"))
    }
}
