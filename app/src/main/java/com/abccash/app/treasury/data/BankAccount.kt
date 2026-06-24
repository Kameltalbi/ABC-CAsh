package com.abccash.app.treasury.data

import java.time.LocalDate
import java.util.UUID

data class BankAccount(
    val id: String = UUID.randomUUID().toString(),
    val entrepriseId: String = "",
    val name: String,
    val bankName: String = "",
    val ibanLast4: String = "",
    val openingBalance: Double = 0.0,
    val alertLowBalance: Double? = null,
    val isDefault: Boolean = false,
    val kind: TreasuryAccountKind = TreasuryAccountKind.BANK,
    val source: BankAccountSource = BankAccountSource.MANUAL,
    val createdDate: LocalDate = LocalDate.now()
)

data class BankAccountSummary(
    val account: BankAccount,
    val balance: Double,
    val movementCount: Int,
    val hasLowBalanceAlert: Boolean
)

enum class BankAccountMovementType {
    INCOME,
    EXPENSE
}

data class BankAccountMovement(
    val id: String,
    val date: LocalDate,
    val label: String,
    val amount: Double,
    val method: PaymentMethod,
    val type: BankAccountMovementType
)
