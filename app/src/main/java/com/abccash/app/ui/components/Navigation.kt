package com.abccash.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderMenuItem(
                label = "Dashboard",
                selected = selectedTab == 0,
                onClick = { onSelectedTab(0) }
            ) {
                Icon(
                    Icons.Filled.Dashboard,
                    contentDescription = "Dashboard",
                    modifier = Modifier.size(20.dp)
                )
            }
            
            HeaderMenuItem(
                label = "Calendrier",
                selected = selectedTab == 1,
                onClick = { onSelectedTab(1) }
            ) {
                Icon(
                    Icons.Filled.CalendarMonth,
                    contentDescription = "Calendrier",
                    modifier = Modifier.size(20.dp)
                )
            }
            
            HeaderMenuItem(
                label = "Planning",
                selected = selectedTab == 2,
                onClick = { onSelectedTab(2) }
            ) {
                Icon(
                    Icons.Filled.EventNote,
                    contentDescription = "Planning",
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Box {
                HeaderMenuItem(
                    label = "Menu",
                    selected = selectedTab == 3,
                    onClick = { menuExpanded = true }
                ) {
                    Icon(
                        Icons.Filled.Menu,
                        contentDescription = "Menu",
                        modifier = Modifier.size(20.dp)
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("⚙️ Paramètres") },
                        onClick = {
                            menuExpanded = false
                            onOpenSettings()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("📁 Catégories") },
                        onClick = {
                            menuExpanded = false
                            onOpenCategories()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("📊 Exporter Dashboard PDF") },
                        onClick = {
                            menuExpanded = false
                            onExportDashboard()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("📄 Exporter Transactions PDF") },
                        onClick = {
                            menuExpanded = false
                            onExportTransactions()
                        }
                    )
                }
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
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) 
            MaterialTheme.colorScheme.primaryContainer 
        else 
            Color.Transparent,
        animationSpec = tween(300),
        label = "backgroundColor"
    )
    
    val contentColor by animateColorAsState(
        targetValue = if (selected) 
            MaterialTheme.colorScheme.onPrimaryContainer 
        else 
            MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(300),
        label = "contentColor"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.1f else 1f,
        animationSpec = tween(300),
        label = "scale"
    )
    
    IconButton(
        onClick = onClick,
        modifier = Modifier.scale(scale)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = backgroundColor,
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides contentColor
            ) {
                icon()
            }
        }
    }
}
