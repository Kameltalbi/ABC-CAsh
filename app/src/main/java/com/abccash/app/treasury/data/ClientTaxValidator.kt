package com.abccash.app.treasury.data

object ClientTaxValidator {

    /** Accepts any non-blank tax ID — format is not enforced (free entry). */
    fun validate(countryCode: String, type: TaxIdType?, value: String): TaxIdValidationResult {
        if (type == null || type == TaxIdType.NONE || value.trim().isBlank()) {
            return TaxIdValidationResult(TaxIdValidationStatus.UNVERIFIED, null)
        }
        return TaxIdValidationResult(TaxIdValidationStatus.UNVERIFIED, null)
    }

    fun formatBillingAddress(
        line1: String,
        line2: String,
        postalCode: String,
        city: String,
        legacyAddress: String
    ): String {
        val parts = listOf(line1, line2, listOf(postalCode, city).filter { it.isNotBlank() }.joinToString(" "))
            .map { it.trim() }
            .filter { it.isNotBlank() }
        return when {
            parts.isNotEmpty() -> parts.joinToString("\n")
            legacyAddress.isNotBlank() -> legacyAddress.trim()
            else -> ""
        }
    }
}

data class TaxIdValidationResult(
    val status: TaxIdValidationStatus,
    val failedType: TaxIdType?
)
