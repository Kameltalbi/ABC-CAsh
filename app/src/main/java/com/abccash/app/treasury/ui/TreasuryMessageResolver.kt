package com.abccash.app.treasury.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.abccash.app.R
import com.abccash.app.treasury.data.ImportFeedback
import com.abccash.app.treasury.data.TreasuryMessage
import com.abccash.app.treasury.repository.TreasuryRepository

@StringRes
private fun treasuryMessageStringRes(code: String): Int? = when (code) {
    TreasuryMessage.PASSWORD_MIN_LENGTH,
    TreasuryMessage.NEW_PASSWORD_MIN_LENGTH -> R.string.password_min_chars
    TreasuryMessage.NO_ACCOUNT_FOUND -> R.string.no_account_found
    TreasuryMessage.EMAIL_REQUIRED -> R.string.email_required
    TreasuryMessage.EMAIL_TAKEN -> R.string.email_taken
    TreasuryMessage.PHONE_TAKEN -> R.string.phone_taken
    TreasuryMessage.PHONE_REQUIRED -> R.string.phone_required
    TreasuryMessage.NAME_REQUIRED -> R.string.name_required
    TreasuryMessage.USER_NOT_FOUND -> R.string.user_not_found
    TreasuryMessage.CURRENT_PASSWORD_WRONG -> R.string.current_password_wrong
    TreasuryMessage.COMPANY_NOT_FOUND -> R.string.company_not_found
    TreasuryMessage.COMPANY_NAME_REQUIRED -> R.string.company_name_required
    TreasuryMessage.ENTREPRISE_ID_REQUIRED -> R.string.entreprise_id_required
    TreasuryMessage.BANK_ACCOUNT_NAME_REQUIRED -> R.string.bank_account_name_required
    TreasuryMessage.CONTACT_NAME_REQUIRED -> R.string.contact_name_required
    TreasuryMessage.CLIENT_NAME_REQUIRED -> R.string.client_name_required
    TreasuryMessage.TOTAL_AMOUNT_POSITIVE -> R.string.total_amount_positive
    TreasuryMessage.PAID_AMOUNT_NEGATIVE -> R.string.paid_amount_negative
    TreasuryMessage.TOTAL_BELOW_PAID -> R.string.total_below_paid
    TreasuryMessage.DRAFT_MUST_NOT_HAVE_INVOICE_NUMBER -> R.string.draft_must_not_have_invoice_number
    TreasuryMessage.INVOICE_NUMBER_REQUIRED -> R.string.invoice_number_required
    TreasuryMessage.INVOICE_NUMBER_EXISTS -> R.string.invoice_number_exists
    TreasuryMessage.INVOICE_NOT_FOUND -> R.string.invoice_not_found
    TreasuryMessage.INVOICE_ALREADY_VALIDATED -> R.string.invoice_already_validated
    TreasuryMessage.INVOICE_ID_REQUIRED -> R.string.invoice_id_required
    TreasuryMessage.DRAFT_MUST_NOT_HAVE_QUOTE_NUMBER -> R.string.draft_must_not_have_quote_number
    TreasuryMessage.QUOTE_NUMBER_REQUIRED -> R.string.quote_number_required
    TreasuryMessage.QUOTE_NUMBER_EXISTS -> R.string.quote_number_exists
    TreasuryMessage.QUOTE_NOT_FOUND -> R.string.quote_not_found
    TreasuryMessage.QUOTE_ALREADY_VALIDATED -> R.string.quote_already_validated
    TreasuryMessage.QUOTE_VALIDATE_BEFORE_STATUS -> R.string.quote_validate_before_status
    TreasuryMessage.QUOTE_CONVERTED_LOCKED -> R.string.quote_converted_locked
    TreasuryMessage.INVALID_STATUS -> R.string.invalid_status
    TreasuryMessage.QUOTE_SENT_ONLY_ACCEPT_REFUSE -> R.string.quote_sent_only_accept_refuse
    TreasuryMessage.QUOTE_ID_REQUIRED -> R.string.quote_id_required
    TreasuryMessage.DRAFT_ONLY_DELETE -> R.string.draft_only_delete
    TreasuryMessage.QUOTE_ACCEPTED_ONLY_CONVERT -> R.string.quote_accepted_only_convert
    TreasuryMessage.QUOTE_ALREADY_CONVERTED -> R.string.quote_already_converted
    TreasuryMessage.PAYMENT_AMOUNT_POSITIVE -> R.string.payment_amount_positive
    TreasuryMessage.EXPENSE_LABEL_REQUIRED -> R.string.label_required
    TreasuryMessage.EXPENSE_AMOUNT_POSITIVE -> R.string.expense_amount_positive
    TreasuryMessage.EXPENSE_ID_REQUIRED -> R.string.expense_id_required
    TreasuryMessage.EXPENSE_NOT_FOUND -> R.string.expense_not_found
    TreasuryMessage.PAYMENT_NOT_FOUND -> R.string.payment_not_found
    TreasuryMessage.BACKUP_WRONG_ENTREPRISE -> R.string.backup_wrong_entreprise
    TreasuryMessage.BACKUP_ORPHAN_PAYMENTS -> R.string.backup_orphan_payments
    TreasuryMessage.DELETE_ERROR -> R.string.delete_error
    TreasuryMessage.SESSION_EXPIRED,
    TreasuryMessage.SESSION_EXPIRED_RECONNECT -> R.string.session_expired
    TreasuryMessage.SESSION_INACTIVE -> R.string.session_inactive
    TreasuryMessage.CONNECT_GOOGLE -> R.string.connect_google_account
    TreasuryMessage.EXPORT_DATA_FAILED -> R.string.export_data_failed
    TreasuryMessage.GOOGLE_BACKUP_SUCCESS -> R.string.google_backup_success
    TreasuryMessage.GOOGLE_DRIVE_ERROR -> R.string.google_drive_error
    TreasuryMessage.GOOGLE_DELETE_BACKUP_FAILED -> R.string.google_delete_backup_failed
    TreasuryMessage.GOOGLE_NO_BACKUP -> R.string.google_no_backup_found
    TreasuryMessage.GOOGLE_DATA_RESTORED -> R.string.google_data_restored
    TreasuryMessage.RESTORE_IMPOSSIBLE -> R.string.google_restore_failed
    TreasuryMessage.BACKUP_RESTORED_SUCCESS -> R.string.backup_restored_success
    TreasuryMessage.ADMIN_ONLY_COLLECTION_ADJUSTMENT -> R.string.admin_only_collection_adjustment
    TreasuryRepository.SUBSCRIPTION_LIMIT_REACHED -> R.string.subscription_limit_reached
    TreasuryRepository.ACCOUNT_LIMIT_REACHED -> R.string.treasury_accounts_limit_reached
    else -> null
}

fun Context.resolveTreasuryMessage(code: String?): String? {
    if (code == null) return null
    TreasuryMessage.parseInvalidPaymentAmountMax(code)?.let { max ->
        return getString(R.string.invalid_payment_amount_max, max.toString())
    }
    TreasuryMessage.parseBackupFileInvalidDetail(code)?.let { detail ->
        return getString(R.string.backup_file_invalid, detail)
    }
  treasuryMessageStringRes(code)?.let { return getString(it) }
  return code
}

@Composable
fun resolveTreasuryMessage(code: String?): String? {
    if (code == null) return null
    TreasuryMessage.parseInvalidPaymentAmountMax(code)?.let { max ->
        val formatAmount = rememberFormatMoney()
        return stringResource(R.string.invalid_payment_amount_max, formatAmount(max))
    }
    TreasuryMessage.parseBackupFileInvalidDetail(code)?.let { detail ->
        return stringResource(R.string.backup_file_invalid, detail)
    }
    treasuryMessageStringRes(code)?.let { return stringResource(it) }
    return code
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
