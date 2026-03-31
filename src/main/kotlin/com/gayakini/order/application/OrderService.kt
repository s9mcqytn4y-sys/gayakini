package com.gayakini.order.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.gayakini.cart.domain.CartRepository
import com.gayakini.catalog.domain.ProductVariantRepository
import com.gayakini.common.util.UuidV7Generator
import com.gayakini.inventory.application.InventoryService
import com.gayakini.order.api.PlaceOrderRequest
import com.gayakini.order.domain.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val cartRepository: CartRepository,
    private val variantRepository: ProductVariantRepository,
    private val inventoryService: InventoryService,
    private val objectMapper: ObjectMapper
) {

    @Transactional
    fun placeOrder(customerId: UUID?, guestTokenHash: String?, request: PlaceOrderRequest): Order {
        // Idempotency check
        orderRepository.findByIdempotencyKey(request.idempotencyKey).ifPresent {
            return it
        }

        // 1. Get Cart
        val cart = if (customerId != null) {
            cartRepository.findByCustomerIdAndIsActiveTrue(customerId)
        } else {
            cartRepository.findByGuestTokenHashAndIsActiveTrue(guestTokenHash!!)
        }.orElseThrow { NoSuchElementException("Keranjang aktif tidak ditemukan.") }

        if (cart.items.isEmpty()) {
            throw IllegalStateException("Keranjang kosong.")
        }

        // 2. Lock and Decrease Stock & Fetch Variants for Pricing
        val variantIds = cart.items.map { it.variantId }
        val variants = variantRepository.findAllByIdIn(variantIds).associateBy { it.id }

        var totalAmount = 0L
        val orderItems = cart.items.map { cartItem ->
            val variant = variants[cartItem.variantId] 
                ?: throw NoSuchElementException("Varian produk \${cartItem.variantId} tidak ditemukan.")
            
            // Explicit Stock Lock & Decrease
            inventoryService.lockAndDecreaseStock(cartItem.variantId, cartItem.quantity)

            val itemSubtotal = variant.price * cartItem.quantity
            totalAmount += itemSubtotal

            OrderItem(
                id = UuidV7Generator.generate(),
                order = null as Any as Order, // Will be set after order creation or handled by JPA
                variantId = variant.id,
                skuSnapshot = variant.sku,
                nameSnapshot = variant.name,
                priceSnapshot = variant.price,
                quantity = cartItem.quantity,
                subtotal = itemSubtotal
            )
        }

        val grandTotal = totalAmount + request.shippingCost

        // 3. Create Order
        val orderId = UuidV7Generator.generate()
        val order = Order(
            id = orderId,
            orderNumber = "ORD-\${System.currentTimeMillis()}-\${Random().nextInt(999)}",
            customerId = customerId,
            guestTokenHash = guestTokenHash,
            status = OrderStatus.PENDING_PAYMENT,
            totalAmount = totalAmount,
            shippingCost = request.shippingCost,
            grandTotal = grandTotal,
            shippingAddressSnapshot = objectMapper.writeValueAsString(request.shippingAddress),
            paymentMethod = request.paymentMethod,
            idempotencyKey = request.idempotencyKey,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        val savedOrder = orderRepository.save(order)

        // 4. Save Order Items with reference to savedOrder
        orderItems.forEach { item ->
            val finalItem = OrderItem(
                id = item.id,
                order = savedOrder,
                variantId = item.variantId,
                skuSnapshot = item.skuSnapshot,
                nameSnapshot = item.nameSnapshot,
                priceSnapshot = item.priceSnapshot,
                quantity = item.quantity,
                subtotal = item.subtotal
            )
            orderItemRepository.save(finalItem)
        }

        // 5. Deactivate Cart
        cart.isActive = false
        cartRepository.save(cart)

        return savedOrder
    }
}
