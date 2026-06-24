package com.abccash.app.treasury.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.abccash.app.R
import com.abccash.app.treasury.data.ImportFeedback
import com.abccash.app.treasury.data.TreasuryMessage
import com.abccash.app.treasury.repository.TreasuryRepository

fun Context.resolveTreasuryMessage(code: String?): String? {
    if (code == null) return null
    TreasuryMessage.parseInvalidPaymentAmountMax(code)?.let { max ->
        return getString(R.string.invalid_payment_amount_max, max.toString())
    }
    return when (code) {
        TreasuryMessage.SESSION_EXPIRED,
        TreasuryMessage.SESSION_EXPIRED_RECONNECT -> getString(R.string.session_expired)
        TreasuryMessage.SESSION_INACTIVE -> getString(R.string.session_inactive)
        TreasuryMessage.CONNECT_GOOGLE -> getString(R.string.connect_google_account)
        TreasuryMessage.EXPORT_DATA_FAILED -> getString(R.string.export_data_failed)
        TreasuryMessage.GOOGLE_BACKUP_SUCCESS -> getString(R.string.google_backup_success)
        TreasuryMessage.GOOGLE_DRIVE_ERROR -> getString(R.string.google_drive_error)
        TreasuryMessage.GOOGLE_DELETE_BACKUP_FAILED -> getString(R.string.google_delete_backup_failed)
        TreasuryMessage.GOOGLE_NO_BACKUP -> getString(R.string.google_no_backup_found)
        TreasuryMessage.GOOGLE_DATA_RESTORED -> getString(R.string.google_data_restored)
        TreasuryMessage.RESTORE_IMPOSSIBLE -> getString(R.string.google_restore_failed)
        TreasuryMessage.INVOICE_NOT_FOUND -> getString(R.string.invoice_not_found)
        TreasuryMessage.EXPENSE_NOT_FOUND -> getString(R.string.expense_not_found)
        TreasuryMessage.PAYMENT_NOT_FOUND -> getString(R.string.payment_not_found)
        TreasuryMessage.BACKUP_RESTORED_SUCCESS -> getString(R.string.backup_restored_success)
        TreasuryMessage.ADMIN_ONLY_COLLECTION_ADJUSTMENT ->
            getString(R.string.admin_only_collection_adjustment)
        TreasuryRepository.SUBSCRIPTION_LIMIT_REACHED -> getString(R.string.subscription_limit_reached)
        TreasuryRepository.ACCOUNT_LIMIT_REACHED -> getString(R.string.treasury_accounts_limit_reached)
        else -> code
    }
}

@Composable
fun resolveTreasuryMessage(code: String?): String? {
    if (code == null) return null
    TreasuryMessage.parseInvalidPaymentAmountMax(code)?.let { max ->
        val formatAmount = rememberFormatMoney()
        return stringResource(R.string.invalid_payment_amount_max, formatAmount(max))
    }
    return when (code) {
        TreasuryMessage.SESSION_EXPIRED,
        TreasuryMessage.SESSION_EXPIRED_RECONNECT -> stringResource(R.string.session_expired)
        TreasuryMessage.SESSION_INACTIVE -> stringResource(R.string.session_inactive)
        TreasuryMessage.CONNECT_GOOGLE -> stringResource(R.string.connect_google_account)
        TreasuryMessage.EXPORT_DATA_FAILED -> stringResource(R.string.export_data_failed)
        TreasuryMessage.GOOGLE_BACKUP_SUCCESS -> stringResource(R.string.google_backup_success)
        TreasuryMessage.GOOGLE_DRIVE_ERROR -> stringResource(R.string.google_drive_error)
        TreasuryMessage.GOOGLE_DELETE_BACKUP_FAILED -> stringResource(R.string.google_delete_backup_failed)
        TreasuryMessage.GOOGLE_NO_BACKUP -> stringResource(R.string.google_no_backup_found)
        TreasuryMessage.GOOGLE_DATA_RESTORED -> stringResource(R.string.google_data_restored)
        TreasuryMessage.RESTORE_IMPOSSIBLE -> stringResource(R.string.google_restore_failed)
        TreasuryMessage.INVOICE_NOT_FOUND -> stringResource(R.string.invoice_not_found)
        TreasuryMessage.EXPENSE_NOT_FOUND -> stringResource(R.string.expense_not_found)
        TreasuryMessage.PAYMENT_NOT_FOUND -> stringResource(R.string.payment_not_found)
        TreasuryMessage.BACKUP_RESTORED_SUCCESS -> stringResource(R.string.backup_restored_success)
        TreasuryMessage.ADMIN_ONLY_COLLECTION_ADJUSTMENT ->
            stringResource(R.string.admin_only_collection_adjustment)
        TreasuryRepository.SUBSCRIPTION_LIMIT_REACHED -> stringResource(R.string.subscription_limit_reached)
        TreasuryRepository.ACCOUNT_LIMIT_REACHED -> stringResource(R.string.treasury_accounts_limit_reached)
        else -> code
    }
}

@Composable
fun formatImportFeedback(feedback: ImportFeedback): String =
    if (feedback.skippedDuplicates > 0) {
        stringResource(
            R.string.import_invoices_result_with_skipped,
            feedback.imported,
            feedback.skippedDuplicates
        )
    } else {
        stringResource(R.string.import_invoices_result, feedback.imported)
    }
