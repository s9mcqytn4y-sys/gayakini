package com.gayakini.cart.domain

import com.gayakini.catalog.domain.Product
import com.gayakini.catalog.domain.ProductVariant
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.PostLoad
import jakarta.persistence.PostPersist
import jakarta.persistence.Table
import jakarta.persistence.Transient
import org.springframework.data.domain.Persistable
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "carts", schema = "commerce")
class Cart(
    @Id
    private val id: UUID,
    @Column(name = "customer_id")
    var customerId: UUID? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: CartStatus = CartStatus.ACTIVE,
    @Column(name = "currency_code", nullable = false, length = 3)
    val currencyCode: String = "IDR",
    @Column(name = "access_token_hash", unique = true, length = 64)
    var accessTokenHash: String? = null,
    @Column(name = "expires_at")
    var expiresAt: Instant? = null,
    @Column(name = "item_count", nullable = false)
    var itemCount: Int = 0,
    @Column(name = "subtotal_amount", nullable = false)
    var subtotalAmount: Long = 0,
    @OneToMany(mappedBy = "cart", cascade = [CascadeType.ALL], orphanRemoval = true)
    val items: MutableList<CartItem> = mutableListOf(),
    @Column(name = "created_at", updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now(),
) : Persistable<UUID> {
    @Transient
    private var _isNew = true

    override fun getId(): UUID = id

    override fun isNew(): Boolean = _isNew

    @PostPersist
    @PostLoad
    fun markNotNew() {
        _isNew = false
    }
}

enum class CartStatus { ACTIVE, CHECKOUT_IN_PROGRESS, CONVERTED, EXPIRED }

@Entity
@Table(name = "cart_items", schema = "commerce")
class CartItem(
    @Id
    val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    val cart: Cart,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    val product: Product?,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    val variant: ProductVariant,
    @Column(name = "product_title_snapshot", length = 180)
    var productTitleSnapshot: String?,
    @Column(name = "sku_snapshot", length = 64)
    var skuSnapshot: String?,
    @Column(length = 50)
    var color: String?,
    @Column(name = "size_code", length = 20)
    var sizeCode: String?,
    @Column(nullable = false)
    var quantity: Int,
    @Column(name = "unit_price_amount", nullable = false)
    var unitPriceAmount: Long,
    @Column(name = "compare_at_amount")
    var compareAtAmount: Long? = null,
    @Column(name = "primary_image_url", columnDefinition = "TEXT")
    var primaryImageUrl: String? = null,
    @Column(name = "line_total_amount", insertable = false, updatable = false)
    val lineTotalAmount: Long = 0,
    @Column(name = "created_at", updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now(),
)
