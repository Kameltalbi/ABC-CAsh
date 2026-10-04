package com.abccash.app.treasury.export

import com.abccash.app.treasury.data.Expense
import com.abccash.app.treasury.data.ExpenseRecurrence
import com.abccash.app.treasury.data.Invoice
import com.abccash.app.treasury.data.Payment
import com.abccash.app.treasury.data.PaymentMethod
import com.abccash.app.treasury.data.User
import com.abccash.app.treasury.data.UserPermission
import com.abccash.app.treasury.data.UserRole
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TreasuryCsvExporterTest {

    @Test
    fun export_includesInvoicesAndSummary() {
        val month = YearMonth.of(2026, 6)
        val invoices = listOf(
            Invoice(
                id = "inv-1",
                invoiceNumber = "F-001",
                clientName = "Client A",
                totalAmount = 1000.0,
                paidAmount = 500.0,
                dueDate = month.atDay(15),
                payments = listOf(
                    Payment(
                        invoiceId = "inv-1",
                        amount = 500.0,
                        date = month.atDay(10),
                        method = PaymentMethod.CASH
                    )
                )
            )
        )
        val expenses = listOf(
            Expense(
                label = "Loyer",
                amount = 200.0,
                date = month.atDay(1)
            )
        )

        val csv = TreasuryCsvExporter.export(invoices, expenses, month)

        assertTrue(csv.contains("F-001"))
        assertTrue(csv.contains("Client A"))
        assertTrue(csv.contains("Loyer"))
        assertTrue(csv.contains("encaissements;500.0"))
        assertTrue(csv.contains("depenses;200.0"))
        assertTrue(csv.contains("solde;300.0"))
    }
}

class TreasuryBackupJsonTest {

    @Test
    fun roundTrip_preservesData() {
        val backup = TreasuryBackupData(
            version = TreasuryBackupJson.CURRENT_VERSION,
            exportedAt = LocalDateTime.of(2026, 6, 17, 12, 0),
            entrepriseId = "ent-1",
            entrepriseNom = "ABC Cash",
            invoices = listOf(
                Invoice(
                    id = "inv-1",
                    invoiceNumber = "F-100",
                    clientName = "Client",
                    totalAmount = 300.0,
                    dueDate = LocalDate.of(2026, 6, 20),
                    entrepriseId = "ent-1",
                    payments = listOf(
                        Payment(
                            id = "pay-1",
                            invoiceId = "inv-1",
                            amount = 100.0,
                            date = LocalDate.of(2026, 6, 10),
                            method = PaymentMethod.TRANSFER
                        )
                    )
                )
            ),
            expenses = listOf(
                Expense(
                    id = "exp-1",
                    label = "Internet",
                    amount = 50.0,
                    date = LocalDate.of(2026, 6, 5),
                    isRecurring = true,
                    recurrence = ExpenseRecurrence.MONTHLY,
                    entrepriseId = "ent-1"
                )
            ),
            users = listOf(
                User(
                    id = "user-1",
                    nom = "Admin",
                    email = "admin@test.com",
                    telephone = "+21612345678",
                    passwordHash = "pbkdf2\$120000\$abc",
                    role = UserRole.ADMIN,
                    permissions = UserPermission.entries.toSet(),
                    entrepriseId = "ent-1",
                    dateInscription = LocalDateTime.of(2026, 1, 1, 0, 0)
                )
            )
        )

        val json = TreasuryBackupJson.toJson(backup)
        val restored = TreasuryBackupJson.fromJson(json)

        assertEquals(backup.entrepriseId, restored.entrepriseId)
        assertEquals(1, restored.invoices.size)
        assertEquals("F-100", restored.invoices.first().invoiceNumber)
        assertEquals(1, restored.invoices.first().payments.size)
        assertEquals(1, restored.expenses.size)
        assertEquals("Internet", restored.expenses.first().label)
        assertEquals(1, restored.users.size)
        assertTrue(restored.users.first().permissions.contains(UserPermission.MANAGE_USERS))
    }

    @Test
    fun version1_resteLisibleSansInventerLePilotage() {
        val json = """
            {
              "version": 1,
              "exportedAt": "2026-06-17T12:00:00",
              "entrepriseId": "ent-1",
              "entrepriseNom": "ABC Cash",
              "invoices": [],
              "expenses": [],
              "users": [],
              "pilotEntries": [
                {
                  "id": "sale-old",
                  "treasuryDate": "2026-01-31",
                  "bankAccountId": "biat"
                }
              ]
            }
        """.trimIndent()

        val restored = TreasuryBackupJson.fromJson(json)

        assertEquals(1, restored.version)
        assertFalse(TreasuryBackupJson.replacesPilotTables(restored.version))
        assertTrue(restored.pilotEntries.isEmpty())
        assertTrue(restored.pilotCategories.isEmpty())
        assertTrue(restored.bankAccounts.isEmpty())
        assertTrue(restored.balanceCorrections.isEmpty())
    }

