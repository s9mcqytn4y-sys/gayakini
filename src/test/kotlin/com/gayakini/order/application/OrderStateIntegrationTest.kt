package com.gayakini.order.application

import com.gayakini.BaseDbIntegrationTest
import com.gayakini.cart.domain.Cart
import com.gayakini.cart.domain.CartRepository
import com.gayakini.catalog.domain.Product
import com.gayakini.catalog.domain.ProductRepository
import com.gayakini.catalog.domain.ProductStatus
import com.gayakini.catalog.domain.ProductVariant
import com.gayakini.catalog.domain.ProductVariantRepository
import com.gayakini.checkout.domain.Checkout
import com.gayakini.checkout.domain.CheckoutRepository
import com.gayakini.inventory.domain.AdjustmentReason
import com.gayakini.inventory.domain.InventoryAdjustmentRepository
import com.gayakini.inventory.domain.InventoryReservationRepository
import com.gayakini.inventory.domain.ReservationStatus
import com.gayakini.order.domain.FulfillmentStatus
import com.gayakini.order.domain.Order
import com.gayakini.order.domain.OrderItem
import com.gayakini.order.domain.OrderRepository
import com.gayakini.order.domain.OrderShippingAddress
import com.gayakini.order.domain.OrderShippingSelection
import com.gayakini.order.domain.OrderStatus
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Transactional
@Tag("integration")
class OrderStateIntegrationTest : BaseDbIntegrationTest() {
    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var reservationRepository: InventoryReservationRepository

    @Autowired
    private lateinit var adjustmentRepository: InventoryAdjustmentRepository

    @Autowired
    private lateinit var productRepository: ProductRepository

    @Autowired
    private lateinit var variantRepository: ProductVariantRepository

    @Autowired
    private lateinit var cartRepository: CartRepository

    @Autowired
    private lateinit var checkoutRepository: CheckoutRepository

    @Test
    fun `should complete full order lifecycle and manage inventory correctly`() {
        // 1. Setup Data: Product with stock
        val initialStock = 10
        val product = createTestProduct("Test Product", initialStock)
        val variant = product.variants.first()
        val variantId = variant.id

        // 2. Create Order
        val order = createTestOrder(variant)
        val orderId = order.id

        // Verify Reservation exists
        val reservations = reservationRepository.findAllByOrderIdAndStatus(orderId, ReservationStatus.ACTIVE)
        assertEquals(1, reservations.size)
        assertEquals(1, reservations[0].quantity)

        // Verify Stock reserved but not consumed yet
        val variantAfterReservation = variantRepository.findById(variantId).get()
        assertEquals(initialStock, variantAfterReservation.stockOnHand)
        assertEquals(1, variantAfterReservation.stockReserved)

        // Verify Ledger for Reservation
        val reservationAdjustments =
            adjustmentRepository.findAll().filter { it.reasonCode == AdjustmentReason.RESERVATION }
        assertTrue(reservationAdjustments.isNotEmpty())

        // 3. Transition to PAID
        val orderForPayment = orderRepository.findById(orderId).get()
        orderForPayment.markAsPaid()
        orderRepository.save(orderForPayment)

        // Re-implementing the key parts of handlePaidOrder in the test to verify consistency
        orderForPayment.items.forEach { inventoryService.consumeReservation(it.id) }

        // Verify Stock consumed
        val variantAfterPayment = variantRepository.findById(variantId).get()
        assertEquals(initialStock - 1, variantAfterPayment.stockOnHand)
        assertEquals(0, variantAfterPayment.stockReserved)

        // Verify Ledger for SALE
        val saleAdjustments = adjustmentRepository.findAll().filter { it.reasonCode == AdjustmentReason.SALE }
        assertEquals(1, saleAdjustments.size)
        assertEquals(-1, saleAdjustments[0].quantityDelta)

        // 4. Transition through Shipping Statuses
        val paidOrder = orderRepository.findById(orderId).get()
        paidOrder.markAsReadyToShip()
        orderRepository.save(paidOrder)

        val readyOrder = orderRepository.findById(orderId).get()
        assertEquals(OrderStatus.READY_TO_SHIP, readyOrder.status)

        readyOrder.markAsShipped()
        orderRepository.save(readyOrder)

        val shippedOrder = orderRepository.findById(orderId).get()
        assertEquals(OrderStatus.SHIPPED, shippedOrder.status)

        shippedOrder.markAsCompleted()
        orderRepository.save(shippedOrder)

        val completedOrder = orderRepository.findById(orderId).get()
        assertEquals(OrderStatus.COMPLETED, completedOrder.status)
        assertEquals(FulfillmentStatus.DELIVERED, completedOrder.fulfillmentStatus)
    }

