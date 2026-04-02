package com.gayakini

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class GayakiniApplication

fun main(args: Array<String>) {
    runApplication<GayakiniApplication>(*args)
}
