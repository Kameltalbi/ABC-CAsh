package com.abccash.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.abccash.app.data.local.entity.CategoryEntity
import com.abccash.app.data.local.entity.TransactionType
import com.abccash.app.ui.theme.AppColors

@Composable
fun CategoriesScreen(
    categories: List<CategoryEntity>,
    onAddCategory: (String, TransactionType) -> Unit,
    onDeleteCategory: (Long) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var newCategoryType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var pendingDelete by remember { mutableStateOf<CategoryEntity?>(null) }

    val incomeCategories = categories.filter { it.type == TransactionType.INCOME }
    val expenseCategories = categories.filter { it.type == TransactionType.EXPENSE }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
            CategorySectionTitle("Revenus", AppColors.RevenueDot)
            CategoryGrid(
                categories = incomeCategories,
                type = TransactionType.INCOME,
                onLongPress = { pendingDelete = it }
            )

            CategorySectionTitle("Depenses", AppColors.ExpenseDot)
            CategoryGrid(
                categories = expenseCategories,
                type = TransactionType.EXPENSE,
                onLongPress = { pendingDelete = it }
            )
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.BottomEnd)
                .padding(16.dp)
        ) { Text("+") }
    }

    if (showAddDialog) {
        AddCategoryDialog(
            categoryName = newCategoryName,
            categoryType = newCategoryType,
            onCategoryNameChange = { newCategoryName = it },
            onCategoryTypeChange = { newCategoryType = it },
            onConfirm = {
                onAddCategory(newCategoryName, newCategoryType)
                newCategoryName = ""
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    if (pendingDelete != null) {
        DeleteCategoryDialog(
            category = pendingDelete!!,
            onConfirm = {
                onDeleteCategory(pendingDelete!!.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
private fun CategorySectionTitle(title: String, dotColor: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("●", color = dotColor, fontWeight = FontWeight.Bold)
        Text(title, color = dotColor, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CategoryGrid(
    categories: List<CategoryEntity>,
    type: TransactionType,
    onLongPress: (CategoryEntity) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(0.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.height(220.dp)
    ) {
        items(categories) { category ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLongPress(category) },
                colors = CardDefaults.cardColors(
                    containerColor = categoryCardColor(category, type)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Icon(
                            imageVector = categoryIcon(category.name, type),
                            contentDescription = category.name,
                            tint = if (type == TransactionType.INCOME) 
                                Color(0xFF047857) 
                            else 
                                Color(0xFF1F2937),
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                    Text(
                        text = category.name,
                        maxLines = 2,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (type == TransactionType.INCOME) 
                            Color(0xFF047857) 
                        else 
                            Color(0xFF1F2937)
                    )
                }
            }
        }
    }
}

@Composable
private fun AddCategoryDialog(
    categoryName: String,
    categoryType: TransactionType,
    onCategoryNameChange: (String) -> Unit,
    onCategoryTypeChange: (TransactionType) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvelle categorie") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = onCategoryNameChange,
                    label = { Text("Nom categorie") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = { onCategoryTypeChange(TransactionType.INCOME) },
                        label = { Text("Revenu") },
                        modifier = Modifier.alpha(if (categoryType == TransactionType.INCOME) 1f else 0.65f)
                    )
                    AssistChip(
                        onClick = { onCategoryTypeChange(TransactionType.EXPENSE) },
                        label = { Text("Depense") },
                        modifier = Modifier.alpha(if (categoryType == TransactionType.EXPENSE) 1f else 0.65f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Ajouter") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
private fun DeleteCategoryDialog(
    category: CategoryEntity,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Supprimer categorie") },
        text = { Text("Supprimer ${category.name} ?") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Supprimer") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

private fun categoryIcon(name: String, type: TransactionType): ImageVector = when {
    // Revenus B2B
    name.contains("salaire", true) -> Icons.Filled.AccountBalance
    name.contains("freelance", true) || name.contains("consultant", true) -> Icons.Filled.Business
    name.contains("revenu", true) || name.contains("autre", true) -> Icons.Filled.TrendingUp
    
    // Dépenses B2B
    name.contains("loyer", true) || name.contains("logement", true) -> Icons.Filled.Home
    name.contains("transport", true) -> Icons.Filled.DirectionsCar
    name.contains("alimentation", true) || name.contains("aliment", true) -> Icons.Filled.Restaurant
    name.contains("sante", true) || name.contains("santé", true) -> Icons.Filled.MedicalServices
    name.contains("loisir", true) -> Icons.Filled.SportsEsports
    name.contains("shopping", true) -> Icons.Filled.ShoppingBag
    name.contains("divers", true) -> Icons.Filled.MoreHoriz
    name.contains("bureau", true) || name.contains("fourniture", true) -> Icons.Filled.BusinessCenter
    name.contains("formation", true) || name.contains("education", true) -> Icons.Filled.School
    name.contains("marketing", true) || name.contains("publicité", true) -> Icons.Filled.Campaign
    name.contains("abonnement", true) || name.contains("subscription", true) -> Icons.Filled.Subscriptions
    name.contains("assurance", true) -> Icons.Filled.Security
    name.contains("impot", true) || name.contains("taxe", true) -> Icons.Filled.Receipt
    name.contains("energie", true) || name.contains("electricite", true) -> Icons.Filled.Bolt
    name.contains("internet", true) || name.contains("telecom", true) -> Icons.Filled.Wifi
    name.contains("restaurant", true) -> Icons.Filled.Restaurant
    
    // Par défaut selon le type
    type == TransactionType.INCOME -> Icons.Filled.Payments
    else -> Icons.Filled.Category
}

private fun categoryCardColor(category: CategoryEntity, type: TransactionType): Color {
    val n = category.name.lowercase()
    if (type == TransactionType.INCOME) return AppColors.CategoryIncome
    return when {
        n.contains("transport") -> AppColors.CategoryTransport
        n.contains("sante") -> AppColors.CategoryHealth
        n.contains("loyer") || n.contains("logement") -> AppColors.CategoryHousing
        n.contains("aliment") -> AppColors.CategoryFood
        n.contains("shopping") -> AppColors.CategoryShopping
        else -> AppColors.CategoryDefault
    }
}
