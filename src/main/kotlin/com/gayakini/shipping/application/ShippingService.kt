package com.gayakini.shipping.application

import com.gayakini.common.infrastructure.IdempotencyService
import com.gayakini.common.util.UuidV7Generator
import com.gayakini.customer.domain.CustomerRepository
import com.gayakini.order.api.AdminCreateShipmentRequest
import com.gayakini.order.domain.FulfillmentStatus
import com.gayakini.order.domain.Order
import com.gayakini.order.domain.OrderRepository
import com.gayakini.order.domain.OrderStatus
import com.gayakini.shipping.domain.ContactInfo
import com.gayakini.shipping.domain.MerchantShippingOriginRepository
import com.gayakini.shipping.domain.ShippingItem
import com.gayakini.shipping.domain.ShippingProvider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.NoSuchElementException
import java.util.Optional
import java.util.UUID

@Service
class ShippingService(
    private val orderRepository: OrderRepository,
    private val shipmentRepository: ShipmentRepository,
    private val merchantOriginRepository: MerchantShippingOriginRepository,
    private val shippingProvider: ShippingProvider,
    private val customerRepository: CustomerRepository,
    private val idempotencyService: IdempotencyService,
) {
    private val logger = LoggerFactory.getLogger(ShippingService::class.java)

    @Transactional
    fun processBiteshipWebhook(payload: Map<String, Any>) {
        val event = payload["event"] as? String ?: return
        val orderIdStr = payload["order_id"] as? String ?: return

        logger.info("Memproses webhook Biteship event: {} untuk order: {}", event, orderIdStr)

        val shipment = findShipmentByExternalId(orderIdStr) ?: return

        when (event) {
            "order.status" -> handleStatusUpdate(shipment, payload)
            "order.waybill_id" -> handleWaybillUpdate(shipment, payload)
        }

        shipment.updatedAt = Instant.now()
        shipmentRepository.save(shipment)
    }

    private fun findShipmentByExternalId(externalId: String): Shipment? {
        val shipment = shipmentRepository.findByProviderOrderId(externalId).orElse(null)
        if (shipment != null) return shipment

        return try {
            val orderId = UUID.fromString(externalId)
            shipmentRepository.findByOrderId(orderId).orElse(null)
        } catch (e: IllegalArgumentException) {
            logger.debug("External ID is not a valid UUID: {}", externalId)
            null
        }
    }

    @Transactional
    fun bookShipment(
        orderId: UUID,
        idempotencyKey: String,
        request: AdminCreateShipmentRequest?,
    ): Shipment {
        return idempotencyService.handle(
            scope = "book_shipment",
            key = idempotencyKey,
            requestPayload = mapOf("orderId" to orderId, "note" to request?.note.orEmpty()),
            requesterType = "ADMIN",
            requesterId = null,
        ) {
            shipmentRepository.findByOrderId(orderId).orElse(null)?.let { return@handle it }

            val order =
                orderRepository.findById(orderId)
                    .orElseThrow { NoSuchElementException("Order tidak ditemukan") }

            validateOrderForShipment(order)

            val selection = order.shippingSelection
            checkNotNull(selection) { "Pilihan pengiriman order belum tersedia." }
            val address = order.shippingAddress
            checkNotNull(address) { "Alamat pengiriman order belum tersedia." }
            val origin =
                merchantOriginRepository.findDefaultActive()
                    .orElseThrow { IllegalStateException("Origin pengiriman merchant belum dikonfigurasi.") }

            val booking =
                shippingProvider.createShipment(
                    orderId = order.id.toString(),
                    rateId = selection.providerReference ?: "${selection.courierCode}_${selection.serviceCode}",
                    sender = createSenderContact(origin),
                    receiver = createReceiverContact(order, address),
                    items = createShippingItems(order),
                )

            val shipment =
                Shipment(
                    id = UuidV7Generator.generate(),
                    orderId = order.id,
                    provider = selection.provider,
                    providerOrderId = booking.bookingId,
                    status = FulfillmentStatus.BOOKED,
                    rawProviderStatus = booking.status,
                    courierCode = selection.courierCode,
                    courierName = selection.courierName,
                    serviceCode = selection.serviceCode,
                    serviceName = selection.serviceName,
                    trackingNumber = booking.waybillId,
                    note = request?.note,
                    providerResponsePayload = booking.rawPayload,
                    bookedAt = Instant.now(),
                )

            updateOrderAfterBooking(order)
            shipmentRepository.save(shipment)
        }
    }

    private fun updateOrderAfterBooking(order: Order) {
        order.status = OrderStatus.READY_TO_SHIP
        order.fulfillmentStatus = FulfillmentStatus.BOOKED
        order.updatedAt = Instant.now()
        orderRepository.save(order)
    }

    private fun validateOrderForShipment(order: Order) {
        check(order.status == OrderStatus.PAID || order.status == OrderStatus.READY_TO_SHIP) {
            "Order belum siap dibuatkan pengiriman."
        }
    }

    private fun createSenderContact(origin: com.gayakini.shipping.domain.MerchantShippingOrigin): ContactInfo {
        return ContactInfo(
            fullName = origin.contactName,
            phone = origin.contactPhone,
            email = origin.contactEmail,
            address =
                listOfNotNull(
                    origin.line1,
                    origin.line2,
                    origin.district,
                    origin.city,
                    origin.province,
                    origin.postalCode,
                ).joinToString(", "),
            areaId = origin.areaId,
        )
    }

    private fun createReceiverContact(
        order: Order,
        address: com.gayakini.order.domain.OrderShippingAddress,
    ): ContactInfo {
        return ContactInfo(
            fullName = address.recipientName,
            phone = address.phone,
            email = order.customerId?.let { customerRepository.findById(it).orElse(null)?.email },
            address =
                listOfNotNull(
                    address.line1,
                    address.line2,
                    address.district,
                    address.city,
                    address.province,
                    address.postalCode,
                ).joinToString(", "),
            areaId = address.areaId,
        )
    }

    private fun createShippingItems(order: Order): List<ShippingItem> {
        return order.items.map {
            ShippingItem(
                name = it.titleSnapshot,
                weightGrams = it.variant.weightGrams,
                quantity = it.quantity,
                valueIdr = it.unitPriceAmount,
            )
        }
    }

    fun findShipmentByOrderId(orderId: UUID): Shipment? = shipmentRepository.findByOrderId(orderId).orElse(null)

    private fun handleStatusUpdate(
        shipment: Shipment,
        payload: Map<String, Any>,
    ) {
        val status = payload["status"] as? String ?: return
        shipment.rawProviderStatus = status

        val order =
            orderRepository.findById(shipment.orderId)
                .orElseThrow { NoSuchElementException("Order tidak ditemukan") }

        when (status) {
            "picked", "shipped" -> {
                if (shipment.status == FulfillmentStatus.DELIVERED) {
                    return
                }
                shipment.status = FulfillmentStatus.IN_TRANSIT
                shipment.shippedAt = shipment.shippedAt ?: Instant.now()
                order.status = OrderStatus.SHIPPED
                order.fulfillmentStatus = FulfillmentStatus.IN_TRANSIT
            }
            "delivered" -> {
                shipment.status = FulfillmentStatus.DELIVERED
                shipment.deliveredAt = shipment.deliveredAt ?: Instant.now()
                order.status = OrderStatus.COMPLETED
                order.fulfillmentStatus = FulfillmentStatus.DELIVERED
            }
        }
        orderRepository.save(order)
    }

    private fun handleWaybillUpdate(
        shipment: Shipment,
        payload: Map<String, Any>,
    ) {
        val waybillId = payload["waybill_id"] as? String
        if (waybillId != null) {
            shipment.trackingNumber = waybillId
        }
    }
}

