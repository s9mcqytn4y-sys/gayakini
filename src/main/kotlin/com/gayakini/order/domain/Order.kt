package com.gayakini.order.domain

import com.gayakini.catalog.domain.Product
import com.gayakini.catalog.domain.ProductVariant
import com.gayakini.common.util.UuidV7Generator
import jakarta.persistence.*
import org.springframework.data.domain.Persistable
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "orders", schema = "commerce")
class Order(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private val id: UUID = UuidV7Generator.generate(),
    @Column(name = "order_number", unique = true, nullable = false, length = 50)
    val orderNumber: String,
    @Column(name = "checkout_id", nullable = false, unique = true)
    val checkoutId: UUID,
    @Column(name = "cart_id", nullable = false, unique = true)
    val cartId: UUID,
    @Column(name = "customer_id")
    val customerId: UUID?,
    @Column(name = "access_token_hash", length = 64, unique = true)
    val accessTokenHash: String?,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: OrderStatus,
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    var paymentStatus: PaymentStatus = PaymentStatus.PENDING,
    @Enumerated(EnumType.STRING)
    @Column(name = "fulfillment_status", nullable = false)
    var fulfillmentStatus: FulfillmentStatus = FulfillmentStatus.UNFULFILLED,
    @Column(name = "currency_code", nullable = false, length = 3)
    val currencyCode: String = "IDR",
    @Column(name = "subtotal_amount", nullable = false)
    val subtotalAmount: Long,
    @Column(name = "shipping_cost_amount", nullable = false)
    val shippingCostAmount: Long,
    // Matched to schema generated column
    @Column(name = "total_amount", insertable = false, updatable = false)
    private val totalAmountGenerated: Long = 0,
    @Column(name = "current_payment_id")
    var currentPaymentId: UUID? = null,
    @Column(name = "customer_notes", length = 500)
    var customerNotes: String? = null,
    @Column(name = "placed_at", nullable = false)
    val placedAt: Instant = Instant.now(),
    @Column(name = "paid_at")
    var paidAt: Instant? = null,
    @Column(name = "cancelled_at")
    var cancelledAt: Instant? = null,
    @Column(name = "cancellation_reason", length = 300)
    var cancellationReason: String? = null,
    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true)
    val items: MutableList<OrderItem> = mutableListOf(),
    @OneToOne(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true)
    var shippingAddress: OrderShippingAddress? = null,
    @OneToOne(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true)
    var shippingSelection: OrderShippingSelection? = null,
    @Column(name = "created_at", updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now(),
) : Persistable<UUID> {
    @Transient
    private var isNewRecord = true

    val totalAmount: Long
        get() = subtotalAmount + shippingCostAmount

    override fun getId(): UUID = id

    override fun isNew(): Boolean = isNewRecord

    @PostPersist
    @PostLoad
    fun markNotNew() {
        isNewRecord = false
    }
}

enum class OrderStatus { PENDING_PAYMENT, PAID, READY_TO_SHIP, SHIPPED, COMPLETED, CANCELLED }

enum class PaymentStatus { PENDING, PAID, FAILED, EXPIRED, CANCELLED, REFUNDED }

enum class FulfillmentStatus { UNFULFILLED, BOOKED, IN_TRANSIT, DELIVERED, RETURNED, CANCELLED }

@Entity
@Table(name = "order_items", schema = "commerce")
class OrderItem(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private val id: UUID = UuidV7Generator.generate(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    val order: Order,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    val product: Product,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    val variant: ProductVariant,
    @Column(name = "sku_snapshot", nullable = false, length = 64)
    val skuSnapshot: String,
    @Column(name = "title_snapshot", nullable = false, length = 180)
    val titleSnapshot: String,
    @Column(nullable = false, length = 50)
    val color: String,
    @Column(name = "size_code", nullable = false, length = 20)
    val sizeCode: String,
    @Column(nullable = false)
    val quantity: Int,
    @Column(name = "unit_price_amount", nullable = false)
    val unitPriceAmount: Long,
    // Matched to schema generated column
    @Column(name = "line_total_amount", insertable = false, updatable = false)
    private val lineTotalAmountGenerated: Long = 0,
    @Column(name = "created_at", updatable = false)
    val createdAt: Instant = Instant.now(),
) : Persistable<UUID> {
    @Transient
    private var isNewRecord = true

    val lineTotalAmount: Long
        get() = unitPriceAmount * quantity

    override fun getId(): UUID = id

    override fun isNew(): Boolean = isNewRecord

    @PostPersist
    @PostLoad
    fun markNotNew() {
        isNewRecord = false
    }
}

@Entity
@Table(name = "order_shipping_addresses", schema = "commerce")
class OrderShippingAddress(
    @Id
    @Column(name = "order_id")
    val orderId: UUID,
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "order_id")
    val order: Order,
    @Column(name = "recipient_name", nullable = false, length = 120)
    val recipientName: String,
    @Column(nullable = false, length = 30)
    val phone: String,
    @Column(nullable = false, length = 200)
    val line1: String,
    @Column(length = 200)
    var line2: String?,
    @Column(length = 200)
    var notes: String?,
    @Column(name = "area_id", nullable = false, length = 100)
    val areaId: String,
    @Column(nullable = false, length = 120)
    val district: String,
    @Column(nullable = false, length = 120)
    val city: String,
    @Column(nullable = false, length = 120)
    val province: String,
    @Column(name = "postal_code", nullable = false, length = 20)
    val postalCode: String,
    @Column(name = "country_code", nullable = false, length = 2)
    val countryCode: String = "ID",
    @Column(name = "created_at", updatable = false)
    val createdAt: Instant = Instant.now(),
) : Persistable<UUID> {
    @Transient
    private var isNewRecord = true

    override fun getId(): UUID = orderId

    override fun isNew(): Boolean = isNewRecord

    @PostPersist
    @PostLoad
    fun markNotNew() {
        isNewRecord = false
    }
}

@Entity
@Table(name = "order_shipping_selections", schema = "commerce")
class OrderShippingSelection(
    @Id
    @Column(name = "order_id")
    val orderId: UUID,
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "order_id")
    val order: Order,
    @Column(nullable = false, length = 20)
    val provider: String = "BITESHIP",
    @Column(name = "provider_reference", length = 120)
    val providerReference: String?,
    @Column(name = "courier_code", nullable = false, length = 50)
    val courierCode: String,
    @Column(name = "courier_name", nullable = false, length = 120)
    val courierName: String,
    @Column(name = "service_code", nullable = false, length = 50)
    val serviceCode: String,
    @Column(name = "service_name", nullable = false, length = 120)
    val serviceName: String,
    @Column(length = 200)
    val description: String?,
    @Column(name = "cost_amount", nullable = false)
    val costAmount: Long,
    @Column(name = "estimated_days_min")
    val estimatedDaysMin: Int?,
    @Column(name = "estimated_days_max")
    val estimatedDaysMax: Int?,
    @Column(name = "raw_quote_payload", columnDefinition = "JSONB")
    val rawQuotePayload: String?,
    @Column(name = "created_at", updatable = false)
    val createdAt: Instant = Instant.now(),
) : Persistable<UUID> {
    @Transient
    private var isNewRecord = true

    override fun getId(): UUID = orderId

    override fun isNew(): Boolean = isNewRecord

    @PostPersist
    @PostLoad
    fun markNotNew() {
        isNewRecord = false
    }
}
