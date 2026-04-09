package com.gayakini.infrastructure.storage

import java.io.InputStream
import java.nio.file.Path

/**
 * Enhanced Storage Service supporting multiple media categories with built-in path normalization.
 */
interface StorageService {
    /**
     * Stores a file in a specific category and returns the relative path.
     * Categories: PRODUCTS, PROFILES, PROOFS, RECEIPTS.
     */
    fun store(
        inputStream: InputStream,
        filename: String,
        category: StorageCategory,
    ): String

    /**
     * Loads a file as an absolute Path for streaming.
     */
    fun loadAsPath(
        relativePath: String,
        category: StorageCategory,
    ): Path

    /**
     * Deletes a file from storage.
     */
    fun delete(
        relativePath: String,
        category: StorageCategory,
    )
}

enum class StorageCategory(val subDir: String) {
    PRODUCTS("products"),
    PROFILES("profiles"),
    PROOFS("proofs"),
    RECEIPTS("receipts"),
}
