package com.gayakini.audit.web.v1

import com.gayakini.audit.application.AuditService
import com.gayakini.audit.domain.AuditLog
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/audits")
@Tag(name = "Admin Audits", description = "Audit trail query API for administrators")
class AdminAuditController(
    private val auditService: AuditService,
) {
    @GetMapping
    @Operation(summary = "Get audit logs with filtering and pagination")
    fun getAudits(
        @RequestParam(required = false) entityType: String?,
        @RequestParam(required = false) entityId: String?,
        pageable: Pageable,
    ): Page<AuditLog> {
        return auditService.getAudits(entityType, entityId, pageable)
    }
}
