package com.gayakini.common.util

import com.github.f4b6a3.uuid.UuidCreator
import java.util.UUID

/**
 * Centralized UUID generation policy for the Gayakini application.
 *
 * This utility enforces the use of UUIDv7 (time-ordered, epoch-based) for all
 * primary keys and operational identifiers generated within the application.
 *
 * Benefits:
 * 1. Database friendly: Sequentially increasing values reduce B-Tree fragmentation in PostgreSQL.
 * 2. Time-sortable: Embedded millisecond timestamp allows sorting by ID.
 * 3. Collision-resistant: High entropy for distributed systems.
 */
object UuidV7Generator {
    /**
     * Generates a new UUIDv7.
     */
    fun generate(): UUID = UuidCreator.getTimeOrderedEpoch()

    /**
     * Helper to generate UUIDv7 as a String.
     */
    fun generateString(): String = generate().toString()
}