    @Test
    fun `should release reservations when pending order is cancelled`() {
        // 1. Setup Data: Product with stock
        val initialStock = 10
        val product = createTestProduct("Pending Cancel Product", initialStock)
        val variant = product.variants.first()
        val variantId = variant.id

        // 2. Create Order (Status: PENDING_PAYMENT)
        val order = createTestOrder(variant)
        val orderId = order.id

        // Verify Stock reserved
        val variantAfterReservation = variantRepository.findById(variantId).get()
        assertEquals(initialStock, variantAfterReservation.stockOnHand)
        assertEquals(1, variantAfterReservation.stockReserved)

        // 3. Cancel Order
        val orderToCancel = orderRepository.findById(orderId).get()
        orderToCancel.cancel("Customer changed mind")
        orderRepository.save(orderToCancel)

        inventoryService.releaseReservations(orderId, "Customer changed mind")

        // 4. Verify Stock levels
        val variantAfterCancel = variantRepository.findById(variantId).get()
        assertEquals(initialStock, variantAfterCancel.stockOnHand)
        assertEquals(0, variantAfterCancel.stockReserved)

        // 5. Verify Ledger
        val adjustments = adjustmentRepository.findAll().filter { it.variant.id == variantId }
        val releaseAdjustment = adjustments.find { it.reasonCode == AdjustmentReason.RESERVATION_RELEASE }
        assertNotNull(releaseAdjustment)
        assertEquals(0, releaseAdjustment?.quantityDelta)
        assertEquals(initialStock, releaseAdjustment?.stockOnHandAfter)
        assertEquals(0, releaseAdjustment?.stockReservedAfter)
    }

    @Test
    fun `should restock inventory when paid order is cancelled`() {
        // 1. Setup Data: Product with stock
        val initialStock = 10
        val product = createTestProduct("Paid Cancel Product", initialStock)
        val variant = product.variants.first()
        val variantId = variant.id

        // 2. Create Order and Pay (Simulate consumption)
        val order = createTestOrder(variant)
        val orderId = order.id

        // Simulate transition to PAID and consumption
        val orderToPay = orderRepository.findById(orderId).get()
        orderToPay.markAsPaid()
        orderRepository.save(orderToPay)
        inventoryService.consumeReservation(orderToPay.items.first().id)

        // Verify Stock consumed
        val variantAfterPayment = variantRepository.findById(variantId).get()
        assertEquals(initialStock - 1, variantAfterPayment.stockOnHand)
        assertEquals(0, variantAfterPayment.stockReserved)

        // 3. Cancel Order
        val orderToCancel = orderRepository.findById(orderId).get()
        orderToCancel.cancel("Out of stock or something")
        orderRepository.save(orderToCancel)

        // In OrderService.cancelOrder, it calls restockOrder if status was PAID
        inventoryService.restockOrder(orderId, AdjustmentReason.CANCELLATION_RESTOCK, "Test restock")

        // 4. Verify Stock levels
        val variantAfterCancel = variantRepository.findById(variantId).get()
        assertEquals(initialStock, variantAfterCancel.stockOnHand)
        assertEquals(0, variantAfterCancel.stockReserved)

        // 5. Verify Ledger
        val adjustments = adjustmentRepository.findAll().filter { it.variant.id == variantId }
        val restockAdjustment = adjustments.find { it.reasonCode == AdjustmentReason.CANCELLATION_RESTOCK }
        assertNotNull(restockAdjustment)
        assertEquals(1, restockAdjustment?.quantityDelta)
        assertEquals(initialStock, restockAdjustment?.stockOnHandAfter)
        assertEquals(0, restockAdjustment?.stockReservedAfter)
    }

