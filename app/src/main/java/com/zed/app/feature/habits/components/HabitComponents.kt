package com.zed.app.feature.habits.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zed.app.R
import com.zed.app.feature.habits.HabitUiItem
import com.zed.app.ui.theme.LocalZedColors
import com.zed.app.ui.theme.ZedRadius
import com.zed.app.ui.theme.ZedSpacing

// Квадратный чекбокс 24dp: пусто = обводка, отмечено = красная заливка + белая галка
@Composable
fun SquareCheckbox(checked: Boolean, onCheckedChange: () -> Unit) {
    val colors = LocalZedColors.current
    Box(
        modifier = Modifier
            .size(24.dp)
            .then(
                if (checked) Modifier.background(colors.accent, RoundedCornerShape(ZedRadius.sm))
                else Modifier.border(1.dp, colors.borderVisible, RoundedCornerShape(ZedRadius.sm))
            )
            .clickable(interactionSource = null, indication = null, onClick = onCheckedChange),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// Точечная матрица недели: 7 квадратов, заполнен = день выполнен
@Composable
fun WeekDots(flags: List<Boolean>) {
    val colors = LocalZedColors.current
    Row(horizontalArrangement = Arrangement.spacedBy(ZedSpacing.xs)) {
        flags.forEach { done ->
            Box(
                Modifier
                    .size(8.dp)
                    .then(
                        if (done) Modifier.background(colors.accent)
                        else Modifier.border(1.dp, colors.borderVisible)
                    )
            )
        }
    }
}

// Карточка привычки: streak + время напоминания + точечная неделя + чекбокс
@Composable
fun HabitCard(item: HabitUiItem, onToggle: () -> Unit) {
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
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textPrimary
                )
                Spacer(Modifier.height(ZedSpacing.xs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.habits_streak, item.streak),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary
                    )
                    // Время напоминания, если включено
                    if (item.reminderLabel != null) {
                        Spacer(Modifier.width(ZedSpacing.md))
                        Text(
                            text = item.reminderLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.accent
                        )
                    }
                }
                Spacer(Modifier.height(ZedSpacing.md))
                WeekDots(item.weekFlags)
            }
            Spacer(Modifier.width(ZedSpacing.md))
            SquareCheckbox(checked = item.doneToday, onCheckedChange = onToggle)
        }
    }
}
