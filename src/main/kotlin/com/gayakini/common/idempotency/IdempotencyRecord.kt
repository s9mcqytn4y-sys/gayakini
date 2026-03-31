package com.gayakini.common.idempotency

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "idempotency_records")
class IdempotencyRecord(
    @Id
    val id: UUID,

    @Column(unique = true, nullable = false)
    val key: String,

    @Column(name = "request_hash")
    val requestHash: String?,

    @Column(name = "response_body", columnDefinition = "JSONB")
    var responseBody: String?,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: IdempotencyStatus,

    @Column(name = "created_at", updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "expires_at")
    val expiresAt: Instant?
)

enum class IdempotencyStatus {
    IN_PROGRESS, COMPLETED, FAILED
}

interface IdempotencyRecordRepository : org.springframework.data.jpa.repository.JpaRepository<IdempotencyRecord, UUID> {
    fun findByKey(key: String): java.util.Optional<IdempotencyRecord>
}
