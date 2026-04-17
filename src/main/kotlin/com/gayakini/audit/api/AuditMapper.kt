package com.gayakini.audit.api

import com.gayakini.audit.domain.AuditLog

object AuditMapper {
    fun toDto(auditLog: AuditLog): AuditLogDto {
        return AuditLogDto(
            id = auditLog.id,
            actorId = auditLog.actorId,
            actorRole = auditLog.actorRole,
            entityType = auditLog.entityType,
            entityId = auditLog.entityId,
            eventType = auditLog.eventType,
            previousState = auditLog.previousState,
            newState = auditLog.newState,
            reason = auditLog.reason,
            createdAt = auditLog.createdAt,
        )
    }
}
