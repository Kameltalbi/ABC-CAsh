package com.abccash.app.treasury.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abccash.app.R
import com.abccash.app.locale.AppLocale
import com.abccash.app.treasury.data.PilotCoverageState
import com.abccash.app.treasury.data.PilotCalculations
import com.abccash.app.treasury.data.PilotCategory
import com.abccash.app.treasury.data.PilotEntry
import com.abccash.app.treasury.data.PilotEntryType
import com.abccash.app.treasury.data.PilotMonthFigures
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

private val PageBlue = Color(0xFF2563EB)
private val PageOrange = Color(0xFFF97316)
private val PageGreen = Color(0xFF16A34A)
private val Ink = Color(0xFF1E293B)
private val Muted = Color(0xFF94A3B8)
private val CardLine = Color(0xFFE8EEF5)

private val SaleBg = Color(0xFFEAF2FF)
private val ExpenseBg = Color(0xFFFFF1E8)
private val ResultBg = Color(0xFFE8F8EE)
private val LossBg = Color(0xFFFEECEC)
private val CoverageBg = Color(0xFFE7F8EF)
private val ZeroBg = Color(0xFFF3FBF6)

@Composable
fun PilotageHome(
    month: YearMonth,
    figures: PilotMonthFigures,
    sales: List<PilotEntry>,
    expenses: List<PilotEntry>,
    movements: List<PilotEntry>,
    accounts: List<com.abccash.app.treasury.data.BankAccount>,
    categories: List<PilotCategory>,
    formatAmount: (Double) -> String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onPickMonth: () -> Unit,
    onEdit: (PilotEntry) -> Unit,
    onDuplicate: (PilotEntry) -> Unit,
    onDelete: (PilotEntry) -> Unit,
    onEditExpenses: (List<PilotEntry>) -> Unit,
    onDeleteExpenses: (List<PilotEntry>) -> Unit,
    clearSelectionTick: Int = 0,
    modifier: Modifier = Modifier
) {
    val rubriqueColors = remember(categories) { PilotCalculations.rubriqueColorIndexes(categories) }
    var expenseSelectionMode by remember { mutableStateOf(false) }
    var selectedExpenseIds by remember { mutableStateOf(setOf<String>()) }
    LaunchedEffect(month) {
        expenseSelectionMode = false
        selectedExpenseIds = emptySet()
    }
    LaunchedEffect(clearSelectionTick) {
        if (clearSelectionTick > 0) {
            expenseSelectionMode = false
            selectedExpenseIds = emptySet()
        }
    }
    LaunchedEffect(expenses.map { it.id }) {
        val valid = expenses.map { it.id }.toSet()
        selectedExpenseIds = selectedExpenseIds.filter { it in valid }.toSet()
        if (selectedExpenseIds.isEmpty() && expenseSelectionMode && expenses.isEmpty()) {
            expenseSelectionMode = false
        }
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MonthCard(month = month, onPrevious = onPreviousMonth, onNext = onNextMonth, onPick = onPickMonth)
        KpiRow(figures = figures, salesCount = sales.size, expenseCount = expenses.size, formatAmount = formatAmount)
        EntrySectionCard(
            title = stringResource(R.string.pilot_sales),
            tint = PageBlue,
            icon = Icons.Filled.BarChart,
            total = figures.sales,
            entries = sales.sortedByDescending { it.date },
            categories = categories,
            rubriqueColors = rubriqueColors,
            formatAmount = formatAmount,
            seeAllRes = R.string.pilot_see_all_sales,
            onEdit = onEdit,
            onDuplicate = onDuplicate,
            onDelete = onDelete
        )
        EntrySectionCard(
            title = stringResource(R.string.pilot_expenses),
            tint = PageOrange,
            icon = Icons.Filled.AccountBalanceWallet,
            total = figures.charges,
            entries = expenses.sortedByDescending { it.date },
            categories = categories,
            rubriqueColors = rubriqueColors,
            formatAmount = formatAmount,
            seeAllRes = R.string.pilot_see_all_expenses,
            selectionMode = expenseSelectionMode,
            selectedIds = selectedExpenseIds,
            onToggleSelect = { id ->
                selectedExpenseIds = if (id in selectedExpenseIds) selectedExpenseIds - id else selectedExpenseIds + id
            },
            onStartSelection = { id ->
                expenseSelectionMode = true
                selectedExpenseIds = setOf(id)
            },
            onEnterSelection = {
                expenseSelectionMode = true
                selectedExpenseIds = emptySet()
            },
            onSelectAll = {
                expenseSelectionMode = true
                selectedExpenseIds = expenses.map { it.id }.toSet()
            },
            onClearSelection = {
                expenseSelectionMode = false
                selectedExpenseIds = emptySet()
            },
            onEditSelected = {
                val chosen = expenses.filter { it.id in selectedExpenseIds }
                if (chosen.isNotEmpty()) onEditExpenses(chosen)
            },
            onDeleteSelected = {
                val chosen = expenses.filter { it.id in selectedExpenseIds }
                if (chosen.isNotEmpty()) onDeleteExpenses(chosen)
            },
            onEdit = onEdit,
            onDuplicate = onDuplicate,
            onDelete = onDelete
        )
        if (movements.isNotEmpty()) {
            EntrySectionCard(
                title = stringResource(R.string.pilot_movements),
                tint = Color(0xFF0369A1),
                icon = Icons.Filled.SwapHoriz,
                total = null,
                entries = movements.sortedByDescending { it.date },
                categories = categories,
                rubriqueColors = rubriqueColors,
                formatAmount = formatAmount,
                seeAllRes = R.string.pilot_see_all_movements,
                accountCaption = { entry -> movementCaption(entry, categories, accounts) },
                onEdit = onEdit,
                onDuplicate = onDuplicate,
                onDelete = onDelete
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            CoverageCard(figures = figures, modifier = Modifier.weight(1.15f))
            ZeroCard(amount = formatAmount(figures.zeroPoint), modifier = Modifier.weight(0.85f))
        }
        ScrollHint()
    }
}

@Composable
private fun MonthCard(
    month: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.previous_month),
                    tint = Muted
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onPick),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = PageBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = AppLocale.monthYear(month),
                        modifier = Modifier.padding(start = 6.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Ink
                    )
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.pilot_pick_month),
                        tint = Ink,
                        modifier = Modifier
                            .padding(start = 2.dp)
                            .size(18.dp)
                    )
                }
                if (month == YearMonth.now()) {
                    Text(
                        stringResource(R.string.pilot_month_current),
                        color = Muted,
                        fontSize = 11.sp
                    )
                }
            }
            IconButton(onClick = onNext, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.next_month),
                    tint = Muted
                )
            }
        }
    }
}

