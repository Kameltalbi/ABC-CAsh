package com.abccash.app.domain.usecase

import com.abccash.app.data.local.dao.BalanceDao
import com.abccash.app.data.local.dao.TransactionDao
import com.abccash.app.domain.model.CumulativeForecastPoint
import java.time.YearMonth

class Build12MonthCumulativeForecastUseCase(
    private val balanceDao: BalanceDao,
    private val transactionDao: TransactionDao
) {
    suspend operator fun invoke(currency: String): List<CumulativeForecastPoint> {
        val startYm = YearMonth.now()
        val endYm = startYm.plusMonths(11)
        val startDate = startYm.atDay(1).toString()
        val endDate = endYm.atEndOfMonth().toString()

        val openingBalance = balanceDao.getByCurrency(currency)?.amountMinor ?: 0L
        val rows = transactionDao.getMonthlyForecast(currency, startDate, endDate)
        val rowsByMonth = rows.associateBy { it.yearMonth }

        var running = openingBalance
        val points = mutableListOf<CumulativeForecastPoint>()
        repeat(12) { idx ->
            val ym = startYm.plusMonths(idx.toLong()).toString()
            val row = rowsByMonth[ym]
            val income = row?.incomeMinor ?: 0L
            val expense = row?.expenseMinor ?: 0L
            val net = row?.netMinor ?: 0L
            running += net
            points += CumulativeForecastPoint(
                yearMonth = ym,
                incomeMinor = income,
                expenseMinor = expense,
                netMinor = net,
                cumulativeMinor = running
            )
        }
        return points
    }
}
