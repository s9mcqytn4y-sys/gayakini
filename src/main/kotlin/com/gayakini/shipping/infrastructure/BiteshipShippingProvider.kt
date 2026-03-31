package com.gayakini.shipping.infrastructure

import com.gayakini.shipping.domain.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class BiteshipShippingProvider(
    @Value("\${biteship.api-key}") private val apiKey: String,
    @Value("\${biteship.base-url}") private val baseUrl: String,
    private val restTemplate: RestTemplate
) : ShippingProvider {

    override fun getRates(origin: String, destination: String, items: List<ShippingItem>): List<ShippingRate> {
        // Implementation logic for Biteship Rates API
        return emptyList()
    }

    override fun createShipment(
        orderId: String,
        rateId: String,
        sender: ContactInfo,
        receiver: ContactInfo,
        items: List<ShippingItem>
    ): ShipmentBooking {
        // Implementation logic for Biteship Order API
        return ShipmentBooking(
            bookingId = "biteship-booking-id-placeholder",
            waybillId = null,
            status = "allocated",
            rawPayload = "{}"
        )
    }

    override fun trackShipment(waybillId: String): ShipmentTracking {
        // Implementation logic for Biteship Tracking API
        return ShipmentTracking(
            waybillId = waybillId,
            status = "picked",
            history = emptyList()
        )
    }
}
