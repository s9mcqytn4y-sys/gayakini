package com.gayakini.customer.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "customers", schema = "commerce")
class Customer(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(nullable = false)
    var email: String,
    @Column(name = "email_normalized", insertable = false, updatable = false)
    val emailNormalized: String? = null,
    @Column(name = "password_hash", nullable = false)
    var passwordHash: String,
    @Column(name = "full_name", nullable = false)
    var fullName: String,
    @Column(name = "phone")
    var phone: String?,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: CustomerRole = CustomerRole.CUSTOMER,
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
    @Column(name = "last_login_at")
    var lastLoginAt: Instant? = null,
    @Column(name = "created_at", updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now(),
)

enum class CustomerRole { CUSTOMER }
