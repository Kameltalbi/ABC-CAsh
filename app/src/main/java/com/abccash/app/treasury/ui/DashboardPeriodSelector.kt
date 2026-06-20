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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abccash.app.R
import com.abccash.app.treasury.data.DashboardViewMode
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardPeriodSelector(
    viewMode: DashboardViewMode,
    onViewModeChange: (DashboardViewMode) -> Unit,
    selectedMonth: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PeriodToggleButton(
                text = stringResource(R.string.dashboard_period_year),
                selected = viewMode == DashboardViewMode.YEAR,
                onClick = { onViewModeChange(DashboardViewMode.YEAR) },
                modifier = Modifier.weight(1f)
            )
            PeriodToggleButton(
                text = stringResource(R.string.dashboard_period_month),
                selected = viewMode == DashboardViewMode.MONTH,
                onClick = { onViewModeChange(DashboardViewMode.MONTH) },
                modifier = Modifier.weight(1f)
            )
        }
        when (viewMode) {
            DashboardViewMode.YEAR -> YearSelectorRow(
                year = selectedMonth.year,
                onYearChange = { year -> onMonthChange(YearMonth.of(year, selectedMonth.month)) }
            )
            DashboardViewMode.MONTH -> MonthSelectorRow(
                selectedMonth = selectedMonth,
                onMonthChange = onMonthChange
            )
        }
    }
}

@Composable
private fun PeriodToggleButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Color(0xFF22C55E) else Color(0xFFF3F4F6),
            contentColor = if (selected) Color.White else Color(0xFF6B7280)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

@Composable
private fun YearSelectorRow(
    year: Int,
    onYearChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onYearChange(year - 1) }) {
                Icon(
                    Icons.Default.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.previous_year),
                    tint = Color(0xFF6B7280)
                )
            }
            Text(
                text = year.toString(),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A)
            )
            IconButton(onClick = { onYearChange(year + 1) }) {
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.next_year),
                    tint = Color(0xFF6B7280)
                )
            }
        }
    }
}
