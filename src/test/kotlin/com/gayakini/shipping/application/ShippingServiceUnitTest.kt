package com.gayakini.shipping.application

import com.gayakini.audit.application.AuditContext
import com.gayakini.audit.domain.AuditEvent
import com.gayakini.common.infrastructure.IdempotencyService
import com.gayakini.common.util.UuidV7Generator
import com.gayakini.customer.domain.CustomerRepository
import com.gayakini.order.api.AdminCreateShipmentRequest
import com.gayakini.order.domain.*
import com.gayakini.shipping.domain.*
import io.mockk.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import java.util.*

class ShippingServiceUnitTest {

    private val orderRepository = mockk<OrderRepository>()
    private val shipmentRepository = mockk<ShipmentRepository>()
    private val merchantOriginRepository = mockk<MerchantShippingOriginRepository>()
    private val shippingProvider = mockk<ShippingProvider>()
    private val customerRepository = mockk<CustomerRepository>()
    private val idempotencyService = mockk<IdempotencyService>()
    private val auditContext = mockk<AuditContext>()
    private val eventPublisher = mockk<ApplicationEventPublisher>()

    private val shippingService = ShippingService(
        orderRepository,
        shipmentRepository,
        merchantOriginRepository,
        shippingProvider,
        customerRepository,
        idempotencyService,
        auditContext,
        eventPublisher
    )

    private fun createOrder(status: OrderStatus = OrderStatus.PAID): Order {
        val orderId = UuidV7Generator.generate()
        val order = Order(
            id = orderId,
            orderNumber = "ORD-TEST-123",
            checkoutId = UUID.randomUUID(),
            cartId = UUID.randomUUID(),
            customerId = null,
            accessTokenHash = null,
            status = status,
            subtotalAmount = 100000L,
            shippingCostAmount = 10000L
        )
        order.shippingAddress = OrderShippingAddress(
            orderId = orderId,
            order = order,
            recipientName = "Recipient",
            phone = "+62812345678",
            email = "recipient@test.com",
            line1 = "Jl. Test No. 1",
            line2 = null,
            notes = null,
            areaId = "area_123",
            district = "District",
            city = "City",
            province = "Province",
            postalCode = "12345",
            countryCode = "ID"
        )
        order.shippingSelection = OrderShippingSelection(
            orderId = orderId,
            order = order,
            provider = "BITESHIP",
            providerReference = "rate_123",
            courierCode = "jne",
            courierName = "JNE",
            serviceCode = "reg",
            serviceName = "Regular",
            description = "Regular",
            costAmount = 10000L,
            estimatedDaysMin = 1,
            estimatedDaysMax = 3,
            rawQuotePayload = "{}"
        )
        return order
    }

    @Test
    fun `bookShipment should create shipment and update order status`() {
        val order = createOrder(OrderStatus.PAID)
        val orderId = order.id
        val origin = mockk<MerchantShippingOrigin>()

        every { shipmentRepository.findByOrderId(orderId) } returns Optional.empty()
        every { orderRepository.findById(orderId) } returns Optional.of(order)
        every { merchantOriginRepository.findDefaultActive() } returns Optional.of(origin)

        every { origin.contactName } returns "Sender"
        every { origin.contactPhone } returns "08111"
        every { origin.contactEmail } returns "sender@test.com"
        every { origin.line1 } returns "Jl. Origin"
        every { origin.line2 } returns null
        every { origin.district } returns "O-District"
        every { origin.city } returns "O-City"
        every { origin.province } returns "O-Province"
        every { origin.postalCode } returns "54321"
        every { origin.areaId } returns "area_orig"

        val booking = ShipmentBooking("b_123", "w_123", "allocated", "{}")
        every { shippingProvider.createShipment(any(), any(), any(), any(), any()) } returns booking

        every { orderRepository.save(any()) } returnsArgument 0
        every { shipmentRepository.save(any()) } answers { it.invocation.args[0] as Shipment }
        every { auditContext.getCurrentActor() } returns ("ADMIN-1" to "ADMIN")
        every { eventPublisher.publishEvent(any<AuditEvent>()) } returns Unit

        every { idempotencyService.handle<Shipment>(any(), any(), any(), any(), any(), any(), any()) } answers {
            val block = invocation.args[6] as () -> Shipment
            block()
        }

        val shipment = shippingService.bookShipment(orderId, "key", AdminCreateShipmentRequest("notes"))

        assertEquals(orderId, shipment.orderId)
        assertEquals("b_123", shipment.providerOrderId)
        assertEquals(FulfillmentStatus.BOOKED, shipment.status)
        assertEquals(OrderStatus.READY_TO_SHIP, order.status)
        assertEquals(FulfillmentStatus.BOOKED, order.fulfillmentStatus)

        verify { orderRepository.save(any()) }
        verify { shipmentRepository.save(any()) }
    }

    @Test
    fun `processBiteshipWebhook should update shipment and order for delivered event`() {
        val order = createOrder(OrderStatus.SHIPPED)
        order.fulfillmentStatus = FulfillmentStatus.IN_TRANSIT
        val orderId = order.id
        val shipment = Shipment(orderId = orderId, status = FulfillmentStatus.IN_TRANSIT)

        val payload = mapOf(
            "event" to "order.status",
            "order_id" to orderId.toString(),
            "status" to "delivered"
        )

        every { shipmentRepository.findByProviderOrderId(orderId.toString()) } returns Optional.empty()
        every { shipmentRepository.findByOrderId(orderId) } returns Optional.of(shipment)
        every { orderRepository.findById(orderId) } returns Optional.of(order)

        every { orderRepository.save(any()) } returnsArgument 0
        every { shipmentRepository.save(any()) } answers { it.invocation.args[0] as Shipment }
        every { auditContext.getCurrentActor() } returns ("SYSTEM" to "SYSTEM")
        every { eventPublisher.publishEvent(any<AuditEvent>()) } returns Unit

        shippingService.processBiteshipWebhook(payload)

        assertEquals(FulfillmentStatus.DELIVERED, shipment.status)
        assertEquals(OrderStatus.COMPLETED, order.status)
        assertEquals(FulfillmentStatus.DELIVERED, order.fulfillmentStatus)
    }

    @Test
    fun `processBiteshipWebhook should update shipment for returned event`() {
        val order = createOrder(OrderStatus.SHIPPED)
        order.fulfillmentStatus = FulfillmentStatus.IN_TRANSIT
        val orderId = order.id
        val shipment = Shipment(orderId = orderId, status = FulfillmentStatus.IN_TRANSIT)

        val payload = mapOf(
            "event" to "order.status",
            "order_id" to orderId.toString(),
            "status" to "returned"
        )

        every { shipmentRepository.findByProviderOrderId(orderId.toString()) } returns Optional.empty()
        every { shipmentRepository.findByOrderId(orderId) } returns Optional.of(shipment)
        every { orderRepository.findById(orderId) } returns Optional.of(order)

        every { orderRepository.save(any()) } returnsArgument 0
        every { shipmentRepository.save(any()) } answers { it.invocation.args[0] as Shipment }
        every { auditContext.getCurrentActor() } returns ("SYSTEM" to "SYSTEM")
        every { eventPublisher.publishEvent(any<AuditEvent>()) } returns Unit

        shippingService.processBiteshipWebhook(payload)

        assertEquals(FulfillmentStatus.RETURNED, shipment.status)
        assertEquals(FulfillmentStatus.RETURNED, order.fulfillmentStatus)
    }
}
