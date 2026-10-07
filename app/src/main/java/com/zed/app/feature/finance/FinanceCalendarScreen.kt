package com.zed.app.feature.finance

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zed.app.R
import com.zed.app.ui.theme.LocalZedColors
import com.zed.app.ui.theme.ZedDataNumber
import com.zed.app.ui.theme.ZedRadius
import com.zed.app.ui.theme.ZedSpacing

// Календарь операций: сетка месяца с точками + список выбранного дня
@Composable
fun FinanceCalendarScreen(
    onBack: () -> Unit,
    onAddTransaction: (Long, Int) -> Unit, // dateMillis, type (0=расход, 1=доход)
    viewModel: FinanceCalendarViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LocalZedColors.current

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Шапка: назад + месяц + стрелки
        Row(
            Modifier.fillMaxWidth().padding(horizontal = ZedSpacing.xs, vertical = ZedSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, tint = colors.textSecondary)
            }
            IconButton(onClick = viewModel::prevMonth) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, null, tint = colors.textSecondary)
            }
            Text(
                text = state.monthLabel,
                style = MaterialTheme.typography.headlineMedium,
                color = colors.textDisplay,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
            IconButton(onClick = viewModel::nextMonth) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = colors.textSecondary)
            }
        }

        // Подписи дней недели
        Row(Modifier.fillMaxWidth().padding(horizontal = ZedSpacing.lg)) {
            state.weekDays.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textDisabled,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
            }
        }
        Spacer(Modifier.height(ZedSpacing.sm))

        // Сетка недель
        state.weeks.forEach { week ->
            Row(Modifier.fillMaxWidth().padding(horizontal = ZedSpacing.lg)) {
                week.forEach { day ->
                    if (day == null) {
                        Spacer(Modifier.weight(1f))
                    } else {
                        DayCell(
                            day = day,
                            selected = state.selectedDate == day.dateMillis,
                            onClick = { viewModel.selectDay(day.dateMillis) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            Spacer(Modifier.height(ZedSpacing.xxs))
        }

        Spacer(Modifier.height(ZedSpacing.lg))

        // Выбранный день
        if (state.selectedDate != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(ZedRadius.md),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(1.dp, colors.borderVisible),
                modifier = Modifier.fillMaxWidth().padding(horizontal = ZedSpacing.lg)
            ) {
                Column(Modifier.padding(ZedSpacing.lg)) {
                    Text(
                        text = state.selectedLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textDisplay
                    )
                    Spacer(Modifier.height(ZedSpacing.xs))
                    Row {
                        Text(
                            text = "+ ${state.dayIncomeText}",
                            style = ZedDataNumber,
                            color = colors.textPrimary
                        )
                        Spacer(Modifier.width2())
                        Text(
                            text = "- ${state.dayExpenseText}",
                            style = ZedDataNumber,
                            color = colors.accent
                        )
                    }

                    Spacer(Modifier.height(ZedSpacing.md))
                    HorizontalDivider(color = colors.border)
                    Spacer(Modifier.height(ZedSpacing.md))

                    if (state.dayItems.isEmpty()) {
                        Text(
                            text = stringResource(R.string.finance_day_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary
                        )
                    } else {
                        state.dayItems.forEach { item ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = ZedSpacing.xxs),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.textPrimary,
                                        maxLines = 1
                                    )
                                    if (item.subtitle.isNotBlank()) {
                                        Text(
                                            text = item.subtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.textSecondary,
                                            maxLines = 1
                                        )
                                    }
                                }
                                Text(
                                    text = (if (item.isExpense) "- " else "+ ") + item.amountText,
                                    style = ZedDataNumber,
                                    color = if (item.isExpense) colors.accent else colors.textPrimary
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(ZedSpacing.md))

                    // Добавить операцию задним числом в выбранный день
                    Row(horizontalArrangement = Arrangement.spacedBy(ZedSpacing.sm)) {
                        TextButton(
                            onClick = { onAddTransaction(state.selectedDate!!, 0) },
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, colors.accent, RoundedCornerShape(ZedRadius.md))
                        ) {
                            Text(
                                stringResource(R.string.finance_add_expense),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.accent
                            )
                        }
                        TextButton(
                            onClick = { onAddTransaction(state.selectedDate!!, 1) },
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, colors.borderVisible, RoundedCornerShape(ZedRadius.md))
                        ) {
                            Text(
                                stringResource(R.string.finance_add_income),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.textPrimary
                            )
                        }
                    }
                }
            }
        } else {
            Text(
                text = stringResource(R.string.finance_day_hint),
                style = MaterialTheme.typography.labelMedium,
                color = colors.textDisabled,
                modifier = Modifier.padding(horizontal = ZedSpacing.xl)
            )
        }

        Spacer(Modifier.height(ZedSpacing.xxl))
    }
}

// Ячейка дня: число + точки (красная = расход, белая = доход)
@Composable
private fun DayCell(
    day: CalendarDayUi,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalZedColors.current
    Column(
        modifier = modifier
            .padding(1.dp)
            .then(
                if (selected) Modifier.border(1.dp, colors.accent, RoundedCornerShape(ZedRadius.sm))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(vertical = ZedSpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = day.day.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = if (day.isToday) colors.accent else colors.textPrimary
        )
        Spacer(Modifier.height(ZedSpacing.xxs))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.height(4.dp)) {
            if (day.hasExpense) Box(Modifier.size(4.dp).background(colors.accent))
            if (day.hasIncome) Box(Modifier.size(4.dp).background(colors.textPrimary))
        }
    }
}

// Маленький хелпер отступа (weight уже занят в Row выше)
private fun Modifier.width2(): Modifier = this.then(androidx.compose.foundation.layout.SpacerModifierHolder.spacer)

private object androidx.compose.foundation.layout.SpacerModifierHolder {
    val spacer: Modifier = Modifier.padding(horizontal = ZedSpacing.sm)
}
