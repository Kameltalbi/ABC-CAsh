package com.abccash.app.treasury.data

enum class TransactionType(val route: String, val title: String) {
    INCOME("income", "Nouvel encaissement"),
    EXPENSE("expense", "Nouvelle dépense");

    companion object {
        fun fromRoute(value: String?): TransactionType? =
            entries.find { it.route == value }
    }
}
