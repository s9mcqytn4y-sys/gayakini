package com.gayakini.infrastructure.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.util.UUID

data class UserPrincipal(
    val id: UUID,
    val email: String,
    val role: String,
    val permissions: Set<String> = emptySet(),
) {
    fun toAuthorities(): Collection<GrantedAuthority> {
        val authorities = mutableListOf<GrantedAuthority>()
        authorities.add(SimpleGrantedAuthority("ROLE_$role"))
        permissions.forEach { authorities.add(SimpleGrantedAuthority(it)) }
        return authorities
    }
}
