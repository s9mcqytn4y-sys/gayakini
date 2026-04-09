package com.gayakini.infrastructure.storage

import org.apache.tika.Tika
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.io.InputStream

@Component
class FileValidator {
    private val tika = Tika()

    private val allowedImageTypes = setOf("image/jpeg", "image/png", "image/webp")
    private val allowedDocumentTypes = setOf("image/jpeg", "image/png", "application/pdf")

    /**
     * Validates file signature (MIME type) using Apache Tika.
     * Prevents MIME-type spoofing and malware renaming.
     */
    fun validate(
        inputStream: InputStream,
        category: StorageCategory,
    ): String {
        val detectedType = tika.detect(inputStream)

        val allowed =
            when (category) {
                StorageCategory.PRODUCTS, StorageCategory.PROFILES -> allowedImageTypes
                StorageCategory.PROOFS, StorageCategory.RECEIPTS -> allowedDocumentTypes
            }

        require(allowed.contains(detectedType)) {
            "Unsupported media type: $detectedType for category: ${category.name}"
        }

        return detectedType
    }

    /**
     * Validates Spring MultipartFile with Tika.
     */
    fun validate(
        file: MultipartFile,
        category: StorageCategory,
    ): String {
        require(!file.isEmpty) { "Uploaded file is empty" }

        file.inputStream.use {
            return validate(it, category)
        }
    }
}
