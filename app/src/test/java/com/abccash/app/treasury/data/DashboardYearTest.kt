package com.abccash.app.treasury.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DashboardYearTest {
    private val org = "org-a"
    private val today = LocalDate.of(2026, 10, 4)
    private val biat = BankAccount(id = "biat", entrepriseId = org, name = "BIAT", openingBalance = 1_000.0, isDefault = true)

    @Test
    fun lanneeEnCoursCompareLaMemePeriode() {
        val entries = listOf(
            sale("s-oct", LocalDate.of(2026, 10, 20), 100.0),
            sale("s-nov", LocalDate.of(2026, 11, 2), 50.0),
            sale("s-ly", LocalDate.of(2025, 10, 15), 80.0),
            sale("s-ly-late", LocalDate.of(2025, 11, 2), 40.0),
            expense("e-oct", LocalDate.of(2026, 10, 31), 30.0),
            expense("e-nov", LocalDate.of(2026, 11, 1), 10.0),
            expense("e-ly", LocalDate.of(2025, 10, 1), 20.0),
            expense("e-ly-late", LocalDate.of(2025, 12, 1), 15.0)
        )
        val snap = DashboardYear.snapshot(2026, today, org, emptyList(), emptyList(), emptyList(), entries, emptyList())
        assertEquals(100.0, snap.sales.amount, 0.001)
        assertEquals(80.0, snap.sales.previous, 0.001)
        assertEquals(25.0, snap.sales.changePercent!!, 0.001)
        assertEquals(30.0, snap.expenses.amount, 0.001)
        assertEquals(20.0, snap.expenses.previous, 0.001)
        assertEquals(70.0, snap.result.amount, 0.001)
        assertEquals(60.0, snap.result.previous, 0.001)
    }

    @Test
    fun uneAnneePasseeCompareLesDouzeMois() {
        val entries = listOf(
            sale("s-dec", LocalDate.of(2025, 12, 15), 30.0),
            sale("s-prev", LocalDate.of(2024, 6, 1), 10.0),
            sale("s-prev-dec", LocalDate.of(2024, 12, 15), 10.0)
        )
        val snap = DashboardYear.snapshot(2025, today, org, emptyList(), emptyList(), emptyList(), entries, emptyList())
        assertEquals(30.0, snap.sales.amount, 0.001)
        assertEquals(20.0, snap.sales.previous, 0.001)
        assertEquals(50.0, snap.sales.changePercent!!, 0.001)
    }

    @Test
    fun laTresorerieADateIgnoreLesMoisFutursEtLeGraphiqueLesAffiche() {
        val collectedLater = PilotEntry(
            id = "sale-dec",
            entrepriseId = org,
            type = PilotEntryType.SALE,
            date = LocalDate.of(2026, 10, 4),
            categoryId = "digital",
            amount = 500.0,
            treasuryDate = LocalDate.of(2026, 12, 15),
            bankAccountId = "biat"
        )
        val payment = Invoice(
            id = "inv",
            invoiceNumber = "F-1",
            clientName = "Client",
            totalAmount = 200.0,
            paidAmount = 200.0,
            dueDate = LocalDate.of(2026, 10, 2),
            entrepriseId = org,
            payments = listOf(
                Payment(
                    id = "p1",
                    invoiceId = "inv",
                    amount = 200.0,
                    date = LocalDate.of(2026, 10, 2),
                    method = PaymentMethod.TRANSFER,
                    bankAccountId = "biat"
                )
            )
        )
        val lastYear = Invoice(
            id = "inv-ly",
            invoiceNumber = "F-0",
            clientName = "Client",
            totalAmount = 100.0,
            paidAmount = 100.0,
            dueDate = LocalDate.of(2025, 10, 2),
            entrepriseId = org,
            payments = listOf(
                Payment(
                    id = "p0",
                    invoiceId = "inv-ly",
                    amount = 100.0,
                    date = LocalDate.of(2025, 10, 2),
                    method = PaymentMethod.TRANSFER,
                    bankAccountId = "biat"
                )
            )
        )
        val snap = DashboardYear.snapshot(
            2026,
            today,
            org,
            listOf(biat),
            listOf(payment, lastYear),
            emptyList(),
            listOf(collectedLater),
            emptyList()
        )
        assertEquals(1_300.0, snap.treasury.amount, 0.001)
        assertEquals(1_100.0, snap.treasury.previous, 0.001)
        val october = snap.selected[9]
        val december = snap.selected[11]
        assertEquals(1_300.0, october.closing, 0.001)
        assertFalse(october.forecast)
        assertEquals(1_800.0, december.closing, 0.001)
        assertTrue(december.forecast)
        assertEquals(
            TreasuryMonth.snapshot(
                october.month, org, listOf(biat), listOf(payment, lastYear), emptyList(), listOf(collectedLater), emptyList()
            ).closing,
            october.closing,
            0.001
        )
        assertFalse(snap.previousYear.last().forecast)
    }

    @Test
    fun unEncaissementHorsActiviteNeChangePasLeResultat() {
        val credit = PilotEntry(
            id = "credit",
            entrepriseId = org,
            type = PilotEntryType.OTHER_INFLOW,
            date = LocalDate.of(2026, 10, 3),
            amount = 5_000.0,
            bankAccountId = "biat"
        )
        val snap = DashboardYear.snapshot(2026, today, org, listOf(biat), emptyList(), emptyList(), listOf(credit), emptyList())
        assertEquals(0.0, snap.sales.amount, 0.001)
        assertEquals(0.0, snap.result.amount, 0.001)
        assertNull(snap.sales.changePercent)
        assertEquals(6_000.0, snap.treasury.amount, 0.001)
    }

    @Test
    fun leSoldeADateEgalLaClotureQuandLaDateEstLaFinDuMois() {
        val closing = TreasuryMonth.snapshot(
            java.time.YearMonth.of(2026, 10), org, listOf(biat), emptyList(), emptyList(), emptyList(), emptyList()
        ).closing
        val asOf = TreasuryMonth.balanceAsOf(
            LocalDate.of(2026, 10, 31), org, listOf(biat), emptyList(), emptyList(), emptyList(), emptyList()
        )
        assertEquals(closing, asOf, 0.001)
        assertEquals(1_000.0, asOf, 0.001)
    }

    private fun sale(id: String, date: LocalDate, amount: Double) = PilotEntry(
        id = id,
        entrepriseId = org,
        type = PilotEntryType.SALE,
        date = date,
        categoryId = "digital",
        amount = amount
    )

    private fun expense(id: String, date: LocalDate, amount: Double) = PilotEntry(
        id = id,
        entrepriseId = org,
        type = PilotEntryType.EXPENSE,
        date = date,
        categoryId = "loyer",
        amount = amount
    )
}
