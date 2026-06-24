package com.abccash.app.treasury.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.abccash.app.R
import com.abccash.app.locale.AppLocale
import java.time.YearMonth

@Composable
fun MonthSelectorRow(
    selectedMonth: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (compact) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onMonthChange(selectedMonth.minusMonths(1)) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.previous_month),
                    tint = Color(0xFF94A3B8)
                )
            }
            Text(
                text = AppLocale.monthYear(selectedMonth),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF94A3B8)
            )
            IconButton(
                onClick = { onMonthChange(selectedMonth.plusMonths(1)) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.next_month),
                    tint = Color(0xFF94A3B8)
                )
            }
        }
        return
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onMonthChange(selectedMonth.minusMonths(1)) }) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = stringResource(R.string.previous_month))
            }
            Text(
                text = AppLocale.monthYear(selectedMonth),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { onMonthChange(selectedMonth.plusMonths(1)) }) {
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = stringResource(R.string.next_month))
            }
        }
    }
}
