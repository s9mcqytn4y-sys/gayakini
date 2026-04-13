package com.gayakini.location.api

import com.gayakini.common.api.ApiMeta
import com.gayakini.location.domain.LocationAreaRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/v1/locations")
@Tag(name = "Location", description = "Layanan pencarian lokasi dan area pengiriman.")
class LocationController(private val areaRepository: LocationAreaRepository) {
    companion object {
        private const val MIN_SEARCH_LENGTH = 2
    }

    @GetMapping("/areas")
    @Operation(
        summary = "Cari area pengiriman",
        description = "Mencari area (kecamatan, kota, provinsi) berdasarkan kata kunci.",
    )
    @SecurityRequirements
    fun searchAreas(
        @Parameter(description = "Kata kunci pencarian (min 2 karakter)", example = "Gambir")
        @RequestParam input: String,
        @Parameter(description = "Batas jumlah hasil", example = "10")
        @RequestParam(defaultValue = "10") limit: Int,
    ): LocationAreaListResponse {
        require(input.length >= MIN_SEARCH_LENGTH) { "Input pencarian minimal $MIN_SEARCH_LENGTH karakter." }

        val areas = areaRepository.searchByLabel(input, PageRequest.of(0, limit))

        return LocationAreaListResponse(
            message = "Area tujuan berhasil ditemukan.",
            data =
                areas.map { area ->
                    LocationAreaDto(
                        areaId = area.areaId,
                        label = area.label,
                        district = area.district,
                        city = area.city,
                        province = area.province,
                        postalCode = area.postalCode,
                        countryCode = area.countryCode,
                    )
                },
            meta = ApiMeta(requestId = UUID.randomUUID().toString()),
        )
    }
}

@Schema(description = "Data area lokasi.")
data class LocationAreaDto(
    @Schema(description = "ID area Biteship", example = "6.31.2.1")
    val areaId: String,
    @Schema(description = "Label lengkap lokasi", example = "Gambir, Jakarta Pusat, DKI Jakarta")
    val label: String,
    @Schema(description = "Kecamatan", example = "Gambir")
    val district: String?,
    @Schema(description = "Kota/Kabupaten", example = "Jakarta Pusat")
    val city: String?,
    @Schema(description = "Provinsi", example = "DKI Jakarta")
    val province: String?,
    @Schema(description = "Kode Pos", example = "10110")
    val postalCode: String?,
    @Schema(description = "Kode Negara", example = "ID")
    val countryCode: String,
)

@Schema(description = "Respons daftar area lokasi.")
data class LocationAreaListResponse(
    @Schema(description = "Pesan status", example = "Area tujuan berhasil ditemukan.")
    val message: String,
    val data: List<LocationAreaDto>,
    val meta: ApiMeta? = null,
)
