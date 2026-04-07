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
        // H2 doesn't support TRUNCATE CASCADE in the same way as Postgres.
        // We'll use a manual order and SET REFERENTIAL_INTEGRITY FALSE if needed,
        // but manual order of dependent tables is safer.
        val tables =
            listOf(
                "commerce.order_items",
                "commerce.payments",
                "commerce.orders",
                "commerce.checkouts",
                "commerce.cart_items",
                "commerce.carts",
                "commerce.customer_addresses",
                "commerce.customers",
            )

        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE")
        tables.forEach { table ->
            jdbcTemplate.execute("TRUNCATE TABLE $table")
        }
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE")
    }
}
