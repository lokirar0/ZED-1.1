package com.zed.app.feature.finance

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zed.app.R
import com.zed.app.core.domain.util.MoneyFormatter
import com.zed.app.ui.components.ZedTopBar
import com.zed.app.ui.theme.LocalZedColors
import com.zed.app.ui.theme.ZedDataNumber
import com.zed.app.ui.theme.ZedRadius
import com.zed.app.ui.theme.ZedSpacing
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FinanceScreen(
    onOpenSettings: () -> Unit,
    onOpenEditor: () -> Unit,
    onOpenCalendar: (Long) -> Unit,
    viewModel: FinanceViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LocalZedColors.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deletedText = stringResource(R.string.finance_deleted)
    val exportedText = stringResource(R.string.finance_exported)
    val addedText = stringResource(R.string.finance_added)
    var showQuickSheet by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                if (viewModel.exportTo(uri)) snackbarHostState.showSnackbar(exportedText)
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ZedTopBar(title = stringResource(R.string.tab_finance), onSettingsClick = onOpenSettings)

            if (state.loaded && state.transactions.isEmpty()) {
                Column(
                    Modifier.fillMaxSize().padding(ZedSpacing.xxl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("∅", style = MaterialTheme.typography.displayMedium, color = colors.textDisabled)
                    Text(
                        text = stringResource(R.string.finance_empty_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.textDisplay,
                        modifier = Modifier.padding(top = ZedSpacing.lg)
                    )
                    Text(
                        text = stringResource(R.string.finance_empty_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = ZedSpacing.sm)
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(ZedSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(ZedSpacing.md)
                ) {
                    item { BalanceCard(state.summary) }
                    item {
                        CalendarCard(
                            monthLabel = state.monthLabel,
                            weeks = state.calendarWeeks,
                            onDayClick = onOpenCalendar,
                            onOpenFull = { onOpenCalendar(System.currentTimeMillis()) }
                        )
                    }
                    item { MonthBars(state.chart) }
                    item {
                        TextButton(onClick = {
                            exportLauncher.launch("zed_finance_${System.currentTimeMillis()}.csv")
                        }) {
                            Icon(Icons.Outlined.Download, null, tint = colors.textSecondary)
                            Spacer(Modifier.width(ZedSpacing.sm))
                            Text(
                                text = stringResource(R.string.finance_export),
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.textSecondary
                            )
                        }
                    }
                    items(state.transactions, key = { it.id }) { item ->
                        val dismissState = rememberSwipeToDismissBoxState()
                        LaunchedEffect(dismissState.currentValue) {
                            if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
                                viewModel.delete(item.id)
                                snackbarHostState.showSnackbar(deletedText)
                            }
                        }
                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                Box(
                                    Modifier.fillMaxWidth().padding(horizontal = ZedSpacing.xl),
                                    contentAlignment = Alignment.CenterEnd
                                ) { Icon(Icons.Outlined.Delete, null, tint = colors.accent) }
                            },
                            content = { TransactionRow(item) }
                        )
                    }
                }
            }
        }

        // FAB: тап = редактор, ДОЛГИЙ ТАП = Quick-Add.
        // ВАЖНО: combinedClickable требует onClick без дефолта, поэтому
        // оба обработчика живут в модификаторе, а не в параметре FAB.
        FloatingActionButton(
            onClick = { },
            shape = RoundedCornerShape(ZedRadius.md),
            containerColor = colors.accent,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(ZedSpacing.xl)
                .combinedClickable(
                    onClick = onOpenEditor,
                    onLongClick = { showQuickSheet = true }
                )
        ) {
            Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.finance_add))
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }

    // --- Quick-Add sheet ---
    if (showQuickSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQuickSheet = false },
            containerColor = colors.surface
        ) {
            Text(
                text = stringResource(R.string.finance_quick),
                style = MaterialTheme.typography.labelMedium,
                color = colors.textDisabled,
                modifier = Modifier.padding(horizontal = ZedSpacing.xl, vertical = ZedSpacing.sm)
            )
            state.quick.forEach { q ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.quickAdd(q)
                            showQuickSheet = false
                            scope.launch { snackbarHostState.showSnackbar(addedText) }
                        }
                        .padding(horizontal = ZedSpacing.xl, vertical = ZedSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(q.label, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary, maxLines = 1)
                        Text("×${q.uses}", style = MaterialTheme.typography.labelSmall, color = colors.textDisabled)
                    }
                    Text(MoneyFormatter.format(q.amountMinor), style = ZedDataNumber, color = colors.accent)
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        showQuickSheet = false
                        onOpenEditor()
                    }
                    .padding(horizontal = ZedSpacing.xl, vertical = ZedSpacing.md)
            ) {
                Text(
                    text = stringResource(R.string.finance_quick_other),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textSecondary
                )
            }
            Spacer(Modifier.height(ZedSpacing.xl))
        }
    }
}

