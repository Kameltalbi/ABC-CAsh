package com.abccash.app.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.abccash.app.data.local.entity.CurrencyCode
import com.abccash.app.domain.model.CumulativeForecastPoint
import com.abccash.app.ui.exportDashboardPdf
import com.abccash.app.ui.theme.AppColors
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter

@Composable
fun DashboardScreen(
    openingMinor: Long,
    forecastRows: List<CumulativeForecastPoint>,
    currency: CurrencyCode,
    onUpdateOpeningBalance: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val stressMonths = forecastRows.filter { it.cumulativeMinor < 0L }
    val worstStress = stressMonths.minByOrNull { it.cumulativeMinor }
    val maxExcedent = forecastRows.maxByOrNull { it.cumulativeMinor }
    
    var showBalanceDialog by remember { mutableStateOf(false) }
    var balanceText by remember { mutableStateOf("") }
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Solde d'ouverture: ${formatAmount(openingMinor, currency)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { 
                balanceText = String.format("%.0f", openingMinor / 100.0)
                showBalanceDialog = true 
            }) {
                Icon(Icons.Default.Edit, contentDescription = "Modifier solde")
            }
        }
        Spacer(Modifier.height(8.dp))
        
        if (showBalanceDialog) {
            LaunchedEffect(showBalanceDialog) {
                balanceText = String.format("%.0f", openingMinor / 100.0)
                android.util.Log.d("DashboardScreen", "Dialog opened, initialized balanceText to: $balanceText")
            }
            
            AlertDialog(
                onDismissRequest = { showBalanceDialog = false },
                title = { Text("Modifier le solde d'ouverture") },
                text = {
                    Column {
                        Text("Entrez le solde initial de votre compte :")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = balanceText,
                            onValueChange = { 
                                balanceText = it
                                android.util.Log.d("DashboardScreen", "TextField changed to: $it")
                            },
                            label = { Text("Montant") },
                            suffix = { Text(currency.name) },
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        // Remplacer la virgule par un point pour le parsing
                        val cleanedText = balanceText.replace(",", ".").replace(" ", "")
                        val newBalance = cleanedText.toDoubleOrNull() ?: (openingMinor / 100.0)
                        val newBalanceMinor = (newBalance * 100).toLong()
                        android.util.Log.d("DashboardScreen", "Confirming balance: cleanedText='$cleanedText', newBalance=$newBalance -> $newBalanceMinor")
                        onUpdateOpeningBalance(newBalanceMinor)
                        showBalanceDialog = false
                    }) {
                        Text("Confirmer")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBalanceDialog = false }) {
                        Text("Annuler")
                    }
                }
            )
        }
        
        TreasuryAlertCard(
            stressMonthsCount = stressMonths.size,
            worstStress = worstStress,
            maxExcedent = maxExcedent,
            currency = currency
        )
        
        Spacer(Modifier.height(12.dp))
        
        Button(
            onClick = {
                runCatching {
                    exportDashboardPdf(
                        context = context,
                        openingMinor = openingMinor,
                        forecastRows = forecastRows,
                        currency = currency
                    )
                }.onSuccess { file ->
                    Toast.makeText(context, "PDF exporte: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                }.onFailure {
                    Toast.makeText(context, "Erreur export PDF", Toast.LENGTH_LONG).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Exporter PDF")
        }
        
        Spacer(Modifier.height(12.dp))
        
        ProfessionalNetBarChart(forecastRows = forecastRows)
        
        Spacer(Modifier.height(12.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(forecastRows) { row ->
                ForecastRowCard(row = row, currency = currency)
            }
        }
    }
}

@Composable
private fun TreasuryAlertCard(
    stressMonthsCount: Int,
    worstStress: CumulativeForecastPoint?,
    maxExcedent: CumulativeForecastPoint?,
    currency: CurrencyCode
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Alerte tresorerie (12 mois)", fontWeight = FontWeight.SemiBold)
            Text("Mois en stress: $stressMonthsCount")
            worstStress?.let {
                Text(
                    "Pire mois: ${it.yearMonth} (manque ${formatAmount(-it.cumulativeMinor, currency)})",
                    color = AppColors.ExpenseRed
                )
            }
            maxExcedent?.let {
                if (it.cumulativeMinor > 0L) {
                    Text(
                        "Meilleur excedent: ${it.yearMonth} (${formatAmount(it.cumulativeMinor, currency)})",
                        color = AppColors.IncomeGreen
                    )
                }
            }
        }
    }
}

@Composable
private fun ForecastRowCard(row: CumulativeForecastPoint, currency: CurrencyCode) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("Mois: ${row.yearMonth}", fontWeight = FontWeight.SemiBold)
            Text("Net: ${formatAmount(row.netMinor, currency)}")
            Spacer(Modifier.height(4.dp))
            Text("Cumul: ${formatAmount(row.cumulativeMinor, currency)}")
            if (row.cumulativeMinor < 0L) {
                Text(
                    "Besoin de tresorerie: ${formatAmount(-row.cumulativeMinor, currency)}",
                    color = AppColors.ExpenseRed,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Text(
                    "Excedent: ${formatAmount(row.cumulativeMinor, currency)}",
                    color = AppColors.IncomeGreen,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun ProfessionalNetBarChart(forecastRows: List<CumulativeForecastPoint>) {
    val positive = AppColors.IncomeGreen.toArgb()
    val negative = AppColors.ExpenseRed.toArgb()
    
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        factory = { context ->
            BarChart(context).apply {
                description.isEnabled = false
                legend.isEnabled = false
                setDrawGridBackground(false)
                setFitBars(true)
                axisRight.isEnabled = false
                setScaleEnabled(false)
                setPinchZoom(false)
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.granularity = 1f
                xAxis.setDrawGridLines(false)
                axisLeft.setDrawGridLines(true)
            }
        },
        update = { chart ->
            val entries = forecastRows.mapIndexed { index, point ->
                BarEntry(index.toFloat(), point.netMinor / 100f)
            }
            val dataSet = BarDataSet(entries, "Net mensuel").apply {
                valueTextSize = 10f
                colors = entries.map { if (it.y >= 0f) positive else negative }
            }
            chart.data = BarData(dataSet).apply { barWidth = 0.62f }
            chart.xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val idx = value.toInt()
                    if (idx !in forecastRows.indices) return ""
                    return forecastRows[idx].yearMonth.substring(5)
                }
            }
            chart.invalidate()
        }
    )
}

private fun formatAmount(amountMinor: Long, currency: CurrencyCode): String {
    val major = amountMinor / 100.0
    val symbol = when (currency) {
        CurrencyCode.DT -> "DT"
        CurrencyCode.USD -> "$"
        CurrencyCode.EUR -> "EUR"
    }
    return "%.2f %s".format(major, symbol)
}

private fun androidx.compose.ui.graphics.Color.toArgb(): Int {
    return android.graphics.Color.argb(
        (alpha * 255).toInt(),
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt()
    )
}
