package com.zed.app.feature.courses

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zed.app.R
import com.zed.app.ui.theme.LocalZedColors
import com.zed.app.ui.theme.ZedDataNumber
import com.zed.app.ui.theme.ZedRadius
import com.zed.app.ui.theme.ZedSpacing

// Список курсов приёма: день X/N, чек-чипы приёмов на сегодня, статусы
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CoursesScreen(
    onBack: () -> Unit,
    onOpenEditor: (Int) -> Unit,
    viewModel: CoursesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LocalZedColors.current

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = ZedSpacing.xs, vertical = ZedSpacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, tint = colors.textSecondary)
                }
                Text(
                    text = stringResource(R.string.courses_title),
                    style = MaterialTheme.typography.displaySmall,
                    color = colors.textDisplay,
                    modifier = Modifier.padding(start = ZedSpacing.sm)
                )
            }

            if (state.loaded && state.courses.isEmpty()) {
                Column(
                    Modifier.fillMaxSize().padding(ZedSpacing.xxl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("∅", style = MaterialTheme.typography.displayMedium, color = colors.textDisabled)
                    Text(
                        text = stringResource(R.string.courses_empty_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.textDisplay,
                        modifier = Modifier.padding(top = ZedSpacing.lg)
                    )
                    Text(
                        text = stringResource(R.string.courses_empty_body),
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
                    items(state.courses, key = { it.id }) { item ->
                        CourseCard(item = item, onToggleDose = { min -> viewModel.toggleDose(item.id, min) }, onOpen = { onOpenEditor(item.id) })
                    }
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
            Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.courses_add))
        }
    }
}

// Карточка курса: тап = редактор, чипы приёмов кликаются отдельно
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CourseCard(
    item: CourseUiItem,
    onToggleDose: (Int) -> Unit,
    onOpen: () -> Unit
) {
    val colors = LocalZedColors.current
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(ZedRadius.md),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, colors.borderVisible),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
    ) {
        Column(Modifier.padding(ZedSpacing.lg)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textPrimary,
                maxLines = 1
            )
            Spacer(Modifier.height(ZedSpacing.xs))

            // Статус-строка: день курса / интервал / даты
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (item.status) {
                    0 -> Text(
                        text = stringResource(R.string.courses_not_started, item.startLabel),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary
                    )
                    2 -> Text(
                        text = stringResource(R.string.courses_finished),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.success
                    )
                    else -> {
                        Text(
                            text = stringResource(R.string.courses_day, item.dayIndex, item.totalDays),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.accent
                        )
                        Spacer(Modifier.width(ZedSpacing.md))
                        Text(
                            text = stringResource(R.string.courses_every, item.intervalHours),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textDisabled
                        )
                    }
                }
            }

            // Приёмы на сегодня: чек-чипы с временами
            if (item.status == 1 && item.doses.isNotEmpty()) {
                Spacer(Modifier.height(ZedSpacing.md))
                Text(
                    text = stringResource(R.string.courses_today_doses),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textDisabled
                )
                Spacer(Modifier.height(ZedSpacing.xs))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(ZedSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(ZedSpacing.sm)
                ) {
                    item.doses.forEach { dose ->
                        DoseChip(label = dose.label, taken = dose.taken) { onToggleDose(dose.minutes) }
                    }
                }
                Spacer(Modifier.height(ZedSpacing.md))
                // Прогресс дня: принято / всего
                Box(
                    Modifier.fillMaxWidth().height(4.dp)
                        .background(colors.border, RoundedCornerShape(ZedRadius.xs))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(
                                (item.takenCount.toFloat() / item.doseCount.coerceAtLeast(1)).coerceIn(0.02f, 1f)
                            )
                            .height(4.dp)
                            .background(
                                if (item.takenCount >= item.doseCount) colors.success else colors.accent,
                                RoundedCornerShape(ZedRadius.xs)
                            )
                    )
                }
                Spacer(Modifier.height(ZedSpacing.xxs))
                Text(
                    text = "${item.takenCount}/${item.doseCount}",
                    style = ZedDataNumber,
                    color = colors.textSecondary
                )
            }
        }
    }
}

// Чип приёма: контур = не принят, красная заливка = принят
@Composable
private fun DoseChip(label: String, taken: Boolean, onClick: () -> Unit) {
    val colors = LocalZedColors.current
    Box(
        modifier = Modifier
            .border(
                1.dp,
                if (taken) colors.accent else colors.borderVisible,
                RoundedCornerShape(ZedRadius.sm)
            )
            .then(if (taken) Modifier.background(colors.accent) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = ZedSpacing.md, vertical = ZedSpacing.xs)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (taken) Color.White else colors.textSecondary
        )
    }
}