// Карточка баланса + прогноз на конец месяца
@Composable
private fun BalanceCard(summary: MonthSummaryUi) {
    val colors = LocalZedColors.current
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(ZedRadius.md),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, colors.borderVisible),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(ZedSpacing.lg)) {
            Row {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.finance_income), style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                    Spacer(Modifier.height(ZedSpacing.xs))
                    Text(summary.incomeText, style = ZedDataNumber, color = colors.textPrimary)
                }
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.finance_expense), style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                    Spacer(Modifier.height(ZedSpacing.xs))
                    Text(summary.expenseText, style = ZedDataNumber, color = colors.accent)
                }
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.finance_balance), style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                    Spacer(Modifier.height(ZedSpacing.xs))
                    Text(
                        summary.balanceText,
                        style = ZedDataNumber,
                        color = if (summary.balanceNegative) colors.accent else colors.textDisplay
                    )
                }
            }
            Spacer(Modifier.height(ZedSpacing.md))
            Text(
                text = stringResource(R.string.finance_forecast, summary.forecastText),
                style = MaterialTheme.typography.labelMedium,
                color = if (summary.forecastNegative) colors.accent else colors.success
            )
            Text(
                text = stringResource(R.string.finance_per_day, summary.perDayText),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textDisabled
            )
        }
    }
}

// Мини-календарь месяца
@Composable
private fun CalendarCard(
    monthLabel: String,
    weeks: List<List<CalendarDayUi?>>,
    onDayClick: (Long) -> Unit,
    onOpenFull: () -> Unit
) {
    val colors = LocalZedColors.current
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(ZedRadius.md),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, colors.borderVisible),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(ZedSpacing.lg)) {
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onOpenFull),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = monthLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textDisplay,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Outlined.CalendarMonth, null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(ZedSpacing.md))
            weeks.forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { day ->
                        if (day == null) {
                            Spacer(Modifier.weight(1f))
                        } else {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onDayClick(day.dateMillis) }
                                    .padding(vertical = ZedSpacing.xxs),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = day.day.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (day.isToday) colors.accent else colors.textSecondary
                                )
                                Spacer(Modifier.height(2.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier.height(4.dp)
                                ) {
                                    if (day.hasExpense) Box(Modifier.size(4.dp).background(colors.accent))
                                    if (day.hasIncome) Box(Modifier.size(4.dp).background(colors.textPrimary))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthBars(points: List<MonthPointUi>) {
    val colors = LocalZedColors.current
    val max = points.maxOfOrNull { maxOf(it.income, it.expense) } ?: 0L
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(ZedRadius.md),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, colors.borderVisible),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(ZedSpacing.lg).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ZedSpacing.sm)
        ) {
            points.forEach { point ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.height(72.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Bar(point.income, max, colors.textSecondary)
                        Bar(point.expense, max, colors.accent)
                    }
                    Spacer(Modifier.height(ZedSpacing.xs))
                    Text(
                        text = point.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textDisabled,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun Bar(value: Long, max: Long, color: Color) {
    val fraction = if (max > 0) value.toFloat() / max else 0f
    val height = (72.dp * fraction).coerceAtLeast(if (value > 0) 2.dp else 0.dp)
    Box(Modifier.width(6.dp).height(height).background(color))
}

@Composable
private fun TransactionRow(item: TransactionUiItem) {
    val colors = LocalZedColors.current
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(ZedRadius.md),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, colors.borderVisible),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(ZedSpacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.categoryName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.textPrimary,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (item.isRecurring) {
                        Spacer(Modifier.width(ZedSpacing.xs))
                        Icon(Icons.Outlined.Repeat, null, tint = colors.accent, modifier = Modifier.size(14.dp))
                    }
                }
                Spacer(Modifier.height(ZedSpacing.xs))
                Text(
                    text = if (item.note.isBlank()) item.dateLabel else "${item.dateLabel} · ${item.note}",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary
                )
            }
            Text(
                text = (if (item.isExpense) "- " else "+ ") + item.amountText,
                style = ZedDataNumber,
                color = if (item.isExpense) colors.accent else colors.textPrimary
            )
        }
    }
}
