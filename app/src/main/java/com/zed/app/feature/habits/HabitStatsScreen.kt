package com.zed.app.feature.habits

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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

// Экран статистики привычек: сводка, heatmap 30 дней, дни недели, по привычкам
@Composable
fun HabitStatsScreen(
    onBack: () -> Unit,
    viewModel: HabitStatsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LocalZedColors.current

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Шапка
        Row(
            Modifier.fillMaxWidth().padding(horizontal = ZedSpacing.xs, vertical = ZedSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, tint = colors.textSecondary)
            }
            Text(
                text = stringResource(R.string.stats_title),
                style = MaterialTheme.typography.displaySmall,
                color = colors.textDisplay,
                modifier = Modifier.padding(start = ZedSpacing.sm)
            )
        }

        Column(Modifier.padding(ZedSpacing.lg), verticalArrangement = Arrangement.spacedBy(ZedSpacing.md)) {

            // --- Сводка: 4 числа ---
            StatsCard {
                Row(Modifier.fillMaxWidth()) {
                    StatCell(value = "${state.todayDone}/${state.todayTotal}", label = stringResource(R.string.stats_today), modifier = Modifier.weight(1f))
                    StatCell(value = state.bestStreak.toString(), label = stringResource(R.string.stats_best), modifier = Modifier.weight(1f))
                    StatCell(value = state.totalMarks.toString(), label = stringResource(R.string.stats_total), modifier = Modifier.weight(1f))
                    StatCell(value = state.activeHabits.toString(), label = stringResource(R.string.stats_active), modifier = Modifier.weight(1f))
                }
            }

            // --- Heatmap: последние 30 дней ---
            StatsCard {
                Text(
                    text = stringResource(R.string.stats_heat),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textDisabled
                )
                Spacer(Modifier.height(ZedSpacing.md))
                state.heat.chunked(7).forEach { rowCells ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ZedSpacing.xs)
                    ) {
                        rowCells.forEach { cell ->
                            HeatCell(ratio = cell.ratio, modifier = Modifier.weight(1f))
                        }
                        // Добиваем неполную строку пустыми, чтобы сетка не плясала
                        repeat(7 - rowCells.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(ZedSpacing.xs))
                }
            }

            // --- По дням недели ---
            StatsCard {
                Text(
                    text = stringResource(R.string.stats_week),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textDisabled
                )
                Spacer(Modifier.height(ZedSpacing.md))
                Row(
                    Modifier.fillMaxWidth().height(64.dp),
                    horizontalArrangement = Arrangement.spacedBy(ZedSpacing.sm),
                    verticalAlignment = Alignment.Bottom
                ) {
                    state.weekDays.forEach { wd ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        ) {
                            Box(
                                Modifier
                                    .width8()
                                    .height((56.dp * wd.ratio).coerceAtLeast(if (wd.ratio > 0f) 2.dp else 0.dp))
                                    .background(colors.accent)
                            )
                            Spacer(Modifier.height(ZedSpacing.xxs))
                            Text(
                                text = wd.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textDisabled,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // --- По привычкам ---
            StatsCard {
                Text(
                    text = stringResource(R.string.stats_habits),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textDisabled
                )
                Spacer(Modifier.height(ZedSpacing.md))
                if (state.perHabit.isEmpty()) {
                    Text(
                        text = "∅",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary
                    )
                }
                state.perHabit.forEach { row ->
                    Column(Modifier.fillMaxWidth().padding(vertical = ZedSpacing.xs)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = row.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textPrimary,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${row.streak} / ${row.best}",
                                style = ZedDataNumber,
                                color = colors.textSecondary
                            )
                        }
                        Spacer(Modifier.height(ZedSpacing.xxs))
                        // Полоса выполнения за 30 дней
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(colors.border, RoundedCornerShape(ZedRadius.xs))
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(row.ratio30.coerceIn(0.02f, 1f))
                                    .height(4.dp)
                                    .background(colors.accent, RoundedCornerShape(ZedRadius.xs))
                            )
                        }
                        Spacer(Modifier.height(ZedSpacing.xxs))
                        Text(
                            text = "${(row.ratio30 * 100).toInt()}% · 30d",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textDisabled
                        )
                    }
                }
            }
        }
    }
}

// Карточка-обёртка статистики
@Composable
private fun StatsCard(content: @Composable () -> Unit) {
    val colors = LocalZedColors.current
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(ZedRadius.md),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, colors.borderVisible),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(ZedSpacing.lg)) { content() }
    }
}

// Число + подпись в сводке
@Composable
private fun StatCell(value: String, label: String, modifier: Modifier = Modifier) {
    val colors = LocalZedColors.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = ZedDataNumber, color = colors.textDisplay, maxLines = 1)
        Spacer(Modifier.height(ZedSpacing.xxs))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = colors.textDisabled, maxLines = 1)
    }
}

// Квадрат heatmap: интенсивность заливки = доля выполнения за день
@Composable
private fun HeatCell(ratio: Float, modifier: Modifier = Modifier) {
    val colors = LocalZedColors.current
    val fill = when {
        ratio <= 0f -> Color_Transparent
        ratio < 0.34f -> colors.accent.copy(alpha = 0.3f)
        ratio < 0.67f -> colors.accent.copy(alpha = 0.6f)
        else -> colors.accent
    }
    Box(
        modifier = modifier
            .size(14.dp)
            .then(
                if (ratio <= 0f) Modifier.background(colors.border, RoundedCornerShape(ZedRadius.xs))
                else Modifier.background(fill, RoundedCornerShape(ZedRadius.xs))
            )
    )
}

private val Color_Transparent = androidx.compose.ui.graphics.Color.Transparent

// Тонкая полоска столбика дня недели
private fun Modifier.width8(): Modifier = this.then(androidx.compose.foundation.layout.widthModifier8)

private val androidx.compose.foundation.layout.widthModifier8: Modifier
    get() = androidx.compose.foundation.layout.SpacerModifier8.w

private object androidx.compose.foundation.layout.SpacerModifier8 {
    val w: Modifier = Modifier.padding(horizontal = 4.dp)
}
