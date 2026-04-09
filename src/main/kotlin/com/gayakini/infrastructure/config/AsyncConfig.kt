package com.gayakini.infrastructure.config

import org.slf4j.LoggerFactory
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.AsyncConfigurer
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Configuration
@EnableAsync
class AsyncConfig : AsyncConfigurer {
    private val logger = LoggerFactory.getLogger(AsyncConfig::class.java)

    companion object {
        private const val CORE_POOL_SIZE = 2
        private const val MAX_POOL_SIZE = 5
        private const val QUEUE_CAPACITY = 50
    }

    @Bean(name = ["documentTaskExecutor"])
    fun documentTaskExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = CORE_POOL_SIZE
        executor.maxPoolSize = MAX_POOL_SIZE
        executor.queueCapacity = QUEUE_CAPACITY
        executor.setThreadNamePrefix("DocGen-")
        executor.initialize()
        return executor
    }

    override fun getAsyncUncaughtExceptionHandler(): AsyncUncaughtExceptionHandler {
        return AsyncUncaughtExceptionHandler { ex, method, params ->
            logger.error("Unexpected error occurred in async method: ${method.name} with params: $params", ex)
            // In a real scenario, we would publish a failure event to the Audit system here
            // via ApplicationEventPublisher if we had access to it, or via a specialized service.
        }
    }
}
