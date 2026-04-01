package com.gayakini.catalog.application

import com.gayakini.catalog.domain.Product
import com.gayakini.catalog.domain.ProductRepository
import com.gayakini.catalog.domain.ProductVariant
import com.gayakini.catalog.domain.ProductVariantRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val productVariantRepository: ProductVariantRepository,
) {
    fun listProducts(): List<Product> {
        return productRepository.findAll()
    }

    fun getProduct(id: UUID): Product {
        return productRepository.findById(id)
            .orElseThrow { NoSuchElementException("Produk tidak ditemukan.") }
    }

    @Transactional
    fun reserveStock(
        variantId: UUID,
        quantity: Int,
    ): ProductVariant {
        val variant =
            productVariantRepository.findWithLockById(variantId)
                .orElseThrow { NoSuchElementException("Varian produk tidak ditemukan.") }

        if (variant.stockAvailable < quantity) {
            throw IllegalStateException("Stok tidak mencukupi untuk produk: \${variant.sku}")
        }

        variant.stockReserved += quantity
        return productVariantRepository.save(variant)
    }
}
