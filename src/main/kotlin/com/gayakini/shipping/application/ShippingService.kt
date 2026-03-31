package com.gayakini.shipping.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.gayakini.order.domain.OrderRepository
import com.gayakini.order.domain.OrderStatus
import com.gayakini.shipping.domain.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
class ShippingService(
    private val orderRepository: OrderRepository,
    private val shipmentRepository: ShipmentRepository,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(ShippingService::class.java)

    @Transactional
    fun processBiteshipWebhook(payload: Map<String, Any>) {
        val event = payload["event"] as? String ?: return
        val orderIdStr = payload["order_id"] as? String ?: return
        
        logger.info("Memproses webhook Biteship event: {} untuk order: {}", event, orderIdStr)

        // Lookup by Biteship order_id (external_id in our shipment table)
        val shipment = shipmentRepository.findByExternalId(orderIdStr)
            .orElseGet {
                // Fallback: lookup by internal order UUID if Biteship sends it as reference
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
            "order.price" -> handlePriceUpdate(shipment, payload)
        }
        
        shipment.updatedAt = Instant.now()
        shipmentRepository.save(shipment)
    }

    private fun handleStatusUpdate(shipment: Shipment, payload: Map<String, Any>) {
        val status = payload["status"] as? String ?: return
        shipment.status = status
        
        // Map Biteship status to internal Order status if necessary
        val order = shipment.order
        when (status) {
            "picked", "shipped" -> {
                if (order.status != OrderStatus.SHIPPED) {
                    order.status = OrderStatus.SHIPPED
                    orderRepository.save(order)
                }
            }
            "delivered" -> {
                if (order.status != OrderStatus.DELIVERED) {
                    order.status = OrderStatus.DELIVERED
                    orderRepository.save(order)
                }
            }
        }
    }

    private fun handleWaybillUpdate(shipment: Shipment, payload: Map<String, Any>) {
        val waybillId = payload["waybill_id"] as? String
        if (waybillId != null) {
            shipment.waybillId = waybillId
        }
    }

    private fun handlePriceUpdate(shipment: Shipment, payload: Map<String, Any>) {
        // Log price changes for audit, update internal cost if needed
        logger.info("Update harga Biteship untuk shipment {}: {}", shipment.id, payload["price"])
        shipment.rawPayload = objectMapper.writeValueAsString(payload)
    }
}

interface ShipmentRepository : org.springframework.data.jpa.repository.JpaRepository<Shipment, UUID> {
    fun findByExternalId(externalId: String): Optional<Shipment>
    fun findByOrderId(orderId: UUID): Optional<Shipment>
}

@jakarta.persistence.Entity
@jakarta.persistence.Table(name = "shipments")
class Shipment(
    @jakarta.persistence.Id
    val id: UUID,

    @jakarta.persistence.OneToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "order_id", nullable = false)
    val order: com.gayakini.order.domain.Order,

    @jakarta.persistence.Column(name = "external_id")
    var externalId: String?,

    @jakarta.persistence.Column(name = "waybill_id")
    var waybillId: String?,

    @jakarta.persistence.Column(name = "courier_company")
    var courierCompany: String?,

    @jakarta.persistence.Column(name = "courier_service")
    var courierService: String?,

    @jakarta.persistence.Column(nullable = false)
    var status: String,

    @jakarta.persistence.Column(columnDefinition = "JSONB")
    var history: String? = null,

    @jakarta.persistence.Column(name = "raw_payload", columnDefinition = "JSONB")
    var rawPayload: String? = null,

    @jakarta.persistence.Column(name = "created_at", updatable = false)
    val createdAt: Instant = Instant.now(),

    @jakarta.persistence.Column(name = "updated_at")
    var updatedAt: Instant = Instant.now()
)
