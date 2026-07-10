package com.abccash.app.treasury.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.abccash.app.ui.theme.AppColors
import com.abccash.app.R
import com.abccash.app.treasury.data.Invoice
import com.abccash.app.treasury.data.InvoiceStatus
import com.abccash.app.treasury.data.Quote
import com.abccash.app.treasury.data.QuoteStatus

@Composable
private fun DocumentShareMenuItems(
    onDismiss: () -> Unit,
    onWhatsApp: () -> Unit,
    onEmail: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(
                stringResource(R.string.share),
                fontSize = 12.sp,
                color = AppColors.TextSecondary
            )
        },
        onClick = {},
        enabled = false
    )
    DropdownMenuItem(
        text = { Text(stringResource(R.string.share_via_whatsapp)) },
        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
        onClick = { onDismiss(); onWhatsApp() }
    )
    DropdownMenuItem(
        text = { Text(stringResource(R.string.share_via_email)) },
        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
        onClick = { onDismiss(); onEmail() }
    )
    HorizontalDivider()
}

@Composable
fun InvoiceRowActionsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    invoice: Invoice,
    isAdmin: Boolean,
    canAddPayment: Boolean,
    onWhatsApp: () -> Unit,
    onEmail: () -> Unit,
    onDownloadPdf: () -> Unit,
    onEdit: () -> Unit,
    onMarkPaid: () -> Unit,
    onDelete: () -> Unit
) {
    val isPaid = invoice.status == InvoiceStatus.PAID
    val showMarkPaid = canAddPayment && !isPaid && !invoice.isDraft
    AbcDropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DocumentShareMenuItems(
            onDismiss = onDismiss,
            onWhatsApp = onWhatsApp,
            onEmail = onEmail
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.invoice_download_pdf)) },
            leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
            onClick = { onDismiss(); onDownloadPdf() }
        )
        if (isAdmin) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.edit)) },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                onClick = { onDismiss(); onEdit() }
            )
        }
        if (showMarkPaid) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.mark_paid)) },
                leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                onClick = { onDismiss(); onMarkPaid() }
            )
        }
        if (isAdmin) {
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete), color = Color(0xFFF44336)) },
                leadingIcon = {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFF44336))
                },
                onClick = { onDismiss(); onDelete() }
            )
        }
    }
}

@Composable
fun QuoteRowActionsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    quote: Quote,
    isAdmin: Boolean,
    onDownloadPdf: () -> Unit,
    onEmailPdf: () -> Unit,
    onWhatsAppPdf: () -> Unit,
    onDelete: () -> Unit
) {
    AbcDropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DocumentShareMenuItems(
            onDismiss = onDismiss,
            onWhatsApp = onWhatsAppPdf,
            onEmail = onEmailPdf
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.invoice_download_pdf)) },
            leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
            onClick = { onDismiss(); onDownloadPdf() }
        )
        if (isAdmin && quote.canDelete) {
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete), color = Color(0xFFF44336)) },
                leadingIcon = {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFF44336))
                },
                onClick = { onDismiss(); onDelete() }
            )
        }
    }
}
