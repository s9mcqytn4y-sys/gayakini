package com.gayakini.common.util

import com.github.f4b6a3.uuid.UuidCreator
import java.util.UUID

object UuidV7Generator {
    fun generate(): UUID = UuidCreator.getTimeOrderedEpoch()
}
