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
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = viewMode == DashboardViewMode.YEAR,
                onClick = { onViewModeChange(DashboardViewMode.YEAR) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                label = { Text(stringResource(R.string.dashboard_period_year)) }
            )
            SegmentedButton(
                selected = viewMode == DashboardViewMode.MONTH,
                onClick = { onViewModeChange(DashboardViewMode.MONTH) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                label = { Text(stringResource(R.string.dashboard_period_month)) }
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
private fun YearSelectorRow(
    year: Int,
    onYearChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
            IconButton(onClick = { onYearChange(year - 1) }) {
                Icon(
                    Icons.Default.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.previous_year)
                )
            }
            Text(
                text = year.toString(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { onYearChange(year + 1) }) {
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.next_year)
                )
            }
        }
    }
}
