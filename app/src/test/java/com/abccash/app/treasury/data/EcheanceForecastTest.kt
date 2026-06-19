package com.abccash.app.treasury.data

import com.abccash.app.locale.AppLocale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class EcheanceForecastTest {

    @Test
    fun `buildItemsForMonth shows only unpaid items in selected month`() {
        val month = YearMonth.of(2026, 6)
        val invoices = listOf(
            Invoice(
                invoiceNumber = "F1",
                clientName = "Client A",
                totalAmount = 500.0,
                dueDate = LocalDate.of(2026, 6, 15)
            ),
            Invoice(
                invoiceNumber = "F2",
                clientName = "Client B",
                totalAmount = 300.0,
                paidAmount = 300.0,
                dueDate = LocalDate.of(2026, 6, 20),
                payments = listOf(
                    Payment(
                        invoiceId = "paid",
                        amount = 300.0,
                        date = LocalDate.of(2026, 6, 1),
                        method = PaymentMethod.CASH
                    )
                )
            ),
            Invoice(
                invoiceNumber = "F3",
                clientName = "Client C",
                totalAmount = 400.0,
                dueDate = LocalDate.of(2026, 7, 1)
            )
        )
        val expenses = listOf(
            Expense(
                label = "Loyer juin",
                amount = 200.0,
                date = LocalDate.of(2026, 6, 5),
                isPaid = false
            ),
            Expense(
                label = "Loyer juillet",
                amount = 220.0,
                date = LocalDate.of(2026, 7, 5),
                isPaid = false
            ),
            Expense(
                label = "Payé",
                amount = 50.0,
                date = LocalDate.of(2026, 6, 10),
                isPaid = true
            )
        )

        val items = EcheanceForecast.buildItemsForMonth(month, invoices, expenses)

        assertEquals(2, items.size)
        assertTrue(items.any { it.type == EcheanceType.INCOME && it.label == "Client A" })
        assertTrue(items.any { it.type == EcheanceType.EXPENSE && it.label == "Loyer juin" })
    }

    @Test
    fun `builds income and expense items grouped by month`() {
        val invoices = listOf(
            Invoice(
                invoiceNumber = "F1",
                clientName = "Client A",
                totalAmount = 500.0,
                dueDate = LocalDate.of(2026, 6, 15)
            ),
            Invoice(
                invoiceNumber = "F2",
                clientName = "Client B",
                totalAmount = 300.0,
                paidAmount = 300.0,
                dueDate = LocalDate.of(2026, 7, 1),
                payments = listOf(
                    Payment(
                        invoiceId = "paid",
                        amount = 300.0,
                        date = LocalDate.of(2026, 6, 1),
                        method = PaymentMethod.CASH
                    )
                )
            )
        )
        val expenses = listOf(
            Expense(
                label = "Loyer",
                amount = 200.0,
                date = LocalDate.of(2026, 7, 5),
                isPaid = false
            )
        )

        val items = EcheanceForecast.buildItems(
            invoices = invoices,
            expenses = expenses,
            from = LocalDate.of(2026, 6, 1),
            to = LocalDate.of(2026, 12, 31)
        )

        assertEquals(2, items.size)
        assertTrue(items.any { it.type == EcheanceType.INCOME && it.label == "Client A" })
        assertTrue(items.any { it.type == EcheanceType.EXPENSE && it.label == "Loyer" })

        val sections = EcheanceForecast.groupByMonth(items)
        assertEquals(2, sections.size)
        assertEquals(AppLocale.monthYear(YearMonth.of(2026, 6)), sections[0].label)
        assertEquals(AppLocale.monthYear(YearMonth.of(2026, 7)), sections[1].label)
    }
}
