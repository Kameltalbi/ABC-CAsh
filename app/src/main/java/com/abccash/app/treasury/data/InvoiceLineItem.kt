package com.abccash.app.treasury.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class InvoiceLineItem(
    val id: String = UUID.randomUUID().toString(),
    val description: String,
    val quantity: Double = 1.0,
    val unitPriceExclTax: Double,
    val productId: String? = null
) {
    val lineTotalExclTax: Double
        get() = (quantity.coerceAtLeast(0.0) * unitPriceExclTax.coerceAtLeast(0.0))
}

object InvoiceLineItemCodec {

    fun encode(items: List<InvoiceLineItem>): String {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("description", item.description)
                    .put("quantity", item.quantity)
                    .put("unitPriceExclTax", item.unitPriceExclTax)
                    .also { obj ->
                        item.productId?.let { obj.put("productId", it) }
                    }
            )
        }
        return array.toString()
    }

    fun decode(json: String?): List<InvoiceLineItem> {
        if (json.isNullOrBlank() || json == "[]") return emptyList()
        return runCatching {
            val array = JSONArray(json)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(
                        InvoiceLineItem(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            description = obj.optString("description", ""),
                            quantity = obj.optDouble("quantity", 1.0),
                            unitPriceExclTax = obj.optDouble("unitPriceExclTax", 0.0),
                            productId = obj.optString("productId", "").takeIf { it.isNotBlank() }
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun totalExclTax(items: List<InvoiceLineItem>): Double =
        items.sumOf { it.lineTotalExclTax }
}
