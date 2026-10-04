package com.abccash.app.treasury.export

import com.abccash.app.treasury.data.BalanceCorrection
import com.abccash.app.treasury.data.BalanceCorrectionType
import com.abccash.app.treasury.data.BankAccount
import com.abccash.app.treasury.data.BankAccountSource
import com.abccash.app.treasury.data.PilotCategory
import com.abccash.app.treasury.data.PilotEntry
import com.abccash.app.treasury.data.PilotEntryType
import com.abccash.app.treasury.data.PilotImportRecord
import com.abccash.app.treasury.data.PilotMonthlyTarget
import com.abccash.app.treasury.data.Expense
import com.abccash.app.treasury.data.Invoice
import com.abccash.app.treasury.data.Payment
import com.abccash.app.treasury.data.User
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class TreasuryBackupData(
    val version: Int,
    val exportedAt: LocalDateTime,
    val entrepriseId: String,
    val entrepriseNom: String,
    val invoices: List<Invoice>,
    val expenses: List<Expense>,
    val users: List<User>,
    val pilotCategories: List<PilotCategory> = emptyList(),
    val pilotEntries: List<PilotEntry> = emptyList(),
    val pilotTargets: List<PilotMonthlyTarget> = emptyList(),
    val pilotImports: List<PilotImportRecord> = emptyList(),
    val bankAccounts: List<BankAccount> = emptyList(),
    val balanceCorrections: List<BalanceCorrection> = emptyList()
)

object TreasuryBackupJson {
    const val CURRENT_VERSION = 2
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    fun toJson(backup: TreasuryBackupData): String {
        return JSONObject().apply {
            put("version", backup.version)
            put("exportedAt", backup.exportedAt.format(dateTimeFormatter))
            put("entrepriseId", backup.entrepriseId)
            put("entrepriseNom", backup.entrepriseNom)
            put("invoices", JSONArray().apply {
                backup.invoices.forEach { invoice ->
                    put(JSONObject().apply {
                        put("id", invoice.id)
                        put("invoiceNumber", invoice.invoiceNumber)
                        put("clientName", invoice.clientName)
                        put("totalAmount", invoice.totalAmount)
                        put("dueDate", invoice.dueDate.format(dateFormatter))
                        put("createdDate", invoice.createdDate.format(dateFormatter))
                        put("entrepriseId", invoice.entrepriseId)
                        put("payments", JSONArray().apply {
                            invoice.payments.forEach { payment ->
                                put(JSONObject().apply {
                                    put("id", payment.id)
                                    put("invoiceId", payment.invoiceId)
                                    put("amount", payment.amount)
                                    put("date", payment.date.format(dateFormatter))
                                    put("method", payment.method.name)
                                    put("note", payment.note)
                                })
                            }
                        })
                    })
                }
            })
            put("expenses", JSONArray().apply {
                backup.expenses.forEach { expense ->
                    put(JSONObject().apply {
                        put("id", expense.id)
                        put("label", expense.label)
                        put("amount", expense.amount)
                        put("date", expense.date.format(dateFormatter))
                        put("isRecurring", expense.isRecurring)
                        put("recurrence", expense.recurrence?.name)
                        put("recurrenceEndDate", expense.recurrenceEndDate?.format(dateFormatter))
                        put("isPaid", expense.isPaid)
                        expense.paymentMethod?.let { put("paymentMethod", it.name) }
                        put("createdDate", expense.createdDate.format(dateFormatter))
                        put("entrepriseId", expense.entrepriseId)
                    })
                }
            })
            put("users", JSONArray().apply {
                backup.users.forEach { user ->
                    put(JSONObject().apply {
                        put("id", user.id)
                        put("nom", user.nom)
                        put("email", user.email)
                        put("telephone", user.telephone)
                        put("passwordHash", user.passwordHash)
                        put("role", user.role.name)
                        put("permissions", JSONArray(user.permissions.map { it.name }))
                        put("entrepriseId", user.entrepriseId)
                        put("dateInscription", user.dateInscription.format(dateTimeFormatter))
                        put("isActive", user.isActive)
                    })
                }
            })
            put("pilotCategories", categoriesJson(backup.pilotCategories))
            put("pilotEntries", entriesJson(backup.pilotEntries))
            put("pilotTargets", targetsJson(backup.pilotTargets))
            put("pilotImports", importsJson(backup.pilotImports))
            put("bankAccounts", accountsJson(backup.bankAccounts))
            put("balanceCorrections", correctionsJson(backup.balanceCorrections))
        }.toString(2)
    }

