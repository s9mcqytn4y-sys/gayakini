package com.gayakini.reporting.application

import com.gayakini.common.api.MoneyDto
import com.gayakini.customer.domain.CustomerRepository
import com.gayakini.catalog.domain.ProductVariantRepository
import com.gayakini.order.domain.OrderStatus
import com.gayakini.reporting.api.DashboardSummaryDto
import com.gayakini.reporting.api.ProductPerformanceDto
import com.gayakini.reporting.api.SalesDataPointDto
import com.gayakini.reporting.infrastructure.ReportingOrderRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

@Service
class ReportingService(
    private val orderRepository: ReportingOrderRepository,
    private val customerRepository: CustomerRepository,
    private val productVariantRepository: ProductVariantRepository,
) {
    companion object {
        private const val DEFAULT_DAYS_FOR_DASHBOARD = 30L
        private const val DEFAULT_LOW_STOCK_THRESHOLD = 10
    }

    fun getDashboardSummary(
        startDate: LocalDate = LocalDate.now().minusDays(DEFAULT_DAYS_FOR_DASHBOARD),
        endDate: LocalDate = LocalDate.now(),
    ): DashboardSummaryDto {
        val startInstant = startDate.atStartOfDay().toInstant(ZoneOffset.UTC)
        val endInstant = endDate.atTime(23, 59, 59).toInstant(ZoneOffset.UTC)

        val totalSales = orderRepository.sumTotalSalesByStatus(OrderStatus.COMPLETED, startInstant, endInstant) ?: 0L
        val orderCount = orderRepository.countOrdersInPeriod(startInstant, endInstant)
        val activeCustomers = customerRepository.count()
        val lowStockAlerts = productVariantRepository.countByStockOnHandLessThan(DEFAULT_LOW_STOCK_THRESHOLD)

        val salesTrend =
            orderRepository.getSalesTrend(startInstant, endInstant).map {
                SalesDataPointDto(
                    date = (it[0] as java.sql.Date).toLocalDate(),
                    totalAmount = it[1] as Long,
                    orderCount = (it[2] as Long).toInt(),
                )
            }

        return DashboardSummaryDto(
            totalSales = MoneyDto(amount = totalSales),
            orderCount = orderCount,
            activeCustomers = activeCustomers,
            lowStockAlerts = lowStockAlerts,
            salesTrend = salesTrend,
        )
    }

    fun getBestSellers(
        limit: Int,
        startDate: LocalDate = LocalDate.now().minusDays(DEFAULT_DAYS_FOR_DASHBOARD),
        endDate: LocalDate = LocalDate.now(),
    ): List<ProductPerformanceDto> {
        val startInstant = startDate.atStartOfDay().toInstant(ZoneOffset.UTC)
        val endInstant = endDate.atTime(23, 59, 59).toInstant(ZoneOffset.UTC)

        return orderRepository.findBestSellers(OrderStatus.COMPLETED, startInstant, endInstant)
            .take(limit)
            .map {
                ProductPerformanceDto(
                    productId = it[0] as UUID,
                    productTitle = it[1] as String,
                    unitsSold = (it[2] as Long).toInt(),
                    revenue = MoneyDto(amount = it[3] as Long),
                )
            }
    }
}
