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
}
