package com.gayakini.order.application

import com.gayakini.cart.domain.CartRepository
import com.gayakini.cart.domain.CartStatus
import com.gayakini.checkout.domain.CheckoutRepository
import com.gayakini.checkout.domain.CheckoutStatus
import com.gayakini.common.infrastructure.IdempotencyService
import com.gayakini.common.util.HashUtils
import com.gayakini.common.util.UuidV7Generator
import com.gayakini.inventory.application.InventoryService
import com.gayakini.order.api.PlaceOrderRequest
import com.gayakini.order.domain.*
import org.springframework.security.core.context.SecurityContextHolder
import com.gayakini.infrastructure.security.UserPrincipal
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.Random
import java.util.UUID

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val checkoutRepository: CheckoutRepository,
    private val cartRepository: CartRepository,
    private val inventoryService: InventoryService,
    private val idempotencyService: IdempotencyService,
) {
    @Transactional
    fun placeOrderFromCheckout(
        checkoutId: UUID,
        idempotencyKey: String,
        checkoutToken: String?,
        request: PlaceOrderRequest,
    ): Order {
        val currentUser = SecurityContextHolder.getContext().authentication?.principal as? UserPrincipal

        return idempotencyService.handle(
            scope = "place_order",
            key = idempotencyKey,
            requestPayload = request,
            requesterType = if (currentUser != null) "CUSTOMER" else "GUEST",
            requesterId = currentUser?.id,
        ) {
            val existingByCheckout = orderRepository.findByCheckoutId(checkoutId)
            if (existingByCheckout.isPresent) {
                return@handle existingByCheckout.get()
            }

            val checkout = checkoutRepository.findById(checkoutId)
                .orElseThrow { NoSuchElementException("Checkout tidak ditemukan.") }

            if (checkout.status != CheckoutStatus.READY_FOR_ORDER) {
                throw IllegalStateException("Checkout belum siap atau sudah diproses (status: ${checkout.status}).")
            }

            // Validate ownership
            if (checkout.customerId != null && checkout.customerId != currentUser?.id) {
                throw IllegalStateException("Akses checkout ditolak.")
            }
            if (checkout.accessTokenHash != null) {
                if (HashUtils.sha256(checkoutToken ?: "") != checkout.accessTokenHash) {
                    throw IllegalStateException("Akses checkout ditolak.")
                }
            }

            val order = Order(
                id = UuidV7Generator.generate(),
                orderNumber = "ORD-${Instant.now().toEpochMilli()}-${Random().nextInt(9999)}",
                checkoutId = checkout.id,
                cartId = checkout.cart.id,
                customerId = checkout.customerId,
                accessTokenHash = checkout.accessTokenHash,
                status = OrderStatus.PENDING_PAYMENT,
                subtotalAmount = checkout.subtotalAmount,
                shippingCostAmount = checkout.shippingCostAmount,
                customerNotes = request.customerNotes,
            )

            // Snapshot Address
            val checkoutAddress = checkout.shippingAddress ?: throw IllegalStateException("Alamat pengiriman belum diset.")
            order.shippingAddress = OrderShippingAddress(
                orderId = order.id,
                order = order,
                recipientName = checkoutAddress.recipientName,
                phone = checkoutAddress.phone,
                line1 = checkoutAddress.line1,
                line2 = checkoutAddress.line2,
                notes = checkoutAddress.notes,
                areaId = checkoutAddress.areaId,
                district = checkoutAddress.district,
                city = checkoutAddress.city,
                province = checkoutAddress.province,
                postalCode = checkoutAddress.postalCode,
                countryCode = checkoutAddress.countryCode,
            )

            // Snapshot Shipping Selection
            val selectedQuote = checkout.availableShippingQuotes.find { it.id == checkout.selectedShippingQuoteId }
                ?: throw IllegalStateException("Pilihan pengiriman tidak valid.")

            order.shippingSelection = OrderShippingSelection(
                orderId = order.id,
                order = order,
                provider = selectedQuote.provider,
                providerReference = selectedQuote.providerReference,
                courierCode = selectedQuote.courierCode,
                courierName = selectedQuote.courierName,
                serviceCode = selectedQuote.serviceCode,
                serviceName = selectedQuote.serviceName,
                description = selectedQuote.description,
                costAmount = selectedQuote.costAmount,
                estimatedDaysMin = selectedQuote.estimatedDaysMin,
                estimatedDaysMax = selectedQuote.estimatedDaysMax,
                rawQuotePayload = selectedQuote.rawPayload
            )

            // Items & Reservations
            checkout.items.forEach { checkoutItem ->
                val orderItem = OrderItem(
                    id = UuidV7Generator.generate(),
                    order = order,
                    product = checkoutItem.product,
                    variant = checkoutItem.variant,
                    skuSnapshot = checkoutItem.skuSnapshot,
                    titleSnapshot = checkoutItem.productTitleSnapshot,
                    color = checkoutItem.color,
                    sizeCode = checkoutItem.sizeCode,
                    quantity = checkoutItem.quantity,
                    unitPriceAmount = checkoutItem.unitPriceAmount,
                )
                order.items.add(orderItem)

                // Atomic stock lock and reservation inside inventoryService
                inventoryService.reserveStock(order.id, orderItem.id, orderItem.variant.id, orderItem.quantity)
            }

            val savedOrder = orderRepository.save(order)

            // Mark Checkout and Cart as converted
            checkout.status = CheckoutStatus.ORDER_CREATED
            checkout.updatedAt = Instant.now()
            checkoutRepository.save(checkout)

            val cart = checkout.cart
            cart.status = CartStatus.CONVERTED
            cart.updatedAt = Instant.now()
            cartRepository.save(cart)

            savedOrder
        }
    }

    fun getOrder(id: UUID): Order {
        return orderRepository.findById(id)
            .orElseThrow { NoSuchElementException("Pesanan tidak ditemukan.") }
    }

    fun listOrders(customerId: UUID?): List<Order> {
        return if (customerId != null) {
            orderRepository.findAllByCustomerIdOrderByCreatedAtDesc(customerId)
        } else {
            orderRepository.findAllByOrderByCreatedAtDesc()
        }
    }

    @Transactional
    fun cancelOrder(id: UUID, reason: String?): Order {
        val order = getOrder(id)
        if (order.status == OrderStatus.COMPLETED || order.status == OrderStatus.CANCELLED) {
            throw IllegalStateException("Pesanan tidak dapat dibatalkan dalam status: ${order.status}")
        }

        order.status = OrderStatus.CANCELLED
        order.cancelledAt = Instant.now()
        order.cancellationReason = reason
        order.updatedAt = Instant.now()

        // Release inventory reservations
        inventoryService.releaseReservations(order.id, "Order cancelled by user: $reason")

        return orderRepository.save(order)
    }
}
