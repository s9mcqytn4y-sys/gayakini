package com.gayakini.infrastructure.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

class JwtAuthenticationFilter(
    private val jwtSecret: String,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val authHeader = request.getHeader("Authorization")

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            val token = authHeader.substring(7)
            try {
                // Baseline: In a real app, use a JWT library like jjwt or auth0 to parse/verify
                // For this sandbox baseline, we'll demonstrate the structure
                val principal = verifyToken(token)

                if (principal != null) {
                    val authentication =
                        UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            listOf(SimpleGrantedAuthority("ROLE_${principal.role}")),
                        )
                    SecurityContextHolder.getContext().authentication = authentication
                }
            } catch (e: Exception) {
                // Token invalid or expired - clear context
                SecurityContextHolder.clearContext()
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun verifyToken(token: String): UserPrincipal? {
        // This is a stub for sandbox baseline.
        // In a real RC hardening, you'd parse JWT claims here.
        if (token == "sandbox-test-token") {
            return UserPrincipal(
                id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
                email = "sandbox@gayakini.com",
                role = "CUSTOMER",
            )
        }
        return null
    }
}
