package com.abccash.app.treasury.data

import java.time.LocalDate
import java.util.UUID

data class Payment(
    val id: String = UUID.randomUUID().toString(),
    val invoiceId: String,
    val amount: Double,
    val date: LocalDate,
    val method: PaymentMethod,
    val note: String = ""
)
