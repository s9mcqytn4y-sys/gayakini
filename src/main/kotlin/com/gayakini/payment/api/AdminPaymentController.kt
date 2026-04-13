package com.gayakini.payment.api

import com.gayakini.common.api.ApiMeta
import com.gayakini.common.api.StandardResponse
import com.gayakini.payment.application.PaymentService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/v1/admin/payments")
@Tag(name = "Admin Payments", description = "Pengelolaan pembayaran dan bukti transfer (Khusus Admin/Customer).")
class AdminPaymentController(
    private val paymentService: PaymentService,
) {
    @PostMapping("/{paymentId}/proof", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER')")
    @Operation(
        summary = "Unggah bukti pembayaran",
        description = "Mengunggah gambar bukti transfer untuk diverifikasi oleh sistem.",
    )
    fun uploadPaymentProof(
        @Parameter(description = "ID unik pembayaran")
        @PathVariable paymentId: UUID,
        @Parameter(
            description = "File gambar bukti transfer (JPEG/PNG)",
            content = [
                Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = Schema(type = "string", format = "binary"),
                ),
            ],
        )
        @RequestParam("file") file: MultipartFile,
    ): StandardResponse<Map<String, String>> {
        val proofUrl =
            paymentService.uploadPaymentProof(
                paymentId = paymentId,
                inputStream = file.inputStream,
                originalFilename = file.originalFilename ?: "proof.jpg",
            )

        return StandardResponse(
            message = "Bukti pembayaran berhasil diunggah.",
            data = mapOf("proofUrl" to proofUrl),
            meta = ApiMeta(),
        )
    }
}
