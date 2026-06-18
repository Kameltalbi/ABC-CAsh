package com.abccash.app.treasury.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.abccash.app.treasury.data.ExpenseCategory
import com.abccash.app.treasury.data.ExpenseRecurrence
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
    val totalAmount: Double,
    val dueDate: LocalDate,
    val createdDate: LocalDate,
    @ColumnInfo(defaultValue = "''")
    val entrepriseId: String,
    @ColumnInfo(defaultValue = "'OTHER'")
    val category: RevenueCategory = RevenueCategory.OTHER,
    @ColumnInfo(defaultValue = "''")
    val categoryLabel: String = ""
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
    val note: String
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
    val createdDate: LocalDate,
    @ColumnInfo(defaultValue = "''")
    val entrepriseId: String,
    @ColumnInfo(defaultValue = "'OTHER'")
    val category: ExpenseCategory = ExpenseCategory.OTHER,
    @ColumnInfo(defaultValue = "''")
    val categoryLabel: String = ""
)
