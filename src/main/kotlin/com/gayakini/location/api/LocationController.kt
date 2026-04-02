package com.gayakini.location.api

import com.gayakini.common.api.ApiMeta
import com.gayakini.location.domain.LocationAreaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping("/v1/locations")
class LocationController(private val areaRepository: LocationAreaRepository) {

    @GetMapping("/areas")
    fun searchAreas(
        @RequestParam input: String,
        @RequestParam(defaultValue = "10") limit: Int,
    ): LocationAreaListResponse {
        if (input.length < 2) {
            throw IllegalArgumentException("Input pencarian minimal 2 karakter.")
        }

        val areas = areaRepository.searchByLabel(input, PageRequest.of(0, limit))

        return LocationAreaListResponse(
            message = "Area tujuan berhasil ditemukan.",
            data = areas.map { area ->
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
            meta = ApiMeta(requestId = UUID.randomUUID().toString())
        )
    }
}

data class LocationAreaDto(
    val areaId: String,
    val label: String,
    val district: String?,
    val city: String?,
    val province: String?,
    val postalCode: String?,
    val countryCode: String,
)

data class LocationAreaListResponse(
    val message: String,
    val data: List<LocationAreaDto>,
    val meta: ApiMeta? = null
)
