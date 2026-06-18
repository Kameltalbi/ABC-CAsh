package com.abccash.app.treasury.data

import java.time.LocalDateTime
import java.util.UUID

data class Entreprise(
    val id: String = UUID.randomUUID().toString(),
    val nom: String,
    val dateCreation: LocalDateTime = LocalDateTime.now(),
    val adminId: String? = null
)
