package com.gayakini.promo.api

import com.gayakini.common.api.ApiResponse
import com.gayakini.promo.application.PromoService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/v1/admin/promos")
@Tag(
    name = "Admin Promo",
    description = "Promotion and coupon management for administrators (Internal/English).",
)
@PreAuthorize("hasRole('ADMIN')")
class AdminPromoController(
    private val promoService: PromoService,
) {
    @GetMapping
    @Operation(summary = "Get all promos", description = "Retrieve a list of all promotions.")
    fun getAllPromos(): ApiResponse<List<PromoResponse>> {
        val promos = promoService.getAllPromos()
        return ApiResponse(
            message = "All promos retrieved successfully.",
            data = promos,
        )
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get promo details",
        description = "Retrieve detailed information about a specific promotion by ID.",
    )
    fun getPromo(
        @Parameter(description = "UUID of the promo")
        @PathVariable id: UUID,
    ): ApiResponse<PromoResponse> {
        val promo = promoService.getPromoById(id)
        return ApiResponse(
            message = "Promo details retrieved successfully.",
            data = promo,
        )
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create new promo", description = "Add a new promotion with specified terms and conditions.")
    fun createPromo(
        @RequestBody @Valid request: CreatePromoRequest,
    ): ApiResponse<PromoResponse> {
        val promo = promoService.createPromo(request)
        return ApiResponse(
            message = "Promo created successfully.",
            data = promo,
        )
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update promo", description = "Modify details of an existing promotion.")
    fun updatePromo(
        @Parameter(description = "UUID of the promo")
        @PathVariable id: UUID,
        @RequestBody @Valid request: UpdatePromoRequest,
    ): ApiResponse<PromoResponse> {
        val promo = promoService.updatePromo(id, request)
        return ApiResponse(
            message = "Promo updated successfully.",
            data = promo,
        )
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete promo", description = "Remove a promotion permanently.")
    fun deletePromo(
        @Parameter(description = "UUID of the promo")
        @PathVariable id: UUID,
    ) {
        promoService.deletePromo(id)
    }

    @PostMapping("/{id}/exclusions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Add promo exclusion",
        description = "Exclude specific products or categories from a promotion.",
    )
    fun addExclusion(
        @Parameter(description = "UUID of the promo")
        @PathVariable id: UUID,
        @RequestBody @Valid request: AddExclusionRequest,
    ): ApiResponse<Unit> {
        promoService.addExclusion(id, request)
        return ApiResponse(
            message = "Promo exclusion added successfully.",
        )
    }

    @DeleteMapping("/{id}/exclusions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Remove promo exclusion",
        description = "Remove a previously added exclusion from a promotion.",
    )
    fun removeExclusion(
        @Parameter(description = "UUID of the promo")
        @PathVariable id: UUID,
        @RequestBody @Valid request: AddExclusionRequest,
    ) {
        promoService.removeExclusion(id, request)
    }
}
