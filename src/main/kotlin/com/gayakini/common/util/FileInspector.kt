package com.gayakini.common.util

import org.apache.tika.Tika
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.InputStream

/**
 * Centralized binary file inspection policy for Gayakini.
 *
 * Uses Apache Tika to determine MIME types based on the actual binary content
 * (magic bytes) rather than relying on the file extension or the "Content-Type"
 * header provided by the client.
 *
 * All uploads in the application (receipts, product images, profile photos)
 * MUST be passed through this inspector for security.
 */
@Component
class FileInspector {
    private val logger = LoggerFactory.getLogger(FileInspector::class.java)
    private val tika = Tika()

    /**
     * Inspects an input stream to determine its real MIME type.
     * Note: This method reads the first few bytes (magic bytes).
     */
    fun detectMimeType(
        inputStream: InputStream,
        filename: String? = null,
    ): String {
        return try {
            val detected = tika.detect(inputStream, filename)
            logger.debug("Detected MIME type for [{}]: {}", filename ?: "stream", detected)
            detected
        } catch (e: java.io.IOException) {
            logger.error("Failed to detect MIME type due to I/O error for [{}]: {}", filename ?: "stream", e.message)
            "application/octet-stream"
        }
    }

    /**
     * Validates if the detected MIME type is in the allowed list for the given category.
     */
    fun isValid(
        mimeType: String,
        category: FileCategory,
    ): Boolean {
        return category.allowedMimeTypes.contains(mimeType)
    }

    /**
     * Predefined file categories for standardized validation.
     */
    enum class FileCategory(val allowedMimeTypes: Set<String>) {
        IMAGE(setOf("image/jpeg", "image/png", "image/webp")),
        DOCUMENT(setOf("application/pdf", "image/jpeg", "image/png")),
        RECEIPT(setOf("application/pdf", "image/jpeg", "image/png", "image/webp")),
        PRODUCT(setOf("image/jpeg", "image/png", "image/webp")),
    }
}
