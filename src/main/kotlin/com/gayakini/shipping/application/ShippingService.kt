package com.gayakini.shipping.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.gayakini.order.domain.FulfillmentStatus
import com.gayakini.order.domain.OrderRepository
import com.gayakini.order.domain.OrderStatus
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
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(ShippingService::class.java)

    @Transactional
    fun processBiteshipWebhook(payload: Map<String, Any>) {
        val event = payload["event"] as? String ?: return
        val orderIdStr = payload["order_id"] as? String ?: return

        logger.info("Memproses webhook Biteship event: {} untuk order: {}", event, orderIdStr)

        val shipment =
            shipmentRepository.findByProviderOrderId(orderIdStr)
                .orElseGet {
                    try {
                        val orderId = UUID.fromString(orderIdStr)
                        shipmentRepository.findByOrderId(orderId).orElse(null)
                    } catch (e: Exception) {
                        null
                    }
                }

        if (shipment == null) {
            logger.warn("Shipment tidak ditemukan untuk external_id/order_id: {}", orderIdStr)
            return
        }

        when (event) {
            "order.status" -> handleStatusUpdate(shipment, payload)
            "order.waybill_id" -> handleWaybillUpdate(shipment, payload)
        }

        shipment.updatedAt = Instant.now()
        shipmentRepository.save(shipment)
    }

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
                shipment.status = FulfillmentStatus.IN_TRANSIT
                order.status = OrderStatus.SHIPPED
                order.fulfillmentStatus = FulfillmentStatus.IN_TRANSIT
            }
            "delivered" -> {
                shipment.status = FulfillmentStatus.DELIVERED
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
    @jakarta.persistence.Column(name = "created_at", updatable = false)
    val createdAt: Instant = Instant.now(),
    @jakarta.persistence.Column(name = "updated_at")
    var updatedAt: Instant = Instant.now(),
)
