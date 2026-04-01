package com.gayakini.location.api

import com.gayakini.common.api.ApiResponse
import com.gayakini.location.domain.LocationAreaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/v1/locations")
class LocationController(private val areaRepository: LocationAreaRepository) {

    @GetMapping("/areas")
    fun searchAreas(
        @RequestParam input: String,
        @RequestParam(defaultValue = "10") limit: Int
    ): ApiResponse<List<LocationAreaResponse>> {
        if (input.length < 2) {
            throw IllegalArgumentException("Input pencarian minimal 2 karakter.")
        }
        
        val areas = areaRepository.searchByLabel(input, PageRequest.of(0, limit))
        
        return ApiResponse.success(
            message = "Area tujuan berhasil ditemukan.",
            data = areas.map { area ->
                LocationAreaResponse(
                    areaId = area.areaId,
                    label = area.label,
                    district = area.district,
                    city = area.city,
                    province = area.province,
                    postalCode = area.postalCode,
                    countryCode = area.countryCode
                )
            }
        )
    }
}

data class LocationAreaResponse(
    val areaId: String,
    val label: String,
    val district: String?,
    val city: String?,
    val province: String?,
    val postalCode: String?,
    val countryCode: String
)
