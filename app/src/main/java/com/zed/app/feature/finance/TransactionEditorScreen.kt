package com.zed.app.feature.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import com.zed.app.core.data.local.CategoryEntity
import com.zed.app.core.data.local.categoryKeyRes
import com.zed.app.core.domain.model.TransactionType
import com.zed.app.ui.theme.LocalZedColors
import com.zed.app.ui.theme.ZedDataNumber
import com.zed.app.ui.theme.ZedRadius
import com.zed.app.ui.theme.ZedSpacing
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TransactionEditorScreen(
    onBack: () -> Unit,
    viewModel: TransactionEditorViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LocalZedColors.current

    var showNameDialog by remember { mutableStateOf(false) }
    var nameDialogFromOther by remember { mutableStateOf(false) }
    var showManageDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

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
                text = stringResource(R.string.editor_tx_title),
                style = MaterialTheme.typography.displaySmall,
                color = colors.textDisplay
            )
        }

        Column(Modifier.padding(ZedSpacing.lg)) {
            // Переключатель ДОХОД / РАСХОД
            Row(horizontalArrangement = Arrangement.spacedBy(ZedSpacing.sm)) {
                TypeSegment(
                    label = stringResource(R.string.finance_income),
                    selected = state.type == TransactionType.INCOME,
                    onClick = { viewModel.onTypeChange(TransactionType.INCOME) },
                    modifier = Modifier.weight(1f)
                )
                TypeSegment(
                    label = stringResource(R.string.finance_expense),
                    selected = state.type == TransactionType.EXPENSE,
                    onClick = { viewModel.onTypeChange(TransactionType.EXPENSE) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(ZedSpacing.lg))

            // Сумма
            OutlinedTextField(
                value = state.amountText,
                onValueChange = viewModel::onAmountChange,
                label = { Text(stringResource(R.string.editor_amount)) },
                singleLine = true,
                textStyle = ZedDataNumber,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(ZedRadius.md),
                colors = fieldColors(),
                isError = state.amountError,
                modifier = Modifier.fillMaxWidth()
            )
            if (state.amountError) {
                Text(
                    text = stringResource(R.string.editor_error_amount),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.accent,
                    modifier = Modifier.padding(top = ZedSpacing.xs)
                )
            }

            Spacer(Modifier.height(ZedSpacing.md))

            // Дата операции: по умолчанию сегодня, можно выбрать любую (задним числом)
            Row(
                Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.borderVisible, RoundedCornerShape(ZedRadius.md))
                    .clickableDate { showDatePicker = true }
                    .padding(horizontal = ZedSpacing.lg, vertical = ZedSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.editor_date),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatEditorDate(state.dateMillis),
                    style = ZedDataNumber,
                    color = colors.textDisplay
                )
            }

            Spacer(Modifier.height(ZedSpacing.lg))

            // Категории + управление
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.editor_category),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textDisabled,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showManageDialog = true }) {
                    Icon(Icons.Outlined.Tune, null, tint = colors.textSecondary)
                }
            }
            Spacer(Modifier.height(ZedSpacing.sm))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(ZedSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(ZedSpacing.sm)
            ) {
                state.categories
                    .filter { it.type == state.type.name }
                    .forEach { category ->
                        CategoryChip(
                            label = chipLabel(category),
                            selected = state.selectedCategoryId == category.id,
                            onClick = {
                                if (category.key.startsWith("OTHER")) {
                                    nameDialogFromOther = true
                                    showNameDialog = true
                                } else {
                                    viewModel.select(category.id)
                                }
                            }
                        )
                    }
                CategoryChip(
                    label = "+",
                    selected = false,
                    onClick = {
                        nameDialogFromOther = false
                        showNameDialog = true
                    }
                )
            }

            Spacer(Modifier.height(ZedSpacing.lg))

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
        }
    }

    // --- DatePicker: выбор дня операции ---
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.dateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val utc = datePickerState.selectedDateMillis
                    if (utc != null) {
                        // DatePicker отдаёт UTC-полночь → переводим в локальный день, 12:00
                        val localDay = Instant.ofEpochMilli(utc).atZone(ZoneOffset.UTC).toLocalDate()
                        val millis = localDay.atTime(12, 0)
                            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        viewModel.setDate(millis)
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

    // --- Панель имени категории ---
    if (showNameDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            containerColor = colors.surface,
            title = { Text(stringResource(R.string.cat_dialog_title), color = colors.textDisplay) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.cat_dialog_hint)) },
                    singleLine = true,
                    shape = RoundedCornerShape(ZedRadius.md),
                    colors = fieldColors()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val trimmed = name.trim()
                    when {
                        trimmed.isNotEmpty() -> viewModel.createAndSelect(trimmed)
                        nameDialogFromOther -> viewModel.selectOtherBuiltIn()
                    }
                    showNameDialog = false
                }) {
                    Text(stringResource(R.string.cat_dialog_ok), color = colors.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text(stringResource(R.string.cat_dialog_cancel), color = colors.textSecondary)
                }
            }
        )
    }

    // --- Управление категориями ---
    if (showManageDialog) {
        var newName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showManageDialog = false },
            containerColor = colors.surface,
            title = { Text(stringResource(R.string.cat_manage), color = colors.textDisplay) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text(stringResource(R.string.cat_dialog_hint)) },
                            singleLine = true,
                            shape = RoundedCornerShape(ZedRadius.md),
                            colors = fieldColors(),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            if (newName.trim().isNotEmpty()) {
                                viewModel.createAndSelect(newName.trim())
                                newName = ""
                            }
                        }) {
                            Icon(Icons.Outlined.Add, null, tint = colors.accent)
                        }
                    }
                    HorizontalDivider(color = colors.border, modifier = Modifier.padding(vertical = ZedSpacing.sm))
                    state.categories
                        .filter { it.type == state.type.name }
                        .forEach { category ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = ZedSpacing.xxs),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = chipLabel(category),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                if (category.key != "CREDIT") {
                                    IconButton(onClick = { viewModel.deleteCategory(category.id) }) {
                                        Icon(Icons.Outlined.Delete, null, tint = colors.warning)
                                    }
                                }
                            }
                        }
                }
            },
            confirmButton = {
                TextButton(onClick = { showManageDialog = false }) {
                    Text(stringResource(R.string.cat_manage_done), color = colors.accent)
                }
            }
        )
    }
}

