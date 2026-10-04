package com.abccash.app.treasury.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abccash.app.R
import com.abccash.app.locale.AppLocale
import com.abccash.app.treasury.data.PilotEntryType
import com.abccash.app.treasury.data.PilotText
import com.abccash.app.treasury.importer.PilotCategoryAction
import com.abccash.app.treasury.importer.PilotColumn
import com.abccash.app.treasury.importer.PilotDuplicateMode
import com.abccash.app.treasury.viewmodel.PilotImportStep
import com.abccash.app.treasury.viewmodel.PilotageViewModel
import com.abccash.app.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PilotImportFlow(
    viewModel: PilotageViewModel,
    formatMoney: (Double) -> String,
    onMessage: (String) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val session = state.importSession ?: return
    val preview = viewModel.preview()
    BackHandler {
        if (session.step == PilotImportStep.PREVIEW && !session.mapping.isComplete) {
            viewModel.cancelImport()
        } else if (session.step == PilotImportStep.PREVIEW) {
            viewModel.backToMapping()
        } else {
            viewModel.cancelImport()
        }
    }

    Dialog(
        onDismissRequest = viewModel::cancelImport,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    if (session.type == PilotEntryType.SALE) stringResource(R.string.pilot_import_sales)
                    else stringResource(R.string.pilot_import_charges),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = AppColors.TextPrimary
                )
                Text(session.filename, color = AppColors.TextSecondary, fontSize = 13.sp)
                if (session.step == PilotImportStep.MAPPING || !session.mapping.isComplete) {
                    Text(stringResource(R.string.pilot_mapping_title), fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
                    Text(stringResource(R.string.pilot_mapping_hint), color = AppColors.TextSecondary, fontSize = 13.sp)
                    session.table.headers.forEachIndexed { index, header ->
                        ColumnField(
                            header = header.ifBlank { stringResource(R.string.pilot_column_n, index + 1) },
                            sample = session.table.dataRows.firstOrNull()?.getOrNull(index).orEmpty(),
                            selected = fieldFor(session.mapping, index),
                            onSelect = { column ->
                                if (column == null) viewModel.clearMappedColumn(index)
                                else viewModel.updateMapping(column, index)
                            }
                        )
                    }
                    if (session.error == "MAPPING") {
                        Text(stringResource(R.string.pilot_mapping_required), color = AppColors.ExpenseRed)
                    }
                    Button(onClick = viewModel::confirmMapping, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.action_validate))
                    }
                    OutlinedButton(onClick = viewModel::cancelImport, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.cancel))
                    }
                } else if (preview != null) {
                    Text(stringResource(R.string.pilot_detected_rows, preview.validLines.size), fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                    Text(stringResource(R.string.pilot_detected_total, formatMoney(preview.total)), color = AppColors.TextPrimary)
                    val start = preview.periodStart
                    val end = preview.periodEnd
                    if (start != null && end != null) {
                        Text(
                            stringResource(R.string.pilot_detected_period, AppLocale.monthYear(start), AppLocale.monthYear(end)),
                            color = AppColors.TextPrimary
                        )
                    }
                    Text(stringResource(R.string.pilot_detected_categories, preview.newCategoryNames.size), color = AppColors.TextPrimary)
                    if (preview.invalidCount > 0) {
                        Text(stringResource(R.string.pilot_invalid_rows, preview.invalidCount), color = AppColors.TextSecondary)
                    }
                    preview.newCategoryNames.forEach { name ->
                        val key = PilotText.categoryKey(name)
                        val action = session.categoryActions[key] ?: PilotCategoryAction()
                        Text(stringResource(R.string.pilot_new_category_detected, name), fontWeight = FontWeight.Medium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = action.create,
                                onClick = { viewModel.setCategoryAction(name, PilotCategoryAction(create = true)) },
                                label = { Text(stringResource(R.string.pilot_create_category)) }
                            )
                            FilterChip(
                                selected = !action.create,
                                onClick = {
                                    val first = state.categories.firstOrNull { it.type == session.type }
                                    viewModel.setCategoryAction(
                                        name,
                                        PilotCategoryAction(create = false, attachToCategoryId = first?.id ?: action.attachToCategoryId)
                                    )
                                },
                                label = { Text(stringResource(R.string.pilot_attach_category)) }
                            )
                        }
                        if (!action.create) {
                            CategoryAttachMenu(
                                names = state.categories.filter { it.type == session.type },
                                selectedId = action.attachToCategoryId,
                                onSelect = { id ->
                                    viewModel.setCategoryAction(name, PilotCategoryAction(create = false, attachToCategoryId = id))
                                }
                            )
                        }
                    }
                    if (preview.duplicateIndexes.isNotEmpty()) {
                        Text(
                            stringResource(R.string.pilot_duplicates, preview.duplicateIndexes.size),
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.TextPrimary
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = session.duplicateMode == PilotDuplicateMode.IGNORE,
                                onClick = { viewModel.setDuplicateMode(PilotDuplicateMode.IGNORE) },
                                label = { Text(stringResource(R.string.pilot_dup_ignore)) }
                            )
                            FilterChip(
                                selected = session.duplicateMode == PilotDuplicateMode.IMPORT,
                                onClick = { viewModel.setDuplicateMode(PilotDuplicateMode.IMPORT) },
                                label = { Text(stringResource(R.string.pilot_dup_import)) }
                            )
                            FilterChip(
                                selected = session.duplicateMode == PilotDuplicateMode.REVIEW,
                                onClick = { viewModel.setDuplicateMode(PilotDuplicateMode.REVIEW) },
                                label = { Text(stringResource(R.string.pilot_dup_review)) }
                            )
                        }
                        if (session.duplicateMode == PilotDuplicateMode.REVIEW) {
                            preview.validLines.filter { it.sourceIndex in preview.duplicateIndexes }.forEach { line ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = line.sourceIndex in session.reviewedIndexes,
                                        onCheckedChange = { checked ->
                                            viewModel.toggleReviewed(line.sourceIndex, checked)
                                        }
                                    )
                                    Text(
                                        "${line.date?.let(AppLocale::dayMonthYear) ?: ""} · ${line.categoryName} · ${line.amount?.let(formatMoney).orEmpty()}",
                                        color = AppColors.TextPrimary,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        stringResource(R.string.pilot_will_import, preview.linesToImport.size),
                        color = AppColors.TextSecondary
                    )
                    Button(
                        onClick = {
                            viewModel.commitImport { error ->
                                if (error != null) onMessage(error)
                            }
                        },
                        enabled = preview.linesToImport.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.pilot_import)) }
                    TextButton(onClick = viewModel::backToMapping) { Text(stringResource(R.string.pilot_edit_mapping)) }
                    OutlinedButton(onClick = viewModel::cancelImport, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnField(
    header: String,
    sample: String,
    selected: PilotColumn?,
    onSelect: (PilotColumn?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val label = when (selected) {
        PilotColumn.DATE -> stringResource(R.string.pilot_field_date)
        PilotColumn.CATEGORY -> stringResource(R.string.pilot_field_category)
        PilotColumn.AMOUNT -> stringResource(R.string.pilot_field_amount)
        PilotColumn.NOTE -> stringResource(R.string.pilot_field_note)
        null -> stringResource(R.string.pilot_field_ignore)
    }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text(header) },
            supportingText = if (sample.isNotBlank()) {
                { Text(sample, maxLines = 1) }
            } else {
                null
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.pilot_field_date)) }, onClick = { onSelect(PilotColumn.DATE); expanded = false })
            DropdownMenuItem(text = { Text(stringResource(R.string.pilot_field_category)) }, onClick = { onSelect(PilotColumn.CATEGORY); expanded = false })
            DropdownMenuItem(text = { Text(stringResource(R.string.pilot_field_amount)) }, onClick = { onSelect(PilotColumn.AMOUNT); expanded = false })
            DropdownMenuItem(text = { Text(stringResource(R.string.pilot_field_note)) }, onClick = { onSelect(PilotColumn.NOTE); expanded = false })
            DropdownMenuItem(text = { Text(stringResource(R.string.pilot_field_ignore)) }, onClick = { onSelect(null); expanded = false })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryAttachMenu(
    names: List<com.abccash.app.treasury.data.PilotCategory>,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = names.find { it.id == selectedId }?.name.orEmpty()
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.pilot_field_category)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            names.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        onSelect(category.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun fieldFor(mapping: com.abccash.app.treasury.importer.PilotColumnMapping, index: Int): PilotColumn? = when (index) {
    mapping.date -> PilotColumn.DATE
    mapping.category -> PilotColumn.CATEGORY
    mapping.amount -> PilotColumn.AMOUNT
    mapping.note -> PilotColumn.NOTE
    else -> null
}
