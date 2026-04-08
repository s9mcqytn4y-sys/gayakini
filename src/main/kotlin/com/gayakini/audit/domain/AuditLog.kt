package com.gayakini.audit.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "audit_logs", schema = "commerce")
class AuditLog(
    @Id
    val id: UUID,
    @Column(name = "actor_id", nullable = false)
    val actorId: String,
    @Column(name = "actor_role", nullable = false)
    val actorRole: String,
    @Column(name = "entity_type", nullable = false)
    val entityType: String,
    @Column(name = "entity_id", nullable = false)
    val entityId: String,
    @Column(name = "event_type", nullable = false)
    val eventType: String,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "previous_state", columnDefinition = "jsonb")
    val previousState: Map<String, Any?>?,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_state", columnDefinition = "jsonb")
    val newState: Map<String, Any?>?,
    @Column(name = "reason")
    val reason: String?,
    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime,
)
