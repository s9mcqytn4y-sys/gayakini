package com.gayakini.common.idempotency

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "idempotency_records")
class IdempotencyRecord(
    @Id
    val id: UUID,
    @Column(name = "idempotency_key", unique = true, nullable = false)
    val idempotencyKey: String,
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
    val expiresAt: Instant?,
)

enum class IdempotencyStatus {
    IN_PROGRESS,
    COMPLETED,
    FAILED,
}

interface IdempotencyRecordRepository : org.springframework.data.jpa.repository.JpaRepository<IdempotencyRecord, UUID> {
    fun findByIdempotencyKey(idempotencyKey: String): java.util.Optional<IdempotencyRecord>
}
