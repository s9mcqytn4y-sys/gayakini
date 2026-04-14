package com.gayakini.reporting.infrastructure

import com.gayakini.order.domain.Order
import com.gayakini.order.domain.OrderStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface ReportingOrderRepository : JpaRepository<Order, UUID> {
    @Query(
        """
        SELECT SUM(o.subtotalAmount + o.shippingCostAmount - o.discountAmount)
        FROM Order o
        WHERE o.status = :status
        AND o.placedAt >= :startDate AND o.placedAt <= :endDate
    """,
    )
    fun sumTotalSalesByStatus(
        @Param("status") status: OrderStatus,
        @Param("startDate") startDate: Instant,
        @Param("endDate") endDate: Instant,
    ): Long?

    @Query(
        """
        SELECT COUNT(o)
        FROM Order o
        WHERE o.placedAt >= :startDate AND o.placedAt <= :endDate
    """,
    )
    fun countOrdersInPeriod(
        @Param("startDate") startDate: Instant,
        @Param("endDate") endDate: Instant,
    ): Long

    @Query(
        """
        SELECT CAST(o.placedAt AS date) as date,
               SUM(o.subtotalAmount + o.shippingCostAmount - o.discountAmount) as totalAmount,
               COUNT(o) as orderCount
        FROM Order o
        WHERE o.placedAt >= :startDate AND o.placedAt <= :endDate
        GROUP BY CAST(o.placedAt AS date)
        ORDER BY CAST(o.placedAt AS date) ASC
    """,
    )
    fun getSalesTrend(
        @Param("startDate") startDate: Instant,
        @Param("endDate") endDate: Instant,
    ): List<Array<Any>>

    @Query(
        """
        SELECT oi.product.id as productId,
               oi.titleSnapshot as productTitle,
               SUM(oi.quantity) as unitsSold,
               SUM(oi.unitPriceAmount * oi.quantity) as revenue
        FROM OrderItem oi
        JOIN oi.order o
        WHERE o.status = :status
        AND o.placedAt >= :startDate AND o.placedAt <= :endDate
        GROUP BY oi.product.id, oi.titleSnapshot
        ORDER BY unitsSold DESC
    """,
    )
    fun findBestSellers(
        @Param("status") status: OrderStatus,
        @Param("startDate") startDate: Instant,
        @Param("endDate") endDate: Instant,
    ): List<Array<Any>>
}
