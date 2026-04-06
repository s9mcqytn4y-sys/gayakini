package com.gayakini.customer.api

import com.gayakini.common.api.StandardResponse
import com.gayakini.customer.application.CustomerService
import com.gayakini.infrastructure.security.SecurityUtils
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/me")
class MeController(private val customerService: CustomerService) {
    @GetMapping
    fun getMyProfile(): StandardResponse<CustomerProfileResponse> {
        val currentUser = SecurityUtils.getCurrentUser() ?: throw IllegalStateException("Unauthorized")
        return StandardResponse(
            message = "Profil berhasil diambil.",
            data = customerService.getProfile(currentUser.id),
        )
    }
}
