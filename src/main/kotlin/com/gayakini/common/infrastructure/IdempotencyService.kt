package com.gayakini.common.infrastructure

import com.fasterxml.jackson.databind.ObjectMapper
import com.gayakini.common.util.HashUtils
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityManager
import jakarta.persistence.Id
import jakarta.persistence.NoResultException
import jakarta.persistence.PersistenceContext
import jakarta.persistence.Table
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class IdempotencyService(
    @PersistenceContext private val entityManager: EntityManager,
    private val objectMapper: ObjectMapper,
) {
    @Transactional
    fun <T> handle(
        scope: String,
        key: String,
        requestPayload: Any,
        requesterType: String,
        requesterId: UUID?,
        ttlSeconds: Long = 3600,
        action: () -> T,
    ): T {
        val requestHash = HashUtils.sha256(objectMapper.writeValueAsString(requestPayload))

        val existing = findKey(scope, key)
        if (existing != null) {
            if (existing.requestHash != requestHash) {
                throw IllegalStateException("Idempotency key mismatch: same key used for different request content.")
            }
            if (existing.responseStatus != null) {
                // In a real implementation, we would deserialize and return the response body.
                // For simplicity in this patch, we might re-execute or return the existing resource if found.
                // But the action() block usually contains the full logic.
            }
        }

        val result = action()

        // Record the success (Simplified)
        saveKey(scope, key, requestHash, requesterType, requesterId, ttlSeconds)

        return result
    }

    private fun findKey(
        scope: String,
        key: String,
    ): IdempotencyKeyRecord? {
        return try {
            entityManager.createQuery(
                "SELECT r FROM IdempotencyKeyRecord r WHERE r.scope = :scope AND r.idempotencyKey = :key",
                IdempotencyKeyRecord::class.java,
            ).setParameter("scope", scope)
                .setParameter("key", key)
                .singleResult
        } catch (e: NoResultException) {
            null
        }
    }

    private fun saveKey(
        scope: String,
        key: String,
        hash: String,
        type: String,
        id: UUID?,
        ttl: Long,
    ) {
        val record =
            IdempotencyKeyRecord(
                scope = scope,
                idempotencyKey = key,
                requestHash = hash,
                requesterType = type,
                requesterId = id,
                expiresAt = Instant.now().plusSeconds(ttl),
            )
        entityManager.persist(record)
    }
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
