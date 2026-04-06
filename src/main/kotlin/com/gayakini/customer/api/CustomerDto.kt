package com.gayakini.customer.api

import com.gayakini.customer.domain.CustomerRole
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class RegisterRequest(
    @field:Email
    @field:NotBlank
    @field:Size(max = 254)
    val email: String,
    @field:Pattern(regexp = "^[0-9+]{8,20}$")
    val phone: String?,
    @field:NotBlank
    @field:Size(max = 120)
    val fullName: String,
    @field:NotBlank
    @field:Size(min = 8, max = 72)
    val password: String,
)

data class LoginRequest(
    @field:Email
    @field:NotBlank
    @field:Size(max = 254)
    val email: String,
    @field:NotBlank
    @field:Size(min = 8, max = 72)
    val password: String,
)

data class RefreshTokenRequest(
    @field:NotBlank
    @field:Size(min = 20, max = 4096)
    val refreshToken: String,
)

data class AuthTokensResponse(
    val message: String,
    val data: AuthTokensData,
)

data class AuthTokensData(
    val tokens: JwtTokenPair,
    val customer: CustomerProfileResponse,
)

data class JwtTokenPair(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Int,
)

data class CustomerProfileResponse(
    val id: UUID,
    val email: String,
    val phone: String?,
    val fullName: String,
    val role: CustomerRole,
    val createdAt: Instant,
)

data class AddressResponse(
    val id: UUID,
    val recipientName: String,
    val phone: String,
    val line1: String,
    val line2: String?,
    val notes: String?,
    val areaId: String,
    val district: String,
    val city: String,
    val province: String,
    val postalCode: String,
    val countryCode: String,
    val isDefault: Boolean,
)

data class AddressUpsertRequest(
    @field:NotBlank
    @field:Size(max = 120)
    val recipientName: String,
    @field:NotBlank
    @field:Pattern(regexp = "^[0-9+]{8,20}$")
    val phone: String,
    @field:NotBlank
    @field:Size(max = 200)
    val line1: String,
    @field:Size(max = 200)
    val line2: String?,
    @field:Size(max = 200)
    val notes: String?,
    @field:NotBlank
    @field:Size(max = 100)
    val areaId: String,
    @field:NotBlank
    @field:Size(max = 120)
    val district: String,
    @field:NotBlank
    @field:Size(max = 120)
    val city: String,
    @field:NotBlank
    @field:Size(max = 120)
    val province: String,
    @field:NotBlank
    @field:Size(max = 20)
    val postalCode: String,
    @field:Pattern(regexp = "^[A-Z]{2}$")
    val countryCode: String = "ID",
    val isDefault: Boolean = false,
)