    fun fromJson(json: String): TreasuryBackupData {
        val root = JSONObject(json)
        val version = root.getInt("version")
        if (version != 1 && version != CURRENT_VERSION) {
            throw IllegalArgumentException("Version de sauvegarde non supportée: $version")
        }

        val invoices = root.getJSONArray("invoices").let { array ->
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                val payments = item.getJSONArray("payments").let { paymentsArray ->
                    List(paymentsArray.length()) { paymentIndex ->
                        val payment = paymentsArray.getJSONObject(paymentIndex)
                        Payment(
                            id = payment.getString("id"),
                            invoiceId = payment.getString("invoiceId"),
                            amount = payment.getDouble("amount"),
                            date = LocalDate.parse(payment.getString("date"), dateFormatter),
                            method = com.abccash.app.treasury.data.PaymentMethod.valueOf(payment.getString("method")),
                            note = payment.optString("note", "")
                        )
                    }
                }
                Invoice(
                    id = item.getString("id"),
                    invoiceNumber = item.getString("invoiceNumber"),
                    clientName = item.getString("clientName"),
                    totalAmount = item.getDouble("totalAmount"),
                    paidAmount = payments.sumOf { it.amount },
                    dueDate = LocalDate.parse(item.getString("dueDate"), dateFormatter),
                    createdDate = LocalDate.parse(item.getString("createdDate"), dateFormatter),
                    entrepriseId = item.getString("entrepriseId"),
                    payments = payments
                )
            }
        }

