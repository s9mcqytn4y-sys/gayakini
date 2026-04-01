package com.gayakini.order.application

import com.gayakini.checkout.domain.CheckoutRepository
import com.gayakini.checkout.domain.CheckoutStatus
import com.gayakini.common.infrastructure.IdempotencyService
import com.gayakini.common.util.HashUtils
import com.gayakini.common.util.UuidV7Generator
import com.gayakini.inventory.application.InventoryService
import com.gayakini.order.api.OrderAddressResponse
import com.gayakini.order.api.OrderItemResponse
import com.gayakini.order.api.OrderPaymentSummaryResponse
import com.gayakini.order.api.OrderResponse
import com.gayakini.order.api.PlaceOrderRequest
import com.gayakini.order.api.MoneyResponse
import com.gayakini.order.domain.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val checkoutRepository: CheckoutRepository,
    private val inventoryService: InventoryService,
    private val idempotencyService: IdempotencyService
) {
    @Transactional
    fun placeOrderFromCheckout(
        checkoutId: UUID,
        idempotencyKey: String,
        checkoutToken: String?,
        request: PlaceOrderRequest
    ): OrderResponse {
        return idempotencyService.handle(
            scope = "place_order",
            key = idempotencyKey,
            requestPayload = request,
            requesterType = if (checkoutToken != null) "GUEST" else "CUSTOMER",
            requesterId = null // TODO: Get current user ID
        ) {
            val existingByCheckout = orderRepository.findByCheckoutId(checkoutId)
            if (existingByCheckout.isPresent) {
                return@handle mapToResponse(existingByCheckout.get())
            }

            val checkout = checkoutRepository.findById(checkoutId)
                .orElseThrow { NoSuchElementException("Checkout tidak ditemukan.") }

            if (checkout.status != CheckoutStatus.ACTIVE && checkout.status != CheckoutStatus.READY_FOR_ORDER) {
                 throw IllegalStateException("Checkout sudah tidak aktif (status: ${checkout.status}).")
            }

            if (checkout.accessTokenHash != null) {
                if (HashUtils.sha256(checkoutToken ?: "") != checkout.accessTokenHash) {
                    throw IllegalStateException("Akses checkout ditolak.")
                }
            }

            val order = Order(
                id = UuidV7Generator.generate(),
                orderNumber = "ORD-${System.currentTimeMillis()}-${Random().nextInt(999)}",
                checkoutId = checkout.id,
                cartId = checkout.cart.id,
                customerId = checkout.customerId,
                accessTokenHash = checkout.accessTokenHash,
                status = OrderStatus.PENDING_PAYMENT,
                subtotalAmount = checkout.subtotalAmount,
                shippingCostAmount = checkout.shippingCostAmount,
                customerNotes = request.customerNotes
            )

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
                countryCode = checkoutAddress.countryCode
            )

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
                    unitPriceAmount = checkoutItem.unitPriceAmount
                )
                order.items.add(orderItem)
                
                inventoryService.reserveStock(order.id, orderItem.id, orderItem.variant.id, orderItem.quantity)
            }

            val savedOrder = orderRepository.save(order)

            checkout.status = CheckoutStatus.ORDER_CREATED
            checkoutRepository.save(checkout)

            mapToResponse(savedOrder)
        }
    }

    private fun mapToResponse(order: Order): OrderResponse {
        return OrderResponse(
            id = order.id,
            orderNumber = order.orderNumber,
            customerId = order.customerId,
            status = order.status,
            fulfillmentStatus = order.fulfillmentStatus,
            paymentSummary = OrderPaymentSummaryResponse(
                provider = "MIDTRANS",
                status = order.paymentStatus
            ),
            shippingAddress = order.shippingAddress!!.let { addr ->
                OrderAddressResponse(
                    id = null,
                    recipientName = addr.recipientName,
                    phone = addr.phone,
                    line1 = addr.line1,
                    district = addr.district,
                    city = addr.city,
                    province = addr.province,
                    postalCode = addr.postalCode,
                    countryCode = addr.countryCode
                )
            },
            items = order.items.map { item ->
                OrderItemResponse(
                    id = item.id,
                    productId = item.product.id,
                    variantId = item.variant.id,
                    skuSnapshot = item.skuSnapshot,
                    titleSnapshot = item.titleSnapshot,
                    quantity = item.quantity,
                    unitPrice = MoneyResponse(amount = item.unitPriceAmount),
                    lineTotal = MoneyResponse(amount = item.lineTotalAmount)
                )
            },
            subtotal = MoneyResponse(amount = order.subtotalAmount),
            shippingCost = MoneyResponse(amount = order.shippingCostAmount),
            total = MoneyResponse(amount = order.totalAmount),
            currency = order.currencyCode,
            customerNotes = order.customerNotes,
            createdAt = order.createdAt,
            paidAt = order.paidAt,
            cancelledAt = order.cancelledAt
        )
    }
}
