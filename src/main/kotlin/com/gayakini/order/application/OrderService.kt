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
    private val objectMapper: ObjectMapper,
) {
    @Transactional
    fun placeOrder(
        customerId: UUID?,
        guestTokenHash: String?,
        request: PlaceOrderRequest,
    ): Order {
        // Idempotency check
        val existingOrder = orderRepository.findByIdempotencyKey(request.idempotencyKey)
        if (existingOrder.isPresent) {
            return existingOrder.get()
        }

        // 1. Get Cart
        val cart =
            if (customerId != null) {
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

        // 3. Prepare Order Metadata
        val totalAmount =
            cart.items.sumOf { cartItem ->
                val variant =
                    variants[cartItem.variantId]
                        ?: throw NoSuchElementException("Varian produk ${cartItem.variantId} tidak ditemukan.")
                variant.price * cartItem.quantity
            }
        val grandTotal = totalAmount + request.shippingCost

        val orderId = UuidV7Generator.generate()
        val order =
            Order(
                id = orderId,
                orderNumber = "ORD-${System.currentTimeMillis()}-${Random().nextInt(999)}",
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
                updatedAt = Instant.now(),
            )

        val savedOrder = orderRepository.save(order)

        // 4. Create and Save Order Items
        cart.items.forEach { cartItem ->
            val variant = variants[cartItem.variantId]!!

            // Explicit Stock Lock & Decrease
            inventoryService.lockAndDecreaseStock(cartItem.variantId, cartItem.quantity)

            val itemSubtotal = variant.price * cartItem.quantity

            val orderItem =
                OrderItem(
                    id = UuidV7Generator.generate(),
                    order = savedOrder,
                    variantId = variant.id,
                    skuSnapshot = variant.sku,
                    nameSnapshot = variant.name,
                    priceSnapshot = variant.price,
                    quantity = cartItem.quantity,
                    subtotal = itemSubtotal,
                )
            orderItemRepository.save(orderItem)
        }

        // 5. Deactivate Cart
        cart.isActive = false
        cartRepository.save(cart)

        return savedOrder
    }
}
