package com.abccash.app.treasury.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abccash.app.R

@Composable
fun AdminBulkSelectionBar(
    totalCount: Int,
    selectedCount: Int,
    onToggleSelectAll: () -> Unit,
    onDeleteSelected: () -> Unit
) {
    if (totalCount == 0) return

    val allSelected = selectedCount == totalCount

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.clickable(onClick = onToggleSelectAll),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = allSelected,
                onCheckedChange = { onToggleSelectAll() },
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (allSelected) {
                    stringResource(R.string.deselect_all)
                } else {
                    stringResource(R.string.select_all)
                },
                fontSize = 13.sp
            )
        }

        if (selectedCount > 0) {
            Row(
                modifier = Modifier
                    .clickable(onClick = onDeleteSelected)
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete_count, selectedCount),
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFFF44336)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.delete_count, selectedCount),
                    color = Color(0xFFF44336),
                    fontSize = 13.sp
                )
            }
        }
    }
}
