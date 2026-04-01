package com.gayakini.customer.api

import com.gayakini.customer.domain.CustomerRole
import java.time.Instant
import java.util.UUID

data class RegisterRequest(
    val email: String,
    val phone: String?,
    val fullName: String,
    val password: String,
)

data class LoginRequest(
    val email: String,
    val password: String,
)

data class RefreshTokenRequest(
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
    val countryCode: String = "ID",
    val isDefault: Boolean = false,
)
