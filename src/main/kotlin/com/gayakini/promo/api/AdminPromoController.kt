package com.gayakini.promo.api

import com.gayakini.promo.application.PromoService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
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
    fun getAllPromos() = promoService.getAllPromos()

    @GetMapping("/{id}")
    @Operation(
        summary = "Get promo details",
        description = "Retrieve detailed information about a specific promotion by ID.",
    )
    fun getPromo(
        @Parameter(description = "UUID of the promo")
        @PathVariable id: UUID,
    ) = promoService.getPromoById(id)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create new promo", description = "Add a new promotion with specified terms and conditions.")
    fun createPromo(
        @RequestBody @Valid request: CreatePromoRequest,
    ) = promoService.createPromo(request)

    @PutMapping("/{id}")
    @Operation(summary = "Update promo", description = "Modify details of an existing promotion.")
    fun updatePromo(
        @Parameter(description = "UUID of the promo")
        @PathVariable id: UUID,
        @RequestBody @Valid request: UpdatePromoRequest,
    ) = promoService.updatePromo(id, request)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete promo", description = "Remove a promotion permanently.")
    fun deletePromo(
        @Parameter(description = "UUID of the promo")
        @PathVariable id: UUID,
    ) = promoService.deletePromo(id)

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
    ) = promoService.addExclusion(id, request)

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
    ) = promoService.removeExclusion(id, request)
}
