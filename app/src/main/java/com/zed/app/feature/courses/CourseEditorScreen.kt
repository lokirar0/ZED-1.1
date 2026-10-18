package com.zed.app.feature.courses

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zed.app.R
import com.zed.app.core.domain.util.DoseScheduleCalculator
import com.zed.app.ui.theme.LocalZedColors
import com.zed.app.ui.theme.ZedDataNumber
import com.zed.app.ui.theme.ZedRadius
import com.zed.app.ui.theme.ZedSpacing
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseEditorScreen(
    courseId: Int,
    onBack: () -> Unit,
    viewModel: CourseEditorViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LocalZedColors.current
    var showDatePicker by remember { mutableStateOf(false) }
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
        Row(
            Modifier.fillMaxWidth().padding(horizontal = ZedSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, tint = colors.textSecondary)
            }
            Text(
                text = stringResource(
                    if (state.isEditing) R.string.editor_course_edit else R.string.editor_course_new
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
                label = { Text(stringResource(R.string.editor_course_title)) },
                singleLine = true,
                shape = RoundedCornerShape(ZedRadius.md),
                colors = fieldColors(),
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

            // Дата старта
            EditorRow(
                label = stringResource(R.string.editor_course_start),
                value = Instant.ofEpochMilli(state.startDateMillis).atZone(ZoneId.systemDefault())
                    .toLocalDate().format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault())),
                onClick = { showDatePicker = true }
            )

            Spacer(Modifier.height(ZedSpacing.md))

            // Длительность в днях
            OutlinedTextField(
                value = state.totalDaysText,
                onValueChange = viewModel::onDaysChange,
                label = { Text(stringResource(R.string.editor_course_days)) },
                singleLine = true,
                textStyle = ZedDataNumber,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(ZedRadius.md),
                colors = fieldColors(),
                isError = state.daysError,
                modifier = Modifier.fillMaxWidth()
            )
            if (state.daysError) {
                Text(
                    text = stringResource(R.string.editor_error_days),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.accent,
                    modifier = Modifier.padding(top = ZedSpacing.xs)
                )
            }

            Spacer(Modifier.height(ZedSpacing.md))

            // Интервал: ЧАСЫ + МИНУТЫ (например 2 ч 30 м)
            Row(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = state.intervalHoursText,
                    onValueChange = viewModel::onIntervalHoursChange,
                    label = { Text(stringResource(R.string.editor_interval_hours)) },
                    singleLine = true,
                    textStyle = ZedDataNumber,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(ZedRadius.md),
                    colors = fieldColors(),
                    isError = state.intervalError,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(ZedSpacing.md))
                OutlinedTextField(
                    value = state.intervalMinutesText,
                    onValueChange = viewModel::onIntervalMinutesChange,
                    label = { Text(stringResource(R.string.editor_interval_minutes)) },
                    singleLine = true,
                    textStyle = ZedDataNumber,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(ZedRadius.md),
                    colors = fieldColors(),
                    isError = state.intervalError,
                    modifier = Modifier.weight(1f)
                )
            }
            if (state.intervalError) {
                Text(
                    text = stringResource(R.string.editor_error_interval),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.accent,
                    modifier = Modifier.padding(top = ZedSpacing.xs)
                )
            }

            Spacer(Modifier.height(ZedSpacing.md))

            // Время первого приёма (якорь расписания)
            EditorRow(
                label = stringResource(R.string.editor_course_first),
                value = DoseScheduleCalculator.formatMinutes(state.firstDoseMinutes),
                onClick = { showTimePicker = true }
            )

            Spacer(Modifier.height(ZedSpacing.md))

            // Заметка
            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::onNoteChange,
                label = { Text(stringResource(R.string.editor_field_note)) },
                minLines = 2,
                shape = RoundedCornerShape(ZedRadius.md),
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

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

    // --- DatePicker: день старта курса ---
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = state.startDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val utc = datePickerState.selectedDateMillis
                    if (utc != null) {
                        val localDay = Instant.ofEpochMilli(utc).atZone(ZoneOffset.UTC).toLocalDate()
                        viewModel.setStartDate(
                            localDay.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        )
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.cat_dialog_ok), color = colors.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cat_dialog_cancel), color = colors.textSecondary)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // --- TimePicker: время первого приёма ---
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = state.firstDoseMinutes / 60,
            initialMinute = state.firstDoseMinutes % 60,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            containerColor = colors.surface,
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setFirstDoseMinutes(timePickerState.hour * 60 + timePickerState.minute)
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

// Строка-кнопка (дата / время) в стиле редакторов ZED
@Composable
private fun EditorRow(label: String, value: String, onClick: () -> Unit) {
    val colors = LocalZedColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .border(1.dp, colors.borderVisible, RoundedCornerShape(ZedRadius.md))
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(horizontal = ZedSpacing.lg, vertical = ZedSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.textSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(text = value, style = ZedDataNumber, color = colors.textDisplay)
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
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
