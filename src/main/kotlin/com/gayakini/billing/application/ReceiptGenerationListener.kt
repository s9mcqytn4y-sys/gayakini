package com.gayakini.billing.application

import com.gayakini.audit.domain.AuditEvent
import com.gayakini.infrastructure.storage.FileNamingGenerator
import com.gayakini.infrastructure.storage.StorageCategory
import com.gayakini.infrastructure.storage.StorageService
import com.gayakini.order.domain.Order
import com.gayakini.order.domain.OrderRepository
import com.gayakini.payment.domain.PaymentSettledEvent
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Locale

@Component
class ReceiptGenerationListener(
    private val orderRepository: OrderRepository,
    private val templateEngine: TemplateEngine,
    private val storageService: StorageService,
    private val eventPublisher: ApplicationEventPublisher,
) {
    private val logger = LoggerFactory.getLogger(ReceiptGenerationListener::class.java)

    @Async("documentTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Suppress("TooGenericExceptionCaught")
    fun handlePaymentSettled(event: PaymentSettledEvent) {
        logger.info("Starting asynchronous receipt generation for order: {}", event.orderId)

        val order =
            orderRepository.findById(event.orderId).orElse(null) ?: run {
                logger.error("Order not found for receipt generation: {}", event.orderId)
                return
            }

        try {
            val pdfBytes = generatePdf(order)
            val filename = FileNamingGenerator.generateSecureInvoiceName(order.orderNumber)

            ByteArrayInputStream(pdfBytes).use { inputStream ->
                val relativePath =
                    storageService.store(
                        inputStream = inputStream,
                        filename = filename,
                        category = StorageCategory.RECEIPTS,
                    )
                order.receiptPath = relativePath
                orderRepository.save(order)
            }

            logger.info("Receipt generated and stored at: {} for order: {}", order.receiptPath, order.orderNumber)

            // Publish Audit Event
            eventPublisher.publishEvent(
                AuditEvent(
                    actorId = "SYSTEM",
                    actorRole = "SYSTEM",
                    entityType = "ORDER",
                    entityId = order.orderNumber,
                    eventType = "RECEIPT_GENERATED",
                    newState = mapOf("receiptPath" to order.receiptPath),
                    reason = "Professional invoice generated after payment settlement",
                ),
            )
        } catch (e: Exception) {
            // Log specifically instead of generic catch block if possible,
            // but for async listeners, a top-level safety catch is recommended.
            logger.error("Failed to generate receipt for order: {}", order.orderNumber, e)

            eventPublisher.publishEvent(
                AuditEvent(
                    actorId = "SYSTEM",
                    actorRole = "SYSTEM",
                    entityType = "ORDER",
                    entityId = order.orderNumber,
                    eventType = "RECEIPT_GENERATION_FAILED",
                    reason = "Error: ${e.message ?: "Unknown error"}",
                ),
            )
        }
    }

    private fun generatePdf(order: Order): ByteArray {
        val context = Context(Locale("id", "ID"))
        context.setVariable("order", order)

        val htmlContent = templateEngine.process("invoice", context)

        val outputStream = ByteArrayOutputStream()
        val builder = PdfRendererBuilder()
        builder.useFastMode()
        builder.withHtmlContent(htmlContent, null)
        builder.toStream(outputStream)
        builder.run()

        return outputStream.toByteArray()
    }
}
