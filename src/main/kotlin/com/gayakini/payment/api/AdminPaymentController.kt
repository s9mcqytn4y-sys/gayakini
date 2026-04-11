package com.gayakini.payment.api

import com.gayakini.common.api.ApiMeta
import com.gayakini.common.api.StandardResponse
import com.gayakini.payment.application.PaymentService
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
class AdminPaymentController(
    private val paymentService: PaymentService,
) {
    @PostMapping("/{paymentId}/proof")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER')")
    fun uploadPaymentProof(
        @PathVariable paymentId: UUID,
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