@Composable
private fun KpiRow(
    figures: PilotMonthFigures,
    salesCount: Int,
    expenseCount: Int,
    formatAmount: (Double) -> String
) {
    val resultPositive = figures.result > 0.00005
    val resultNegative = figures.result < -0.00005
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        KpiCard(
            modifier = Modifier.weight(1f),
            background = SaleBg,
            icon = Icons.Filled.BarChart,
            tint = PageBlue,
            label = stringResource(R.string.pilot_sales),
            value = formatAmount(figures.sales),
            caption = transactionCaption(salesCount)
        )
        KpiCard(
            modifier = Modifier.weight(1f),
            background = ExpenseBg,
            icon = Icons.Filled.AccountBalanceWallet,
            tint = PageOrange,
            label = stringResource(R.string.pilot_expenses),
            value = formatAmount(figures.charges),
            caption = transactionCaption(expenseCount)
        )
        KpiCard(
            modifier = Modifier.weight(1f),
            background = if (resultNegative) LossBg else ResultBg,
            icon = Icons.Filled.TrendingUp,
            tint = if (resultNegative) Color(0xFFDC2626) else PageGreen,
            label = stringResource(R.string.pilot_result),
            value = signedAmount(figures.result, formatAmount),
            caption = when {
                resultPositive -> stringResource(R.string.pilot_benefit)
                resultNegative -> stringResource(R.string.pilot_deficit)
                else -> stringResource(R.string.pilot_even)
            }
        )
    }
}

