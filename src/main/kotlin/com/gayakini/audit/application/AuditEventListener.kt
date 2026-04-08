package com.gayakini.audit.application

import com.gayakini.audit.domain.AuditEvent
import com.gayakini.audit.domain.AuditLog
import com.gayakini.audit.infrastructure.AuditRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class AuditEventListener(
    private val auditRepository: AuditRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun handleAuditEvent(event: AuditEvent) {
        try {
            val log =
                AuditLog(
                    id = event.id,
                    actorId = event.actorId,
                    actorRole = event.actorRole,
                    entityType = event.entityType,
                    entityId = event.entityId,
                    eventType = event.eventType,
                    previousState = redactSensitiveData(event.previousState),
                    newState = redactSensitiveData(event.newState),
                    reason = event.reason,
                    createdAt = event.createdAt,
                )
            auditRepository.save(log)
            logger.info("Audit log saved for {} - {}: {}", event.entityType, event.entityId, event.eventType)
        } catch (e: IllegalArgumentException) {
            logger.error("Failed to save audit log: {}", e.message)
        } catch (e: IllegalStateException) {
            logger.error("Failed to save audit log: {}", e.message)
        }
    }

    private fun redactSensitiveData(data: Map<String, Any?>?): Map<String, Any?>? {
        if (data == null) return null

        val sensitiveKeys = setOf("password", "signatureKey", "phoneNumber", "token", "snapToken")

        return data.entries.associate { (key, value) ->
            if (sensitiveKeys.any { it.equals(key, ignoreCase = true) }) {
                key to "[REDACTED]"
            } else if (value is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                key to redactSensitiveData(value as Map<String, Any?>)
            } else {
                key to value
            }
        }
    }
}
