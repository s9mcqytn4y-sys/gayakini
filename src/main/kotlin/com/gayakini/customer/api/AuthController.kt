package com.gayakini.customer.api

import com.gayakini.common.api.ApiResponse
import com.gayakini.customer.domain.CustomerRole
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/v1/auth")
class AuthController {
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(
        @RequestBody request: RegisterRequest,
    ): ApiResponse<AuthTokensData> {
        // TODO: Implement registration logic in CustomerService
        return ApiResponse.success(
            message = "Akun berhasil dibuat.",
            data = mockAuthResponse(),
        )
    }

    @PostMapping("/login")
    fun login(
        @RequestBody request: LoginRequest,
    ): ApiResponse<AuthTokensData> {
        // TODO: Implement login logic
        return ApiResponse.success(
            message = "Login berhasil.",
            data = mockAuthResponse(),
        )
    }

    @PostMapping("/refresh")
    fun refresh(
        @RequestBody request: RefreshTokenRequest,
    ): ApiResponse<AuthTokensData> {
        // TODO: Implement refresh logic
        return ApiResponse.success(
            message = "Sesi login berhasil diperbarui.",
            data = mockAuthResponse(),
        )
    }

    private fun mockAuthResponse() =
        AuthTokensData(
            tokens =
                JwtTokenPair(
                    accessToken = "mock-access-token",
                    refreshToken = "mock-refresh-token",
                    expiresIn = 3600,
                ),
            customer =
                CustomerProfileResponse(
                    id = UUID.randomUUID(),
                    email = "user@example.com",
                    phone = "08123456789",
                    fullName = "John Doe",
                    role = CustomerRole.CUSTOMER,
                    createdAt = Instant.now(),
                ),
        )
}
