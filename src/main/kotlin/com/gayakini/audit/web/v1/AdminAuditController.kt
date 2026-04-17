import com.gayakini.audit.api.AuditLogDto
import com.gayakini.audit.api.AuditMapper
import com.gayakini.audit.application.AuditService
import com.gayakini.common.api.PaginatedResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/admin/audits")
@Tag(name = "Admin Audits", description = "API jejak audit untuk administrator (Internal/English).")
class AdminAuditController(
    private val auditService: AuditService,
) {
    @GetMapping
    @Operation(
        summary = "Get audit logs",
        description = "Retrieve audit logs with optional filtering by entity type and ID.",
    )
    fun getAudits(
        @Parameter(description = "Type of the entity (e.g., Product, Order)")
        @RequestParam(required = false)
        entityType: String?,
        @Parameter(description = "Unique ID of the entity")
        @RequestParam(required = false)
        entityId: String?,
        @Parameter(hidden = true)
        pageable: Pageable,
    ): PaginatedResponse<AuditLogDto> {
        val page = auditService.getAudits(entityType, entityId, pageable).map { AuditMapper.toDto(it) }
        return PaginatedResponse.from("Audit logs retrieved successfully.", page)
    }
}
