package com.zed.app.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zed.app.R
import com.zed.app.ui.theme.LocalZedColors
import com.zed.app.ui.theme.ZedDataNumber
import com.zed.app.ui.theme.ZedRadius
import com.zed.app.ui.theme.ZedSpacing
import kotlin.math.roundToInt

// Пресеты эквалайзера (5 полос: 60, 230, 910, 3.6k, 14k)
val EQ_FLAT = listOf(0.5f, 0.5f, 0.5f, 0.5f, 0.5f)
val EQ_BASS = listOf(0.9f, 0.75f, 0.5f, 0.4f, 0.35f)
val EQ_VOCAL = listOf(0.35f, 0.6f, 0.8f, 0.65f, 0.45f)
val EQ_TREBLE = listOf(0.35f, 0.45f, 0.55f, 0.75f, 0.9f)
private val EQ_LABELS = listOf("60", "230", "910", "3.6k", "14k")

// BottomSheet FX: эквалайзер + Slowed + Reverb + RESET
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FxBottomSheet(
    speed: Float,
    reverb: Int,
    eqGains: List<Float>,
    onSpeed: (Float) -> Unit,
    onReverb: (Int) -> Unit,
    onEqBand: (Int, Float) -> Unit,
    onPreset: (List<Float>) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalZedColors.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.borderVisible) }
    ) {
        Column(Modifier.padding(horizontal = ZedSpacing.xl, vertical = ZedSpacing.lg)) {
            Text("FX", style = MaterialTheme.typography.labelMedium, color = colors.textDisabled)
            Spacer(Modifier.height(ZedSpacing.lg))

            // --- Эквалайзер: 5 вертикальных ползунков ---
            Text(stringResource(R.string.player_eq), style = MaterialTheme.typography.labelMedium, color = colors.textDisabled)
            Spacer(Modifier.height(ZedSpacing.md))
            Row(
                modifier = Modifier.fillMaxWidth().height(140.dp),
                horizontalArrangement = Arrangement.spacedBy(ZedSpacing.md)
            ) {
                eqGains.forEachIndexed { index, gain ->
                    VerticalEqSlider(
                        value = gain,
                        label = EQ_LABELS.getOrElse(index) { "" },
                        onValueChange = { v -> onEqBand(index, v) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(ZedSpacing.md))
            Row(horizontalArrangement = Arrangement.spacedBy(ZedSpacing.sm)) {
                PresetChip(stringResource(R.string.player_preset_flat), EQ_FLAT, eqGains, onPreset)
                PresetChip(stringResource(R.string.player_preset_bass), EQ_BASS, eqGains, onPreset)
                PresetChip(stringResource(R.string.player_preset_vocal), EQ_VOCAL, eqGains, onPreset)
                PresetChip(stringResource(R.string.player_preset_treble), EQ_TREBLE, eqGains, onPreset)
            }

            HorizontalDivider(color = colors.border, modifier = Modifier.padding(vertical = ZedSpacing.lg))

            // --- Slowed + Reverb ---
            Text("SLOWED + REVERB", style = MaterialTheme.typography.labelMedium, color = colors.textDisabled)
            Spacer(Modifier.height(ZedSpacing.md))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.player_speed), style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                Spacer(Modifier.weight(1f))
                Text("%.2fx".format(speed), style = ZedDataNumber, color = colors.textDisplay)
            }
            Slider(
                value = speed,
                onValueChange = onSpeed,
                valueRange = 0.5f..1.5f,
                colors = SliderDefaults.colors(
                    thumbColor = colors.accent,
                    activeTrackColor = colors.accent,
                    inactiveTrackColor = colors.borderVisible
                )
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.player_reverb), style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                Spacer(Modifier.weight(1f))
                Text("$reverb%", style = ZedDataNumber, color = colors.textDisplay)
            }
            Slider(
                value = reverb.toFloat(),
                onValueChange = { onReverb(it.roundToInt()) },
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = colors.accent,
                    activeTrackColor = colors.accent,
                    inactiveTrackColor = colors.borderVisible
                )
            )

            Spacer(Modifier.height(ZedSpacing.lg))

            // RESET: красная кнопка
            TextButton(
                onClick = onReset,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(colors.accent, RoundedCornerShape(ZedRadius.md))
            ) {
                Text(
                    text = stringResource(R.string.player_reset),
                    style = MaterialTheme.typography.labelLarge,
                    color = androidx.compose.ui.graphics.Color.White
                )
            }

            Spacer(Modifier.height(ZedSpacing.xl))
        }
    }
}

// Вертикальный ползунок: драг = уровень, красная точка-индикатор при драге
@Composable
private fun VerticalEqSlider(
    value: Float,
    label: String,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalZedColors.current
    val haptic = LocalHapticFeedback.current
    var dragging by remember { mutableStateOf(false) }
    val lastStep = remember { mutableStateOf(-1) }
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(6.dp)
                .background(if (dragging) colors.accent else colors.borderVisible)
        )
        Spacer(Modifier.height(ZedSpacing.xs))
        Box(
            modifier = Modifier
                .width(24.dp)
                .weight(1f)
                .background(colors.border, RoundedCornerShape(ZedRadius.xs))
                .pointerInput(value) {
                    detectVerticalDragGestures(
                        onDragStart = { dragging = true },
                        onDragEnd = { dragging = false },
                        onDragCancel = { dragging = false }
                    ) { change, dragAmount ->
                        change.consume()
                        val v = (value - dragAmount / size.height).coerceIn(0f, 1f)
                        onValueChange(v)
                        val step = (v * 20).toInt()
                        if (step != lastStep.value) {
                            lastStep.value = step
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    }
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(value.coerceIn(0.02f, 1f))
                    .background(colors.accent, RoundedCornerShape(ZedRadius.xs))
            )
        }
        Spacer(Modifier.height(ZedSpacing.xs))
        Text(label, style = MaterialTheme.typography.labelSmall, color = colors.textDisabled)
    }
}

// Чип пресета
@Composable
private fun PresetChip(label: String, preset: List<Float>, current: List<Float>, onPreset: (List<Float>) -> Unit) {
    val colors = LocalZedColors.current
    val selected = current == preset
    TextButton(
        onClick = { onPreset(preset) },
        modifier = Modifier.border(
            1.dp,
            if (selected) colors.accent else colors.borderVisible,
            RoundedCornerShape(ZedRadius.sm)
        )
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = if (selected) colors.accent else colors.textSecondary)
    }
}
