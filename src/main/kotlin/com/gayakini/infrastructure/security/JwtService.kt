package com.gayakini.infrastructure.security

import com.gayakini.infrastructure.config.GayakiniProperties
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Service
class JwtService(private val properties: GayakiniProperties) {
    private val signingKey: SecretKey by lazy {
        val secret = properties.jwt.secret
        val bytes = secret.toByteArray(StandardCharsets.UTF_8)
        if (bytes.size < 32) {
            Keys.hmacShaKeyFor(secret.padEnd(32, '0').toByteArray(StandardCharsets.UTF_8))
        } else {
            Keys.hmacShaKeyFor(bytes)
        }
    }

    fun generateAccessToken(
        userId: UUID,
        email: String,
        role: String,
        permissions: Set<String> = emptySet(),
    ): String {
        val now = Date()
        val expiry = Date(now.time + properties.jwt.accessTokenExpirationMinutes * 60 * 1000)

        return Jwts.builder()
            .subject(userId.toString())
            .claim("email", email)
            .claim("role", role)
            .claim("perms", permissions.toList())
            .issuedAt(now)
            .expiration(expiry)
            .signWith(signingKey)
            .compact()
    }

    fun generateRefreshToken(userId: UUID): String {
        val now = Date()
        val expiry = Date(now.time + properties.jwt.refreshTokenExpirationDays * 24 * 60 * 60 * 1000)

        return Jwts.builder()
            .subject(userId.toString())
            .issuedAt(now)
            .expiration(expiry)
            .signWith(signingKey)
            .compact()
    }

    @Suppress("UNCHECKED_CAST")
    fun parseToken(token: String): UserPrincipal? {
        return try {
            val claims: Claims =
                Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .payload

            UserPrincipal(
                id = UUID.fromString(claims.subject),
                email = claims["email"] as String,
                role = claims["role"] as String,
                permissions = (claims["perms"] as? List<String>)?.toSet() ?: emptySet(),
            )
        } catch (e: Exception) {
            null
        }
    }
}
