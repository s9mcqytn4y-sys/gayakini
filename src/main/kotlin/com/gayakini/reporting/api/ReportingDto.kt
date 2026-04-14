package com.gayakini.reporting.api

import com.gayakini.common.api.ApiMeta
import com.gayakini.common.api.MoneyDto
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "Summary dashboard metrics.")
data class DashboardSummaryResponse(
    val success: Boolean = true,
    val message: String = "Dashboard summary retrieved.",
    val data: DashboardSummaryDto,
    val meta: ApiMeta? = null,
)

data class DashboardSummaryDto(
    val totalSales: MoneyDto,
    val orderCount: Long,
    val activeCustomers: Long,
    val lowStockAlerts: Int,
    val salesTrend: List<SalesDataPointDto>,
)

data class SalesDataPointDto(
    val date: LocalDate,
    val totalAmount: Long,
    val orderCount: Int,
)

@Schema(description = "Best selling products report.")
data class BestSellersResponse(
    val success: Boolean = true,
    val message: String = "Best sellers retrieved.",
    val data: List<ProductPerformanceDto>,
    val meta: ApiMeta? = null,
)

data class ProductPerformanceDto(
    val productId: java.util.UUID,
    val productTitle: String,
    val unitsSold: Int,
    val revenue: MoneyDto,
)
