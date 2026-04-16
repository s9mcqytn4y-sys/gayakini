package com.gayakini.inventory.application

import com.gayakini.BaseDbIntegrationTest
import com.gayakini.cart.domain.Cart
import com.gayakini.cart.domain.CartRepository
import com.gayakini.catalog.domain.Product
import com.gayakini.catalog.domain.ProductRepository
import com.gayakini.catalog.domain.ProductStatus
import com.gayakini.catalog.domain.ProductVariant
import com.gayakini.checkout.domain.Checkout
import com.gayakini.checkout.domain.CheckoutRepository
import com.gayakini.inventory.domain.InventoryMovementRepository
import com.gayakini.inventory.domain.MovementType
import com.gayakini.operations.api.PackOrderRequest
import com.gayakini.operations.application.WarehouseService
import com.gayakini.order.domain.FulfillmentStatus
import com.gayakini.order.domain.Order
import com.gayakini.order.domain.OrderItem
import com.gayakini.order.domain.OrderRepository
import com.gayakini.order.domain.OrderStatus
import com.gayakini.shipping.application.Shipment
import com.gayakini.shipping.application.ShipmentRepository
import com.gayakini.shipping.application.ShippingService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional
@Tag("integration")
class InventoryMovementIntegrationTest : BaseDbIntegrationTest() {
    @Autowired
    private lateinit var warehouseService: WarehouseService

    @Autowired
    private lateinit var shippingService: ShippingService

    @Autowired
    private lateinit var shipmentRepository: ShipmentRepository

    @Autowired
    private lateinit var inventoryMovementRepository: InventoryMovementRepository

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var productRepository: ProductRepository

    @Autowired
    private lateinit var cartRepository: CartRepository

    @Autowired
    private lateinit var checkoutRepository: CheckoutRepository

    @Autowired
    private lateinit var inventoryService: InventoryService

    @Test
    fun `should track movement from PAID to SHIPPED via webhook`() {
        // 1. Setup: Paid Order and Shipment
        val product = createTestProduct("Movement Test", 10)
        val variant = product.variants.first()
        val order = createTestOrder(variant, OrderStatus.PAID)

        // Ensure reservation is consumed (simulating what happens when order is marked PAID)
        order.items.forEach { inventoryService.consumeReservation(it.id) }

        // Manual shipment creation for the test
        val shipment =
            Shipment(
                orderId = order.id,
                status = FulfillmentStatus.BOOKED,
                providerOrderId = "TEST-PROVIDER-ID",
            )
        shipmentRepository.saveAndFlush(shipment)

        // 2. Action: Pack Order (STORAGE -> PACKING)
        warehouseService.packOrder(
            order.id,
            PackOrderRequest(weightInGrams = 500, dimensionCm = "10x10x10"),
        )

        // 3. Action: Simulate Webhook for SHIPPED (PACKING -> EXTERNAL)
        val webhookPayload =
            mapOf(
                "event" to "order.status",
                "order_id" to "TEST-PROVIDER-ID",
                "status" to "shipped",
            )
        shippingService.processBiteshipWebhook(webhookPayload)

        // 4. Verify Movements
        val movements =
            inventoryMovementRepository.findAll()
                .filter {
                    it.referenceId == order.id.toString() ||
                        it.referenceId == shipment.id.toString() ||
                        order.items.any { item -> it.referenceId == item.id.toString() }
                }
                .sortedBy { it.createdAt }

        // Expected: 1. STORAGE -> PACKING (from consumeReservation), 2. PACKING -> EXTERNAL (from webhook)
        assertEquals(2, movements.size)

        assertEquals(MovementType.WAREHOUSE_PICKING, movements[0].movementType)
        assertEquals("STORAGE", movements[0].sourceLocation)
        assertEquals("PACKING", movements[0].destinationLocation)

        assertEquals(MovementType.INTERNAL_TRANSFER, movements[1].movementType)
        assertEquals("PACKING", movements[1].sourceLocation)
        assertEquals("EXTERNAL", movements[1].destinationLocation)
    }

    private fun createTestProduct(
        name: String,
        stock: Int,
    ): Product {
        val uniqueId = UUID.randomUUID().toString().take(5)
        val product =
            Product(
                id = UUID.randomUUID(),
                title = name,
                slug = name.lowercase().replace(" ", "-") + "-" + uniqueId,
                subtitle = null,
                brandName = "Test",
                description = "Test",
                status = ProductStatus.PUBLISHED,
            )
        val variant =
            ProductVariant(
                id = UUID.randomUUID(),
                product = product,
                sku = "SKU-" + uniqueId,
                priceAmount = 1000,
                stockOnHand = stock,
                weightGrams = 100,
                color = "Default",
                sizeCode = "All",
            )
        product.variants.add(variant)
        return productRepository.save(product)
    }

    private fun createTestOrder(
        variant: ProductVariant,
        status: OrderStatus,
    ): Order {
        val cart =
            Cart(
                id = UUID.randomUUID(),
                currencyCode = "IDR",
                itemCount = 1,
                subtotalAmount = 1000,
                accessTokenHash = "cart-" + UUID.randomUUID().toString().take(8),
            )
        cartRepository.saveAndFlush(cart)

        val checkout =
            Checkout(
                id = UUID.randomUUID(),
                cart = cart,
                currencyCode = "IDR",
                subtotalAmount = 1000,
                shippingCostAmount = 0,
                discountAmount = 0,
                accessTokenHash = "checkout-" + UUID.randomUUID().toString().take(8),
            )
        checkoutRepository.saveAndFlush(checkout)

        val order =
            Order(
                orderNumber = "ORD-" + UUID.randomUUID().toString().take(8),
                checkoutId = checkout.id,
                cartId = cart.id,
                customerId = null,
                status = status,
                subtotalAmount = 1000,
                shippingCostAmount = 0,
                discountAmount = 0,
                accessTokenHash = "order-" + UUID.randomUUID().toString().take(8),
            )
        val item =
            OrderItem(
                order = order,
                product = variant.product,
                variant = variant,
                skuSnapshot = variant.sku,
                titleSnapshot = variant.product.title,
                color = variant.color,
                sizeCode = variant.sizeCode,
                quantity = 1,
                unitPriceAmount = 1000,
            )
        order.items.add(item)

        val saved = orderRepository.saveAndFlush(order)

        // Create reservation via InventoryService to avoid constraint issues and ensure consistency
        inventoryService.reserveStock(
            orderId = saved.id,
            orderItemId = saved.items.first().id,
            variantId = variant.id,
            quantity = 1,
        )

        return saved
    }
}
