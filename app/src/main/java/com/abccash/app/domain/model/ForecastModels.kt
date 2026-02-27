package com.abccash.app.domain.model

data class MonthlyForecastRow(
    val yearMonth: String,
    val incomeMinor: Long,
    val expenseMinor: Long,
    val netMinor: Long
)

data class CumulativeForecastPoint(
    val yearMonth: String,
    val incomeMinor: Long,
    val expenseMinor: Long,
    val netMinor: Long,
    val cumulativeMinor: Long
)
