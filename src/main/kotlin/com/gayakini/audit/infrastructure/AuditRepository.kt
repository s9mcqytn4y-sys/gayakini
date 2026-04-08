package com.gayakini.audit.infrastructure

import com.gayakini.audit.domain.AuditLog
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AuditRepository : JpaRepository<AuditLog, UUID> {
    fun findByEntityTypeAndEntityId(
        entityType: String,
        entityId: String,
        pageable: Pageable,
    ): Page<AuditLog>
}
