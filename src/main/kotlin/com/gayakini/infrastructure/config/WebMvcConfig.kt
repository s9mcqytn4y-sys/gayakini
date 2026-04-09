package com.gayakini.infrastructure.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.nio.file.Paths

/**
 * Configure public resource handlers for non-sensitive media.
 */
@Configuration
class WebMvcConfig(
    @Value("\${gayakini.storage.local-path:storage}") private val storageRoot: String,
) : WebMvcConfigurer {
    companion object {
        private const val CACHE_PERIOD_ONE_HOUR = 3600
    }

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        val productsRoot = Paths.get(storageRoot, "products").toAbsolutePath().toUri().toString()

        registry.addResourceHandler("/media/products/**")
            .addResourceLocations(productsRoot)
            .setCachePeriod(CACHE_PERIOD_ONE_HOUR)
    }
}
