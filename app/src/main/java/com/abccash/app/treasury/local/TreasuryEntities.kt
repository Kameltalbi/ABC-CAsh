package com.abccash.app.treasury.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.abccash.app.treasury.data.BankAccountSource
import com.abccash.app.treasury.data.TreasuryAccountKind
import com.abccash.app.treasury.data.ContactType
import com.abccash.app.treasury.data.TaxIdType
import com.abccash.app.treasury.data.TaxIdValidationStatus
import com.abccash.app.treasury.data.ExpenseCategory
import com.abccash.app.treasury.data.ExpenseRecurrence
import com.abccash.app.treasury.data.InvoiceDocumentStatus
import com.abccash.app.treasury.data.OtherTaxMode
import com.abccash.app.treasury.data.QuoteStatus
import com.abccash.app.treasury.data.PaymentMethod
import com.abccash.app.treasury.data.RevenueCategory
import com.abccash.app.treasury.data.UserPermission
import com.abccash.app.treasury.data.UserRole
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "entreprises")
data class EntrepriseEntity(
    @PrimaryKey val id: String,
    val nom: String,
    @ColumnInfo(defaultValue = "''")
    val email: String = "",
    @ColumnInfo(defaultValue = "''")
    val telephone: String = "",
    @ColumnInfo(defaultValue = "''")
    val adresse: String = "",
    val dateCreation: LocalDateTime,
    val adminId: String?
)

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["email"], unique = true),
        Index(value = ["telephone"], unique = true),
        Index(value = ["entrepriseId"])
    ]
)
data class UserEntity(
    @PrimaryKey val id: String,
    val nom: String,
    val email: String,
    val telephone: String,
    val passwordHash: String,
    val role: UserRole,
    val permissions: Set<UserPermission>,
    val entrepriseId: String,
    val dateInscription: LocalDateTime,
    val isActive: Boolean
)

@Entity(
    tableName = "invoices",
    indices = [Index(value = ["entrepriseId"])]
)
data class InvoiceEntity(
    @PrimaryKey val id: String,
    val invoiceNumber: String,
    val clientName: String,
    @ColumnInfo(defaultValue = "NULL")
    val clientContactId: String? = null,
    val totalAmount: Double,
    val dueDate: LocalDate,
    val createdDate: LocalDate,
    @ColumnInfo(defaultValue = "''")
    val entrepriseId: String,
    @ColumnInfo(defaultValue = "'OTHER'")
    val category: RevenueCategory = RevenueCategory.OTHER,
    @ColumnInfo(defaultValue = "''")
    val categoryLabel: String = "",
    @ColumnInfo(defaultValue = "'VALIDATED'")
    val documentStatus: InvoiceDocumentStatus = InvoiceDocumentStatus.VALIDATED,
    val amountExclTax: Double? = null,
    @ColumnInfo(defaultValue = "0")
    val tvaRate: Double = 0.0,
    @ColumnInfo(defaultValue = "0")
    val otherTaxRate: Double = 0.0,
    @ColumnInfo(defaultValue = "'PERCENTAGE'")
    val otherTaxMode: OtherTaxMode = OtherTaxMode.PERCENTAGE,
    @ColumnInfo(defaultValue = "''")
    val otherTaxLabel: String = "",
    @ColumnInfo(defaultValue = "'[]'")
    val lineItemsJson: String = "[]"
)

@Entity(
    tableName = "quotes",
    indices = [Index(value = ["entrepriseId"])]
)
data class QuoteEntity(
    @PrimaryKey val id: String,
    val quoteNumber: String,
    val clientName: String,
    @ColumnInfo(defaultValue = "NULL")
    val clientContactId: String? = null,
    val totalAmount: Double,
    val issueDate: LocalDate,
    val validUntil: LocalDate,
    val createdDate: LocalDate,
    @ColumnInfo(defaultValue = "''")
    val entrepriseId: String,
    @ColumnInfo(defaultValue = "'OTHER'")
    val category: RevenueCategory = RevenueCategory.OTHER,
    @ColumnInfo(defaultValue = "''")
    val categoryLabel: String = "",
    @ColumnInfo(defaultValue = "'DRAFT'")
    val status: QuoteStatus = QuoteStatus.DRAFT,
    val amountExclTax: Double? = null,
    @ColumnInfo(defaultValue = "0")
    val tvaRate: Double = 0.0,
    @ColumnInfo(defaultValue = "0")
    val otherTaxRate: Double = 0.0,
    @ColumnInfo(defaultValue = "'PERCENTAGE'")
    val otherTaxMode: OtherTaxMode = OtherTaxMode.PERCENTAGE,
    @ColumnInfo(defaultValue = "''")
    val otherTaxLabel: String = "",
    @ColumnInfo(defaultValue = "'[]'")
    val lineItemsJson: String = "[]",
    @ColumnInfo(defaultValue = "NULL")
    val convertedInvoiceId: String? = null,
    @ColumnInfo(defaultValue = "''")
    val notes: String = ""
)

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = InvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["invoiceId"])]
)
data class PaymentEntity(
    @PrimaryKey val id: String,
    val invoiceId: String,
    val amount: Double,
    val date: LocalDate,
    val method: PaymentMethod,
    val note: String,
    val bankAccountId: String? = null
)

