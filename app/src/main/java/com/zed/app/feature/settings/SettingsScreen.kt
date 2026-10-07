package com.zed.app.feature.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zed.app.BuildConfig
import com.zed.app.R
import com.zed.app.core.settings.ThemeMode
import com.zed.app.ui.theme.LocalZedColors
import com.zed.app.ui.theme.ZedRadius
import com.zed.app.ui.theme.ZedSpacing
import kotlinx.coroutines.launch

// Экран настроек: тема, язык, уведомления (+тест), данные (бэкап), о приложении
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val colors = LocalZedColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showClearDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* при отказе расписание остаётся: система просто не покажет уведомление */ }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) scope.launch {
            val ok = viewModel.exportBackup(uri)
            snackbarHostState.showSnackbar(
                context.getString(if (ok) R.string.backup_exported else R.string.backup_failed)
            )
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) scope.launch {
            val ok = viewModel.importBackup(uri)
            snackbarHostState.showSnackbar(
                context.getString(if (ok) R.string.backup_imported else R.string.backup_failed)
            )
        }
    }

    Box(Modifier.fillMaxSize()) {
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
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.displaySmall,
                    color = colors.textDisplay
                )
            }

            // --- ТЕМА ---
            SectionLabel(stringResource(R.string.settings_theme))
            ThemeMode.entries.forEach { mode ->
                SettingsRow(
                    title = stringResource(mode.titleRes()),
                    selected = settings.themeMode == mode,
                    onClick = { viewModel.setThemeMode(mode) }
                )
            }

            HorizontalDivider(color = colors.border, modifier = Modifier.padding(vertical = ZedSpacing.sm))

            // --- ЯЗЫК ---
            SectionLabel(stringResource(R.string.settings_language))
            listOf("system" to R.string.lang_system, "ru" to R.string.lang_ru, "en" to R.string.lang_en)
                .forEach { (code, labelRes) ->
                    SettingsRow(
                        title = stringResource(labelRes),
                        selected = settings.language == code,
                        onClick = { viewModel.setLanguage(code) }
                    )
                }

            HorizontalDivider(color = colors.border, modifier = Modifier.padding(vertical = ZedSpacing.sm))

            // --- УВЕДОМЛЕНИЯ ---
            SectionLabel(stringResource(R.string.settings_notifications))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = ZedSpacing.lg, vertical = ZedSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.settings_notifications_enabled),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = settings.notificationsEnabled,
                    onCheckedChange = { enabled ->
                        viewModel.setNotificationsEnabled(enabled)
                        if (enabled &&
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.accent,
                        checkedTrackColor = colors.accentSubtle,
                        uncheckedThumbColor = colors.textSecondary,
                        uncheckedTrackColor = colors.surfaceRaised
                    )
                )
            }

            // Тестовое уведомление: мгновенная проверка канала и разрешений
            Column(Modifier.padding(horizontal = ZedSpacing.lg)) {
                TextButton(
                    onClick = { viewModel.sendTestNotification() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, colors.borderVisible, RoundedCornerShape(ZedRadius.md))
                ) {
                    Text(
                        text = stringResource(R.string.notif_test_button),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textSecondary
                    )
                }
            }

            HorizontalDivider(color = colors.border, modifier = Modifier.padding(vertical = ZedSpacing.sm))

            // --- ДАННЫЕ ---
            SectionLabel(stringResource(R.string.settings_data))
            Column(Modifier.padding(horizontal = ZedSpacing.lg)) {
                DataButton(
                    label = stringResource(R.string.backup_export),
                    color = colors.textSecondary,
                    borderColor = colors.borderVisible,
                    onClick = { exportLauncher.launch("zed_backup_${System.currentTimeMillis()}.json") }
                )
                DataButton(
                    label = stringResource(R.string.backup_import),
                    color = colors.textSecondary,
                    borderColor = colors.borderVisible,
                    onClick = { importLauncher.launch(arrayOf("application/json")) }
                )
                DataButton(
                    label = stringResource(R.string.backup_clear),
                    color = colors.warning,
                    borderColor = colors.warning,
                    onClick = { showClearDialog = true }
                )
            }

            HorizontalDivider(color = colors.border, modifier = Modifier.padding(vertical = ZedSpacing.sm))

            // --- О ПРИЛОЖЕНИИ ---
            SectionLabel(stringResource(R.string.settings_about))
            Row(Modifier.fillMaxWidth().padding(horizontal = ZedSpacing.lg, vertical = ZedSpacing.md)) {
                Text(
                    text = stringResource(R.string.settings_version),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = BuildConfig.VERSION_NAME,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textSecondary
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = colors.surface,
            title = { Text(stringResource(R.string.backup_clear_title), color = colors.textDisplay) },
            text = { Text(stringResource(R.string.backup_clear_body), color = colors.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    scope.launch {
                        viewModel.clearAll()
                        snackbarHostState.showSnackbar(context.getString(R.string.backup_cleared))
                    }
                }) {
                    Text(
                        stringResource(R.string.backup_clear_confirm),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.accent
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(
                        stringResource(R.string.backup_clear_cancel),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textSecondary
                    )
                }
            }
        )
    }
}

@Composable
private fun DataButton(label: String, color: Color, borderColor: Color, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = ZedSpacing.xxs)
            .border(1.dp, borderColor, RoundedCornerShape(ZedRadius.md))
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

@Composable
private fun SectionLabel(text: String) {
    val colors = LocalZedColors.current
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = colors.textDisabled,
        modifier = Modifier.padding(start = ZedSpacing.lg, top = ZedSpacing.lg, bottom = ZedSpacing.sm)
    )
}

@Composable
private fun SettingsRow(title: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalZedColors.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = ZedSpacing.lg, vertical = ZedSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) colors.textDisplay else colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = colors.accent)
        )
    }
}

private fun ThemeMode.titleRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}
