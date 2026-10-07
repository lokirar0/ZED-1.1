package com.zed.app.feature.finance

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zed.app.R
import com.zed.app.ui.components.ZedTopBar
import com.zed.app.ui.theme.LocalZedColors
import com.zed.app.ui.theme.ZedDataNumber
import com.zed.app.ui.theme.ZedRadius
import com.zed.app.ui.theme.ZedSpacing
import kotlinx.coroutines.launch

@Composable
fun FinanceScreen(
    onOpenSettings: () -> Unit,
    onOpenEditor: () -> Unit,
    viewModel: FinanceViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LocalZedColors.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deletedText = stringResource(R.string.finance_deleted)
    val exportedText = stringResource(R.string.finance_exported)

    // Системный диалог сохранения файла (SAF, без разрешений)
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
                // Пустое состояние
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

        FloatingActionButton(
            onClick = onOpenEditor,
            shape = RoundedCornerShape(ZedRadius.md),
            containerColor = colors.accent,
            contentColor = Color.White,
            modifier = Modifier.align(Alignment.BottomEnd).padding(ZedSpacing.xl)
        ) {
            Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.finance_add))
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

// Карточка баланса: ДОХОД / РАСХОД / БАЛАНС моно-цифрами
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
        Row(Modifier.padding(ZedSpacing.lg)) {
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
    }
}

// Точечный график: 6 месяцев, пары столбиков (доход / расход)
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

// Один столбик: высота пропорциональна значению
@Composable
private fun Bar(value: Long, max: Long, color: Color) {
    val fraction = if (max > 0) value.toFloat() / max else 0f
    val height = (72.dp * fraction).coerceAtLeast(if (value > 0) 2.dp else 0.dp)
    Box(Modifier.width(6.dp).height(height).background(color))
}

// Строка операции: категория + дата слева, моно-сумма справа
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
                Text(
                    text = item.categoryName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textPrimary
                )
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
