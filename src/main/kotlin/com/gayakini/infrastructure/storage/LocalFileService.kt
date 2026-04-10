package com.gayakini.infrastructure.storage

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class LocalFileService(
    @Value("\${gayakini.storage.local-path:storage}") private val storageRoot: String,
) : StorageService {
    private val logger = LoggerFactory.getLogger(LocalFileService::class.java)

    override fun store(
        inputStream: InputStream,
        filename: String,
        category: StorageCategory,
    ): String {
        val relativeDir = getRelativeDirectory(category)
        val targetDir = Paths.get(storageRoot, category.subDir, relativeDir)

        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir)
        }

        // Sanitize filename to prevent directory traversal
        val sanitizedFilename = Paths.get(filename).fileName.toString()
        val targetPath = targetDir.resolve(sanitizedFilename)

        Files.newOutputStream(targetPath).use { outputStream ->
            inputStream.copyTo(outputStream)
        }

        logger.info("File stored successfully in {}: {}", category.name, targetPath)

        // Relative path returned for storage in database
        return if (relativeDir.isEmpty()) "$sanitizedFilename" else "$relativeDir/$sanitizedFilename"
    }

    override fun loadAsPath(
        relativePath: String,
        category: StorageCategory,
    ): Path {
        val path = Paths.get(storageRoot, category.subDir, relativePath).normalize()

        // Security check: ensure the path is within the category base directory (Anti-Path Traversal)
        val categoryRoot = Paths.get(storageRoot, category.subDir).toAbsolutePath().normalize()
        val absolutePath = path.toAbsolutePath().normalize()

        if (!absolutePath.startsWith(categoryRoot)) {
            throw SecurityException("Access denied: path is outside of base storage directory.")
        }

        if (!Files.exists(absolutePath)) {
            throw NoSuchElementException("File not found: $relativePath in ${category.name}")
        }

        return absolutePath
    }

    override fun delete(
        relativePath: String,
        category: StorageCategory,
    ) {
        val path = loadAsPath(relativePath, category)
        Files.deleteIfExists(path)
        logger.info("File deleted from {}: {}", category.name, relativePath)
    }

    private fun getRelativeDirectory(category: StorageCategory): String {
        return when (category) {
            StorageCategory.PROOFS, StorageCategory.RECEIPTS -> {
                val today = LocalDate.now()
                today.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
            }
            else -> "" // Flat structure for products and profiles
        }
    }
}
