package com.gayakini.customer.api

import com.gayakini.common.api.StandardResponse
import com.gayakini.common.api.UnauthorizedException
import com.gayakini.customer.application.CustomerService
import com.gayakini.infrastructure.security.SecurityUtils
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.MediaType
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
@Tag(name = "My Profile", description = "Endpoint untuk mengelola profil pengguna yang sedang login.")
class MeController(private val customerService: CustomerService) {
    @GetMapping
    @Operation(summary = "Ambil profil saya", description = "Mengambil data detail profil pengguna yang sedang login.")
    fun getMyProfile(): StandardResponse<CustomerProfileResponse> {
        val currentUser = SecurityUtils.getCurrentUser() ?: throw UnauthorizedException()
        return StandardResponse(
            message = "Profil berhasil diambil.",
            data = customerService.getProfile(currentUser.id),
        )
    }

    @PatchMapping
    @Operation(summary = "Update profil saya", description = "Memperbarui informasi profil pengguna yang sedang login.")
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

    @PostMapping("/profile-picture", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(
        summary = "Unggah foto profil",
        description = "Mengunggah foto profil baru untuk pengguna yang sedang login.",
    )
    fun uploadProfilePicture(
        @Parameter(
            description = "File gambar profil (JPEG/PNG)",
            content = [
                Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = Schema(type = "string", format = "binary"),
                ),
            ],
        )
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
