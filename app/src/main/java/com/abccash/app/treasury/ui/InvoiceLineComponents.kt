package com.abccash.app.treasury.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abccash.app.R
import com.abccash.app.treasury.data.InvoiceLineItem
import com.abccash.app.treasury.data.Product
import java.util.UUID

data class LineItemDraft(
    val id: String = UUID.randomUUID().toString(),
    var description: String = "",
    var quantity: String = "1",
    var unitPrice: String = "",
    var selectedProductId: String? = null
) {
    fun toLineItemOrNull(): InvoiceLineItem? {
        val qty = quantity.replace(" ", "").replace(",", ".").toDoubleOrNull()?.coerceAtLeast(0.0) ?: 1.0
        val price = unitPrice.replace(" ", "").replace(",", ".").toDoubleOrNull() ?: return null
        if (description.isBlank() || price <= 0) return null
        return InvoiceLineItem(
            id = id,
            description = description.trim(),
            quantity = qty,
            unitPriceExclTax = price,
            productId = selectedProductId
        )
    }
}

fun InvoiceLineItem.toLineItemDraft(): LineItemDraft {
    val qtyText = if (quantity == quantity.toLong().toDouble()) {
        quantity.toLong().toString()
    } else {
        quantity.toString()
    }
    val priceText = unitPriceExclTax.toString().replace('.', ',')
    return LineItemDraft(
        id = id,
        description = description,
        quantity = qtyText,
        unitPrice = priceText,
        selectedProductId = productId
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceLineItemCard(
    draft: LineItemDraft,
    products: List<Product>,
    formatAmount: (Double) -> String,
    canDelete: Boolean,
    onDescriptionChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onUnitPriceChange: (String) -> Unit,
    onProductSelected: (Product?) -> Unit,
    onAddProduct: () -> Unit,
    onDelete: () -> Unit
) {
    val lineTotal = draft.toLineItemOrNull()?.lineTotalExclTax
    val activeProducts = remember(products) { products.filter { it.isActive } }
    var showProductMenu by remember { mutableStateOf(false) }
    val selectedProduct = activeProducts.find { it.id == draft.selectedProductId }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.invoice_line_product_service),
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = Color(0xFF64748B)
                )
                if (canDelete) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = Color(0xFFDC2626)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExposedDropdownMenuBox(
                    expanded = showProductMenu,
                    onExpandedChange = { showProductMenu = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedProduct?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.product_select)) },
                        placeholder = { Text(stringResource(R.string.product_select_hint)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showProductMenu) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = showProductMenu,
                        onDismissRequest = { showProductMenu = false }
                    ) {
                        if (activeProducts.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.products_empty)) },
                                onClick = {},
                                enabled = false
                            )
                        } else {
                            activeProducts.forEach { product ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(product.name, fontWeight = FontWeight.Medium)
                                            Text(
                                                formatAmount(product.unitPriceExclTax),
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    },
                                    onClick = {
                                        onProductSelected(product)
                                        showProductMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
                FilledIconButton(
                    onClick = onAddProduct,
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.product_add))
                }
            }

            OutlinedTextField(
                value = draft.description,
                onValueChange = onDescriptionChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.invoice_line_description)) },
                placeholder = { Text(stringResource(R.string.invoice_line_description_hint)) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = draft.quantity,
                    onValueChange = onQuantityChange,
                    modifier = Modifier.weight(0.35f),
                    label = { Text(stringResource(R.string.invoice_line_qty)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = draft.unitPrice,
                    onValueChange = onUnitPriceChange,
                    modifier = Modifier.weight(0.65f),
                    label = { Text(stringResource(R.string.invoice_line_unit_ht)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    suffix = { CurrencySuffix() },
                    shape = RoundedCornerShape(10.dp)
                )
            }
            lineTotal?.let { total ->
                Text(
                    stringResource(R.string.invoice_line_total, formatAmount(total)),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun InvoiceTaxLine(label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = Color(0xFF64748B))
        Text(
            value,
            fontSize = if (bold) 15.sp else 13.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium
        )
    }
}
