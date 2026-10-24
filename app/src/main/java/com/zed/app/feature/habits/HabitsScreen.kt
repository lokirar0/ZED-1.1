package com.zed.app.feature.habits

import androidx.compose.foundation.background
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

// ============================================================
// Экран «Привычки» в стиле Nothing OS (стеклянные карточки).
// ФУНКЦИИ СОХРАНЕНЫ ПОЛНОСТЬЮ:
// — чекбокс строки = отметить/снять за сегодня;
// — тап по строке = редактор привычки;
// — свайп строки = удаление со снекбаром «ОТМЕНИТЬ» (undo);
// — карточка-вход «КУРСЫ» и карточка-вход «СТАТИСТИКА»;
// — FAB «+» = новая привычка; поиск/настройки в топбаре.
// ============================================================
@Composable
fun HabitsScreen(
    onOpenSettings: () -> Unit,
    onOpenEditor: (Int) -> Unit,
    onOpenStats: () -> Unit,
    onOpenCourses: () -> Unit,
    viewModel: HabitsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LocalZedColors.current
    val snackbarHostState = remember { SnackbarHostState() }
    val deletedText = stringResource(R.string.habits_deleted)
    val undoText = stringResource(R.string.undo)

    val pct = if (state.todayTotal > 0) state.todayDone * 100 / state.todayTotal else 0

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ZedTopBar(
                title = stringResource(R.string.tab_habits),
                onSettingsClick = onOpenSettings
            )

            LazyColumn(
                contentPadding = PaddingValues(ZedSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(ZedSpacing.md)
            ) {
                // --- Стеклянная карточка TODAY: процент, прогресс, серия ---
                item {
                    ZedGlassCard {
                        Text(
                            text = stringResource(R.string.habits_today),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textDisabled
                        )
                        Spacer(Modifier.height(ZedSpacing.xs))
                        Text(
                            text = stringResource(R.string.habits_completed_format, pct),
                            style = TextStyle(
                                fontFamily = DotoFont,
                                fontSize = 26.sp,
                                letterSpacing = 0.06.em,
                                color = colors.textDisplay
                            )
                        )
                        Spacer(Modifier.height(ZedSpacing.md))

                        // Прогресс за сегодня
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.habits_section),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${state.todayDone}/${state.todayTotal}",
                                style = ZedDataNumber,
                                color = colors.textPrimary
                            )
                        }
                        Spacer(Modifier.height(ZedSpacing.xxs))
                        ThinBar(
                            fraction = if (state.todayTotal > 0) {
                                state.todayDone.toFloat() / state.todayTotal
                            } else 0f,
                            color = colors.accent
                        )
                        Spacer(Modifier.height(ZedSpacing.md))

                        // Счётчики серии: сегменты 7 дней
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.habits_streak),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = stringResource(R.string.habits_days_format, state.bestStreak),
                                style = ZedDataNumber,
                                color = colors.textPrimary
                            )
                        }
                        Spacer(Modifier.height(ZedSpacing.xxs))
                        Row(horizontalArrangement = Arrangement.spacedBy(ZedSpacing.xxs)) {
                            val filled = state.bestStreak.coerceIn(0, 7)
                            for (i in 0 until 7) {
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .height(8.dp)
                                        .background(
                                            if (i < filled) colors.accent else colors.border,
                                            RoundedCornerShape(ZedRadius.xs)
                                        )
                                )
                            }
                        }
                    }
                }

                // --- Вход в курсы приёма (стеклянная строка) ---
                item {
                    ZedGlassCard(
                        modifier = Modifier.clickable(onClick = onOpenCourses)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = state.coursesLabel
                                    ?: stringResource(R.string.courses_entry_empty),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (state.coursesLabel != null) colors.textSecondary else colors.textDisabled,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = stringResource(R.string.courses_entry),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.accent
                            )
                            Icon(
                                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                null,
                                tint = colors.textDisabled
                            )
                        }
                    }
                }

                // --- Вход в статистику (стеклянная строка) ---
                if (state.todayTotal > 0) {
                    item {
                        ZedGlassCard(
                            modifier = Modifier.clickable(onClick = onOpenStats)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(
                                        R.string.habits_stats,
                                        state.todayDone,
                                        state.todayTotal,
                                        state.bestStreak
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.textSecondary,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = stringResource(R.string.stats_title),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.accent
                                )
                                Icon(
                                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                    null,
                                    tint = colors.textDisabled
                                )
                            }
                        }
                    }
                }

                // --- Список привычек: стеклянные строки ---
                if (state.loaded && state.habits.isEmpty()) {
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
                                text = stringResource(R.string.habits_empty_title),
                                style = MaterialTheme.typography.headlineMedium,
                                color = colors.textDisplay,
                                modifier = Modifier.padding(top = ZedSpacing.lg)
                            )
                            Text(
                                text = stringResource(R.string.habits_empty_body),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(top = ZedSpacing.sm)
                            )
                        }
                    }
                }

                items(state.habits, key = { it.id }) { item ->
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
                            HabitGlassRow(
                                item = item,
                                onToggle = { viewModel.toggle(item.id) },
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
            Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.habits_add))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

// Стеклянная строка привычки: чекбокс + название + серия + тонкий бар недели
@Composable
private fun HabitGlassRow(
    item: HabitUiItem,
    onToggle: () -> Unit,
    onClick: () -> Unit
) {
    val colors = LocalZedColors.current
    val weekDone = item.weekFlags.count { it }

    ZedGlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Квадратный чекбокс в стиле ZED (тап = отметить/снять)
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .then(
                        if (item.doneToday) {
                            Modifier.background(colors.accent, RoundedCornerShape(ZedRadius.xs))
                        } else {
                            Modifier.background(Color.Transparent, RoundedCornerShape(ZedRadius.xs))
                                .then(Modifier.border1dp(colors.borderVisible, RoundedCornerShape(ZedRadius.xs)))
                        }
                    )
                    .clickable(onClick = onToggle),
                contentAlignment = Alignment.Center
            ) {
                if (item.doneToday) {
                    Text(
                        text = "✓",
                        style = TextStyle(fontSize = 12.sp, color = Color.White)
                    )
                }
            }
            Spacer(Modifier.width(ZedSpacing.md))

            // Название + серия + бар недели; тап по этой зоне = редактор
            Column(
                Modifier
                    .weight(1f)
                    .clickable(onClick = onClick)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (item.doneToday) colors.textDisplay else colors.textPrimary,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = stringResource(R.string.habits_days_format, item.streak),
                        style = ZedDataNumber,
                        color = if (item.streak > 0) colors.accent else colors.textDisabled
                    )
                }
                if (item.note.isNotBlank()) {
                    Text(
                        text = item.note,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textDisabled,
                        maxLines = 1
                    )
                }
                Spacer(Modifier.height(ZedSpacing.xxs))
                ThinBar(fraction = weekDone / 7f, color = colors.borderVisible)
            }
        }
    }
}

// Тонкий прогресс-бар (4dp) со скруглением
@Composable
private fun ThinBar(fraction: Float, color: Color) {
    val colors = LocalZedColors.current
    Box(
        Modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(colors.border, RoundedCornerShape(ZedRadius.xs))
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                .height(4.dp)
                .background(color, RoundedCornerShape(ZedRadius.xs))
        )
    }
}

// Хелпер границы 1dp (чтобы не тащить лишние импорты в тело строки)
private fun Modifier.border1dp(color: Color, shape: RoundedCornerShape): Modifier =
    this.then(androidx.compose.foundation.border(1.dp, color, shape))
