package com.gayakini.catalog.application

import com.gayakini.catalog.domain.ProductVariant
import com.gayakini.catalog.domain.ProductVariantRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ProductService(
    private val productVariantRepository: ProductVariantRepository
) {

    @Transactional
    fun reserveStock(variantId: UUID, quantity: Int): ProductVariant {
        val variant = productVariantRepository.findWithLockById(variantId)
            .orElseThrow { NoSuchElementException("Varian produk tidak ditemukan.") }

        if (variant.stock < quantity) {
            throw IllegalStateException("Stok tidak mencukupi untuk produk: \${variant.name}")
        }

        variant.stock -= quantity
        return productVariantRepository.save(variant)
    }
}