@Composable
private fun KpiCard(
    modifier: Modifier,
    background: Color,
    icon: ImageVector,
    tint: Color,
    label: String,
    value: String,
    caption: String
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(12.dp))
            Text(
                label,
                modifier = Modifier.padding(start = 4.dp).weight(1f),
                color = tint,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = value,
            color = tint,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
        if (caption.isNotBlank()) {
            Text(
                text = caption,
                color = Muted,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private const val PreviewLineLimit = 3

@Composable
private fun EntrySectionCard(
    title: String,
    tint: Color,
    icon: ImageVector,
    total: Double?,
    entries: List<PilotEntry>,
    categories: List<PilotCategory>,
    rubriqueColors: Map<String, Int>,
    formatAmount: (Double) -> String,
    @StringRes seeAllRes: Int,
    accountCaption: ((PilotEntry) -> String)? = null,
    selectionMode: Boolean = false,
    selectedIds: Set<String> = emptySet(),
    onToggleSelect: ((String) -> Unit)? = null,
    onStartSelection: ((String) -> Unit)? = null,
    onEnterSelection: (() -> Unit)? = null,
    onSelectAll: (() -> Unit)? = null,
    onClearSelection: (() -> Unit)? = null,
    onEditSelected: (() -> Unit)? = null,
    onDeleteSelected: (() -> Unit)? = null,
    onEdit: (PilotEntry) -> Unit,
    onDuplicate: (PilotEntry) -> Unit,
    onDelete: (PilotEntry) -> Unit
) {
    val selectable = onToggleSelect != null && onStartSelection != null
    var expanded by remember(entries.map { it.id }) { mutableStateOf(false) }
    val showAll = expanded || selectionMode
    val visible = if (showAll) entries else entries.take(PreviewLineLimit)
    val hasMore = entries.size > PreviewLineLimit
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (hasMore && !selectionMode) {
                            Modifier.clickable { expanded = !expanded }
                        } else {
                            Modifier
                        }
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(tint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
                }
                Text(
                    if (selectionMode) stringResource(R.string.pilot_selected_count, selectedIds.size) else title,
                    modifier = Modifier.padding(start = 8.dp).weight(1f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = tint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!selectionMode) {
                    Column(horizontalAlignment = Alignment.End) {
                        if (total != null) {
                            Text(
                                formatAmount(total),
                                color = tint,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (entries.isNotEmpty()) {
                            Text(
                                lineCaption(entries.size),
                                color = Muted,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = tint.copy(alpha = 0.7f),
                        modifier = Modifier
                            .padding(start = 2.dp)
                            .size(18.dp)
                    )
                }
                if (!selectionMode && selectable && entries.isNotEmpty()) {
                    TextButton(onClick = { onEnterSelection?.invoke() }) {
                        Text(stringResource(R.string.pilot_select), color = tint, fontSize = 11.sp)
                    }
                }
            }
            if (selectionMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { onSelectAll?.invoke() }) {
                        Text(stringResource(R.string.select_all), fontSize = 12.sp)
                    }
                    TextButton(
                        onClick = { onEditSelected?.invoke() },
                        enabled = selectedIds.isNotEmpty()
                    ) {
                        Text(stringResource(R.string.edit), fontSize = 12.sp, color = PageBlue)
                    }
                    TextButton(
                        onClick = { onDeleteSelected?.invoke() },
                        enabled = selectedIds.isNotEmpty()
                    ) {
                        Text(stringResource(R.string.delete), fontSize = 12.sp, color = Color(0xFFDC2626))
                    }
                    TextButton(onClick = { onClearSelection?.invoke() }) {
                        Text(stringResource(R.string.cancel), fontSize = 12.sp, color = Muted)
                    }
                }
            }
            if (entries.isEmpty()) {
                Text(
                    stringResource(R.string.pilot_empty_lines),
                    color = Muted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                visible.forEach { entry ->
                    val category = accountCaption?.invoke(entry)
                        ?: categories.find { it.id == entry.categoryId }?.name.orEmpty()
                    EntryRow(
                        entry = entry,
                        category = category,
                        chip = rubriqueChip(entry.type, rubriqueColors[entry.categoryId] ?: 0),
                        amount = formatAmount(entry.amount),
                        selected = entry.id in selectedIds,
                        selectionMode = selectionMode,
                        onToggleSelect = onToggleSelect?.let { { it(entry.id) } },
                        onStartSelection = onStartSelection?.let { { it(entry.id) } },
                        onEdit = { onEdit(entry) },
                        onDuplicate = { onDuplicate(entry) },
                        onDelete = { onDelete(entry) }
                    )
                }
                if (hasMore && !selectionMode) {
                    TextButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text(
                            text = if (expanded) {
                                stringResource(R.string.pilot_see_less)
                            } else {
                                stringResource(seeAllRes, entries.size)
                            },
                            color = tint,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Icon(
                            if (expanded) Icons.Filled.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScrollHint() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = Muted.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = stringResource(R.string.pilot_scroll_hint),
            color = Muted.copy(alpha = 0.75f),
            fontSize = 11.sp
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EntryRow(
    entry: PilotEntry,
    category: String,
    chip: ChipColors,
    amount: String,
    selected: Boolean = false,
    selectionMode: Boolean = false,
    onToggleSelect: (() -> Unit)? = null,
    onStartSelection: (() -> Unit)? = null,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    val title = entry.note.ifBlank { category }.ifBlank { "—" }
    val iconTint = chip.foreground
    val rowModifier = if (onToggleSelect != null && onStartSelection != null) {
        Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (selectionMode) onToggleSelect() else onEdit()
                },
                onLongClick = {
                    if (selectionMode) onToggleSelect() else onStartSelection()
                }
            )
    } else {
        Modifier.fillMaxWidth()
    }
    Row(
        modifier = rowModifier
            .then(
                if (selected) Modifier.background(Color(0xFFFFF7ED), RoundedCornerShape(10.dp))
                else Modifier
            )
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectionMode) {
            Checkbox(
                checked = selected,
                onCheckedChange = { onToggleSelect?.invoke() },
                modifier = Modifier.size(32.dp)
            )
        } else {
            Box(
                modifier = Modifier.size(28.dp).clip(CircleShape).background(chip.background),
                contentAlignment = Alignment.Center
            ) {
                Icon(rubriqueIcon(title, category), contentDescription = null, tint = iconTint, modifier = Modifier.size(14.dp))
            }
        }
        Column(Modifier.weight(1f).padding(start = 8.dp, end = 6.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = Ink, fontSize = 12.sp, lineHeight = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(dashboardDate(entry.date), color = Muted, fontSize = 10.sp, maxLines = 1, softWrap = false)
                if (category.isNotBlank()) {
                    Text(
                        text = category,
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(chip.background)
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                        color = chip.foreground,
                        fontSize = 9.sp,
                        lineHeight = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Text(amount, fontWeight = FontWeight.Bold, color = Ink, fontSize = 12.sp, maxLines = 1, softWrap = false)
        if (!selectionMode) {
            Box {
                IconButton(onClick = { menu = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.edit), tint = Muted)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.edit)) }, onClick = { menu = false; onEdit() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.pilot_duplicate)) }, onClick = { menu = false; onDuplicate() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.delete), color = Color(0xFFDC2626)) }, onClick = { menu = false; onDelete() })
                }
            }
        }
    }
}

@Composable
private fun CoverageCard(figures: PilotMonthFigures, modifier: Modifier = Modifier) {
    val state = PilotCalculations.coverageState(figures)
    val ratio = figures.coverage
    val whole = rememberFormatMoneyWhole()
    val percent = if (ratio == null) {
        stringResource(R.string.pilot_coverage_none)
    } else {
        String.format(Locale.getDefault(), "%d %%", kotlin.math.round(ratio * 100).toInt())
    }
    val caption = when (state) {
        PilotCoverageState.NO_CHARGES -> stringResource(R.string.pilot_no_charges)
        PilotCoverageState.AT_ZERO -> stringResource(R.string.pilot_zero_reached)
        PilotCoverageState.BELOW -> stringResource(R.string.pilot_missing, whole(abs(figures.result)))
        PilotCoverageState.ABOVE -> stringResource(R.string.pilot_cover_times, formatTimes(ratio ?: 0.0))
    }
    val tint = if (state == PilotCoverageState.BELOW) Color(0xFFDC2626) else PageGreen
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (state == PilotCoverageState.BELOW) LossBg else CoverageBg)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.TrackChanges, contentDescription = null, tint = tint, modifier = Modifier.size(13.dp))
            Text(
                stringResource(R.string.pilot_coverage_card),
                modifier = Modifier.padding(start = 4.dp).weight(1f),
                color = Ink,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(percent, color = tint, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, softWrap = false)
        }
        Text(caption, color = Muted, fontSize = 9.sp, lineHeight = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (ratio != null) {
            LinearProgressIndicator(
                progress = { ratio.toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                color = tint,
                trackColor = tint.copy(alpha = 0.18f)
            )
        }
    }
}

@Composable
private fun ZeroCard(amount: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(ZeroBg)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Flag, contentDescription = null, tint = PageGreen, modifier = Modifier.size(13.dp))
            Text(
                stringResource(R.string.pilot_zero_label),
                modifier = Modifier.padding(start = 4.dp).weight(1f),
                color = Ink,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(amount, color = Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, softWrap = false)
        }
        Text(
            stringResource(R.string.pilot_zero_hint),
            color = Muted,
            fontSize = 9.sp,
            lineHeight = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun transactionCaption(count: Int): String = lineCaption(count)

@Composable
private fun lineCaption(count: Int): String =
    if (count == 1) stringResource(R.string.pilot_one_line)
    else stringResource(R.string.pilot_many_lines, count)

private fun signedAmount(amount: Double, format: (Double) -> String): String {
    val text = format(abs(amount))
    return when {
        amount > 0.00005 -> "+$text"
        amount < -0.00005 -> "-$text"
        else -> text
    }
}

private fun formatTimes(ratio: Double): String =
    String.format(Locale.getDefault(), "%.1f", ratio)

private fun dashboardDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault()))

internal data class ChipColors(val background: Color, val foreground: Color)

private val salePalette = listOf(
    ChipColors(Color(0xFFDCFCE7), Color(0xFF15803D)),
    ChipColors(Color(0xFFCCFBF1), Color(0xFF0F766E)),
    ChipColors(Color(0xFFE7EEEA), Color(0xFF4B6358)),
    ChipColors(Color(0xFFD1FAE5), Color(0xFF047857)),
    ChipColors(Color(0xFFE5F6F4), Color(0xFF14635C)),
    ChipColors(Color(0xFFE8F5EC), Color(0xFF1F7A4D)),
    ChipColors(Color(0xFFEEF3F0), Color(0xFF5C7268)),
    ChipColors(Color(0xFFE3F4EA), Color(0xFF166534)),
    ChipColors(Color(0xFFE7F6F5), Color(0xFF1D6A66)),
    ChipColors(Color(0xFFF1F5F3), Color(0xFF3E5C54)),
    ChipColors(Color(0xFFD8F3EA), Color(0xFF0F6E56)),
    ChipColors(Color(0xFFE4F2EE), Color(0xFF3F6F64)),
    ChipColors(Color(0xFFEDF7F2), Color(0xFF2F6F4E)),
    ChipColors(Color(0xFFE6F4F1), Color(0xFF115E59)),
    ChipColors(Color(0xFFEAF3EE), Color(0xFF56756A)),
    ChipColors(Color(0xFFDFF5EF), Color(0xFF1A7A68))
)

private val expensePalette = listOf(
    ChipColors(Color(0xFFFFEDD5), Color(0xFFEA580C)),
    ChipColors(Color(0xFFFEE2E2), Color(0xFFB91C1C)),
    ChipColors(Color(0xFFFFE4E6), Color(0xFFE11D48)),
    ChipColors(Color(0xFFFEF3C7), Color(0xFFB45309)),
    ChipColors(Color(0xFFFCE7F3), Color(0xFFBE185D)),
    ChipColors(Color(0xFFFFF1E8), Color(0xFFC2410C)),
    ChipColors(Color(0xFFFEE4D6), Color(0xFF9A3412)),
    ChipColors(Color(0xFFF3E8FF), Color(0xFF7C3AED)),
    ChipColors(Color(0xFFEDE9FE), Color(0xFF5B21B6)),
    ChipColors(Color(0xFFFFF1F2), Color(0xFF9F1239)),
    ChipColors(Color(0xFFE8F1FF), Color(0xFF2563EB)),
    ChipColors(Color(0xFFE0E7FF), Color(0xFF3730A3)),
    ChipColors(Color(0xFFE0F2FE), Color(0xFF0369A1)),
    ChipColors(Color(0xFFF1F5F9), Color(0xFF475569)),
    ChipColors(Color(0xFFFAE8FF), Color(0xFFA21CAF)),
    ChipColors(Color(0xFFFFF7ED), Color(0xFFC2410C))
)

@Composable
internal fun RubriqueSwatch(type: PilotEntryType, index: Int, modifier: Modifier = Modifier) {
    val chip = rubriqueChip(type, index)
    Box(
        modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(chip.foreground)
    )
}

internal fun rubriqueChip(type: PilotEntryType, index: Int): ChipColors {
    val palette = when (type) {
        PilotEntryType.SALE -> salePalette
        PilotEntryType.EXPENSE -> expensePalette
        PilotEntryType.OTHER_INFLOW -> inflowPalette
        PilotEntryType.OTHER_OUTFLOW -> outflowPalette
        PilotEntryType.TRANSFER -> inflowPalette
    }
    if (index in palette.indices) return palette[index]
    val hueStart = when (type) {
        PilotEntryType.SALE -> 128f
        PilotEntryType.EXPENSE -> 6f
        else -> 205f
    }
    val hueSpan = if (type == PilotEntryType.SALE) 56f else 36f
    val hue = hueStart + (index * 17f) % hueSpan
    val saturation = if (type == PilotEntryType.SALE) 0.16f + (index % 4) * 0.06f else 0.28f + (index % 3) * 0.06f
    val background = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation.coerceAtMost(0.32f), 0.96f)))
    val foreground = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, (saturation + 0.38f).coerceAtMost(0.72f), 0.34f)))
    return ChipColors(background, foreground)
}

