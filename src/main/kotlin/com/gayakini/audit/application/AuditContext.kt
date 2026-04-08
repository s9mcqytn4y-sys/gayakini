package com.gayakini.audit.application

import com.gayakini.infrastructure.security.UserPrincipal
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class AuditContext {
    fun getCurrentActor(): Pair<String, String> {
        val authentication = SecurityContextHolder.getContext().authentication

        if (authentication == null || !authentication.isAuthenticated || authentication.principal == "anonymousUser") {
            return "SYSTEM" to "SYSTEM"
        }

        return when (val principal = authentication.principal) {
            is UserPrincipal -> principal.id.toString() to principal.role
            else -> authentication.name to "UNKNOWN"
        }
    }
}
