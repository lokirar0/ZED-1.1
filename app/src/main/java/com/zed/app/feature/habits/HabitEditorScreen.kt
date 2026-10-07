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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.zed.app.ui.theme.ZedRadius
import com.zed.app.ui.theme.ZedSpacing

@Composable
fun HabitEditorScreen(
    habitId: Int,
    onBack: () -> Unit,
    viewModel: HabitEditorViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LocalZedColors.current

    // Сохранение/удаление завершено → закрываем экран
    LaunchedEffect(state.finished) {
        if (state.finished) onBack()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Шапка: назад + заголовок
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
            // Название привычки
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

            // Заметка (необязательно)
            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::onNoteChange,
                label = { Text(stringResource(R.string.editor_field_note)) },
                minLines = 2,
                shape = RoundedCornerShape(ZedRadius.md),
                colors = editorFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(ZedSpacing.xl))

            // Сохранить: прозрачная кнопка с красной обводкой
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

            // Удалить (только в режиме редактирования)
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
}

// Цвета полей ввода под токены ZED
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
