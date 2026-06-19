package com.abccash.server.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.timestamp

object Entreprises : Table("entreprises") {
    val id = text("id")
    val nom = text("nom")
    val email = text("email")
    val telephone = text("telephone")
    val adresse = text("adresse")
    val dateCreation = timestamp("date_creation")
    val adminId = text("admin_id").nullable()
    val updatedAt = timestamp("updated_at")
    override val primaryKey = PrimaryKey(id)
}

object Users : Table("users") {
    val id = text("id")
    val entrepriseId = text("entreprise_id")
    val nom = text("nom")
    val email = text("email")
    val telephone = text("telephone")
    val passwordHash = text("password_hash")
    val role = text("role")
    val permissions = text("permissions")
    val dateInscription = timestamp("date_inscription")
    val isActive = bool("is_active")
    val updatedAt = timestamp("updated_at")
    override val primaryKey = PrimaryKey(id)
}

object Invoices : Table("invoices") {
    val id = text("id")
    val entrepriseId = text("entreprise_id")
    val invoiceNumber = text("invoice_number")
    val clientName = text("client_name")
    val totalAmount = double("total_amount")
    val dueDate = date("due_date")
    val createdDate = date("created_date")
    val category = text("category")
    val categoryLabel = text("category_label")
    val updatedAt = timestamp("updated_at")
    override val primaryKey = PrimaryKey(id)
}

object Payments : Table("payments") {
    val id = text("id")
    val invoiceId = text("invoice_id")
    val amount = double("amount")
    val date = date("date")
    val method = text("method")
    val note = text("note")
    val updatedAt = timestamp("updated_at")
    override val primaryKey = PrimaryKey(id)
}

object Expenses : Table("expenses") {
    val id = text("id")
    val entrepriseId = text("entreprise_id")
    val label = text("label")
    val amount = double("amount")
    val date = date("date")
    val isRecurring = bool("is_recurring")
    val recurrence = text("recurrence").nullable()
    val recurrenceEndDate = date("recurrence_end_date").nullable()
    val isPaid = bool("is_paid")
    val paymentMethod = text("payment_method").nullable()
    val createdDate = date("created_date")
    val category = text("category")
    val categoryLabel = text("category_label")
    val updatedAt = timestamp("updated_at")
    override val primaryKey = PrimaryKey(id)
}
