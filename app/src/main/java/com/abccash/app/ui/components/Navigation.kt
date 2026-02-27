package com.abccash.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

@Composable
fun HeaderMenu(
    selectedTab: Int,
    onSelectedTab: (Int) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCategories: () -> Unit,
    onExportDashboard: () -> Unit,
    onExportTransactions: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        HeaderMenuItem(
            label = "Dashboard",
            selected = selectedTab == 0,
            onClick = { onSelectedTab(0) }
        ) {
            Icon(Icons.Filled.Dashboard, contentDescription = "Dashboard")
        }
        HeaderMenuItem(
            label = "Calendrier",
            selected = selectedTab == 1,
            onClick = { onSelectedTab(1) }
        ) {
            Icon(Icons.Filled.CalendarMonth, contentDescription = "Calendrier")
        }
        HeaderMenuItem(
            label = "Planning",
            selected = selectedTab == 2,
            onClick = { onSelectedTab(2) }
        ) {
            Icon(Icons.Filled.EventNote, contentDescription = "Planning")
        }
        Box {
            HeaderMenuItem(
                label = "Menu",
                selected = selectedTab == 3,
                onClick = { menuExpanded = true }
            ) {
                Icon(Icons.Filled.Menu, contentDescription = "Menu")
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Parametres") },
                    onClick = {
                        menuExpanded = false
                        onOpenSettings()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Categories") },
                    onClick = {
                        menuExpanded = false
                        onOpenCategories()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Exporter Dashboard PDF") },
                    onClick = {
                        menuExpanded = false
                        onExportDashboard()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Exporter Transactions PDF") },
                    onClick = {
                        menuExpanded = false
                        onExportTransactions()
                    }
                )
            }
        }
    }
}

@Composable
private fun HeaderMenuItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        IconButton(onClick = onClick) {
            icon()
        }
        Text(
            text = label,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
