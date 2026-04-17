package com.gayakini.operations.api

import com.gayakini.BaseWebMvcTest
import com.gayakini.operations.application.WarehouseService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.util.UUID

@WebMvcTest(OperationsController::class)
@Import(BaseWebMvcTest.SecurityTestConfig::class)
class OperationsControllerWebMvcTest : BaseWebMvcTest() {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var warehouseService: WarehouseService

    @Test
    fun `restockAfterQC should be accessible by OPERATOR`() {
        val orderId = UUID.randomUUID()
        val orderItemId = UUID.randomUUID()

        every {
            warehouseService.processReturnQC(any(), any(), any())
        } returns RestockQCResponse(orderId, orderItemId)

        mockMvc.post("/v1/operations/orders/$orderId/items/$orderItemId/qc-restock") {
            header("Authorization", "Bearer valid-operator-token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"note": "Test restock"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.orderId") { value(orderId.toString()) }
        }
    }

    @Test
    fun `restockAfterQC should be forbidden for CUSTOMER`() {
        val orderId = UUID.randomUUID()
        val orderItemId = UUID.randomUUID()

        mockMvc.post("/v1/operations/orders/$orderId/items/$orderItemId/qc-restock") {
            header("Authorization", "Bearer valid-customer-token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"note": "Test restock"}"""
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `restockAfterQC should be unauthorized for guest`() {
        val orderId = UUID.randomUUID()
        val orderItemId = UUID.randomUUID()

        mockMvc.post("/v1/operations/orders/$orderId/items/$orderItemId/qc-restock") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"note": "Test restock"}"""
        }.andExpect {
            status { isUnauthorized() }
        }
    }
}
