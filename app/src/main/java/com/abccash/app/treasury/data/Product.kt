package com.abccash.app.treasury.data

import java.time.LocalDate
import java.util.UUID

enum class ProductKind {
    PRODUCT,
    SERVICE
}

enum class ProductUnit {
    PIECE,
    HOUR,
    DAY,
    FLAT
}

data class Product(
    val id: String = UUID.randomUUID().toString(),
    val entrepriseId: String = "",
    val name: String,
    val unitPriceExclTax: Double,
    val kind: ProductKind = ProductKind.SERVICE,
    val unit: ProductUnit = ProductUnit.PIECE,
    val category: RevenueCategory = RevenueCategory.OTHER,
    val categoryLabel: String = "",
    val isActive: Boolean = true,
    val createdDate: LocalDate = LocalDate.now()
)
