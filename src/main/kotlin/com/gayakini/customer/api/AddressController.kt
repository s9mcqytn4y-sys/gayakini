package com.gayakini.customer.api

import com.gayakini.common.api.StandardResponse
import com.gayakini.common.api.UnauthorizedException
import com.gayakini.customer.application.CustomerService
import com.gayakini.infrastructure.security.SecurityUtils
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
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
@Tag(name = "Customer Addresses", description = "Manajemen buku alamat pengiriman pelanggan.")
class AddressController(private val customerService: CustomerService) {
    @GetMapping
    @Operation(
        summary = "Daftar alamat saya",
        description = "Mengambil semua alamat pengiriman yang terdaftar untuk akun ini.",
    )
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
    @Operation(summary = "Tambah alamat baru", description = "Menambahkan alamat pengiriman baru ke buku alamat.")
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
    @Operation(summary = "Perbarui alamat", description = "Mengubah detail alamat pengiriman yang sudah ada.")
    fun updateAddress(
        @Parameter(description = "ID unik alamat") @PathVariable addressId: UUID,
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
    @Operation(summary = "Hapus alamat", description = "Menghapus alamat dari buku alamat.")
    fun deleteAddress(
        @Parameter(description = "ID unik alamat") @PathVariable addressId: UUID,
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
