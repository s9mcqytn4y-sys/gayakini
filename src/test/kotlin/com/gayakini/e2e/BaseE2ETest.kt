package com.gayakini.e2e

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
abstract class BaseE2ETest {

    @Autowired
    protected lateinit var restTemplate: TestRestTemplate

    @Autowired
    protected lateinit var jdbcTemplate: JdbcTemplate

    protected fun cleanupDatabase() {
        // Full order of truncation to avoid foreign key issues even with integrity off
        val tables = listOf(
            "commerce.order_items",
            "commerce.payments",
            "commerce.orders",
            "commerce.checkouts",
            "commerce.cart_items",
            "commerce.carts",
            "commerce.customer_addresses",
            "commerce.customers",
            "commerce.product_media",
            "commerce.product_variants",
            "commerce.product_collections",
            "commerce.products",
            "commerce.categories",
            "commerce.collections"
        )

        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE")
        tables.forEach { table ->
            try {
                jdbcTemplate.execute("TRUNCATE TABLE $table")
            } catch (e: Exception) {
                // Ignore if table doesn't exist in the current test environment (e.g. H2 vs Postgres differences)
            }
        }
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE")
    }
}
