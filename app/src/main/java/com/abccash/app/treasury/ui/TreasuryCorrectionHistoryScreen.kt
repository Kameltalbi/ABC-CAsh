package com.abccash.app.treasury.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.abccash.app.treasury.data.BalanceCorrection
import com.abccash.app.treasury.data.BalanceCorrectionType
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreasuryCorrectionHistoryScreen(
    corrections: List<BalanceCorrection>,
    formatAmount: (Double) -> String,
    onBack: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.treasury_history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        if (corrections.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.treasury_history_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(corrections) { correction ->
                    CorrectionHistoryCard(
                        correction = correction,
                        formatAmount = formatAmount,
                        dateFormatter = dateFormatter
                    )
                }
            }
        }
    }
}

@Composable
private fun CorrectionHistoryCard(
    correction: BalanceCorrection,
    formatAmount: (Double) -> String,
    dateFormatter: DateTimeFormatter
) {
    val isInitial = correction.type == BalanceCorrectionType.INITIAL
    val isOpeningRevision = correction.type == BalanceCorrectionType.OPENING_REVISION
    val ecart = correction.ecart
    val ecartColor = when {
        ecart > 0 -> Color(0xFF16A34A)
        ecart < 0 -> Color(0xFFDC2626)
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isInitial -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                isOpeningRevision -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = correction.correctionDate.format(dateFormatter),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        isInitial -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        isOpeningRevision -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.secondaryContainer
                    }
                ) {
                    Text(
                        text = when {
                            isInitial -> stringResource(R.string.treasury_history_initial)
                            isOpeningRevision -> stringResource(R.string.treasury_history_opening_revision)
                            else -> stringResource(R.string.treasury_correction_title)
                        },
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        color = when {
                            isInitial -> MaterialTheme.colorScheme.primary
                            isOpeningRevision -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.onSecondaryContainer
                        }
                    )
                }
            }

            HorizontalDivider(thickness = 0.5.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.treasury_history_old_balance),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatAmount(correction.oldBalance),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.treasury_history_gap),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = (if (ecart >= 0) "+" else "") + formatAmount(ecart),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ecartColor
                    )
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.treasury_history_new_balance),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatAmount(correction.newBalance),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (correction.motif.isNotBlank() && !isInitial) {
                Text(
                    text = "💬 ${correction.motif}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (correction.userName.isNotBlank()) {
                Text(
                    text = "👤 ${correction.userName}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
