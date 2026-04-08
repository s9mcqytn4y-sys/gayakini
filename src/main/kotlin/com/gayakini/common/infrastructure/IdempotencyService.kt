package com.gayakini.common.infrastructure

import com.fasterxml.jackson.databind.ObjectMapper
import com.gayakini.common.util.HashUtils
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.Optional
import java.util.UUID

@Service
class IdempotencyService(
    private val repository: IdempotencyKeyRepository,
    private val objectMapper: ObjectMapper,
) {
    /**
     * Handles idempotency for a given scope and key.
     * Note: Propagation is REQUIRED (default) to support integration tests that use @Transactional.
     */
    @Transactional
    fun <T> handle(
        scope: String,
        key: String,
        requestPayload: Any,
        requesterType: String,
        requesterId: UUID?,
        ttlSeconds: Long = 86400,
        action: () -> T,
    ): T {
        val requestHash = HashUtils.sha256(objectMapper.writeValueAsString(requestPayload))
        val existing = repository.findByScopeAndIdempotencyKey(scope, key)

        if (existing.isPresent) {
            val record = existing.get()
            if (record.requestHash != requestHash) {
                error("Idempotency key mismatch: same key used for different request content.")
            }
            if (record.responseBody != null) {
                @Suppress("UNCHECKED_CAST")
                return objectMapper.readValue(record.responseBody, Any::class.java) as T
            }
        }

        val result = action()

        val record =
            existing.orElseGet {
                IdempotencyKeyRecord(
                    scope = scope,
                    idempotencyKey = key,
                    requesterType = requesterType,
                    requesterId = requesterId,
                    requestHash = requestHash,
                    expiresAt = Instant.now().plusSeconds(ttlSeconds),
                )
            }

        record.responseBody = objectMapper.writeValueAsString(result)
        repository.save(record)

        return result
    }
}

@Repository
interface IdempotencyKeyRepository : JpaRepository<IdempotencyKeyRecord, UUID> {
    fun findByScopeAndIdempotencyKey(
        scope: String,
        idempotencyKey: String,
    ): Optional<IdempotencyKeyRecord>
}

@Entity
@Table(name = "idempotency_keys", schema = "commerce")
class IdempotencyKeyRecord(
    @Id
    val id: UUID = UUID.randomUUID(),
    val scope: String,
    @Column(name = "idempotency_key")
    val idempotencyKey: String,
    @Column(name = "requester_type")
    val requesterType: String,
    @Column(name = "requester_id")
    val requesterId: UUID?,
    @Column(name = "request_hash")
    val requestHash: String,
    @Column(name = "response_status")
    var responseStatus: Int? = null,
    @Column(name = "response_body", columnDefinition = "JSONB")
    var responseBody: String? = null,
    @Column(name = "created_at")
    val createdAt: Instant = Instant.now(),
    @Column(name = "expires_at")
    val expiresAt: Instant,
)
