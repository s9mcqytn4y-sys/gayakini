package com.gayakini.customer.api

import com.gayakini.common.api.StandardResponse
import com.gayakini.common.api.UnauthorizedException
import com.gayakini.customer.application.CustomerService
import com.gayakini.infrastructure.security.SecurityUtils
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

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

    @PostMapping("/profile-picture")
    fun uploadProfilePicture(
        @RequestParam("file") file: MultipartFile,
    ): StandardResponse<CustomerProfileResponse> {
        val currentUser = SecurityUtils.getCurrentUser() ?: throw UnauthorizedException()

        customerService.updateProfilePicture(
            customerId = currentUser.id,
            inputStream = file.inputStream,
            originalFilename = file.originalFilename ?: "profile.jpg",
        )

        return StandardResponse(
            message = "Foto profil berhasil diunggah.",
            data = customerService.getProfile(currentUser.id),
        )
    }
}