    @Test
    fun version2_restaureLesNouveauxChampsSansLesInventer() {
        val created = LocalDateTime.of(2026, 10, 4, 9, 0)
        val backup = TreasuryBackupData(
            version = 2,
            exportedAt = created,
            entrepriseId = "ent-1",
            entrepriseNom = "ABC Cash",
            invoices = emptyList(),
            expenses = emptyList(),
            users = emptyList(),
            pilotCategories = listOf(
                com.abccash.app.treasury.data.PilotCategory(
                    id = "cat-1",
                    entrepriseId = "ent-1",
                    type = com.abccash.app.treasury.data.PilotEntryType.SALE,
                    name = "Abonnements",
                    colorIndex = 2,
                    createdAt = created,
                    updatedAt = created
                )
            ),
            pilotEntries = listOf(
                com.abccash.app.treasury.data.PilotEntry(
                    id = "sale-old",
                    entrepriseId = "ent-1",
                    type = com.abccash.app.treasury.data.PilotEntryType.SALE,
                    date = LocalDate.of(2026, 1, 12),
                    categoryId = "cat-1",
                    amount = 4200.0,
                    createdAt = created,
                    updatedAt = created
                ),
                com.abccash.app.treasury.data.PilotEntry(
                    id = "sale-new",
                    entrepriseId = "ent-1",
                    type = com.abccash.app.treasury.data.PilotEntryType.SALE,
                    date = LocalDate.of(2026, 10, 4),
                    categoryId = "cat-1",
                    amount = 10000.0,
                    treasuryDate = LocalDate.of(2026, 11, 15),
                    bankAccountId = "biat",
                    createdAt = created,
                    updatedAt = created
                ),
                com.abccash.app.treasury.data.PilotEntry(
                    id = "move-1",
                    entrepriseId = "ent-1",
                    type = com.abccash.app.treasury.data.PilotEntryType.TRANSFER,
                    date = LocalDate.of(2026, 10, 9),
                    amount = 1000.0,
                    bankAccountId = "biat",
                    counterAccountId = "stb",
                    createdAt = created,
                    updatedAt = created
                )
            ),
            pilotTargets = listOf(
                com.abccash.app.treasury.data.PilotMonthlyTarget(
                    id = "target-1",
                    entrepriseId = "ent-1",
                    year = 2026,
                    month = 10,
                    salesTarget = 20000.0,
                    createdAt = created,
                    updatedAt = created
                )
            ),
            pilotImports = listOf(
                com.abccash.app.treasury.data.PilotImportRecord(
                    id = "import-1",
                    entrepriseId = "ent-1",
                    type = com.abccash.app.treasury.data.PilotEntryType.SALE,
                    filename = "Factures_2026_Pilotage_par_mois_CORRIGE.xlsx",
                    importedRows = 12,
                    ignoredRows = 0,
                    createdAt = created
                )
            ),
            bankAccounts = listOf(
                com.abccash.app.treasury.data.BankAccount(
                    id = "biat",
                    entrepriseId = "ent-1",
                    name = "BIAT",
                    isDefault = true,
                    openingBalance = 1500.0,
                    createdDate = LocalDate.of(2026, 1, 1)
                )
            ),
            balanceCorrections = listOf(
                com.abccash.app.treasury.data.BalanceCorrection(
                    id = "corr-1",
                    entrepriseId = "ent-1",
                    bankAccountId = "biat",
                    type = com.abccash.app.treasury.data.BalanceCorrectionType.CORRECTION,
                    oldBalance = 1500.0,
                    newBalance = 1800.0,
                    correctionDate = LocalDate.of(2026, 3, 1),
                    motif = "Relevé",
                    userId = "user-1",
                    userName = "Admin",
                    createdAt = LocalDate.of(2026, 3, 1)
                )
            )
        )

        val restored = TreasuryBackupJson.fromJson(TreasuryBackupJson.toJson(backup))
        val historical = restored.pilotEntries.first { it.id == "sale-old" }
        val fresh = restored.pilotEntries.first { it.id == "sale-new" }
        val transfer = restored.pilotEntries.first { it.id == "move-1" }

        assertTrue(TreasuryBackupJson.replacesPilotTables(restored.version))
        assertNull(historical.treasuryDate)
        assertNull(historical.bankAccountId)
        assertEquals(LocalDate.of(2026, 11, 15), fresh.treasuryDate)
        assertEquals("biat", fresh.bankAccountId)
        assertNull(transfer.categoryId)
        assertEquals("stb", transfer.counterAccountId)
        assertEquals(2, restored.pilotCategories.first().colorIndex)
        assertEquals("Factures_2026_Pilotage_par_mois_CORRIGE.xlsx", restored.pilotImports.first().filename)
        assertEquals(20_000.0, restored.pilotTargets.first().salesTarget, 0.001)
        assertEquals(1500.0, restored.bankAccounts.first().openingBalance, 0.001)
        assertEquals(1800.0, restored.balanceCorrections.first().newBalance, 0.001)
    }
}
