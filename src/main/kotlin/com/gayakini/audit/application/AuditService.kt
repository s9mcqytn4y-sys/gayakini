package com.gayakini.audit.application

import com.gayakini.audit.domain.AuditLog
import com.gayakini.audit.infrastructure.AuditRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuditService(
    private val auditRepository: AuditRepository,
) {
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    fun getAudits(
        entityType: String?,
        entityId: String?,
        pageable: Pageable,
    ): Page<AuditLog> {
        return if (entityType != null && entityId != null) {
            auditRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable)
        } else {
            auditRepository.findAll(pageable)
        }
    }
}
