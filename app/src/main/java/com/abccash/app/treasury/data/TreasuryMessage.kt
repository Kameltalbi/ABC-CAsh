package com.abccash.app.treasury.data

data class ImportFeedback(
    val imported: Int,
    val skippedDuplicates: Int = 0
)

object TreasuryMessage {
    const val SESSION_EXPIRED = "SESSION_EXPIRED"
    const val SESSION_EXPIRED_RECONNECT = "SESSION_EXPIRED_RECONNECT"
    const val SESSION_INACTIVE = "SESSION_INACTIVE"
    const val CONNECT_GOOGLE = "CONNECT_GOOGLE"
    const val EXPORT_DATA_FAILED = "EXPORT_DATA_FAILED"
    const val GOOGLE_BACKUP_SUCCESS = "GOOGLE_BACKUP_SUCCESS"
    const val GOOGLE_DRIVE_ERROR = "GOOGLE_DRIVE_ERROR"
    const val GOOGLE_DELETE_BACKUP_FAILED = "GOOGLE_DELETE_BACKUP_FAILED"
    const val GOOGLE_NO_BACKUP = "GOOGLE_NO_BACKUP"
    const val GOOGLE_DATA_RESTORED = "GOOGLE_DATA_RESTORED"
    const val RESTORE_IMPOSSIBLE = "RESTORE_IMPOSSIBLE"
    const val INVOICE_NOT_FOUND = "INVOICE_NOT_FOUND"
    const val EXPENSE_NOT_FOUND = "EXPENSE_NOT_FOUND"
    const val PAYMENT_NOT_FOUND = "PAYMENT_NOT_FOUND"
    const val BACKUP_RESTORED_SUCCESS = "BACKUP_RESTORED_SUCCESS"
    const val ADMIN_ONLY_COLLECTION_ADJUSTMENT = "ADMIN_ONLY_COLLECTION_ADJUSTMENT"

    private const val INVALID_PAYMENT_AMOUNT_PREFIX = "INVALID_PAYMENT_AMOUNT|"

    fun invalidPaymentAmount(max: Double) = "$INVALID_PAYMENT_AMOUNT_PREFIX$max"

    fun parseInvalidPaymentAmountMax(code: String): Double? =
        if (code.startsWith(INVALID_PAYMENT_AMOUNT_PREFIX)) {
            code.removePrefix(INVALID_PAYMENT_AMOUNT_PREFIX).toDoubleOrNull()
        } else {
            null
        }
}
