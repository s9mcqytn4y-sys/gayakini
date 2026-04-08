package com.gayakini.audit.domain

import java.time.OffsetDateTime
import java.util.UUID

data class AuditEvent(
    val actorId: String,
    val actorRole: String,
    val entityType: String,
    val entityId: String,
    val eventType: String,
    val previousState: Map<String, Any?>? = null,
    val newState: Map<String, Any?>? = null,
    val reason: String? = null,
    val id: UUID = UUID.randomUUID(),
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
)
