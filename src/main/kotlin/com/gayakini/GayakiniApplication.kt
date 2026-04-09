package com.gayakini

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.context.event.EventListener
import org.springframework.core.env.Environment

@SpringBootApplication
@ConfigurationPropertiesScan
class GayakiniApplication(private val env: Environment) {
    private val log = LoggerFactory.getLogger(GayakiniApplication::class.java)

    @EventListener(ApplicationReadyEvent::class)
    fun onReady() {
        val port = env.getProperty("local.server.port") ?: env.getProperty("server.port") ?: "8080"
        val contextPath = env.getProperty("server.servlet.context-path").orEmpty()
        val profiles = env.activeProfiles.joinToString(", ")
        val host = "localhost"

        log.info(
            """
            ----------------------------------------------------------
            Application 'gayakini' is running! Access URLs:
            Local:      http://$host:$port$contextPath
            Profile(s): $profiles
            Swagger UI: http://$host:$port$contextPath/swagger-ui.html
            API Docs:   http://$host:$port$contextPath/api-docs
            ----------------------------------------------------------
            """.trimIndent(),
        )
    }
}

fun main(args: Array<String>) {
    runApplication<GayakiniApplication>(*args)
}
