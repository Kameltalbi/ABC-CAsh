package com.abccash.app.treasury.data

import java.time.LocalDateTime
import java.util.UUID

data class User(
    val id: String = UUID.randomUUID().toString(),
    val nom: String,
    val email: String,
    val telephone: String,
    val passwordHash: String, // En production, utiliser un hash sécurisé
    val role: UserRole,
    val permissions: Set<UserPermission> = emptySet(),
    val entrepriseId: String,
    val dateInscription: LocalDateTime = LocalDateTime.now(),
    val isActive: Boolean = true
)
