package com.gayakini.customer.api

import com.gayakini.common.api.StandardResponse
import com.gayakini.common.api.UnauthorizedException
import com.gayakini.customer.application.CustomerService
import com.gayakini.infrastructure.security.SecurityUtils
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/me")
class MeController(private val customerService: CustomerService) {
    @GetMapping
    fun getMyProfile(): StandardResponse<CustomerProfileResponse> {
        val currentUser = SecurityUtils.getCurrentUser() ?: throw UnauthorizedException()
        return StandardResponse(
            message = "Profil berhasil diambil.",
            data = customerService.getProfile(currentUser.id),
        )
    }

    @PatchMapping
    fun updateMyProfile(
        @Valid @RequestBody
        request: UpdateProfileRequest,
    ): StandardResponse<CustomerProfileResponse> {
        val currentUser = SecurityUtils.getCurrentUser() ?: throw UnauthorizedException()
        return StandardResponse(
            message = "Profil berhasil diperbarui.",
            data = customerService.updateProfile(currentUser.id, request),
        )
    }
}
