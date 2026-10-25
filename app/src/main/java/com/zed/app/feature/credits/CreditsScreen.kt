package com.zed.app.feature.credits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextAlign
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
import java.time.format.DateTimeFormatter
import java.util.Locale

// ============================================================
// Экран «Кредиты» в стиле Nothing OS (стеклянные карточки).
// ФУНКЦИИ СОХРАНЕНЫ ПОЛНОСТЬЮ:
// — чекбокс строки = отметить/снять оплату месяца (автотранзакция в финансах);
// — тап по строке = редактор кредита;
// — свайп строки = удаление со снекбаром «ОТМЕНИТЬ» (undo возвращает оплаты);
// — FAB «+» = новый кредит.
// ИЗ МАКЕТОВ: календарь платежей точечной сеткой (красный = списание близко
// или просрочено, серый = оплачено, контур = будущий платёж), ближайший
// платёж крупной точечной суммой, полосы прогресса погашения.
// ============================================================
@Composable
fun CreditsScreen(
    onOpenSettings: () -> Unit,
    onOpenEditor: (Int) -> Unit,
    viewModel: CreditsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LocalZedColors.current
    val snackbarHostState = remember { SnackbarHostState() }
    val deletedText = stringResource(R.string.credits_deleted)
    val undoText = stringResource(R.string.undo)

    val today = LocalDate.now()
    // Ближайший неоплаченный платёж для крупной карточки
    val next = state.credits.filter { !it.paidThisMonth }.minByOrNull { it.days }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ZedTopBar(
                title = stringResource(R.string.tab_credits),
                onSettingsClick = onOpenSettings
            )

            LazyColumn(
                contentPadding = PaddingValues(ZedSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(ZedSpacing.md)
            ) {
                // --- Календарь платежей: точечная сетка месяца ---
                item {
                    ZedGlassCard {
                        Text(
                            text = stringResource(R.string.cr_calendar),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textDisabled
                        )
                        Spacer(Modifier.height(ZedSpacing.md))

                        // Подписи дней недели
                        val weekdays = stringResource(R.string.cr_weekdays).split(",")
                        Row(Modifier.fillMaxWidth()) {
                            weekdays.forEach { wd ->
                                Text(
                                    text = wd,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textDisabled,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Spacer(Modifier.height(ZedSpacing.xs))

                        // Сетка дней: смещение первого дня (неделя с понедельника)
                        val firstOffset = today.withDayOfMonth(1).dayOfWeek.value - 1
                        val daysInMonth = today.lengthOfMonth()
                        val cells = List(firstOffset) { 0 } + (1..daysInMonth).toList()

                        cells.chunked(7).forEach { row ->
                            Row(Modifier.fillMaxWidth().padding(vertical = ZedSpacing.xxs)) {
                                row.forEach { day ->
                                    Box(
                                        Modifier.weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (day > 0) DayCell(day = day, credits = state.credits)
                                    }
                                }
                                repeat(7 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }

                // --- Ближайший платёж крупной точечной суммой ---
                if (next != null) {
                    item {
                        ZedGlassCard {
                            Text(
                                text = stringResource(R.string.cr_next),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.textDisabled
                            )
                            Spacer(Modifier.height(ZedSpacing.xs))
                            Text(
                                text = next.monthlyText,
                                style = TextStyle(
                                    fontFamily = DotoFont,
                                    fontSize = 30.sp,
                                    letterSpacing = 0.06.em,
                                    color = if (next.countdownDanger) colors.accent else colors.textDisplay
                                )
                            )
                            Spacer(Modifier.height(ZedSpacing.xxs))
                            val dueDate = today.plusDays(next.days)
                                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.getDefault()))
                            Text(
                                text = next.title + " · " + stringResource(R.string.cr_due_format, dueDate),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.textSecondary
                            )
                        }
                    }
                }

                // --- Пустое состояние ---
                if (state.loaded && state.credits.isEmpty()) {
                    item {
                        Column(
                            Modifier.fillMaxWidth().padding(ZedSpacing.xxl),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "∅",
                                style = MaterialTheme.typography.displayMedium,
                                color = colors.textDisabled
                            )
                            Text(
                                text = stringResource(R.string.cr_empty_title),
                                style = MaterialTheme.typography.headlineMedium,
                                color = colors.textDisplay,
                                modifier = Modifier.padding(top = ZedSpacing.lg)
                            )
                            Text(
                                text = stringResource(R.string.cr_empty_body),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(top = ZedSpacing.sm)
                            )
                        }
                    }
                }

                // --- Строки кредитов: чекбокс оплаты + прогресс погашения ---
                items(state.credits, key = { it.id }) { item ->
                    val dismissState = rememberSwipeToDismissBoxState()
                    LaunchedEffect(dismissState.currentValue) {
                        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
                            viewModel.deleteWithSnapshot(item.id)
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
                            }
                        },
                        content = {
                            CreditGlassRow(
                                item = item,
                                onTogglePaid = { viewModel.togglePaid(item.id, !item.paidThisMonth) },
                                onClick = { onOpenEditor(item.id) }
                            )
                        }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { onOpenEditor(-1) },
            shape = RoundedCornerShape(ZedRadius.md),
            containerColor = colors.accent,
            contentColor = Color.White,
            modifier = Modifier.align(Alignment.BottomEnd).padding(ZedSpacing.xl)
        ) {
            Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.cr_add))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

// Ячейка дня календаря: серый = оплачено, красный = списание ≤3 дней
// или просрочено, контур = будущий платёж, без рамки = обычный день
@Composable
private fun DayCell(day: Int, credits: List<CreditUiItem>) {
    val colors = LocalZedColors.current
    val marks = credits.filter { it.payDay == day }
    val danger = marks.any { !it.paidThisMonth && it.days <= 3 }
    val paid = marks.any { it.paidThisMonth }
    val future = marks.isNotEmpty() && !danger && !paid

    Box(
        Modifier
            .size(30.dp)
            .then(
                when {
                    danger -> Modifier.background(colors.accent, RoundedCornerShape(ZedRadius.sm))
                    paid -> Modifier.background(colors.borderVisible, RoundedCornerShape(ZedRadius.sm))
                    future -> Modifier.border(1.dp, colors.borderVisible, RoundedCornerShape(ZedRadius.sm))
                    else -> Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = when {
                danger -> Color.White
                paid -> colors.textSecondary
                future -> colors.textPrimary
                else -> colors.textDisabled
            }
        )
    }
}

// Стеклянная строка кредита: чекбокс оплаты + название + прогресс погашения
@Composable
private fun CreditGlassRow(
    item: CreditUiItem,
    onTogglePaid: () -> Unit,
    onClick: () -> Unit
) {
    val colors = LocalZedColors.current

    ZedGlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Чекбокс «оплачено за этот месяц»
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .then(
                        if (item.paidThisMonth) {
                            Modifier.background(colors.success, RoundedCornerShape(ZedRadius.xs))
                        } else {
                            Modifier.border(1.dp, colors.borderVisible, RoundedCornerShape(ZedRadius.xs))
                        }
                    )
                    .clickable(onClick = onTogglePaid),
                contentAlignment = Alignment.Center
            ) {
                if (item.paidThisMonth) {
                    Text(text = "✓", style = TextStyle(fontSize = 12.sp, color = Color.White))
                }
            }
            Spacer(Modifier.width(ZedSpacing.md))

            // Название + сумма + прогресс; тап по зоне = редактор
            Column(
                Modifier
                    .weight(1f)
                    .clickable(onClick = onClick)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.textPrimary,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = item.monthlyText,
                        style = ZedDataNumber,
                        color = colors.textSecondary
                    )
                }
                Spacer(Modifier.height(ZedSpacing.xxs))
                // Полоса погашения + счётчик месяцев
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(4.dp)
                            .background(colors.border, RoundedCornerShape(ZedRadius.xs))
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(item.progressFraction.coerceIn(0.02f, 1f))
                                .height(4.dp)
                                .background(colors.accent, RoundedCornerShape(ZedRadius.xs))
                        )
                    }
                    Spacer(Modifier.width(ZedSpacing.sm))
                    Text(
                        text = item.monthsLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textDisabled
                    )
                }
                Spacer(Modifier.height(ZedSpacing.xxs))
                Text(
                    text = when {
                        item.paidThisMonth -> stringResource(R.string.widget_paid)
                        item.days < 0 -> stringResource(R.string.widget_overdue)
                        else -> stringResource(R.string.widget_day, item.days.toInt())
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = when {
                        item.paidThisMonth -> colors.success
                        item.countdownDanger -> colors.accent
                        else -> colors.textSecondary
                    }
                )
            }
        }
    }
}
