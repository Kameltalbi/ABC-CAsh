package com.abccash.app.treasury.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class InvoiceKpisTest {

    @Test
    fun compute_aggregatesMonthInvoices() {
        val month = YearMonth.of(2026, 6)
        val invoices = listOf(
            Invoice(
                invoiceNumber = "F1",
                clientName = "A",
                totalAmount = 1000.0,
                paidAmount = 400.0,
                dueDate = LocalDate.of(2026, 6, 10)
            ),
            Invoice(
                invoiceNumber = "F2",
                clientName = "B",
                totalAmount = 500.0,
                dueDate = LocalDate.of(2026, 6, 20)
            ),
            Invoice(
                invoiceNumber = "F3",
                clientName = "C",
                totalAmount = 200.0,
                dueDate = LocalDate.of(2026, 7, 1)
            )
        )

        val kpis = InvoiceKpisCalculations.compute(
            invoices,
            month,
            today = LocalDate.of(2026, 6, 25)
        )

        assertEquals(1500.0, kpis.totalBilled, 0.001)
        assertEquals(400.0, kpis.totalCollected, 0.001)
        assertEquals(1100.0, kpis.totalPending, 0.001)
        assertEquals(2, kpis.invoiceCount)
        assertEquals(2, kpis.overdueCount)
    }
}
