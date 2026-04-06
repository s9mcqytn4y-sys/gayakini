package com.gayakini.customer.domain

import com.gayakini.common.util.UuidV7Generator
import jakarta.persistence.*
import org.springframework.data.domain.Persistable
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "customers", schema = "commerce")
class Customer(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private val id: UUID = UuidV7Generator.generate(),
    @Column(nullable = false, unique = true)
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
) : Persistable<UUID> {
    @Transient
    private var _isNew = true

    override fun getId(): UUID = id

    override fun isNew(): Boolean = _isNew

    @PostPersist
    @PostLoad
    fun markNotNew() {
        _isNew = false
    }
}
