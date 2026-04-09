package com.gayakini.infrastructure.storage

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.BufferedOutputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class LocalFileService(
    @Value("\${gayakini.storage.local-path:storage}") private val basePath: String,
) : StorageService {
    private val logger = LoggerFactory.getLogger(LocalFileService::class.java)

    override fun store(
        inputStream: InputStream,
        filename: String,
    ): String {
        val today = LocalDate.now()
        val relativeDir = today.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
        val targetDir = Paths.get(basePath, relativeDir)

        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir)
        }

        // Sanitize filename to prevent directory traversal
        val sanitizedFilename = Paths.get(filename).fileName.toString()
        val targetPath = targetDir.resolve(sanitizedFilename)

        Files.newOutputStream(targetPath).use { outputStream ->
            BufferedOutputStream(outputStream).use { bufferedOutput ->
                inputStream.copyTo(bufferedOutput)
            }
        }

        logger.info("File stored successfully at: {}", targetPath)
        return "$relativeDir/$sanitizedFilename"
    }

    override fun loadAsPath(relativePath: String): Path {
        val path = Paths.get(basePath, relativePath).normalize()

        // Security check: ensure the path is within the base storage directory
        val absoluteBasePath = Paths.get(basePath).toAbsolutePath().normalize()
        val absolutePath = path.toAbsolutePath().normalize()

        if (!absolutePath.startsWith(absoluteBasePath)) {
            throw SecurityException("Access denied: path is outside of base storage directory.")
        }

        if (!Files.exists(absolutePath)) {
            throw NoSuchElementException("File not found: $relativePath")
        }

        return absolutePath
    }

    override fun delete(relativePath: String) {
        val path = loadAsPath(relativePath)
        Files.deleteIfExists(path)
        logger.info("File deleted: {}", relativePath)
    }
}
