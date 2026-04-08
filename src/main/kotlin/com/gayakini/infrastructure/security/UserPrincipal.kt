package com.gayakini.infrastructure.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.util.UUID

data class UserPrincipal(
    val id: UUID,
    val email: String,
    val role: String,
) {
    fun toAuthorities(): Collection<GrantedAuthority> {
        return listOf(SimpleGrantedAuthority("ROLE_$role"))
    }
}