// Дата операции человеком: "6 октября 2026"
private fun formatEditorDate(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
        .format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault()))

// Кликабельность строки даты без ripple (Nothing-стиль).
// ВАЖНО: clickable — extension-функция, работает только с импортом
// androidx.compose.foundation.clickable и вызовом от ресивера Modifier.
private fun Modifier.clickableDate(onClick: () -> Unit): Modifier =
    this.then(
        clickable(
            interactionSource = null,
            indication = null,
            onClick = onClick
        )
    )

@Composable
private fun chipLabel(category: CategoryEntity): String =
    if (category.builtIn) stringResource(categoryKeyRes(category.key)) else category.name

@Composable
private fun TypeSegment(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalZedColors.current
    Row(
        modifier = modifier
            .height(44.dp)
            .border(
                1.dp,
                if (selected) colors.accent else colors.borderVisible,
                RoundedCornerShape(ZedRadius.md)
            )
            .then(if (selected) Modifier.background(colors.accentSubtle) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        TextButton(onClick = onClick) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) colors.accent else colors.textSecondary
            )
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalZedColors.current
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .border(
                1.dp,
                if (selected) colors.accent else colors.borderVisible,
                RoundedCornerShape(ZedRadius.sm)
            )
            .then(if (selected) Modifier.background(colors.accentSubtle) else Modifier)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) colors.accent else colors.textSecondary
        )
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
