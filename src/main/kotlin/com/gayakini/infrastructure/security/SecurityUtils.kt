package com.gayakini.infrastructure.security

import org.springframework.security.core.context.SecurityContextHolder
import java.util.*

object SecurityUtils {
    fun getCurrentUser(): UserPrincipal? {
        val authentication = SecurityContextHolder.getContext().authentication ?: return null
        if (authentication.principal is UserPrincipal) {
            return authentication.principal as UserPrincipal
        }
        // For starter/dev purposes, if not authenticated, we return a mock or null
        return null
    }

    fun getCurrentUserId(): UUID? = getCurrentUser()?.id
}
