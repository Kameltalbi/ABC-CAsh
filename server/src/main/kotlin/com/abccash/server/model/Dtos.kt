package com.abccash.server.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiError(val error: String)

@Serializable
data class RegisterRequest(
    val entrepriseNom: String,
    val nom: String,
    val email: String,
    val telephone: String,
    val password: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class UserDto(
    val id: String,
    val nom: String,
    val email: String,
    val telephone: String,
    val role: String,
    val permissions: List<String>,
    val entrepriseId: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val user: UserDto,
    val entrepriseId: String
)

@Serializable
data class EntrepriseDto(
    val id: String,
    val nom: String,
    val email: String,
    val telephone: String,
    val adresse: String
)

@Serializable
data class PaymentDto(
    val id: String,
    val invoiceId: String,
    val amount: Double,
    val date: String,
    val method: String,
    val note: String
)

@Serializable
data class InvoiceDto(
    val id: String,
    val invoiceNumber: String,
    val clientName: String,
    val totalAmount: Double,
    val dueDate: String,
    val createdDate: String,
    val entrepriseId: String,
    val category: String,
    val categoryLabel: String,
    val payments: List<PaymentDto> = emptyList()
)

@Serializable
data class ExpenseDto(
    val id: String,
    val label: String,
    val amount: Double,
    val date: String,
    val isRecurring: Boolean,
    val recurrence: String? = null,
    val recurrenceEndDate: String? = null,
    val isPaid: Boolean,
    val paymentMethod: String? = null,
    val createdDate: String,
    val entrepriseId: String,
    val category: String,
    val categoryLabel: String
)

@Serializable
data class SyncPullResponse(
    val entreprise: EntrepriseDto,
    val users: List<UserDto>,
    val invoices: List<InvoiceDto>,
    val expenses: List<ExpenseDto>,
    val serverTime: String
)

@Serializable
data class UserPushDto(
    val id: String,
    val nom: String,
    val email: String,
    val telephone: String,
    val role: String,
    val permissions: List<String>,
    val entrepriseId: String,
    val passwordHash: String,
    val isActive: Boolean = true
)

@Serializable
data class SyncPushRequest(
    val invoices: List<InvoiceDto> = emptyList(),
    val expenses: List<ExpenseDto> = emptyList(),
    val users: List<UserPushDto> = emptyList(),
    val deletedUserIds: List<String> = emptyList()
)

@Serializable
data class SyncPushResponse(
    val ok: Boolean,
    val message: String
)

@Serializable
data class HealthResponse(
    val status: String,
    val service: String = "abc-cash-api",
    val version: String = "1.0.0"
)
