package com.gayakini.shipping.domain

interface ShippingProvider {
    fun getRates(origin: String, destination: String, items: List<ShippingItem>): List<ShippingRate>
    fun createShipment(orderId: String, rateId: String, sender: ContactInfo, receiver: ContactInfo, items: List<ShippingItem>): ShipmentBooking
    fun trackShipment(waybillId: String): ShipmentTracking
}

data class ShippingItem(
    val name: String,
    val weightGrams: Int,
    val quantity: Int,
    val valueIdr: Long
)

data class ShippingRate(
    val id: String,
    val courierName: String,
    val courierService: String,
    val costIdr: Long,
    val estimatedDays: String
)

data class ContactInfo(
    val fullName: String,
    val phone: String,
    val email: String?,
    val address: String,
    val areaId: String? = null
)

data class ShipmentBooking(
    val bookingId: String,
    val waybillId: String?,
    val status: String,
    val rawPayload: String
)

data class ShipmentTracking(
    val waybillId: String,
    val status: String,
    val history: List<TrackingEvent>
)

data class TrackingEvent(
    val timestamp: String,
    val description: String,
    val location: String?
)
