package com.gayakini.order.application

import com.gayakini.cart.domain.CartRepository
import com.gayakini.checkout.domain.CheckoutRepository
import com.gayakini.common.api.ForbiddenException
import com.gayakini.common.api.UnauthorizedException
import com.gayakini.common.util.HashUtils
import com.gayakini.infrastructure.security.SecurityUtils
import com.gayakini.infrastructure.security.UserPrincipal
import com.gayakini.inventory.domain.AdjustmentReason
import com.gayakini.order.domain.*
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*

class OrderServiceUnitTest {

    private val orderRepository = mockk<OrderRepository>()
    private val checkoutRepository = mockk<CheckoutRepository>()
    private val cartRepository = mockk<CartRepository>()
    private val cartService = mockk<com.gayakini.cart.application.CartService>()
    private val inventoryService = mockk<com.gayakini.inventory.application.InventoryService>()
    private val promoService = mockk<com.gayakini.promo.application.PromoService>()
    private val idempotencyService = mockk<com.gayakini.common.infrastructure.IdempotencyService>()
    private val eventPublisher = mockk<org.springframework.context.ApplicationEventPublisher>()
    private val auditContext = mockk<com.gayakini.audit.application.AuditContext>()

    private val orderService = OrderService(
        orderRepository,
        checkoutRepository,
        cartRepository,
        cartService,
        inventoryService,
        promoService,
        idempotencyService,
        eventPublisher,
        auditContext
    )

    @BeforeEach
    fun setUp() {
        mockkObject(SecurityUtils)
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(SecurityUtils)
    }

    @Test
    fun `validateOrderAccess should allow ADMIN regardless of ownership`() {
        val admin = UserPrincipal(UUID.randomUUID(), "admin@test.com", "ADMIN")
        val order = mockk<Order>()
        every { order.customerId } returns UUID.randomUUID()
        every { SecurityUtils.getCurrentUser() } returns admin

        orderService.validateOrderAccess(order, null)
    }

    @Test
    fun `validateOrderAccess should throw Forbidden if CUSTOMER tries to access another's order`() {
        val customerId = UUID.randomUUID()
        val otherCustomerId = UUID.randomUUID()
        val customer = UserPrincipal(customerId, "user@test.com", "CUSTOMER")
        val order = mockk<Order>()
        every { order.customerId } returns otherCustomerId
        every { SecurityUtils.getCurrentUser() } returns customer

        assertThrows<ForbiddenException> {
            orderService.validateOrderAccess(order, null)
        }
    }

    @Test
    fun `validateOrderAccess should allow GUEST with valid token`() {
        val guestToken = "secret-guest-token"
        val tokenHash = HashUtils.sha256(guestToken)
        val order = mockk<Order>()
        every { order.customerId } returns null
        every { order.accessTokenHash } returns tokenHash
        every { SecurityUtils.getCurrentUser() } returns null

        orderService.validateOrderAccess(order, guestToken)
    }

    @Test
    fun `validateOrderAccess should throw Unauthorized for GUEST with invalid token`() {
        val order = mockk<Order>()
        every { order.customerId } returns null
        every { order.accessTokenHash } returns "some-hash"
        every { SecurityUtils.getCurrentUser() } returns null

        assertThrows<UnauthorizedException> {
            orderService.validateOrderAccess(order, "wrong-token")
        }
    }

    @Test
    fun `cancelOrder should trigger restock for SHIPPED orders`() {
        val orderId = UUID.randomUUID()
        val customerId = UUID.randomUUID()
        val order = createTestOrder(orderId, customerId, OrderStatus.SHIPPED)

        setupCancelMocks(order, customerId)

        orderService.cancelOrder(orderId, "Customer changed mind", "idempotency-key", null)

        verify { order.cancel("Customer changed mind") }
        verify { inventoryService.releaseReservations(orderId, any()) }
        verify {
            inventoryService.restockOrder(
                orderId = orderId,
                reason = AdjustmentReason.CANCELLATION_RESTOCK,
                note = match { it.contains("Customer changed mind") }
            )
        }
        verify { orderRepository.save(order) }
    }

    @Test
    fun `cancelOrder should trigger restock for COMPLETED orders`() {
        val orderId = UUID.randomUUID()
        val customerId = UUID.randomUUID()
        val order = createTestOrder(orderId, customerId, OrderStatus.COMPLETED)

        setupCancelMocks(order, customerId)

        orderService.cancelOrder(orderId, "Defective product", "idempotency-key", null)

        verify { order.cancel("Defective product") }
        verify {
            inventoryService.restockOrder(
                orderId = orderId,
                reason = AdjustmentReason.CANCELLATION_RESTOCK,
                note = match { it.contains("Defective product") }
            )
        }
    }

    private fun createTestOrder(id: UUID, customerId: UUID?, status: OrderStatus): Order {
        val order = mockk<Order>(relaxed = true)
        every { order.id } returns id
        every { order.customerId } returns customerId
        every { order.status } returns status
        every { order.promoCode } returns null
        return order
    }

    private fun setupCancelMocks(order: Order, customerId: UUID?) {
        val orderId = order.id
        every { orderRepository.findById(orderId) } returns Optional.of(order)
        every { SecurityUtils.getCurrentUserId() } returns customerId
        every { SecurityUtils.getCurrentUser() } returns UserPrincipal(customerId ?: UUID.randomUUID(), "test@test.com", "CUSTOMER")

        // Mock idempotencyService to just execute the block
        val slot = slot<() -> Order>()
        every {
            idempotencyService.handle<Order>(
                scope = any(),
                key = any(),
                requestPayload = any(),
                requesterType = any(),
                requesterId = any(),
                ttlSeconds = any(),
                action = capture(slot)
            )
        } answers { slot.captured() }

        every { inventoryService.releaseReservations(any(), any()) } just Runs
        every { inventoryService.restockOrder(any(), any(), any()) } just Runs
        every { orderRepository.save(any()) } returns order
    }
}
