package com.zed.app.feature.settings

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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
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
import com.zed.app.ui.components.zedAppear
import com.zed.app.ui.theme.LocalZedColors
import com.zed.app.ui.theme.ZedDataNumber
import com.zed.app.ui.theme.ZedRadius
import com.zed.app.ui.theme.ZedSpacing

// Экран «Активность и аналитика»: сессии, сравнение месяцев, цели привычек
@Composable
fun AnalyticsScreen(
    onBack: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val colors = LocalZedColors.current

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = ZedSpacing.xs, vertical = ZedSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, tint = colors.textSecondary)
            }
            Text(
                text = stringResource(R.string.analytics_title),
                style = MaterialTheme.typography.displaySmall,
                color = colors.textDisplay,
                modifier = Modifier.padding(start = ZedSpacing.sm)
            )
        }

        Column(
            Modifier.padding(ZedSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(ZedSpacing.md)
        ) {
            // --- Карточка активности: дни, время, запуски + точечная сетка месяца ---
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(ZedRadius.md),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(1.dp, colors.borderVisible),
                modifier = Modifier.fillMaxWidth().zedAppear(0)
            ) {
                Column(Modifier.padding(ZedSpacing.lg)) {
                    Text(
                        text = stringResource(R.string.act_section),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textDisabled
                    )
                    Spacer(Modifier.height(ZedSpacing.md))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.act_days_format, s.daysUsed, s.daysInMonth),
                            style = MaterialTheme.typography.displayMedium,
                            color = colors.textDisplay,
                            modifier = Modifier.weight(1f)
                        )
                        if (s.timeDeltaPct != null) {
                            Text(
                                text = (if (s.timeDeltaPct!! >= 0) "+" else "") + "${s.timeDeltaPct}%",
                                style = ZedDataNumber,
                                color = if (s.timeDeltaPct!! >= 0) colors.accent else colors.textSecondary
                            )
                        }
                    }
                    Spacer(Modifier.height(ZedSpacing.md))
                    // Точечная сетка дней месяца
                    s.dayFlags.chunked(7).forEach { rowFlags ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(ZedSpacing.xs)
                        ) {
                            rowFlags.forEach { used ->
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .height(10.dp)
                                        .background(
                                            if (used) colors.accent else colors.border,
                                            RoundedCornerShape(ZedRadius.xs)
                                        )
                                )
                            }
                            repeat(7 - rowFlags.size) { Spacer(Modifier.weight(1f)) }
                        }
                        Spacer(Modifier.height(ZedSpacing.xxs))
                    }
                    Spacer(Modifier.height(ZedSpacing.md))
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.act_time), style = MaterialTheme.typography.labelSmall, color = colors.textDisabled)
                            Text(
                                fmtHm(s.totalSeconds),
                                style = ZedDataNumber,
                                color = colors.textPrimary
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.act_avg), style = MaterialTheme.typography.labelSmall, color = colors.textDisabled)
                            Text(
                                fmtMs(s.avgSeconds),
                                style = ZedDataNumber,
                                color = colors.textPrimary
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.act_launches), style = MaterialTheme.typography.labelSmall, color = colors.textDisabled)
                            Text(
                                s.launches.toString(),
                                style = ZedDataNumber,
                                color = colors.textPrimary
                            )
                        }
                    }
                }
            }

            // --- Сравнение месяцев по расходам ---
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(ZedRadius.md),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(1.dp, colors.borderVisible),
                modifier = Modifier.fillMaxWidth().zedAppear(1)
            ) {
                Column(Modifier.padding(ZedSpacing.lg)) {
                    Text(
                        text = stringResource(R.string.fin_compare),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textDisabled
                    )
                    Spacer(Modifier.height(ZedSpacing.md))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(s.expenseCurText, style = ZedDataNumber, color = colors.textDisplay)
                            Text(
                                stringResource(R.string.cmp_cur),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textDisabled
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(s.expensePrevText, style = ZedDataNumber, color = colors.textSecondary)
                            Text(
                                stringResource(R.string.cmp_prev),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textDisabled
                            )
                        }
                    }
                    Spacer(Modifier.height(ZedSpacing.md))
                    Text(
                        text = when {
                            s.expenseDeltaPct == null -> stringResource(R.string.cmp_no_data)
                            s.expenseDeltaPct!! > 0 -> stringResource(R.string.cmp_more, s.expenseDeltaPct!!)
                            s.expenseDeltaPct!! < 0 -> stringResource(R.string.cmp_less, -s.expenseDeltaPct!!)
                            else -> stringResource(R.string.cmp_same)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = when {
                            s.expenseDeltaPct == null -> colors.textDisabled
                            s.expenseDeltaPct!! > 0 -> colors.accent
                            else -> colors.success
                        }
                    )
                }
            }

            // --- Цели по привычкам ---
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(ZedRadius.md),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(1.dp, colors.borderVisible),
                modifier = Modifier.fillMaxWidth().zedAppear(2)
            ) {
                Column(Modifier.padding(ZedSpacing.lg)) {
                    Text(
                        text = stringResource(R.string.goals_title),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textDisabled
                    )
                    Text(
                        text = stringResource(R.string.goals_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textDisabled
                    )
                    Spacer(Modifier.height(ZedSpacing.md))
                    if (s.goals.isEmpty()) {
                        Text("∅", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                    }
                    s.goals.forEach { goal ->
                        Column(Modifier.fillMaxWidth().padding(vertical = ZedSpacing.xs)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = goal.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.changeGoal(goal.habitId, goal.target - 1) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Outlined.Remove, null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                                }
                                Text(
                                    text = goal.target.toString(),
                                    style = ZedDataNumber,
                                    color = if (goal.target > 0) colors.accent else colors.textDisabled
                                )
                                IconButton(
                                    onClick = { viewModel.changeGoal(goal.habitId, goal.target + 1) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Outlined.Add, null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                            if (goal.target > 0) {
                                Spacer(Modifier.height(ZedSpacing.xxs))
                                Box(
                                    Modifier.fillMaxWidth().height(4.dp)
                                        .background(colors.border, RoundedCornerShape(ZedRadius.xs))
                                ) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth((goal.done30.toFloat() / goal.target).coerceIn(0.02f, 1f))
                                            .height(4.dp)
                                            .background(
                                                if (goal.done30 >= goal.target) colors.success else colors.accent,
                                                RoundedCornerShape(ZedRadius.xs)
                                            )
                                    )
                                }
                                Text(
                                    text = stringResource(R.string.goals_progress, goal.done30, goal.target),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (goal.done30 >= goal.target) colors.success else colors.textDisabled
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 3725 c → "1h 02m" / «1 ч 02 мин» через строки не тянем: формат универсальный
private fun fmtHm(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return "%d:%02d h".format(h, m)
}

private fun fmtMs(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d m".format(m, s)
}
