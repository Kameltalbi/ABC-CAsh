package com.abccash.app.treasury.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abccash.app.R
import com.abccash.app.locale.AppLocale
import com.abccash.app.treasury.data.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientContactFormContent(
    initialContact: Contact?,
    entrepriseId: String,
    onDismiss: () -> Unit,
    onSave: (Contact) -> Unit
) {
    val french = AppLocale.current().language == Locale.FRENCH.language
    val defaultCountry = initialContact?.countryCode?.takeIf { it.isNotBlank() } ?: "FR"

    var advancedMode by remember { mutableStateOf(false) }
    var countryCode by remember(initialContact) {
        mutableStateOf(defaultCountry)
    }
    var legalName by remember(initialContact) {
        mutableStateOf(initialContact?.legalName?.ifBlank { initialContact.name }.orEmpty())
    }
    var displayName by remember(initialContact) {
        mutableStateOf(
            initialContact?.name?.takeIf { it != initialContact.legalName }.orEmpty()
        )
    }
    var taxIdType by remember(initialContact, countryCode) {
        mutableStateOf(initialContact?.taxIdType ?: ClientCountryProfiles.defaultTaxIdType(countryCode))
    }
    var taxIdValue by remember(initialContact) { mutableStateOf(initialContact?.taxIdValue.orEmpty()) }
    var email by remember(initialContact) { mutableStateOf(initialContact?.email.orEmpty()) }
    var phone by remember(initialContact) { mutableStateOf(initialContact?.phone.orEmpty()) }
    var addressLine1 by remember(initialContact) {
        mutableStateOf(
            initialContact?.addressLine1?.ifBlank { initialContact.address.lines().firstOrNull().orEmpty() }.orEmpty()
        )
    }
    var addressLine2 by remember(initialContact) { mutableStateOf(initialContact?.addressLine2.orEmpty()) }
    var postalCode by remember(initialContact) { mutableStateOf(initialContact?.postalCode.orEmpty()) }
    var city by remember(initialContact) { mutableStateOf(initialContact?.city.orEmpty()) }
    var notes by remember(initialContact) { mutableStateOf(initialContact?.notes.orEmpty()) }
    var showCountryMenu by remember { mutableStateOf(false) }
    var showTaxTypeMenu by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }

    val taxOptions = remember(countryCode) { ClientCountryProfiles.taxIdOptionsForCountry(countryCode) }
    val selectedTaxOption = taxOptions.find { it.type == taxIdType } ?: taxOptions.first()

    LaunchedEffect(countryCode) {
        if (taxOptions.none { it.type == taxIdType }) {
            taxIdType = taxOptions.first().type
        }
    }

    val legalNameRequiredError = stringResource(R.string.client_legal_name_required)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                stringResource(if (initialContact == null) R.string.contact_add_client else R.string.contact_edit_client),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = { advancedMode = !advancedMode }) {
                Text(
                    if (advancedMode) stringResource(R.string.client_form_simple_mode)
                    else stringResource(R.string.client_form_advanced_mode),
                    fontSize = 13.sp
                )
            }
        }

        Text(
            stringResource(R.string.client_form_country_help),
            fontSize = 12.sp,
            color = Color(0xFF64748B)
        )

        ExposedDropdownMenuBox(
            expanded = showCountryMenu,
            onExpandedChange = { showCountryMenu = it }
        ) {
            OutlinedTextField(
                value = ClientCountryProfiles.countryLabel(countryCode, french),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.client_country)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCountryMenu) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(expanded = showCountryMenu, onDismissRequest = { showCountryMenu = false }) {
                ClientCountryProfiles.countries.forEach { country ->
                    DropdownMenuItem(
                        text = { Text(if (french) country.labelFr else country.labelEn) },
                        onClick = {
                            countryCode = country.code
                            showCountryMenu = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = legalName,
            onValueChange = { legalName = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.client_legal_name)) },
            placeholder = { Text(stringResource(R.string.client_legal_name_hint)) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        ExposedDropdownMenuBox(
            expanded = showTaxTypeMenu,
            onExpandedChange = { showTaxTypeMenu = it }
        ) {
            OutlinedTextField(
                value = if (french) selectedTaxOption.labelFr else selectedTaxOption.labelEn,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.client_tax_id_type)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showTaxTypeMenu) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(expanded = showTaxTypeMenu, onDismissRequest = { showTaxTypeMenu = false }) {
                taxOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(if (french) option.labelFr else option.labelEn) },
                        onClick = {
                            taxIdType = option.type
                            showTaxTypeMenu = false
                        }
                    )
                }
            }
        }

        if (taxIdType != TaxIdType.NONE) {
            OutlinedTextField(
                value = taxIdValue,
                onValueChange = { taxIdValue = it },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(if (french) selectedTaxOption.labelFr else selectedTaxOption.labelEn)
                },
                placeholder = {
                    Text(if (french) selectedTaxOption.placeholderFr else selectedTaxOption.placeholderEn)
                },
                singleLine = true,
                supportingText = {
                    Text(if (french) selectedTaxOption.hintFr else selectedTaxOption.hintEn)
                },
                shape = RoundedCornerShape(12.dp)
            )
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(R.string.email)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(12.dp)
        )
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text(stringResource(R.string.phone)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = addressLine1,
            onValueChange = { addressLine1 = it },
            label = { Text(stringResource(R.string.client_billing_address)) },
            placeholder = { Text(stringResource(R.string.client_billing_address_hint)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = if (advancedMode) 1 else 2,
            shape = RoundedCornerShape(12.dp)
        )

        if (advancedMode) {
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text(stringResource(R.string.client_display_name)) },
                placeholder = { Text(stringResource(R.string.client_display_name_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = addressLine2,
                onValueChange = { addressLine2 = it },
                label = { Text(stringResource(R.string.client_address_line2)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = postalCode,
                    onValueChange = { postalCode = it },
                    label = { Text(stringResource(R.string.client_postal_code)) },
                    modifier = Modifier.weight(0.4f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text(stringResource(R.string.client_city)) },
                    modifier = Modifier.weight(0.6f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.note_optional)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                shape = RoundedCornerShape(12.dp)
            )
        }

        formError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.cancel))
            }
            Button(
                onClick = {
                    if (legalName.isBlank()) {
                        formError = legalNameRequiredError
                        return@Button
                    }
                    formError = null
                    val resolvedDisplay = displayName.trim().ifBlank { legalName.trim() }
                    onSave(
                        (initialContact ?: Contact(entrepriseId = entrepriseId, type = ContactType.CLIENT, name = "")).copy(
                            name = resolvedDisplay,
                            legalName = legalName.trim(),
                            email = email.trim(),
                            phone = phone.trim(),
                            countryCode = countryCode,
                            taxIdType = taxIdType,
                            taxIdValue = taxIdValue.trim(),
                            taxIdValidationStatus = TaxIdValidationStatus.UNVERIFIED,
                            addressLine1 = addressLine1.trim(),
                            addressLine2 = addressLine2.trim(),
                            postalCode = postalCode.trim(),
                            city = city.trim(),
                            notes = notes.trim(),
                            type = ContactType.CLIENT,
                            entrepriseId = entrepriseId
                        )
                    )
                },
                enabled = legalName.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}
