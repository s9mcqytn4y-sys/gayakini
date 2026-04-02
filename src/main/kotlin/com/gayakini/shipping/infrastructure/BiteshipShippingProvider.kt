package com.gayakini.shipping.infrastructure

import com.gayakini.infrastructure.config.GayakiniProperties
import com.gayakini.shipping.domain.*
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class BiteshipShippingProvider(
    private val properties: GayakiniProperties,
    private val restTemplate: RestTemplate,
) : ShippingProvider {
    private val logger = LoggerFactory.getLogger(BiteshipShippingProvider::class.java)

    override fun getRates(
        origin: String,
        destination: String,
        items: List<ShippingItem>,
    ): List<ShippingRate> {
        val url = "${properties.biteship.apiUrl}/rates/couriers"

        val requestBody =
            mapOf(
                "origin_area_id" to origin,
                "destination_area_id" to destination,
                "couriers" to "jne,jnt,sicepat,tiki,anteraja",
                "items" to
                    items.map {
                        mapOf(
                            "name" to it.name,
                            "weight" to it.weightGrams,
                            "quantity" to it.quantity,
                            "value" to it.valueIdr,
                        )
                    },
            )

        val headers = createHeaders()

        return try {
            val response = restTemplate.postForEntity(url, HttpEntity(requestBody, headers), Map::class.java)
            if (response.statusCode.is2xxSuccessful) {
                val body = response.body as Map<*, *>
                val pricing = body["pricing"] as List<Map<String, Any>>
                pricing.map {
                    ShippingRate(
                        id = it["company"].toString() + "_" + it["type"].toString(),
                        courierCode = it["company"].toString(),
                        courierName = it["courier_name"].toString(),
                        serviceCode = it["type"].toString(),
                        serviceName = it["courier_service_name"].toString(),
                        description = it["description"]?.toString(),
                        price = (it["price"] as Number).toLong(),
                        minDuration = parseDuration(it["duration"]?.toString(), true),
                        maxDuration = parseDuration(it["duration"]?.toString(), false),
                    )
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            logger.error("Gagal mengambil tarif pengiriman Biteship", e)
            emptyList()
        }
    }

    private fun parseDuration(
        duration: String?,
        isMin: Boolean,
    ): Int? {
        if (duration == null) return null
        val parts = duration.split("-").map { it.trim().filter { c -> c.isDigit() }.toIntOrNull() }
        return if (isMin) parts.firstOrNull() else parts.lastOrNull()
    }

    override fun createShipment(
        orderId: String,
        rateId: String,
        sender: ContactInfo,
        receiver: ContactInfo,
        items: List<ShippingItem>,
    ): ShipmentBooking {
        val url = "${properties.biteship.apiUrl}/orders"

        val requestBody =
            mapOf(
                "shipper_contact_name" to sender.fullName,
                "shipper_contact_phone" to sender.phone,
                "shipper_contact_email" to sender.email,
                "shipper_organization" to "gayakini",
                "origin_contact_name" to sender.fullName,
                "origin_contact_phone" to sender.phone,
                "origin_address" to sender.address,
                "origin_area_id" to sender.areaId,
                "destination_contact_name" to receiver.fullName,
                "destination_contact_phone" to receiver.phone,
                "destination_contact_email" to receiver.email,
                "destination_address" to receiver.address,
                "destination_area_id" to receiver.areaId,
                "courier_company" to rateId.split("_")[0],
                "courier_type" to rateId.split("_")[1],
                "delivery_type" to "now",
                "items" to
                    items.map {
                        mapOf(
                            "name" to it.name,
                            "description" to it.name,
                            "weight" to it.weightGrams,
                            "quantity" to it.quantity,
                            "value" to it.valueIdr,
                        )
                    },
            )

        val headers = createHeaders()

        return try {
            val response = restTemplate.postForEntity(url, HttpEntity(requestBody, headers), Map::class.java)
            if (response.statusCode.is2xxSuccessful) {
                val body = response.body as Map<*, *>
                ShipmentBooking(
                    bookingId = body["id"].toString(),
                    waybillId = body["waybill_id"]?.toString(),
                    status = body["status"].toString(),
                    rawPayload = body.toString(),
                )
            } else {
                throw IllegalStateException("Biteship order creation failed")
            }
        } catch (e: Exception) {
            logger.error("Gagal membuat order pengiriman Biteship", e)
            throw e
        }
    }

    override fun trackShipment(waybillId: String): ShipmentTracking {
        val url = "${properties.biteship.apiUrl}/trackings/$waybillId"

        val headers = createHeaders()

        return try {
            val response = restTemplate.getForEntity(url, Map::class.java, headers)
            if (response.statusCode.is2xxSuccessful) {
                val body = response.body as Map<*, *>
                val history =
                    (body["history"] as List<Map<String, Any>>).map {
                        TrackingEvent(
                            timestamp = it["updated_at"].toString(),
                            description = it["note"].toString(),
                            location = it["status"].toString(),
                        )
                    }
                ShipmentTracking(
                    waybillId = waybillId,
                    status = body["status"].toString(),
                    history = history,
                )
            } else {
                throw IllegalStateException("Tracking failed")
            }
        } catch (e: Exception) {
            logger.error("Gagal melacak pengiriman Biteship", e)
            throw e
        }
    }

    private fun createHeaders(): HttpHeaders {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.set("Authorization", "Bearer ${properties.biteship.apiKey}")
        return headers
    }
}