interface ShipmentRepository : org.springframework.data.jpa.repository.JpaRepository<Shipment, UUID> {
    @org.springframework.data.jpa.repository.Query(
        "SELECT s FROM Shipment s WHERE s.providerOrderId = :providerOrderId",
    )
    fun findByProviderOrderId(providerOrderId: String): Optional<Shipment>

    fun findByOrderId(orderId: UUID): Optional<Shipment>
}

@jakarta.persistence.Entity
@jakarta.persistence.Table(name = "shipments", schema = "commerce")
class Shipment(
    @jakarta.persistence.Id
    val id: UUID = UUID.randomUUID(),
    @jakarta.persistence.Column(name = "order_id", nullable = false, unique = true)
    val orderId: UUID,
    @jakarta.persistence.Column(nullable = false, length = 20)
    val provider: String = "BITESHIP",
    @jakarta.persistence.Column(name = "provider_order_id")
    var providerOrderId: String? = null,
    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @jakarta.persistence.Column(nullable = false)
    var status: FulfillmentStatus,
    @jakarta.persistence.Column(name = "raw_provider_status")
    var rawProviderStatus: String? = null,
    @jakarta.persistence.Column(name = "courier_code")
    var courierCode: String? = null,
    @jakarta.persistence.Column(name = "courier_name")
    var courierName: String? = null,
    @jakarta.persistence.Column(name = "service_code")
    var serviceCode: String? = null,
    @jakarta.persistence.Column(name = "service_name")
    var serviceName: String? = null,
    @jakarta.persistence.Column(name = "tracking_number")
    var trackingNumber: String? = null,
    @jakarta.persistence.Column(name = "tracking_url", columnDefinition = "TEXT")
    var trackingUrl: String? = null,
    @jakarta.persistence.Column(length = 300)
    var note: String? = null,
    @jakarta.persistence.Column(name = "provider_response_payload", columnDefinition = "JSONB")
    var providerResponsePayload: String? = null,
    @jakarta.persistence.Column(name = "booked_at")
    var bookedAt: Instant? = null,
    @jakarta.persistence.Column(name = "shipped_at")
    var shippedAt: Instant? = null,
    @jakarta.persistence.Column(name = "delivered_at")
    var deliveredAt: Instant? = null,
    @jakarta.persistence.Column(name = "created_at", updatable = false)
    val createdAt: Instant = Instant.now(),
    @jakarta.persistence.Column(name = "updated_at")
    var updatedAt: Instant = Instant.now(),
)