@Entity(
    tableName = "bank_accounts",
    indices = [Index(value = ["entrepriseId"])]
)
data class BankAccountEntity(
    @PrimaryKey val id: String,
    val entrepriseId: String,
    val name: String,
    @ColumnInfo(defaultValue = "''")
    val bankName: String = "",
    @ColumnInfo(defaultValue = "''")
    val ibanLast4: String = "",
    @ColumnInfo(defaultValue = "0")
    val openingBalance: Double = 0.0,
    val alertLowBalance: Double? = null,
    @ColumnInfo(defaultValue = "0")
    val isDefault: Boolean = false,
    @ColumnInfo(defaultValue = "'BANK'")
    val kind: TreasuryAccountKind = TreasuryAccountKind.BANK,
    @ColumnInfo(defaultValue = "'MANUAL'")
    val source: BankAccountSource = BankAccountSource.MANUAL,
    val createdDate: LocalDate
)

@Entity(
    tableName = "contacts",
    indices = [Index(value = ["entrepriseId", "type"])]
)
data class ContactEntity(
    @PrimaryKey val id: String,
    val entrepriseId: String,
    val type: ContactType,
    val name: String,
    @ColumnInfo(defaultValue = "''")
    val email: String = "",
    @ColumnInfo(defaultValue = "''")
    val phone: String = "",
    @ColumnInfo(defaultValue = "''")
    val address: String = "",
    @ColumnInfo(defaultValue = "''")
    val notes: String = "",
    @ColumnInfo(defaultValue = "''")
    val countryCode: String = "",
    @ColumnInfo(defaultValue = "''")
    val legalName: String = "",
    @ColumnInfo(defaultValue = "NULL")
    val taxIdType: TaxIdType? = null,
    @ColumnInfo(defaultValue = "''")
    val taxIdValue: String = "",
    @ColumnInfo(defaultValue = "'UNVERIFIED'")
    val taxIdValidationStatus: TaxIdValidationStatus = TaxIdValidationStatus.UNVERIFIED,
    @ColumnInfo(defaultValue = "''")
    val addressLine1: String = "",
    @ColumnInfo(defaultValue = "''")
    val addressLine2: String = "",
    @ColumnInfo(defaultValue = "''")
    val postalCode: String = "",
    @ColumnInfo(defaultValue = "''")
    val city: String = "",
    val createdDate: LocalDate
)

@Entity(
    tableName = "expenses",
    indices = [Index(value = ["entrepriseId"])]
)
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val label: String,
    val amount: Double,
    val date: LocalDate,
    val isRecurring: Boolean,
    val recurrence: ExpenseRecurrence?,
    val recurrenceEndDate: LocalDate?,
    val isPaid: Boolean,
    val paymentMethod: PaymentMethod? = null,
    @ColumnInfo(defaultValue = "NULL")
    val bankAccountId: String? = null,
    val createdDate: LocalDate,
    @ColumnInfo(defaultValue = "''")
    val entrepriseId: String,
    @ColumnInfo(defaultValue = "'OTHER'")
    val category: ExpenseCategory = ExpenseCategory.OTHER,
    @ColumnInfo(defaultValue = "''")
    val categoryLabel: String = "",
    @ColumnInfo(defaultValue = "NULL")
    val supplierContactId: String? = null,
    @ColumnInfo(defaultValue = "''")
    val note: String = "",
    @ColumnInfo(defaultValue = "NULL")
    val receiptImagePath: String? = null,
    @ColumnInfo(defaultValue = "0")
    val isExpenseNote: Boolean = false
)
