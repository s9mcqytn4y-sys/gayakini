package com.gayakini.order.application

import com.gayakini.cart.application.CartService
import com.gayakini.cart.domain.CartRepository
import com.gayakini.cart.domain.CartStatus
import com.gayakini.catalog.domain.ProductStatus
import com.gayakini.catalog.domain.VariantStatus
import com.gayakini.checkout.domain.Checkout
import com.gayakini.checkout.domain.CheckoutRepository
import com.gayakini.checkout.domain.CheckoutStatus
import com.gayakini.common.api.ForbiddenException
import com.gayakini.common.api.UnauthorizedException
import com.gayakini.common.infrastructure.IdempotencyService
import com.gayakini.common.util.HashUtils
import com.gayakini.common.util.UuidV7Generator
import com.gayakini.infrastructure.security.SecurityUtils
import com.gayakini.infrastructure.security.UserPrincipal
import com.gayakini.inventory.application.InventoryService
import com.gayakini.order.api.PlaceOrderRequest
import com.gayakini.order.domain.*
import org.springframework.security.core.context.SecurityContextHolder
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
    private val cartService: CartService,
    private val inventoryService: InventoryService,
    private val idempotencyService: IdempotencyService,
) {
    companion object {
        private const val ORDER_NUMBER_RANDOM_BOUND = 9999
    }

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

            val checkout =
                checkoutRepository.findById(checkoutId)
                    .orElseThrow { NoSuchElementException("Checkout tidak ditemukan.") }

            validateCheckoutState(checkout, currentUser, checkoutToken)

            // Final re-validation of pricing and availability before order placement
            val cart = checkout.cart
            cartService.refreshCartPrices(cart)

            checkout.items.forEach { checkoutItem ->
                val variant = checkoutItem.variant
                check(variant.status == VariantStatus.ACTIVE) { "Produk ${checkoutItem.productTitleSnapshot} tidak tersedia." }
                check(variant.product.status == ProductStatus.PUBLISHED) { "Produk ${checkoutItem.productTitleSnapshot} tidak tersedia." }

                // Update checkout snapshots if cart was refreshed
                val cartItem = cart.items.find { it.variant.id == variant.id }
                if (cartItem != null) {
                    checkoutItem.unitPriceAmount = cartItem.unitPriceAmount
                    checkoutItem.compareAtAmount = cartItem.compareAtAmount
                }
            }

            // Recalculate checkout subtotal
            checkout.subtotalAmount = checkout.items.sumOf { it.unitPriceAmount * it.quantity }
            checkout.updatedAt = Instant.now()
            checkoutRepository.save(checkout)

            val order = createOrderFromCheckout(checkout, request)

            // Items & Reservations
            checkout.items.forEach { checkoutItem ->
                val orderItem =
                    OrderItem(
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

            finalizeCheckout(checkout)

            savedOrder
        }
    }

    private fun validateCheckoutState(
        checkout: Checkout,
        currentUser: UserPrincipal?,
        checkoutToken: String?,
    ) {
        check(checkout.status == CheckoutStatus.READY_FOR_ORDER) {
            "Checkout belum siap atau sudah diproses (status: ${checkout.status})."
        }

        // Validate ownership
        if (checkout.customerId != null && checkout.customerId != currentUser?.id) {
            if (currentUser == null) {
                throw UnauthorizedException("Silakan login untuk mengakses checkout ini.")
            }
            throw ForbiddenException("Akses checkout ditolak.")
        }
        if (checkout.accessTokenHash != null) {
            val token = checkoutToken ?: throw UnauthorizedException("Token checkout diperlukan.")
            if (HashUtils.sha256(token) != checkout.accessTokenHash) {
                throw UnauthorizedException("Token checkout tidak valid.")
            }
        }
    }

    private fun createOrderFromCheckout(
        checkout: Checkout,
        request: PlaceOrderRequest,
    ): Order {
        val order =
            Order(
                id = UuidV7Generator.generate(),
                orderNumber = "ORD-${Instant.now().toEpochMilli()}-${Random().nextInt(ORDER_NUMBER_RANDOM_BOUND)}",
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
        val checkoutAddress = checkout.shippingAddress
        checkNotNull(checkoutAddress) { "Alamat pengiriman belum diset." }

        order.shippingAddress =
            OrderShippingAddress(
                orderId = order.id,
                order = order,
                recipientName = checkoutAddress.recipientName,
                phone = checkoutAddress.phone,
                email = checkoutAddress.email,
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
        val selectedQuote =
            checkout.availableShippingQuotes.find { it.id == checkout.selectedShippingQuoteId }
        checkNotNull(selectedQuote) { "Pilihan pengiriman tidak valid." }

        order.shippingSelection =
            OrderShippingSelection(
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
                rawQuotePayload = selectedQuote.rawPayload,
            )
        return order
    }

    private fun finalizeCheckout(checkout: Checkout) {
        checkout.status = CheckoutStatus.ORDER_CREATED
        checkout.updatedAt = Instant.now()
        checkoutRepository.save(checkout)

        val cart = checkout.cart
        cart.status = CartStatus.CONVERTED
        cart.updatedAt = Instant.now()
        cartRepository.save(cart)
    }

    fun getOrder(id: UUID): Order {
        return orderRepository.findById(id)
            .orElseThrow { NoSuchElementException("Pesanan tidak ditemukan.") }
    }

    fun getAuthorizedOrder(
        id: UUID,
        orderToken: String?,
    ): Order {
        val order = getOrder(id)
        validateOrderAccess(order, orderToken)
        return order
    }

    fun listOrders(customerId: UUID?): List<Order> {
        return if (customerId != null) {
            orderRepository.findAllByCustomerIdOrderByCreatedAtDesc(customerId)
        } else {
            orderRepository.findAllByOrderByCreatedAtDesc()
        }
    }

    fun listOrdersForAdmin(
        status: OrderStatus?,
        paymentStatus: PaymentStatus?,
        fulfillmentStatus: FulfillmentStatus?,
        orderNumber: String?,
    ): List<Order> {
        return orderRepository.findAllByOrderByCreatedAtDesc()
            .filter { status == null || it.status == status }
            .filter { paymentStatus == null || it.paymentStatus == paymentStatus }
            .filter { fulfillmentStatus == null || it.fulfillmentStatus == fulfillmentStatus }
            .filter { orderNumber.isNullOrBlank() || it.orderNumber.contains(orderNumber.trim(), ignoreCase = true) }
    }

    @Transactional
    fun cancelOrder(
        id: UUID,
        reason: String?,
        idempotencyKey: String,
        orderToken: String?,
    ): Order {
        val currentUserId = SecurityUtils.getCurrentUserId()

        return idempotencyService.handle(
            scope = "cancel_order",
            key = idempotencyKey,
            requestPayload = mapOf("orderId" to id, "reason" to (reason ?: "")),
            requesterType = if (currentUserId != null) "CUSTOMER" else "GUEST",
            requesterId = currentUserId,
        ) {
            val order = getOrder(id)
            validateOrderAccess(order, orderToken)

            if (order.status == OrderStatus.CANCELLED) {
                return@handle order
            }

            check(order.status != OrderStatus.COMPLETED) {
                "Pesanan tidak dapat dibatalkan dalam status: ${order.status}"
            }

            order.status = OrderStatus.CANCELLED
            order.cancelledAt = Instant.now()
            order.cancellationReason = reason
            order.updatedAt = Instant.now()

            inventoryService.releaseReservations(order.id, "Order cancelled by user: $reason")

            orderRepository.save(order)
        }
    }

    @Transactional
    fun cancelOrderAsAdmin(
        id: UUID,
        reason: String?,
        idempotencyKey: String,
    ): Order {
        val currentUserId = SecurityUtils.getCurrentUserId()

        return idempotencyService.handle(
            scope = "admin_cancel_order",
            key = idempotencyKey,
            requestPayload = mapOf("orderId" to id, "reason" to (reason ?: "")),
            requesterType = "ADMIN",
            requesterId = currentUserId,
        ) {
            val order = getOrder(id)

            if (order.status == OrderStatus.CANCELLED) {
                return@handle order
            }

            check(order.status != OrderStatus.COMPLETED) {
                "Pesanan tidak dapat dibatalkan dalam status: ${order.status}"
            }

            order.status = OrderStatus.CANCELLED
            order.fulfillmentStatus = FulfillmentStatus.CANCELLED
            order.cancelledAt = Instant.now()
            order.cancellationReason = reason
            order.updatedAt = Instant.now()

            inventoryService.releaseReservations(order.id, "Order cancelled by admin: $reason")

            orderRepository.save(order)
        }
    }

    fun validateOrderAccess(
        order: Order,
        orderToken: String?,
    ) {
        val currentUserId = SecurityUtils.getCurrentUserId()

        if (order.customerId != null) {
            validateCustomerAccess(order, currentUserId)
            return
        }

        if (order.accessTokenHash != null) {
            validateGuestAccess(order, orderToken)
        }
    }

    private fun validateCustomerAccess(
        order: Order,
        currentUserId: UUID?,
    ) {
        if (order.customerId != currentUserId) {
            if (currentUserId == null) {
                throw UnauthorizedException("Silakan login untuk mengakses pesanan ini.")
            }
            throw ForbiddenException("Akses pesanan ditolak.")
        }
    }

    private fun validateGuestAccess(
        order: Order,
        orderToken: String?,
    ) {
        val token = orderToken ?: throw UnauthorizedException("Token pesanan diperlukan.")
        if (HashUtils.sha256(token) != order.accessTokenHash) {
            throw UnauthorizedException("Token pesanan tidak valid.")
        }
    }
}
