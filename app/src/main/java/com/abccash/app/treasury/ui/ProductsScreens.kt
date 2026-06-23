package com.abccash.app.treasury.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.abccash.app.treasury.data.CategorySelection
import com.abccash.app.treasury.data.Product
import com.abccash.app.treasury.data.ProductKind
import com.abccash.app.treasury.data.ProductUnit
import com.abccash.app.treasury.data.RevenueCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsListScreen(
    products: List<Product>,
    onBack: () -> Unit,
    onAddProduct: () -> Unit,
    onEditProduct: (Product) -> Unit,
    onDeleteProduct: (String) -> Unit
) {
    val formatAmount = rememberFormatMoney()
    var productToDelete by remember { mutableStateOf<Product?>(null) }

    if (productToDelete != null) {
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text(stringResource(R.string.delete_product_question)) },
            text = { Text(stringResource(R.string.delete_product_confirm, productToDelete!!.name)) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteProduct(productToDelete!!.id)
                    productToDelete = null
                }) {
                    Text(stringResource(R.string.delete), color = Color(0xFFDC2626))
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.products_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            AbcCashFab(
                onClick = onAddProduct,
                contentDescription = stringResource(R.string.product_add)
            )
        }
    ) { padding ->
        if (products.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.products_empty), color = Color.Gray, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(products.filter { it.isActive }, key = { it.id }) { product ->
                    ProductListItem(
                        product = product,
                        formatAmount = formatAmount,
                        onEdit = { onEditProduct(product) },
                        onDelete = { productToDelete = product }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductListItem(
    product: Product,
    formatAmount: (Double) -> String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val kindLabel = when (product.kind) {
        ProductKind.PRODUCT -> stringResource(R.string.product_kind_product)
        ProductKind.SERVICE -> stringResource(R.string.product_kind_service)
    }
    val unitLabel = productUnitLabel(product.unit)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onEdit)
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(product.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(
                    "$kindLabel · $unitLabel · ${formatAmount(product.unitPriceExclTax)} HT",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                if (product.categoryLabel.isNotBlank()) {
                    Text(product.categoryLabel, fontSize = 11.sp, color = Color(0xFF64748B))
                }
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = Color(0xFFDC2626))
                }
            }
        }
    }
}

@Composable
fun productUnitLabel(unit: ProductUnit): String = when (unit) {
    ProductUnit.PIECE -> stringResource(R.string.product_unit_piece)
    ProductUnit.HOUR -> stringResource(R.string.product_unit_hour)
    ProductUnit.DAY -> stringResource(R.string.product_unit_day)
    ProductUnit.FLAT -> stringResource(R.string.product_unit_flat)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormSheet(
    visible: Boolean,
    initialProduct: Product?,
    entrepriseId: String,
    customIncomeCategories: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    if (!visible) return

    val categoryOptions = incomeCategoryOptions(customIncomeCategories)
    val defaultCategoryLabel = categoryOptions.firstOrNull().orEmpty()

    var name by remember(initialProduct) { mutableStateOf(initialProduct?.name.orEmpty()) }
    var price by remember(initialProduct) {
        mutableStateOf(initialProduct?.unitPriceExclTax?.toString().orEmpty())
    }
    var kind by remember(initialProduct) { mutableStateOf(initialProduct?.kind ?: ProductKind.SERVICE) }
    var unit by remember(initialProduct) { mutableStateOf(initialProduct?.unit ?: ProductUnit.PIECE) }
    var selectedCategoryLabel by remember(initialProduct, categoryOptions) {
        mutableStateOf(
            initialProduct?.categoryLabel?.takeIf { it.isNotBlank() } ?: defaultCategoryLabel
        )
    }
    var showKindMenu by remember { mutableStateOf(false) }
    var showUnitMenu by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(if (initialProduct == null) R.string.product_add else R.string.product_edit),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.product_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                label = { Text(stringResource(R.string.product_price_ht)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                suffix = { CurrencySuffix() },
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenuBox(expanded = showKindMenu, onExpandedChange = { showKindMenu = it }) {
                OutlinedTextField(
                    value = when (kind) {
                        ProductKind.PRODUCT -> stringResource(R.string.product_kind_product)
                        ProductKind.SERVICE -> stringResource(R.string.product_kind_service)
                    },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.product_kind)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showKindMenu) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(expanded = showKindMenu, onDismissRequest = { showKindMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.product_kind_product)) },
                        onClick = { kind = ProductKind.PRODUCT; showKindMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.product_kind_service)) },
                        onClick = { kind = ProductKind.SERVICE; showKindMenu = false }
                    )
                }
            }
            ExposedDropdownMenuBox(expanded = showUnitMenu, onExpandedChange = { showUnitMenu = it }) {
                OutlinedTextField(
                    value = productUnitLabel(unit),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.product_unit)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showUnitMenu) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(expanded = showUnitMenu, onDismissRequest = { showUnitMenu = false }) {
                    ProductUnit.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(productUnitLabel(option)) },
                            onClick = { unit = option; showUnitMenu = false }
                        )
                    }
                }
            }
            ExposedDropdownMenuBox(expanded = showCategoryMenu, onExpandedChange = { showCategoryMenu = it }) {
                OutlinedTextField(
                    value = selectedCategoryLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.income_category)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryMenu) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(expanded = showCategoryMenu, onDismissRequest = { showCategoryMenu = false }) {
                    if (categoryOptions.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.settings_income_categories_sub)) },
                            onClick = {},
                            enabled = false
                        )
                    } else {
                        categoryOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    selectedCategoryLabel = option
                                    showCategoryMenu = false
                                }
                            )
                        }
                    }
                }
            }
            Button(
                onClick = {
                    val parsedPrice = price.replace(",", ".").toDoubleOrNull() ?: return@Button
                    val resolved = CategorySelection.resolveIncome(selectedCategoryLabel, customIncomeCategories)
                    onSave(
                        (initialProduct ?: Product(entrepriseId = entrepriseId, name = "", unitPriceExclTax = 0.0)).copy(
                            name = name.trim(),
                            unitPriceExclTax = parsedPrice,
                            kind = kind,
                            unit = unit,
                            category = resolved.revenueCategory ?: RevenueCategory.OTHER,
                            categoryLabel = resolved.customLabel.orEmpty(),
                            entrepriseId = entrepriseId
                        )
                    )
                },
                enabled = name.isNotBlank() &&
                    selectedCategoryLabel.isNotBlank() &&
                    price.replace(",", ".").toDoubleOrNull()?.let { it > 0 } == true,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}
