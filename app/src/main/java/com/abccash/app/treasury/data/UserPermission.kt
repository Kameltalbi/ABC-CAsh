package com.abccash.app.treasury.data

enum class UserPermission(val label: String) {
    VIEW_INVOICES("Voir factures"),
    ADD_PAYMENTS("Ajouter paiements"),
    MANAGE_EXPENSES("Gérer dépenses"),
    VIEW_TREASURY("Voir trésorerie"),
    MANAGE_USERS("Gérer utilisateurs")
}
