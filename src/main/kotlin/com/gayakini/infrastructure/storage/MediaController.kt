package com.gayakini.infrastructure.storage

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.nio.file.Files

@RestController
@RequestMapping("/v1/media/secure")
@Tag(name = "Media", description = "Akses file internal yang memerlukan autentikasi dan pemeriksaan ownership.")
class MediaController(
    private val storageService: StorageService,
) {
    @GetMapping("/profiles/{filename}")
    @PreAuthorize("hasRole('ADMIN') or @securityService.isOwnerOfProfile(#filename)")
    @Operation(summary = "Unduh foto profil aman", description = "Mengambil file foto profil milik user atau admin.")
    fun getProfilePicture(
        @Parameter(description = "Nama file foto profil yang tersimpan")
        @PathVariable filename: String,
    ): ResponseEntity<Resource> {
        return serveFile(filename, StorageCategory.PROFILES)
    }

    @GetMapping("/proofs/{year}/{month}/{day}/{filename}")
    @PreAuthorize("hasRole('ADMIN') or @securityService.isOwnerOfProof(#year, #month, #day, #filename)")
    @Operation(
        summary = "Unduh bukti pembayaran aman",
        description = "Mengambil file bukti pembayaran berdasarkan struktur penyimpanan tanggal.",
    )
    fun getPaymentProof(
        @Parameter(description = "Tahun penyimpanan bukti") @PathVariable year: String,
        @Parameter(description = "Bulan penyimpanan bukti") @PathVariable month: String,
        @Parameter(description = "Tanggal penyimpanan bukti") @PathVariable day: String,
        @Parameter(description = "Nama file bukti pembayaran")
        @PathVariable filename: String,
    ): ResponseEntity<Resource> {
        val relativePath = "$year/$month/$day/$filename"
        return serveFile(relativePath, StorageCategory.PROOFS)
    }

    private fun serveFile(
        relativePath: String,
        category: StorageCategory,
    ): ResponseEntity<Resource> {
        val path = storageService.loadAsPath(relativePath, category)
        val resource = UrlResource(path.toUri())

        if (!resource.exists() || !resource.isReadable) {
            return ResponseEntity.notFound().build()
        }

        val contentType = Files.probeContentType(path) ?: "application/octet-stream"

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"${path.fileName}\"")
            .body(resource)
    }
}
