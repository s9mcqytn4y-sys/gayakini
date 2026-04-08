package com.gayakini.common.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicInteger

object BusinessIdGenerator {
    private const val DATE_FORMAT = "yyyyMMdd"
    private const val SEQ_PAD_LENGTH = 4
    private val dateFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT)
    private val lastDate = AtomicInteger(0)
    private val sequence = AtomicInteger(0)

    fun generateOrderNumber(): String {
        return "ORD-${generateTimestampedSequence()}"
    }

    fun generateTransactionNumber(): String {
        return "TRX-${generateTimestampedSequence()}"
    }

    private fun generateTimestampedSequence(): String {
        val now = LocalDateTime.now()
        val dateStr = now.format(dateFormatter)
        val dateInt = dateStr.toInt()

        val currentSeq =
            synchronized(this) {
                if (lastDate.get() != dateInt) {
                    lastDate.set(dateInt)
                    sequence.set(0)
                }
                sequence.incrementAndGet()
            }

        return "$dateStr-${currentSeq.toString().padStart(SEQ_PAD_LENGTH, '0')}"
    }
}
