package com.gayakini.billing.api

import com.gayakini.common.api.ForbiddenException
import com.gayakini.infrastructure.security.SecurityUtils
import com.gayakini.infrastructure.storage.StorageCategory
import com.gayakini.infrastructure.storage.StorageService
import com.gayakini.order.domain.OrderRepository
import org.slf4j.LoggerFactory
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/v1/orders")
class ReceiptController(
    private val orderRepository: OrderRepository,
    private val storageService: StorageService,
) {
    private val logger = LoggerFactory.getLogger(ReceiptController::class.java)

    @GetMapping("/{orderId}/invoice")
    fun downloadInvoice(
        @PathVariable orderId: UUID,
    ): ResponseEntity<Resource> {
        val order =
            orderRepository.findById(orderId)
                .orElseThrow { NoSuchElementException("Order tidak ditemukan.") }

        val currentUserId = SecurityUtils.getCurrentUserId()
        val isAdmin = SecurityUtils.hasRole("ADMIN")

        // RBAC: Admin can access all, Customer only their own
        if (!isAdmin && order.customerId != currentUserId) {
            throw ForbiddenException("Anda tidak memiliki akses ke invoice ini.")
        }

        val receiptPath =
            order.receiptPath
                ?: throw NoSuchElementException("Invoice belum tersedia untuk order ini.")

        val file = storageService.loadAsPath(receiptPath, StorageCategory.RECEIPTS).toFile()
        val resource = FileSystemResource(file)

        logger.info("Streaming invoice for order: {} (Path: {})", order.orderNumber, receiptPath)

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .contentLength(file.length())
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"Invoice_${order.orderNumber}.pdf\"")
            .body(resource)
    }
}
