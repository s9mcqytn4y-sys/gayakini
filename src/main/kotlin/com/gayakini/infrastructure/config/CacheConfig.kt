package com.gayakini.infrastructure.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

@Configuration
@EnableCaching
@Suppress("MagicNumber")
class CacheConfig {
    companion object {
        private const val INITIAL_CAPACITY = 100
        private const val MAXIMUM_SIZE = 500L
        private const val EXPIRE_MINUTES = 10L
    }

    @Bean
    fun cacheManager(): CacheManager {
        val cacheManager = CaffeineCacheManager()

        // Default fallback cache config
        cacheManager.setCaffeine(
            Caffeine.newBuilder()
                .initialCapacity(INITIAL_CAPACITY)
                .maximumSize(MAXIMUM_SIZE)
                .expireAfterWrite(EXPIRE_MINUTES, TimeUnit.MINUTES)
                .recordStats(),
        )

        // Named Caches
        cacheManager.registerCustomCache(
            "locations",
            Caffeine.newBuilder()
                .initialCapacity(50)
                .maximumSize(1000)
                .expireAfterWrite(24L, TimeUnit.HOURS)
                .recordStats()
                .build(),
        )

        cacheManager.registerCustomCache(
            "promos",
            Caffeine.newBuilder()
                .initialCapacity(10)
                .maximumSize(100)
                .expireAfterWrite(5L, TimeUnit.MINUTES)
                .recordStats()
                .build(),
        )

        cacheManager.registerCustomCache(
            "products",
            Caffeine.newBuilder()
                .initialCapacity(100)
                .maximumSize(2000)
                .expireAfterWrite(15L, TimeUnit.MINUTES)
                .recordStats()
                .build(),
        )

        return cacheManager
    }
}
