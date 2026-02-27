package com.abccash.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

enum class CurrencyCode {
    DT, USD, EUR
}

enum class TransactionType {
    INCOME, EXPENSE
}

enum class TransactionStatus {
    PLANIFIEE, EN_RETARD, VALIDEE
}

enum class RecurrenceRule {
    NONE, WEEKLY, MONTHLY, QUARTERLY, FOUR_MONTHLY
}

@Entity(tableName = "balances", indices = [Index(value = ["currency"], unique = true)])
data class BalanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val currency: CurrencyCode,
    val amountMinor: Long,
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

@Entity(
    tableName = "transactions",
    indices = [
        Index("date"),
        Index("currency"),
        Index("status"),
        Index("parentRecurringId")
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val type: TransactionType,
    val amountMinor: Long,
    val currency: CurrencyCode,
    val date: LocalDate,
    val category: String,
    val status: TransactionStatus,
    val isRecurring: Boolean = false,
    val recurrenceRule: RecurrenceRule = RecurrenceRule.NONE,
    val parentRecurringId: Long? = null,
    val note: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name", "type"], unique = true)]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val type: TransactionType,
    val createdAt: LocalDateTime = LocalDateTime.now()
)
