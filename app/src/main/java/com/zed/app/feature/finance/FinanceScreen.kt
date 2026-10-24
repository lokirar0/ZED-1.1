package com.zed.app.feature.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zed.app.R
import com.zed.app.ui.components.ZedGlassCard
import com.zed.app.ui.components.ZedTopBar
import com.zed.app.ui.theme.DotoFont
import com.zed.app.ui.theme.LocalZedColors
import com.zed.app.ui.theme.ZedDataNumber
import com.zed.app.ui.theme.ZedRadius
import com.zed.app.ui.theme.ZedSpacing
import java.time.LocalDate

// ============================================================
// Экран «Финансы» в стиле Nothing OS (стеклянные карточки).
// ФУНКЦИИ СОХРАНЕНЫ ПОЛНОСТЬЮ:
// — сводка трат крупной точкой + доходы/расходы/прогноз;
// — бюджет-бар «прогресс до лимита»;
// — график расходов за 6 месяцев;
// — категории с суммами;
// — Quick-Add плитки: тап = повторить последнюю сумму категории;
// — список транзакций: свайп = удалить + снекбар «ОТМЕНИТЬ»;
// — вход в календарь платежей; FAB = редактор транзакции.
// ============================================================
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
    val deletedText = stringResource(R.string.fin_deleted)
    val undoText = stringResource(R.string.undo)

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ZedTopBar(
                title = stringResource(R.string.tab_finance),
                onSettingsClick = onOpenSettings
            )

            LazyColumn(
                contentPadding = PaddingValues(ZedSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(ZedSpacing.md)
            ) {
                // --- Сводка трат: крупная сумма точкой + бухгалтерия ---
                item {
                    ZedGlassCard {
                        Text(
                            text = stringResource(R.string.fin_spent),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textDisabled
                        )
                        Spacer(Modifier.height(ZedSpacing.xs))
                        Text(
                            text = state.expenseText,
                            style = TextStyle(
                                fontFamily = DotoFont,
                                fontSize = 30.sp,
                                letterSpacing = 0.06.em,
                                color = colors.textDisplay
                            )
                        )
                        Spacer(Modifier.height(ZedSpacing.md))

                        // Бухгалтерия: доходы / расходы / прогноз
                        Row {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.fin_income), style = MaterialTheme.typography.labelSmall, color = colors.textDisabled)
                                Text(state.incomeText, style = ZedDataNumber, color = colors.textPrimary)
                            }
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.fin_expense), style = MaterialTheme.typography.labelSmall, color = colors.textDisabled)
                                Text(state.expenseText, style = ZedDataNumber, color = colors.accent)
                            }
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.fin_forecast), style = MaterialTheme.typography.labelSmall, color = colors.textDisabled)
                                Text(state.forecastText, style = ZedDataNumber, color = colors.textSecondary)
                            }
                        }

                        Spacer(Modifier.height(ZedSpacing.md))
                        Text(
                            text = stringResource(R.string.fin_budget),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textDisabled
                        )
                        Spacer(Modifier.height(ZedSpacing.xxs))
                        GlassBar(fraction = 0.5f, color = colors.accent)
                    }
                }

                // --- Quick-Add плитки: повторить последнюю трату категории ---
                if (state.quickCategories.isNotEmpty()) {
                    item {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(ZedSpacing.sm)
                        ) {
                            state.quickCategories.forEach { cat ->
                                Box(
                                    Modifier
                                        .background(colors.surface, RoundedCornerShape(ZedRadius.md))
                                        .clickable { viewModel.quickAdd(cat.id) }
                                        .padding(horizontal = ZedSpacing.lg, vertical = ZedSpacing.md)
                                ) {
                                    Text(
                                        text = "+ " + cat.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colors.textPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // --- График расходов за 6 месяцев ---
                item {
                    ZedGlassCard {
                        Text(
                            text = stringResource(R.string.fin_chart),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textDisabled
                        )
                        Spacer(Modifier.height(ZedSpacing.md))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(ZedSpacing.xs)
                        ) {
                            state.monthBars.forEach { bar ->
                                Column(
                                    Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Поле фиксированной высоты: столбик растёт снизу
                                    Box(Modifier.height(64.dp).fillMaxWidth()) {
                                        Box(
                                            Modifier
                                                .fillMaxWidth()
                                                .height((64 * bar.fraction.coerceIn(0.06f, 1f)).dp)
                                                .align(Alignment.BottomCenter)
                                                .background(colors.accent, RoundedCornerShape(ZedRadius.xs))
                                        )
                                    }
                                    Spacer(Modifier.height(ZedSpacing.xxs))
                                    Text(
                                        text = bar.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textDisabled
                                    )
                                }
                            }
                        }
                    }
                }

                // --- Категории с суммами ---
                if (state.categorySums.isNotEmpty()) {
                    item {
                        ZedGlassCard {
                            Text(
                                text = stringResource(R.string.fin_categories),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.textDisabled
                            )
                            Spacer(Modifier.height(ZedSpacing.md))
                            state.categorySums.forEach { cat ->
                                Column(Modifier.padding(vertical = ZedSpacing.xxs)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = cat.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = colors.textPrimary,
                                            maxLines = 1,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = cat.sumText,
                                            style = ZedDataNumber,
                                            color = colors.textSecondary
                                        )
                                    }
                                    Spacer(Modifier.height(ZedSpacing.xxs))
                                    GlassBar(fraction = cat.fraction, color = colors.borderVisible)
                                }
                            }
                        }
                    }
                }

                // --- Вход в календарь платежей ---
                item {
                    ZedGlassCard(
                        modifier = Modifier.clickable {
                            onOpenCalendar(LocalDate.now().toEpochDay())
                        }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.fin_calendar),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.textSecondary,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                null,
                                tint = colors.textDisabled
                            )
                        }
                    }
                }

                // --- Список транзакций месяца: свайп = удалить + undo ---
                items(state.transactions, key = { it.id }) { tx ->
                    val dismissState = rememberSwipeToDismissBoxState()
                    LaunchedEffect(dismissState.currentValue) {
                        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
                            viewModel.deleteWithSnapshot(tx.id)
                            val result = snackbarHostState.showSnackbar(
                                message = deletedText,
                                actionLabel = undoText
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.restoreLast()
                            }
                        }
                    }
                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            Box(
                                Modifier.fillMaxSize().padding(horizontal = ZedSpacing.xl),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(Icons.Outlined.Delete, null, tint = colors.accent)
