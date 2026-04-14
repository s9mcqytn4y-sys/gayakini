package com.gayakini.reporting.api

import com.gayakini.reporting.application.ReportingService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/v1/admin/reports")
@Tag(name = "Admin Reports", description = "Analytical reports for store administrators.")
@PreAuthorize("hasRole('ADMIN')")
class AdminReportingController(private val reportingService: ReportingService) {
    @GetMapping("/summary")
    @Operation(summary = "Get dashboard summary", description = "Top-level KPI metrics.")
    fun getSummary(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate?,
    ): DashboardSummaryResponse {
        val summary =
            if (startDate != null && endDate != null) {
                reportingService.getDashboardSummary(startDate, endDate)
            } else {
                reportingService.getDashboardSummary()
            }
        return DashboardSummaryResponse(data = summary)
    }

    @GetMapping("/best-sellers")
    @Operation(summary = "Get best selling products", description = "Performance analytics for products.")
    fun getBestSellers(
        @RequestParam(defaultValue = "10") limit: Int,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate?,
    ): BestSellersResponse {
        val bestSellers =
            if (startDate != null && endDate != null) {
                reportingService.getBestSellers(limit, startDate, endDate)
            } else {
                reportingService.getBestSellers(limit)
            }
        return BestSellersResponse(data = bestSellers)
    }
}
