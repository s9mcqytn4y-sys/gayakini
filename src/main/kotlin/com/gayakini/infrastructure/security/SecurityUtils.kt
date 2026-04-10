package com.gayakini.infrastructure.security

import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import java.util.UUID

object SecurityUtils {
    fun getCurrentUser(): UserPrincipal? {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication == null || !authentication.isAuthenticated) {
            return null
        }
        return authentication.principal as? UserPrincipal
    }

    fun getCurrentUserId(): UUID {
        return getCurrentUser()?.id ?: throw AccessDeniedException("Sesi tidak valid.")
    }

    fun getCurrentUserRole(): String {
        return getCurrentUser()?.role ?: throw AccessDeniedException("Sesi tidak valid.")
    }

    fun isAdmin(): Boolean = getCurrentUser()?.role == "ADMIN"

    fun checkOwnership(
        ownerId: UUID,
        resourceName: String = "Sumber daya",
    ) {
        val currentUser = getCurrentUser() ?: throw AccessDeniedException("Sesi tidak valid.")
        if (currentUser.role != "ADMIN" && currentUser.id != ownerId) {
            throw AccessDeniedException("$resourceName bukan milik Anda.")
        }
    }
}
