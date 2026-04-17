package com.gayakini.audit.api

import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime
import java.util.UUID

@Schema(description = "Data transfer object for audit logs.")
data class AuditLogDto(
    val id: UUID,
    val actorId: String,
    val actorRole: String,
    val entityType: String,
    val entityId: String,
    val eventType: String,
    val previousState: Map<String, Any?>?,
    val newState: Map<String, Any?>?,
    val reason: String?,
    val createdAt: OffsetDateTime,
)
