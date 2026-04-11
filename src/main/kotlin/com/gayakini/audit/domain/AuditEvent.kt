package com.gayakini.audit.domain

import com.gayakini.common.util.UuidV7Generator
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Standardized Audit Event for the Gayakini application.
 *
 * Captures point-in-time state changes for domain entities.
 * Uses UUIDv7 for deterministic, time-ordered event sequence.
 */
data class AuditEvent(
    val actorId: String,
    val actorRole: String,
    val entityType: String,
    val entityId: String,
    val eventType: String,
    val previousState: Map<String, Any?>? = null,
    val newState: Map<String, Any?>? = null,
    val reason: String? = null,
    val id: UUID = UuidV7Generator.generate(),
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
)
