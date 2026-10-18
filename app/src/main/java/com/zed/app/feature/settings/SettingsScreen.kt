package com.zed.app.feature.settings

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
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
import com.zed.app.ui.components.rememberHapticClick
import com.zed.app.ui.theme.LocalZedColors
import com.zed.app.ui.theme.ZedRadius
import com.zed.app.ui.theme.ZedSpacing
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenYearReport: () -> Unit,
    onOpenAnalytics: () -> Unit,
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
    ) { /* при отказе расписание остаётся */ }

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

    // Системный picker звуков уведомления: видит и пользовательские рингтоны (pace-np и т.д.)
    val soundUri = settings.notificationSoundUri
    val soundName = remember(soundUri) {
        if (soundUri.isBlank()) {
            context.getString(R.string.sound_default)
        } else {
            runCatching {
                RingtoneManager.getRingtone(context, Uri.parse(soundUri))?.getTitle(context)
            }.getOrNull() ?: context.getString(R.string.sound_default)
        }
    }
    val soundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val picked: Uri? = result.data
                ?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            viewModel.setNotificationSound(picked?.toString() ?: "")
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
                    onClick = rememberHapticClick { viewModel.setThemeMode(mode) }
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
                        onClick = rememberHapticClick { viewModel.setLanguage(code) }
                    )
                }

            HorizontalDivider(color = colors.border, modifier = Modifier.padding(vertical = ZedSpacing.sm))

            // --- УВЕДОМЛЕНИЯ: тумблер, звук, полноэкранные, тест ---
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
            Column(Modifier.padding(horizontal = ZedSpacing.lg)) {
                // Звук уведомлений (глобально: курсы, привычки, кредиты)
                DataButton(
                    label = stringResource(R.string.settings_sound) + ": " + soundName,
                    color = colors.textSecondary,
                    borderColor = colors.borderVisible,
                    onClick = rememberHapticClick {
                        soundLauncher.launch(
                            Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                if (soundUri.isNotBlank()) {
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(soundUri))
                                }
                            }
                        )
                    }
                )
                // Полноэкранные оповещения (Android 14+): как у будильников
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val nm = context.getSystemService(NotificationManager::class.java)
                    if (!nm.canUseFullScreenIntent()) {
                        DataButton(
                            label = stringResource(R.string.settings_fullscreen),
                            color = colors.warning,
                            borderColor = colors.warning,
                            onClick = rememberHapticClick {
                                runCatching {
                                    context.startActivity(
                                        Intent(
                                            android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                                            Uri.parse("package:" + context.packageName)
                                        )
                                    )
                                }
                            }
                        )
                    }
                }
                DataButton(
                    label = stringResource(R.string.notif_test_button),
                    color = colors.textSecondary,
                    borderColor = colors.borderVisible,
                    onClick = rememberHapticClick { viewModel.sendTestNotification() }
                )
            }

            HorizontalDivider(color = colors.border, modifier = Modifier.padding(vertical = ZedSpacing.sm))

            // --- ДАННЫЕ ---
            SectionLabel(stringResource(R.string.settings_data))
            Column(Modifier.padding(horizontal = ZedSpacing.lg)) {
                DataButton(
                    label = stringResource(R.string.backup_export),
                    color = colors.textSecondary,
                    borderColor = colors.borderVisible,
                    onClick = rememberHapticClick { exportLauncher.launch("zed_backup_${System.currentTimeMillis()}.json") }
                )
                DataButton(
                    label = stringResource(R.string.backup_import),
                    color = colors.textSecondary,
                    borderColor = colors.borderVisible,
                    onClick = rememberHapticClick { importLauncher.launch(arrayOf("application/json")) }
                )
                DataButton(
                    label = stringResource(R.string.backup_clear),
                    color = colors.warning,
                    borderColor = colors.warning,
                    onClick = rememberHapticClick { showClearDialog = true }
                )
            }

            HorizontalDivider(color = colors.border, modifier = Modifier.padding(vertical = ZedSpacing.sm))

            // --- О ПРИЛОЖЕНИИ ---
            SectionLabel(stringResource(R.string.settings_about))
            Column(Modifier.padding(horizontal = ZedSpacing.lg)) {
                DataButton(
                    label = stringResource(R.string.report_title),
                    color = colors.accent,
                    borderColor = colors.accent,
                    onClick = rememberHapticClick(onOpenYearReport)
                )
                DataButton(
                    label = stringResource(R.string.analytics_title),
                    color = colors.textSecondary,
                    borderColor = colors.borderVisible,
                    onClick = rememberHapticClick(onOpenAnalytics)
                )
            }
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
