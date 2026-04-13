package com.gayakini.customer.api

import com.gayakini.common.api.StandardResponse
import com.gayakini.customer.application.CustomerService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/v1/auth")
@Tag(name = "Authentication", description = "Endpoint untuk pendaftaran, login, dan penyegaran token.")
class AuthController(private val customerService: CustomerService) {
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Pendaftaran akun customer baru")
    @SecurityRequirements // Menandai endpoint ini sebagai publik di OpenAPI
    fun register(
        @Valid @RequestBody request: RegisterRequest,
    ): StandardResponse<AuthTokensData> {
        val data = customerService.register(request)
        return StandardResponse(
            message = "Akun berhasil dibuat.",
            data = data,
        )
    }

    @PostMapping("/login")
    @Operation(summary = "Login customer atau admin")
    @SecurityRequirements
    fun login(
        @Valid @RequestBody request: LoginRequest,
    ): StandardResponse<AuthTokensData> {
        val data = customerService.login(request)
        return StandardResponse(
            message = "Login berhasil.",
            data = data,
        )
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token menggunakan refresh token")
    @SecurityRequirements
    fun refresh(
        @Valid @RequestBody request: RefreshTokenRequest,
    ): StandardResponse<AuthTokensData> {
        return StandardResponse(
            message = "Sesi login berhasil diperbarui.",
            data = customerService.refresh(request),
        )
    }
}
