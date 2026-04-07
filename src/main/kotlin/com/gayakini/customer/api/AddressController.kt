package com.gayakini.customer.api

import com.gayakini.common.api.StandardResponse
import com.gayakini.common.api.UnauthorizedException
import com.gayakini.customer.application.CustomerService
import com.gayakini.infrastructure.security.SecurityUtils
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/v1/me/addresses")
class AddressController(private val customerService: CustomerService) {
    @GetMapping
    fun listMyAddresses(): StandardResponse<List<AddressResponse>> {
        val currentUser = SecurityUtils.getCurrentUser() ?: throw UnauthorizedException()
        val addresses = customerService.getAddresses(currentUser.id)

        return StandardResponse(
            message = "Daftar alamat berhasil diambil.",
            data = addresses.map { mapToResponse(it) },
        )
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createAddress(
        @Valid @RequestBody request: AddressUpsertRequest,
    ): StandardResponse<AddressResponse> {
        val currentUser = SecurityUtils.getCurrentUser() ?: throw UnauthorizedException()

        val saved = customerService.upsertAddress(currentUser.id, request)

        return StandardResponse(
            message = "Alamat berhasil disimpan.",
            data = mapToResponse(saved),
        )
    }

    @PatchMapping("/{addressId}")
    fun updateAddress(
        @PathVariable addressId: UUID,
        @Valid @RequestBody request: AddressUpsertRequest,
    ): StandardResponse<AddressResponse> {
        val currentUser = SecurityUtils.getCurrentUser() ?: throw UnauthorizedException()
        val saved = customerService.updateAddress(currentUser.id, addressId, request)

        return StandardResponse(
            message = "Alamat berhasil diperbarui.",
            data = mapToResponse(saved),
        )
    }

    @DeleteMapping("/{addressId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteAddress(
        @PathVariable addressId: UUID,
    ) {
        val currentUser = SecurityUtils.getCurrentUser() ?: throw UnauthorizedException()
        customerService.deleteAddress(currentUser.id, addressId)
    }

    private fun mapToResponse(address: com.gayakini.customer.domain.CustomerAddress): AddressResponse {
        return AddressResponse(
            id = address.id,
            recipientName = address.recipientName,
            phone = address.phone,
            line1 = address.line1,
            line2 = address.line2,
            notes = address.notes,
            areaId = address.areaId,
            district = address.district,
            city = address.city,
            province = address.province,
            postalCode = address.postalCode,
            countryCode = address.countryCode,
            isDefault = address.isDefault,
        )
    }
}
