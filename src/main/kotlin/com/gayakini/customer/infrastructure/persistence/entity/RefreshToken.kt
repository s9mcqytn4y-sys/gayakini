package com.gayakini.customer.infrastructure.persistence.entity

import com.gayakini.customer.domain.Customer
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "refresh_tokens", schema = "commerce")
class RefreshToken(
    @Id
    val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    val customer: Customer,
    @Column(nullable = false, unique = true)
    val token: String,
    @Column(nullable = false)
    val expiryDate: Instant,
    @Column(nullable = false)
    var revoked: Boolean = false,
    @Column(name = "replaced_by_token")
    var replacedByToken: String? = null,
    @Column(name = "family_id", nullable = false)
    val familyId: UUID = UUID.randomUUID(),
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
) {
    val isExpired: Boolean get() = Instant.now().isAfter(expiryDate)
    val isActive: Boolean get() = !revoked && !isExpired
}
