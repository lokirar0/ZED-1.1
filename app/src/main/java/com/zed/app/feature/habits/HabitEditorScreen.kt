package com.zed.app.feature.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitEditorScreen(
    habitId: Int,
    onBack: () -> Unit,
    viewModel: HabitEditorViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LocalZedColors.current
    var showTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.finished) {
        if (state.finished) onBack()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Шапка
        Row(
            Modifier.fillMaxWidth().padding(horizontal = ZedSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, tint = colors.textSecondary)
            }
            Text(
                text = stringResource(
                    if (state.isEditing) R.string.editor_title_edit else R.string.editor_title_new
                ),
                style = MaterialTheme.typography.displaySmall,
                color = colors.textDisplay
            )
        }

        Column(Modifier.padding(ZedSpacing.lg)) {
            // Название
            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text(stringResource(R.string.editor_field_title)) },
                singleLine = true,
                shape = RoundedCornerShape(ZedRadius.md),
                colors = editorFieldColors(),
                isError = state.titleError,
                modifier = Modifier.fillMaxWidth()
            )
            if (state.titleError) {
                Text(
                    text = stringResource(R.string.editor_error_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.accent,
                    modifier = Modifier.padding(top = ZedSpacing.xs)
                )
            }

            Spacer(Modifier.height(ZedSpacing.md))

            // Заметка
            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::onNoteChange,
                label = { Text(stringResource(R.string.editor_field_note)) },
                minLines = 2,
                shape = RoundedCornerShape(ZedRadius.md),
                colors = editorFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(ZedSpacing.lg))

            // --- Напоминание: тумблер + время ---
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.editor_reminder),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = state.reminderEnabled,
                    onCheckedChange = viewModel::setReminderEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.accent,
                        checkedTrackColor = colors.accentSubtle,
                        uncheckedThumbColor = colors.textSecondary,
                        uncheckedTrackColor = colors.surfaceRaised
                    )
                )
            }

            if (state.reminderEnabled) {
                Spacer(Modifier.height(ZedSpacing.sm))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .border(1.dp, colors.borderVisible, RoundedCornerShape(ZedRadius.md))
                        .then(
                            Modifier.clickableNoRipple { showTimePicker = true }
                        )
                        .padding(horizontal = ZedSpacing.lg, vertical = ZedSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.editor_reminder_time),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "%02d:%02d".format(state.reminderHour, state.reminderMinute),
                        style = ZedDataNumber,
                        color = colors.accent
                    )
                }
            }

            Spacer(Modifier.height(ZedSpacing.xl))

            // Сохранить
            TextButton(
                onClick = viewModel::save,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .border(1.dp, colors.accent, RoundedCornerShape(ZedRadius.md))
            ) {
                Text(
                    text = stringResource(R.string.editor_save),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.accent
                )
            }

            // Удалить
            if (state.isEditing) {
                Spacer(Modifier.height(ZedSpacing.sm))
                TextButton(onClick = viewModel::delete, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.editor_delete),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.warning
                    )
                }
            }
        }
    }

    // --- TimePicker: выбор времени напоминания ---
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = state.reminderHour,
            initialMinute = state.reminderMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            containerColor = colors.surface,
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setReminderTime(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) {
                    Text(stringResource(R.string.cat_dialog_ok), color = colors.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.cat_dialog_cancel), color = colors.textSecondary)
                }
            }
        )
    }
}

// Кликабельность без ripple (Nothing-стиль)
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.then(
        androidx.compose.foundation.clickable(
            interactionSource = null,
            indication = null,
            onClick = onClick
        )
    )

@Composable
private fun editorFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = LocalZedColors.current.accent,
    unfocusedBorderColor = LocalZedColors.current.borderVisible,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    cursorColor = LocalZedColors.current.accent,
    focusedTextColor = LocalZedColors.current.textPrimary,
    unfocusedTextColor = LocalZedColors.current.textPrimary,
    focusedLabelColor = LocalZedColors.current.accent,
    unfocusedLabelColor = LocalZedColors.current.textSecondary,
    errorBorderColor = LocalZedColors.current.accent
)
