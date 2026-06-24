package com.abccash.app.treasury.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.abccash.app.R
import com.abccash.app.locale.AppLocale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.abccash.app.treasury.importer.ReceiptOcrProcessor
import com.abccash.app.treasury.importer.ReceiptParseResult
import kotlinx.coroutines.launch
import java.io.File
import com.abccash.app.treasury.data.AppCurrency
import com.abccash.app.treasury.data.AppCurrencyFormatter
import com.abccash.app.treasury.data.LocalAppCurrency

data class ReceiptScanUiState(
    val isScanning: Boolean = false,
    val successMessage: String? = null,
    val lastScannedUri: Uri? = null
)

@Composable
fun rememberReceiptScan(
    snackbarHostState: SnackbarHostState,
    ocrEnabled: Boolean = true,
    onParsed: (ReceiptParseResult) -> Unit
): Pair<ReceiptScanUiState, ReceiptScanActions> {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var uiState by remember { mutableStateOf(ReceiptScanUiState()) }
    var showSourcePicker by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val currency = LocalAppCurrency.current

    val receiptNoData = stringResource(R.string.receipt_no_data)
    val receiptReadFailed = stringResource(R.string.receipt_read_failed)
    val cameraDenied = stringResource(R.string.camera_permission_denied)

    fun processImage(uri: Uri) {
        if (!ocrEnabled) {
            uiState = uiState.copy(lastScannedUri = uri, isScanning = false, successMessage = null)
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.receipt_attached))
            }
            return
        }
        scope.launch {
            uiState = uiState.copy(isScanning = true, successMessage = null)
            runCatching { ReceiptOcrProcessor.scanReceipt(context, uri) }
                .onSuccess { result ->
                    if (result.amount == null && result.date == null && result.merchantHint == null) {
                        snackbarHostState.showSnackbar(receiptNoData)
                        uiState = uiState.copy(isScanning = false, lastScannedUri = uri)
                    } else {
                        onParsed(result)
                        uiState = uiState.copy(
                            successMessage = buildReceiptSuccessMessage(result, currency, context),
                            lastScannedUri = uri
                        )
                    }
                }
                .onFailure {
                    snackbarHostState.showSnackbar(receiptReadFailed)
                }
            uiState = uiState.copy(isScanning = false)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) processImage(uri)
    }

    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) processImage(uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createReceiptPhotoUri(context)
            pendingCameraUri = uri
            takePictureLauncher.launch(uri)
        } else {
            scope.launch { snackbarHostState.showSnackbar(cameraDenied) }
        }
    }

    val actions = ReceiptScanActions(
        onOpenSourcePicker = { showSourcePicker = true },
        onDismissSourcePicker = { showSourcePicker = false },
        isSourcePickerVisible = { showSourcePicker },
        onTakePhoto = {
            showSourcePicker = false
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                val uri = createReceiptPhotoUri(context)
                pendingCameraUri = uri
                takePictureLauncher.launch(uri)
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        },
        onPickGallery = {
            showSourcePicker = false
            galleryLauncher.launch("image/*")
        }
    )

    return uiState to actions
}

class ReceiptScanActions(
    val onOpenSourcePicker: () -> Unit,
    val onDismissSourcePicker: () -> Unit,
    val isSourcePickerVisible: () -> Boolean,
    val onTakePhoto: () -> Unit,
    val onPickGallery: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptSourcePickerSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickGallery: () -> Unit,
    titleRes: Int = R.string.scan_receipt
) {
    if (!visible) return
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(stringResource(titleRes), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = onTakePhoto, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.take_photo))
            }
            OutlinedButton(onClick = onPickGallery, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.choose_from_gallery))
            }
        }
    }
}

@Composable
fun ReceiptScanSuccessBanner(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
        color = androidx.compose.ui.graphics.Color(0xFFE8F5E9)
    ) {
        Text(
            text = stringResource(R.string.scan_success, message),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            fontSize = 13.sp,
            color = androidx.compose.ui.graphics.Color(0xFF2E7D32)
        )
    }
}

private fun createReceiptPhotoUri(context: Context): Uri {
    val file = File(context.cacheDir, "receipt_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private fun buildReceiptSuccessMessage(
    result: ReceiptParseResult,
    currency: AppCurrency,
    context: Context
): String {
    val parts = buildList {
        result.amount?.let { add(AppCurrencyFormatter.format(it, currency)) }
        result.date?.let { add(AppLocale.dayMonth(it)) }
    }
    return if (parts.isEmpty()) {
        context.getString(R.string.receipt_read_ok)
    } else {
        context.getString(R.string.receipt_read_detected, parts.joinToString(", "))
    }
}
