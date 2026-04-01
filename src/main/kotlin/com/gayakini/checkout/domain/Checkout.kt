package com.gayakini.checkout.domain

import com.gayakini.cart.domain.Cart
import com.gayakini.catalog.domain.Product
import com.gayakini.catalog.domain.ProductVariant
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "checkouts", schema = "commerce")
class Checkout(
    @Id
    val id: UUID = UUID.randomUUID(),
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false, unique = true)
    val cart: Cart,
    @Column(name = "customer_id")
    var customerId: UUID? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: CheckoutStatus = CheckoutStatus.ACTIVE,
    @Column(name = "currency_code", nullable = false, length = 3)
    val currencyCode: String = "IDR",
    @Column(name = "access_token_hash", unique = true, length = 64)
    var accessTokenHash: String? = null,
    @Column(name = "subtotal_amount", nullable = false)
    var subtotalAmount: Long = 0,
    @Column(name = "shipping_cost_amount", nullable = false)
    var shippingCostAmount: Long = 0,
    @Column(name = "total_amount", insertable = false, updatable = false)
    val totalAmount: Long = 0,
    @Column(name = "selected_shipping_quote_id")
    var selectedShippingQuoteId: UUID? = null,
    @OneToMany(mappedBy = "checkout", cascade = [CascadeType.ALL], orphanRemoval = true)
    val items: MutableList<CheckoutItem> = mutableListOf(),
    @OneToOne(mappedBy = "checkout", cascade = [CascadeType.ALL])
    @PrimaryKeyJoinColumn
    var shippingAddress: CheckoutShippingAddress? = null,
    @Column(name = "expires_at")
    var expiresAt: Instant? = null,
    @Column(name = "created_at", updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now(),
)

enum class CheckoutStatus { ACTIVE, READY_FOR_ORDER, ORDER_CREATED, EXPIRED }

@Entity
@Table(name = "checkout_items", schema = "commerce")
class CheckoutItem(
    @Id
    val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checkout_id", nullable = false)
    val checkout: Checkout,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    val product: Product,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    val variant: ProductVariant,
    @Column(name = "product_title_snapshot", nullable = false, length = 180)
    val productTitleSnapshot: String,
    @Column(name = "sku_snapshot", nullable = false, length = 64)
    val skuSnapshot: String,
    @Column(nullable = false, length = 50)
    val color: String,
    @Column(name = "size_code", nullable = false, length = 20)
    val sizeCode: String,
    @Column(nullable = false)
    val quantity: Int,
    @Column(name = "unit_price_amount", nullable = false)
    val unitPriceAmount: Long,
    @Column(name = "compare_at_amount")
    val compareAtAmount: Long? = null,
    @Column(name = "primary_image_url", columnDefinition = "TEXT")
    val primaryImageUrl: String? = null,
    @Column(name = "line_total_amount", insertable = false, updatable = false)
    val lineTotalAmount: Long = 0,
    @Column(name = "created_at", updatable = false)
    val createdAt: Instant = Instant.now(),
)

@Entity
@Table(name = "checkout_shipping_addresses", schema = "commerce")
class CheckoutShippingAddress(
    @Id
    @Column(name = "checkout_id")
    val checkoutId: UUID,
    @OneToOne
    @MapsId
    @JoinColumn(name = "checkout_id")
    val checkout: Checkout,
    @Column(name = "customer_address_id")
    var customerAddressId: UUID? = null,
    @Column(name = "recipient_name", nullable = false, length = 120)
    var recipientName: String,
    @Column(nullable = false, length = 30)
    var phone: String,
    @Column(nullable = false, length = 200)
    var line1: String,
    @Column(length = 200)
    var line2: String?,
    @Column(length = 200)
    var notes: String?,
    @Column(name = "area_id", nullable = false, length = 100)
    var areaId: String,
    @Column(nullable = false, length = 120)
    var district: String,
    @Column(nullable = false, length = 120)
    var city: String,
    @Column(nullable = false, length = 120)
    var province: String,
    @Column(name = "postal_code", nullable = false, length = 20)
    var postalCode: String,
    @Column(name = "country_code", nullable = false, length = 2)
    var countryCode: String = "ID",
    @Column(name = "created_at", updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now(),
)

@Entity
@Table(name = "checkout_shipping_quotes", schema = "commerce")
class CheckoutShippingQuote(
    @Id
    val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checkout_id", nullable = false)
    val checkout: Checkout,
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
    @Column(name = "is_recommended", nullable = false)
    val isRecommended: Boolean = false,
    @Column(name = "raw_payload", columnDefinition = "JSONB")
    val rawPayload: String?,
    @Column(name = "expires_at")
    val expiresAt: Instant?,
    @Column(name = "created_at", updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now(),
)
