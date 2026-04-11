package com.gayakini.promo.api

import com.gayakini.promo.application.PromoService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
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
class AdminPromoController(
    private val promoService: PromoService,
) {
    @GetMapping
    fun getAllPromos() = promoService.getAllPromos()

    @GetMapping("/{id}")
    fun getPromo(
        @PathVariable id: UUID,
    ) = promoService.getPromoById(id)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createPromo(
        @RequestBody @Valid request: CreatePromoRequest,
    ) = promoService.createPromo(request)

    @PutMapping("/{id}")
    fun updatePromo(
        @PathVariable id: UUID,
        @RequestBody @Valid request: UpdatePromoRequest,
    ) = promoService.updatePromo(id, request)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deletePromo(
        @PathVariable id: UUID,
    ) = promoService.deletePromo(id)

    @PostMapping("/{id}/exclusions")
    @ResponseStatus(HttpStatus.CREATED)
    fun addExclusion(
        @PathVariable id: UUID,
        @RequestBody @Valid request: AddExclusionRequest,
    ) = promoService.addExclusion(id, request)

    @DeleteMapping("/{id}/exclusions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removeExclusion(
        @PathVariable id: UUID,
        @RequestBody @Valid request: AddExclusionRequest,
    ) = promoService.removeExclusion(id, request)
}