private val inflowPalette = listOf(
    ChipColors(Color(0xFFE0F2FE), Color(0xFF0369A1)),
    ChipColors(Color(0xFFE0E7FF), Color(0xFF3730A3)),
    ChipColors(Color(0xFFE8F1FF), Color(0xFF1D4ED8)),
    ChipColors(Color(0xFFECFEFF), Color(0xFF0E7490)),
    ChipColors(Color(0xFFF0F9FF), Color(0xFF075985))
)

private val outflowPalette = listOf(
    ChipColors(Color(0xFFF1F5F9), Color(0xFF334155)),
    ChipColors(Color(0xFFE2E8F0), Color(0xFF1E293B)),
    ChipColors(Color(0xFFE5E7EB), Color(0xFF3F3F46)),
    ChipColors(Color(0xFFF8FAFC), Color(0xFF475569)),
    ChipColors(Color(0xFFE7E5E4), Color(0xFF44403C))
)

private fun movementCaption(
    entry: PilotEntry,
    categories: List<PilotCategory>,
    accounts: List<com.abccash.app.treasury.data.BankAccount>
): String {
    fun name(id: String?) = accounts.find { it.id == id }?.name.orEmpty()
    return when (entry.type) {
        PilotEntryType.TRANSFER -> listOf(name(entry.bankAccountId), name(entry.counterAccountId))
            .filter { it.isNotBlank() }
            .joinToString(" → ")
        else -> categories.find { it.id == entry.categoryId }?.name.orEmpty()
    }
}

private fun rubriqueIcon(title: String, category: String): ImageVector {
    val key = "$title $category".lowercase(Locale.ROOT)
    return when {
        listOf("market", "pub", "magazine", "com").any { key.contains(it) } -> Icons.Filled.Campaign
        listOf("conseil", "prestation", "client", "service").any { key.contains(it) } -> Icons.Filled.Person
        listOf("loyer", "immobilier", "maison", "local").any { key.contains(it) } -> Icons.Filled.Home
        listOf("logiciel", "info", "saas", "abonnement").any { key.contains(it) } -> Icons.Filled.Computer
        listOf("transport", "voiture", "carbur", "essence").any { key.contains(it) } -> Icons.Filled.DirectionsCar
        listOf("salaire", "paie", "cnss").any { key.contains(it) } -> Icons.Filled.Payments
        listOf("impot", "impôt", "taxe", "banque").any { key.contains(it) } -> Icons.Filled.AccountBalance
        else -> Icons.Filled.Label
    }
}
