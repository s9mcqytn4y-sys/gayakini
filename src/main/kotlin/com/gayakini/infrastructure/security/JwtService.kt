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
    companion object {
        private const val MIN_SECRET_BYTE_SIZE = 32
    }

    init {
        val secret = properties.jwt.secret
        val bytes = secret.toByteArray(StandardCharsets.UTF_8)
        check(bytes.size >= MIN_SECRET_BYTE_SIZE) {
            "CRITICAL SECURITY FAILURE: JWT secret key must be at least 256 bits (32 characters). " +
                "Current length: ${bytes.size}. Application cannot start in this insecure state."
        }
    }

    private val signingKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(properties.jwt.secret.toByteArray(StandardCharsets.UTF_8))
    }

    fun generateAccessToken(
        userId: UUID,
        email: String,
        role: String,
    ): String {
        val now = Date()
        val expiry = Date(now.time + properties.jwt.accessTokenExpirationMinutes * 60 * 1000)

        return Jwts.builder()
            .subject(userId.toString())
            .claim("email", email)
            .claim("role", role)
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

    fun parseRefreshTokenSubject(token: String): UUID? {
        return try {
            val claims: Claims =
                Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .payload
            UUID.fromString(claims.subject)
        } catch (e: Exception) {
            null
        }
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
            )
        } catch (e: Exception) {
            null
        }
    }
}
