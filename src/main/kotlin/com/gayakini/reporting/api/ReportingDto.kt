package com.gayakini.reporting.api

import com.gayakini.common.api.MoneyDto
import java.time.LocalDate

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

data class ProductPerformanceDto(
    val productId: java.util.UUID,
    val productTitle: String,
    val unitsSold: Int,
    val revenue: MoneyDto,
)
