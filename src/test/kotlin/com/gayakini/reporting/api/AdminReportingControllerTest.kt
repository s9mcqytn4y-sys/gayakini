package com.gayakini.reporting.api

import com.gayakini.BaseWebMvcTest
import com.gayakini.common.api.MoneyDto
import com.gayakini.reporting.application.ReportingService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.time.LocalDate

@WebMvcTest(AdminReportingController::class)
@Import(BaseWebMvcTest.SecurityTestConfig::class)
class AdminReportingControllerTest : BaseWebMvcTest() {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var reportingService: ReportingService

    @org.springframework.boot.test.context.TestConfiguration
    class ControllerTestConfig {
        @org.springframework.context.annotation.Bean
        fun reportingService(): ReportingService = mockk()
    }

    @Test
    fun `getSummary should return 200 for admin`() {
        every { reportingService.getDashboardSummary() } returns
            DashboardSummaryDto(
                totalSales = MoneyDto(amount = 1000000L),
                orderCount = 10,
                activeCustomers = 5,
                lowStockAlerts = 2,
                salesTrend = listOf(SalesDataPointDto(LocalDate.now(), 1000000L, 10)),
            )

        mockMvc.get("/v1/admin/reports/summary") {
            header("Authorization", "Bearer valid-admin-token")
        }.andExpectStandardResponse(200)
            .andExpect {
                jsonPath("$.data.orderCount") { value(10) }
            }
    }

    @Test
    fun `getBestSellers should return 200 for admin`() {
        every { reportingService.getBestSellers(any()) } returns emptyList()

        mockMvc.get("/v1/admin/reports/best-sellers") {
            header("Authorization", "Bearer valid-admin-token")
            param("limit", "5")
        }.andExpectStandardResponse(200)
            .andExpect {
                jsonPath("$.data") { isArray() }
            }
    }

    @Test
    fun `reports should be forbidden for customers`() {
        mockMvc.get("/v1/admin/reports/summary") {
            header("Authorization", "Bearer valid-customer-token")
        }.andExpect {
            status { isForbidden() }
        }
    }
}
