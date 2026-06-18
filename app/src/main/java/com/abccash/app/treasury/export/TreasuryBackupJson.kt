package com.abccash.app.treasury.export

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
    val users: List<User>
)

object TreasuryBackupJson {
    const val CURRENT_VERSION = 1
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
        }.toString(2)
    }

    fun fromJson(json: String): TreasuryBackupData {
        val root = JSONObject(json)
        val version = root.getInt("version")
        if (version != CURRENT_VERSION) {
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
            users = users
        )
    }
}