    // Helper for manual consumption in test context since we're skipping the webhook controller layer
    @Autowired
    private lateinit var inventoryService: com.gayakini.inventory.application.InventoryService

    private fun createTestProduct(
        name: String,
        stock: Int,
    ): Product {
        val uniqueSuffix = UUID.randomUUID().toString().take(8)
        val product =
            Product(
                id = UUID.randomUUID(),
                title = "$name $uniqueSuffix",
                slug = "${name.lowercase().replace(" ", "-")}-$uniqueSuffix",
                subtitle = null,
                brandName = "Test Brand",
                description = "Test Description",
                status = ProductStatus.PUBLISHED,
            )
        val variant =
            ProductVariant(
                id = UUID.randomUUID(),
                product = product,
                sku = "SKU-${UUID.randomUUID().toString().take(8)}",
                priceAmount = 100000,
                stockOnHand = stock,
                weightGrams = 500,
                color = "Default",
                sizeCode = "All",
            )
        product.variants.add(variant)
        return productRepository.save(product)
    }

    private fun createTestOrder(variant: ProductVariant): Order {
        val cart = cartRepository.saveAndFlush(Cart(id = UUID.randomUUID(), accessTokenHash = "test-token"))
        val checkout =
            checkoutRepository.saveAndFlush(
                Checkout(
                    id = UUID.randomUUID(),
                    cart = cart,
                    accessTokenHash = cart.accessTokenHash,
                ),
            )

        val order =
            Order(
                orderNumber = "ORD-TEST-${UUID.randomUUID().toString().take(12).uppercase()}",
                checkoutId = checkout.id,
                cartId = cart.id,
                customerId = null,
                accessTokenHash = cart.accessTokenHash,
                status = OrderStatus.PENDING_PAYMENT,
                subtotalAmount = variant.priceAmount,
                shippingCostAmount = 10000,
                discountAmount = 0,
            )
        val item =
            OrderItem(
                order = order,
                product = variant.product,
                variant = variant,
                skuSnapshot = variant.sku,
                titleSnapshot = variant.product.title,
                color = "Default",
                sizeCode = "All",
                quantity = 1,
                unitPriceAmount = variant.priceAmount,
            )
        order.items.add(item)

        // Manual shipping address snapshot
        order.shippingAddress =
            OrderShippingAddress(
                orderId = order.id,
                order = order,
                recipientName = "Test",
                phone = "08123",
                line1 = "Jl. Test",
                line2 = null,
                notes = null,
                areaId = "1",
                district = "Test",
                city = "Test",
                province = "Test",
                postalCode = "12345",
            )

        // Manual shipping selection snapshot
        order.shippingSelection =
            OrderShippingSelection(
                orderId = order.id,
                order = order,
                courierCode = "jne",
                courierName = "JNE",
                serviceCode = "reg",
                serviceName = "Regular",
                costAmount = 10000,
                providerReference = "ref",
                description = null,
                estimatedDaysMin = null,
                estimatedDaysMax = null,
                rawQuotePayload = null,
            )

        val saved = orderRepository.save(order)

        // Proper reservation via InventoryService to ensure ledger consistency
        inventoryService.reserveStock(
            orderId = saved.id,
            orderItemId = saved.items.first().id,
            variantId = variant.id,
            quantity = 1,
        )

        return saved
    }
}
