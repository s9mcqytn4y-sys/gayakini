package com.gayakini.infrastructure.storage

import java.io.InputStream
import java.nio.file.Path

interface StorageService {
    /**
     * Stores a file and returns the relative path.
     * The implementation should handle directory chunking (e.g., YYYY/MM/DD).
     */
    fun store(
        inputStream: InputStream,
        filename: String,
    ): String

    /**
     * Loads a file as a Resource or Path for streaming.
     */
    fun loadAsPath(relativePath: String): Path

    /**
     * Deletes a file.
     */
    fun delete(relativePath: String)
}
