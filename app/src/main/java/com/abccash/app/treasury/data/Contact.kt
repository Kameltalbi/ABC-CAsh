package com.abccash.app.treasury.data

import java.time.LocalDate
import java.util.UUID

enum class ContactType {
    CLIENT,
    SUPPLIER
}

data class Contact(
    val id: String = UUID.randomUUID().toString(),
    val entrepriseId: String = "",
    val type: ContactType,
    val name: String,
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val notes: String = "",
    val countryCode: String = "",
    val legalName: String = "",
    val taxIdType: TaxIdType? = null,
    val taxIdValue: String = "",
    val taxIdValidationStatus: TaxIdValidationStatus = TaxIdValidationStatus.UNVERIFIED,
    val addressLine1: String = "",
    val addressLine2: String = "",
    val postalCode: String = "",
    val city: String = "",
    val createdDate: LocalDate = LocalDate.now()
) {
    val displayName: String get() = name.ifBlank { legalName }
    val billingAddressFormatted: String get() = ClientTaxValidator.formatBillingAddress(
        addressLine1, addressLine2, postalCode, city, address
    )
}

data class ContactSummary(
    val contact: Contact,
    val transactionCount: Int,
    val totalAmount: Double
)
