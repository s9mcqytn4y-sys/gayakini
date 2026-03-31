package com.gayakini.inventory.application

import com.gayakini.catalog.domain.ProductVariantRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class InventoryService(
    private val productVariantRepository: ProductVariantRepository
) {

    @Transactional
    fun lockAndDecreaseStock(variantId: UUID, quantity: Int) {
        val variant = productVariantRepository.findWithLockById(variantId)
            .orElseThrow { NoSuchElementException("Varian produk dengan ID \$variantId tidak ditemukan.") }

        if (variant.stock < quantity) {
            throw IllegalStateException("Stok tidak mencukupi untuk varian \${variant.name}. Stok tersedia: \${variant.stock}")
        }

        variant.stock -= quantity
        productVariantRepository.save(variant)
    }

    @Transactional
    fun releaseStock(variantId: UUID, quantity: Int) {
        val variant = productVariantRepository.findWithLockById(variantId)
            .orElseThrow { NoSuchElementException("Varian produk dengan ID \$variantId tidak ditemukan.") }

        variant.stock += quantity
        productVariantRepository.save(variant)
    }
}