        val expenses = root.getJSONArray("expenses").let { array ->
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                Expense(
                    id = item.getString("id"),
                    label = item.getString("label"),
                    amount = item.getDouble("amount"),
                    date = LocalDate.parse(item.getString("date"), dateFormatter),
                    isRecurring = item.getBoolean("isRecurring"),
                    recurrence = item.optString("recurrence").takeIf { it.isNotBlank() }
                        ?.let { com.abccash.app.treasury.data.ExpenseRecurrence.valueOf(it) },
                    recurrenceEndDate = item.optString("recurrenceEndDate").takeIf { it.isNotBlank() }
                        ?.let { LocalDate.parse(it, dateFormatter) },
                    isPaid = item.getBoolean("isPaid"),
                    paymentMethod = item.optString("paymentMethod").takeIf { it.isNotBlank() }
                        ?.let { com.abccash.app.treasury.data.PaymentMethod.valueOf(it) },
                    createdDate = LocalDate.parse(item.getString("createdDate"), dateFormatter),
                    entrepriseId = item.getString("entrepriseId")
                )
            }
        }

        val users = root.getJSONArray("users").let { array ->
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                val permissions = item.getJSONArray("permissions").let { permissionsArray ->
                    buildSet {
                        for (i in 0 until permissionsArray.length()) {
                            add(com.abccash.app.treasury.data.UserPermission.valueOf(permissionsArray.getString(i)))
                        }
                    }
                }
                User(
                    id = item.getString("id"),
                    nom = item.getString("nom"),
                    email = item.getString("email"),
                    telephone = item.getString("telephone"),
                    passwordHash = item.getString("passwordHash"),
                    role = com.abccash.app.treasury.data.UserRole.valueOf(item.getString("role")),
                    permissions = permissions,
                    entrepriseId = item.getString("entrepriseId"),
                    dateInscription = LocalDateTime.parse(item.getString("dateInscription"), dateTimeFormatter),
                    isActive = item.getBoolean("isActive")
                )
            }
        }

        return TreasuryBackupData(
            version = version,
            exportedAt = LocalDateTime.parse(root.getString("exportedAt"), dateTimeFormatter),
            entrepriseId = root.getString("entrepriseId"),
            entrepriseNom = root.getString("entrepriseNom"),
            invoices = invoices,
            expenses = expenses,
            users = users,
            pilotCategories = if (version >= 2) categories(root) else emptyList(),
            pilotEntries = if (version >= 2) entries(root) else emptyList(),
            pilotTargets = if (version >= 2) targets(root) else emptyList(),
            pilotImports = if (version >= 2) imports(root) else emptyList(),
            bankAccounts = if (version >= 2) accounts(root) else emptyList(),
            balanceCorrections = if (version >= 2) corrections(root) else emptyList()
        )
    }

    fun replacesPilotTables(version: Int): Boolean = version >= 2

    private fun categoriesJson(categories: List<PilotCategory>) = JSONArray().apply {
        categories.forEach { category ->
            put(JSONObject().apply {
                put("id", category.id)
                put("entrepriseId", category.entrepriseId)
                put("type", category.type.name)
                put("name", category.name)
                put("active", category.active)
                put("colorIndex", category.colorIndex)
                put("createdAt", category.createdAt.format(dateTimeFormatter))
                put("updatedAt", category.updatedAt.format(dateTimeFormatter))
            })
        }
    }

    private fun entriesJson(entries: List<PilotEntry>) = JSONArray().apply {
        entries.forEach { entry ->
            put(JSONObject().apply {
                put("id", entry.id)
                put("entrepriseId", entry.entrepriseId)
                put("type", entry.type.name)
                put("date", entry.date.format(dateFormatter))
                put("categoryId", entry.categoryId ?: JSONObject.NULL)
                put("amount", entry.amount)
                put("note", entry.note)
                put("recurring", entry.recurring)
                put("recurrenceMonths", entry.recurrenceMonths)
                put("treasuryDate", entry.treasuryDate?.format(dateFormatter) ?: JSONObject.NULL)
                put("bankAccountId", entry.bankAccountId ?: JSONObject.NULL)
                put("counterAccountId", entry.counterAccountId ?: JSONObject.NULL)
                put("importId", entry.importId ?: JSONObject.NULL)
                put("createdAt", entry.createdAt.format(dateTimeFormatter))
                put("updatedAt", entry.updatedAt.format(dateTimeFormatter))
            })
        }
    }

    private fun targetsJson(targets: List<PilotMonthlyTarget>) = JSONArray().apply {
        targets.forEach { target ->
            put(JSONObject().apply {
                put("id", target.id)
                put("entrepriseId", target.entrepriseId)
                put("year", target.year)
                put("month", target.month)
                put("salesTarget", target.salesTarget)
                put("createdAt", target.createdAt.format(dateTimeFormatter))
                put("updatedAt", target.updatedAt.format(dateTimeFormatter))
            })
        }
    }

    private fun importsJson(imports: List<PilotImportRecord>) = JSONArray().apply {
        imports.forEach { record ->
            put(JSONObject().apply {
                put("id", record.id)
                put("entrepriseId", record.entrepriseId)
                put("type", record.type.name)
                put("filename", record.filename)
                put("importedRows", record.importedRows)
                put("ignoredRows", record.ignoredRows)
                put("createdAt", record.createdAt.format(dateTimeFormatter))
            })
        }
    }

    private fun accountsJson(accounts: List<BankAccount>) = JSONArray().apply {
        accounts.forEach { account ->
            put(JSONObject().apply {
                put("id", account.id)
                put("entrepriseId", account.entrepriseId)
                put("name", account.name)
                put("bankName", account.bankName)
                put("ibanLast4", account.ibanLast4)
                put("openingBalance", account.openingBalance)
                put("alertLowBalance", account.alertLowBalance ?: JSONObject.NULL)
                put("isDefault", account.isDefault)
                put("kind", account.kind.name)
                put("source", account.source.name)
                put("createdDate", account.createdDate.format(dateFormatter))
            })
        }
    }

    private fun correctionsJson(corrections: List<BalanceCorrection>) = JSONArray().apply {
        corrections.forEach { correction ->
            put(JSONObject().apply {
                put("id", correction.id)
                put("entrepriseId", correction.entrepriseId)
                put("bankAccountId", correction.bankAccountId)
                put("type", correction.type.name)
                put("oldBalance", correction.oldBalance)
                put("newBalance", correction.newBalance)
                put("correctionDate", correction.correctionDate.format(dateFormatter))
                put("motif", correction.motif)
                put("userId", correction.userId)
                put("userName", correction.userName)
                put("createdAt", correction.createdAt.format(dateFormatter))
            })
        }
    }

    private fun categories(root: JSONObject) = root.array("pilotCategories").mapObjects { item ->
        PilotCategory(
            id = item.getString("id"),
            entrepriseId = item.getString("entrepriseId"),
            type = PilotEntryType.valueOf(item.getString("type")),
            name = item.getString("name"),
            active = item.optBoolean("active", true),
            colorIndex = item.optInt("colorIndex", 0),
            createdAt = LocalDateTime.parse(item.getString("createdAt"), dateTimeFormatter),
            updatedAt = LocalDateTime.parse(item.getString("updatedAt"), dateTimeFormatter)
        )
    }

    private fun entries(root: JSONObject) = root.array("pilotEntries").mapObjects { item ->
        val type = PilotEntryType.fromStored(item.getString("type"))
            ?: throw IllegalArgumentException("Nature Pilotage inconnue")
        PilotEntry(
            id = item.getString("id"),
            entrepriseId = item.getString("entrepriseId"),
            type = type,
            date = LocalDate.parse(item.getString("date"), dateFormatter),
            categoryId = item.optText("categoryId"),
            amount = item.getDouble("amount"),
            note = item.optString("note", ""),
            recurring = item.optBoolean("recurring", false),
            recurrenceMonths = item.optInt("recurrenceMonths", 0),
            treasuryDate = item.optDate("treasuryDate"),
            bankAccountId = item.optText("bankAccountId"),
            counterAccountId = item.optText("counterAccountId"),
            importId = item.optText("importId"),
            createdAt = LocalDateTime.parse(item.getString("createdAt"), dateTimeFormatter),
            updatedAt = LocalDateTime.parse(item.getString("updatedAt"), dateTimeFormatter)
        )
    }

    private fun targets(root: JSONObject) = root.array("pilotTargets").mapObjects { item ->
        PilotMonthlyTarget(
            id = item.getString("id"),
            entrepriseId = item.getString("entrepriseId"),
            year = item.getInt("year"),
            month = item.getInt("month"),
            salesTarget = item.getDouble("salesTarget"),
            createdAt = LocalDateTime.parse(item.getString("createdAt"), dateTimeFormatter),
            updatedAt = LocalDateTime.parse(item.getString("updatedAt"), dateTimeFormatter)
        )
    }

    private fun imports(root: JSONObject) = root.array("pilotImports").mapObjects { item ->
        PilotImportRecord(
            id = item.getString("id"),
            entrepriseId = item.getString("entrepriseId"),
            type = PilotEntryType.valueOf(item.getString("type")),
            filename = item.getString("filename"),
            importedRows = item.getInt("importedRows"),
            ignoredRows = item.getInt("ignoredRows"),
            createdAt = LocalDateTime.parse(item.getString("createdAt"), dateTimeFormatter)
        )
    }

    private fun accounts(root: JSONObject) = root.array("bankAccounts").mapObjects { item ->
        BankAccount(
            id = item.getString("id"),
            entrepriseId = item.getString("entrepriseId"),
            name = item.getString("name"),
            bankName = item.optString("bankName", ""),
            ibanLast4 = item.optString("ibanLast4", ""),
            openingBalance = item.optDouble("openingBalance", 0.0),
            alertLowBalance = if (!item.has("alertLowBalance") || item.isNull("alertLowBalance")) {
                null
            } else {
                item.getDouble("alertLowBalance")
            },
            isDefault = item.optBoolean("isDefault", false),
            kind = com.abccash.app.treasury.data.TreasuryAccountKind.valueOf(item.optString("kind", "BANK")),
            source = BankAccountSource.valueOf(item.optString("source", "MANUAL")),
            createdDate = LocalDate.parse(item.getString("createdDate"), dateFormatter)
        )
    }

    private fun corrections(root: JSONObject) = root.array("balanceCorrections").mapObjects { item ->
        BalanceCorrection(
            id = item.getString("id"),
            entrepriseId = item.getString("entrepriseId"),
            bankAccountId = item.getString("bankAccountId"),
            type = BalanceCorrectionType.valueOf(item.getString("type")),
            oldBalance = item.getDouble("oldBalance"),
            newBalance = item.getDouble("newBalance"),
            correctionDate = LocalDate.parse(item.getString("correctionDate"), dateFormatter),
            motif = item.getString("motif"),
            userId = item.getString("userId"),
            userName = item.getString("userName"),
            createdAt = LocalDate.parse(item.getString("createdAt"), dateFormatter)
        )
    }

    private fun JSONObject.array(name: String): JSONArray =
        if (has(name) && !isNull(name)) getJSONArray(name) else JSONArray()

    private fun JSONObject.optText(name: String): String? =
        if (!has(name) || isNull(name)) null else optString(name).takeIf { it.isNotBlank() }

    private fun JSONObject.optDate(name: String): LocalDate? =
        optText(name)?.let { LocalDate.parse(it, dateFormatter) }

    private fun <T> JSONArray.mapObjects(read: (JSONObject) -> T): List<T> =
        List(length()) { index -> read(getJSONObject(index)) }
}
