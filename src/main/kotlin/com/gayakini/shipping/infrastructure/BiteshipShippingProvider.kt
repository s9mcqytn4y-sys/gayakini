package com.gayakini.shipping.infrastructure

import com.gayakini.infrastructure.config.GayakiniProperties
import com.gayakini.shipping.domain.*
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate

@Component
class BiteshipShippingProvider(
    private val properties: GayakiniProperties,
    private val restTemplate: RestTemplate,
) : ShippingProvider {
    private val logger = LoggerFactory.getLogger(BiteshipShippingProvider::class.java)

    @Retryable(
        value = [RestClientException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 1000, multiplier = 2.0),
    )
    override fun getRates(
        origin: String,
        destination: String,
        items: List<ShippingItem>,
    ): List<ShippingRate> {
        val url = "${properties.biteship.apiUrl}/rates/couriers"
        val requestBody = createRatesRequestBody(origin, destination, items)
        val headers = createHeaders()

        return try {
            val response = restTemplate.postForEntity(url, HttpEntity(requestBody, headers), Map::class.java)
            if (!response.statusCode.is2xxSuccessful) return emptyList()

            val body = response.body as? Map<*, *> ?: return emptyList()
            val pricing = body["pricing"] as? List<*> ?: return emptyList()
            pricing.mapNotNull { it as? Map<*, *> }.map { mapToShippingRate(it) }
        } catch (e: RestClientException) {
            logger.error("Gagal mengambil tarif pengiriman Biteship", e)
            emptyList()
        }
    }

    private fun createRatesRequestBody(
        origin: String,
        destination: String,
        items: List<ShippingItem>,
    ): Map<String, Any> {
        return mapOf(
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
    }

    private fun mapToShippingRate(it: Map<*, *>): ShippingRate {
        return ShippingRate(
            id = it["company"].toString() + "_" + it["type"].toString(),
            courierCode = it["company"].toString(),
            courierName = it["courier_name"].toString(),
            serviceCode = it["type"].toString(),
            serviceName = it["courier_service_name"].toString(),
            description = it["description"]?.toString(),
            price = (it["price"] as? Number)?.toLong() ?: 0L,
            minDuration = parseDuration(it["duration"]?.toString(), true),
            maxDuration = parseDuration(it["duration"]?.toString(), false),
        )
    }

    private fun parseDuration(
        duration: String?,
        isMin: Boolean,
    ): Int? {
        if (duration == null) return null
        val parts = duration.split("-").map { it.trim().filter { c -> c.isDigit() }.toIntOrNull() }
        return if (isMin) parts.firstOrNull() else parts.lastOrNull()
    }

    @Retryable(
        value = [RestClientException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 1000, multiplier = 2.0),
    )
    override fun createShipment(
        orderId: String,
        rateId: String,
        sender: ContactInfo,
        receiver: ContactInfo,
        items: List<ShippingItem>,
    ): ShipmentBooking {
        val url = "${properties.biteship.apiUrl}/orders"
        val requestBody = createShipmentRequestBody(orderId, rateId, sender, receiver, items)
        val headers = createHeaders()

        return try {
            val response = restTemplate.postForEntity(url, HttpEntity(requestBody, headers), Map::class.java)
            if (response.statusCode.is2xxSuccessful) {
                val body = response.body as? Map<*, *>
                checkNotNull(body) { "Empty body" }
                ShipmentBooking(
                    bookingId = body["id"].toString(),
                    waybillId = body["waybill_id"]?.toString(),
                    status = body["status"].toString(),
                    rawPayload = body.toString(),
                )
            } else {
                error("Biteship order creation failed")
            }
        } catch (e: RestClientException) {
            logger.error("Gagal membuat order pengiriman Biteship", e)
            throw e
        }
    }

    private fun createShipmentRequestBody(
        orderId: String,
        rateId: String,
        sender: ContactInfo,
        receiver: ContactInfo,
        items: List<ShippingItem>,
    ): Map<String, Any> {
        val request: Map<String, Any> =
            mapOf(
                "shipper_contact_name" to sender.fullName,
                "shipper_contact_phone" to sender.phone,
                "shipper_contact_email" to sender.email.orEmpty(),
                "shipper_organization" to "gayakini",
                "origin_contact_name" to sender.fullName,
                "origin_contact_phone" to sender.phone,
                "origin_address" to sender.address,
                "origin_area_id" to sender.areaId.orEmpty(),
                "destination_contact_name" to receiver.fullName,
                "destination_contact_phone" to receiver.phone,
                "destination_contact_email" to receiver.email.orEmpty(),
                "destination_address" to receiver.address,
                "destination_area_id" to receiver.areaId.orEmpty(),
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
        return request
    }

    @Retryable(
        value = [RestClientException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 1000, multiplier = 2.0),
    )
    override fun trackShipment(waybillId: String): ShipmentTracking {
        val url = "${properties.biteship.apiUrl}/trackings/$waybillId"
        val headers = createHeaders()

        return try {
            val response = restTemplate.getForEntity(url, Map::class.java, headers)
            if (response.statusCode.is2xxSuccessful) {
                val body = response.body as? Map<*, *>
                checkNotNull(body) { "Empty body" }
                val historyRaw = body["history"] as? List<*> ?: emptyList<Any>()
                val history =
                    historyRaw.mapNotNull { it as? Map<*, *> }.map {
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
                error("Tracking failed")
            }
        } catch (e: RestClientException) {
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
