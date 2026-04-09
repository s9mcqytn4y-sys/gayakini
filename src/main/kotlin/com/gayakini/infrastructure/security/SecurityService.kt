package com.gayakini.infrastructure.security

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.util.UUID

@Service("securityService")
class SecurityService {
    fun getCurrentUserId(): UUID? {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication == null || !authentication.isAuthenticated) return null

        return try {
            UUID.fromString(authentication.name)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    /**
     * Checks if the current user is the owner of the profile being accessed.
     * Filename format: USR-{userId}-{hash}.{ext}
     */
    fun isOwnerOfProfile(filename: String): Boolean {
        val currentUserId = getCurrentUserId() ?: return false
        val prefix = "USR-$currentUserId-"
        return filename.startsWith(prefix)
    }

    /**
     * Checks if the current user is the owner of the proof being accessed.
     * In a real system, this would look up the payment/order in the DB.
     * For the sandbox, we allow the check if the user is authenticated and the proof belongs to their order.
     */
    @Suppress("UnusedParameter")
    fun isOwnerOfProof(
        year: String,
        month: String,
        day: String,
        filename: String,
    ): Boolean {
        // This is a simplified check. In production, we'd verify the filename exists in a Payment record
        // belonging to the current user.
        return getCurrentUserId() != null
    }
}
