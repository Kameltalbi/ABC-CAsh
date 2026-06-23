package com.abccash.app.treasury.ui

import com.abccash.app.treasury.data.Product
import com.abccash.app.treasury.data.RevenueCategory

object DocumentCategoryResolver {

    fun resolve(
        lineDrafts: List<LineItemDraft>,
        products: List<Product>,
        fallbackCategory: RevenueCategory = RevenueCategory.OTHER,
        fallbackLabel: String = ""
    ): Pair<RevenueCategory, String> {
        if (!lineDrafts.any { it.selectedProductId != null }) {
            return fallbackCategory to fallbackLabel
        }
        val weighted = lineDrafts.mapNotNull { draft ->
            val productId = draft.selectedProductId ?: return@mapNotNull null
            val product = products.find { it.id == productId } ?: return@mapNotNull null
            val qty = draft.quantity.replace(" ", "").replace(",", ".").toDoubleOrNull()?.coerceAtLeast(0.0) ?: 1.0
            val price = draft.unitPrice.replace(" ", "").replace(",", ".").toDoubleOrNull() ?: return@mapNotNull null
            val amount = qty * price
            if (amount <= 0) return@mapNotNull null
            Triple(product.category, product.categoryLabel, amount)
        }
        if (weighted.isEmpty()) return fallbackCategory to fallbackLabel
        val dominant = weighted.maxBy { it.third }
        return dominant.first to dominant.second
    }
}
