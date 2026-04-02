package com.gayakini.shipping.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "merchant_shipping_origins", schema = "commerce")
class MerchantShippingOrigin(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(nullable = false, unique = true, length = 50)
    var code: String,
    @Column(nullable = false, length = 120)
    var name: String,
    @Column(name = "contact_name", nullable = false, length = 120)
    var contactName: String,
    @Column(name = "contact_phone", nullable = false, length = 30)
    var contactPhone: String,
    @Column(name = "contact_email", length = 254)
    var contactEmail: String?,
    @Column(nullable = false, length = 200)
    var line1: String,
    @Column(length = 200)
    var line2: String?,
    @Column(length = 200)
    var notes: String?,
    @Column(name = "area_id", length = 100)
    var areaId: String?,
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
    @Column(precision = 10, scale = 7)
    var latitude: BigDecimal? = null,
    @Column(precision = 10, scale = 7)
    var longitude: BigDecimal? = null,
    @Column(name = "is_default", nullable = false)
    var isDefault: Boolean = false,
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
    @Column(name = "created_at", updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now(),
)
