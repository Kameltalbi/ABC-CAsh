package com.abccash.app.treasury.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class TreasuryMonthTest {
    private val org = "org-a"
    private val other = "org-b"
    private val october = YearMonth.of(2026, 10)
    private val november = YearMonth.of(2026, 11)

    @Test
    fun leSoldeDeFinDevientLeSoldeDeDebutDuMoisSuivant() {
        val biat = account("biat", "BIAT", opening = 1_000.0, default = true)
        val invoice = invoice(
            "Client A",
            payment("p1", LocalDate.of(2026, 10, 4), 200.0, "biat")
        )
        val expense = expense("Loyer", 50.0, LocalDate.of(2026, 10, 8), "biat")
        val octoberSnap = snap(october, accounts = listOf(biat), invoices = listOf(invoice), expenses = listOf(expense))
        val novemberSnap = snap(november, accounts = listOf(biat), invoices = listOf(invoice), expenses = listOf(expense))
        assertEquals(1_000.0, octoberSnap.opening, 0.001)
        assertEquals(200.0, octoberSnap.inflows, 0.001)
        assertEquals(50.0, octoberSnap.outflows, 0.001)
        assertEquals(1_150.0, octoberSnap.closing, 0.001)
        assertEquals(octoberSnap.closing, novemberSnap.opening, 0.001)
        assertEquals(1_150.0, novemberSnap.accounts.single().opening, 0.001)
        assertEquals(1_150.0, novemberSnap.closing, 0.001)
    }

    @Test
    fun uneVenteHistoriqueNeDoublePasLePaiementLegacy() {
        val biat = account("biat", "BIAT")
        val historical = PilotEntry(
            id = "sale-old",
            entrepriseId = org,
            type = PilotEntryType.SALE,
            date = LocalDate.of(2026, 3, 12),
            categoryId = "digital",
            amount = 10_000.0
        )
        val invoice = invoice("Client A", payment("p1", LocalDate.of(2026, 10, 2), 10_000.0, "biat"))
        val snap = snap(october, accounts = listOf(biat), invoices = listOf(invoice), pilot = listOf(historical))
        assertEquals(10_000.0, snap.inflows, 0.001)
        assertNull(snap.inflowLines.single().pilotEntryId)
    }

    @Test
    fun uneNouvelleVenteEstCompteeUneSeuleFois() {
        val biat = account("biat", "BIAT")
        val sale = PilotEntry(
            id = "sale-new",
            entrepriseId = org,
            type = PilotEntryType.SALE,
            date = LocalDate.of(2026, 10, 4),
            categoryId = "digital",
            amount = 10_000.0,
            treasuryDate = LocalDate.of(2026, 11, 15),
            bankAccountId = "biat"
        )
        val octoberSnap = snap(october, accounts = listOf(biat), pilot = listOf(sale))
        val novemberSnap = snap(november, accounts = listOf(biat), pilot = listOf(sale))
        assertEquals(0.0, octoberSnap.inflows, 0.001)
        assertEquals(10_000.0, novemberSnap.inflows, 0.001)
        assertEquals("sale-new", novemberSnap.inflowLines.single().pilotEntryId)
    }

    @Test
    fun unTransfertDeplaceLesComptesSansChangerLeTotal() {
        val biat = account("biat", "BIAT", opening = 1_000.0)
        val stb = account("stb", "STB")
        val transfer = PilotEntry(
            id = "move",
            entrepriseId = org,
            type = PilotEntryType.TRANSFER,
            date = LocalDate.of(2026, 10, 6),
            amount = 200.0,
            bankAccountId = "biat",
            counterAccountId = "stb"
        )
        val snap = snap(october, accounts = listOf(biat, stb), pilot = listOf(transfer))
        assertEquals(1_000.0, snap.opening, 0.001)
        assertEquals(200.0, snap.inflows, 0.001)
        assertEquals(200.0, snap.outflows, 0.001)
        assertEquals(1_000.0, snap.closing, 0.001)
        assertEquals(800.0, snap.accounts.first { it.accountId == "biat" }.closing, 0.001)
        assertEquals(200.0, snap.accounts.first { it.accountId == "stb" }.closing, 0.001)
        assertEquals("move", snap.inflowLines.single().pilotEntryId)
        assertEquals("move", snap.outflowLines.single().pilotEntryId)
    }

    @Test
    fun leSoldeInitialNEstPasUnEncaissementDuMois() {
        val biat = account("biat", "BIAT")
        val initial = correction("init", "biat", BalanceCorrectionType.INITIAL, 0.0, 5_000.0, LocalDate.of(2026, 1, 1))
        val revision = correction("rev", "biat", BalanceCorrectionType.OPENING_REVISION, 5_000.0, 5_000.0, LocalDate.of(2026, 2, 1))
        val snap = snap(october, accounts = listOf(biat), corrections = listOf(initial, revision))
        assertEquals(5_000.0, snap.opening, 0.001)
        assertEquals(0.0, snap.inflows, 0.001)
        assertEquals(5_000.0, snap.closing, 0.001)
    }

    @Test
    fun unSoldeDOuvertureDejaRenseigneNEstPasAdditionneAuSoldeInitial() {
        val biat = account("biat", "BIAT", opening = 1_000.0)
        val initial = correction("init", "biat", BalanceCorrectionType.INITIAL, 0.0, 5_000.0, LocalDate.of(2026, 1, 1))
        val snap = snap(october, accounts = listOf(biat), corrections = listOf(initial))
        assertEquals(1_000.0, snap.opening, 0.001)
        assertEquals(1_000.0, snap.closing, 0.001)
    }

    @Test
    fun uneCorrectionDateeChangeLeMois() {
        val biat = account("biat", "BIAT", opening = 1_000.0)
        val correction = correction("fix", "biat", BalanceCorrectionType.CORRECTION, 1_000.0, 1_080.0, LocalDate.of(2026, 10, 20), "Ajustement")
        val snap = snap(october, accounts = listOf(biat), corrections = listOf(correction))
        assertEquals(80.0, snap.inflows, 0.001)
        assertEquals(1_080.0, snap.closing, 0.001)
        assertNull(snap.inflowLines.single().pilotEntryId)
        assertEquals("Ajustement", snap.inflowLines.single().label)
    }

    @Test
    fun uneDepenseImpayeeEtUnDoublonNeComptentPas() {
        val biat = account("biat", "BIAT", opening = 500.0, default = true)
        val paid = expense("Loyer", 50.0, LocalDate.of(2026, 10, 2), "biat")
        val duplicate = paid.copy(id = "other-copy")
        val unpaid = expense("Fournisseur", 80.0, LocalDate.of(2026, 10, 3), "biat").copy(isPaid = false)
        val first = invoice("Client A", payment("p1", LocalDate.of(2026, 10, 4), 200.0, "biat"))
        val again = invoice(
            "Client A",
            payment("p2", LocalDate.of(2026, 10, 4), 200.0, "biat"),
            id = "invoice-2"
        )
        val snap = snap(
            october,
            accounts = listOf(biat),
            invoices = listOf(first, again),
            expenses = listOf(paid, duplicate, unpaid)
        )
        assertEquals(200.0, snap.inflows, 0.001)
        assertEquals(50.0, snap.outflows, 0.001)
        assertEquals(650.0, snap.closing, 0.001)
    }

    @Test
    fun lesDonneesDUneAutreEntrepriseRestentDehors() {
        val biat = account("biat", "BIAT", opening = 100.0)
        val foreign = invoice("Autre", payment("p1", LocalDate.of(2026, 10, 4), 900.0, "biat"), entrepriseId = other)
        val snap = snap(october, accounts = listOf(biat), invoices = listOf(foreign))
        assertEquals(100.0, snap.closing, 0.001)
        assertEquals(0.0, snap.inflows, 0.001)
    }

    private fun snap(
        month: YearMonth,
        accounts: List<BankAccount> = emptyList(),
        invoices: List<Invoice> = emptyList(),
        expenses: List<Expense> = emptyList(),
        pilot: List<PilotEntry> = emptyList(),
        corrections: List<BalanceCorrection> = emptyList()
    ) = TreasuryMonth.snapshot(month, org, accounts, invoices, expenses, pilot, corrections)

    private fun account(id: String, name: String, opening: Double = 0.0, default: Boolean = false) = BankAccount(
        id = id,
        entrepriseId = org,
        name = name,
        openingBalance = opening,
        isDefault = default
    )

    private fun invoice(
        client: String,
        payment: Payment,
        id: String = "invoice-1",
        entrepriseId: String = org
    ) = Invoice(
        id = id,
        invoiceNumber = "F-1",
        clientName = client,
        totalAmount = payment.amount,
        paidAmount = payment.amount,
        dueDate = payment.date,
        entrepriseId = entrepriseId,
        payments = listOf(payment.copy(invoiceId = id))
    )

    private fun payment(id: String, date: LocalDate, amount: Double, accountId: String) = Payment(
        id = id,
        invoiceId = "invoice-1",
        amount = amount,
        date = date,
        method = PaymentMethod.TRANSFER,
        bankAccountId = accountId
    )

    private fun expense(label: String, amount: Double, date: LocalDate, accountId: String) = Expense(
        label = label,
        amount = amount,
        date = date,
        isPaid = true,
        paymentMethod = PaymentMethod.TRANSFER,
        bankAccountId = accountId,
        entrepriseId = org
    )

    private fun correction(
        id: String,
        accountId: String,
        type: BalanceCorrectionType,
        oldBalance: Double,
        newBalance: Double,
        date: LocalDate,
        motif: String = ""
    ) = BalanceCorrection(
        id = id,
        entrepriseId = org,
        bankAccountId = accountId,
        type = type,
        oldBalance = oldBalance,
        newBalance = newBalance,
        correctionDate = date,
        motif = motif,
        userId = "u",
        userName = "Kam"
    )
}
