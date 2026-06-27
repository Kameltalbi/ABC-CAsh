package com.abccash.app.treasury.data

import java.time.LocalDate
import java.util.UUID

enum class BalanceCorrectionType {
    INITIAL,     // Solde initial saisi au démarrage
    CORRECTION   // Correction manuelle ultérieure
}

data class BalanceCorrection(
    val id: String = UUID.randomUUID().toString(),
    val entrepriseId: String,
    val bankAccountId: String,
    val type: BalanceCorrectionType,
    val oldBalance: Double,
    val newBalance: Double,
    val correctionDate: LocalDate,
    val motif: String,
    val userId: String,
    val userName: String,
    val createdAt: LocalDate = LocalDate.now()
) {
    val ecart: Double get() = newBalance - oldBalance
}
