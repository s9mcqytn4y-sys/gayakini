package com.gayakini.customer.api

import com.gayakini.common.api.StandardResponse
import com.gayakini.customer.application.CustomerService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/v1/auth")
class AuthController(private val customerService: CustomerService) {
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
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
    fun refresh(
        @Valid @RequestBody request: RefreshTokenRequest,
    ): StandardResponse<AuthTokensData> {
        // TODO: Implement refresh token rotation logic in CustomerService
        // For now, it's a stub to keep the path but mark it as needing implementation if not already done.
        throw UnsupportedOperationException("Token refresh rotation is being implemented.")
    }
}
