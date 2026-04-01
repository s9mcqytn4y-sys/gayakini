package com.gayakini.customer.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "customer_addresses", schema = "commerce")
class CustomerAddress(
    @Id
    val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    val customer: Customer,
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
    @Column(name = "is_default", nullable = false)
    var isDefault: Boolean = false,
    @Column(name = "created_at", updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now(),
)
