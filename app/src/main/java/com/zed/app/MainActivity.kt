package com.zed.app

import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.zed.app.core.settings.Settings
import com.zed.app.core.settings.SettingsRepository
import com.zed.app.core.settings.ThemeMode
import com.zed.app.core.settings.applyAppLocale
import com.zed.app.core.widgets.ZedWidgetRefresher
import com.zed.app.navigation.ZedNavHost
import com.zed.app.ui.theme.LocalZedColors
import com.zed.app.ui.theme.ZedBlack
import com.zed.app.ui.theme.ZedTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val vm: MainViewModel by viewModels()

    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setupShortcuts()

        lifecycleScope.launch {
            applyAppLocale(settingsRepository.settings.first().language)
        }

        // Действие из шортката иконки (долгое нажатие на иконку ZED)
        val startAction = intent?.getStringExtra("zed_action")

        setContent {
            val viewModel: MainViewModel = hiltViewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()

            if (settings == null) {
                Box(Modifier.fillMaxSize().background(ZedBlack))
            } else {
                val s = settings as Settings
                val systemDark = isSystemInDarkTheme()
                val dark = when (s.themeMode) {
                    ThemeMode.SYSTEM -> systemDark
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                }
                ZedTheme(darkTheme = dark) {
                    ExactAlarmPrompt()
                    ZedNavHost(
                        startDestination = if (s.onboardingCompleted) "habits" else "onboarding",
                        startAction = startAction
                    )
                }
            }
        }
    }

    // Уходим в фон / на домашний экран → виджеты получают свежие данные
    override fun onPause() {
        super.onPause()
        ZedWidgetRefresher.update(this)
    }

    override fun onResume() {
        super.onResume()
        vm.ensureSchedules()
    }

    // Шорткаты на иконке приложения (долгое нажатие на иконку ZED в лаунчере)
    private fun setupShortcuts() {
        val shortcuts = listOf(
            shortcut("add_expense", R.string.shortcut_add_expense, R.drawable.sc_add, "finance_editor"),
            shortcut("calendar", R.string.shortcut_calendar, R.drawable.sc_calendar, "finance_calendar"),
            shortcut("stats", R.string.shortcut_stats, R.drawable.sc_stats, "habits_stats"),
            shortcut("player", R.string.shortcut_player, R.drawable.sc_player, "player_screen")
        )
        ShortcutManagerCompat.setDynamicShortcuts(this, shortcuts)
    }

    private fun shortcut(id: String, labelRes: Int, iconRes: Int, action: String): ShortcutInfoCompat =
        ShortcutInfoCompat.Builder(this, id)
            .setShortLabel(getString(labelRes))
            .setIcon(IconCompat.createWithResource(this, iconRes))
            .setIntent(
                Intent(this, MainActivity::class.java)
                    .setAction(Intent.ACTION_VIEW)
                    .putExtra("zed_action", action)
            )
            .build()
}

// Диалог разрешения точных будильников
@Composable
private fun ExactAlarmPrompt() {
    val context = LocalContext.current
    val colors = LocalZedColors.current

    val denied = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        !(context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms())

    var show by remember { mutableStateOf(denied) }

    if (show && denied) {
        AlertDialog(
            onDismissRequest = { show = false },
            containerColor = colors.surface,
            title = { Text(stringResource(R.string.exact_alarm_title), color = colors.textDisplay) },
            text = { Text(stringResource(R.string.exact_alarm_body), color = colors.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    show = false
                    runCatching {
                        context.startActivity(
                            Intent(
                                AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                Uri.parse("package:" + context.packageName)
                            )
                        )
                    }.onFailure {
                        runCatching {
                            context.startActivity(
                                Intent(
                                    AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.parse("package:" + context.packageName)
                                )
                            )
                        }
                    }
                }) {
                    Text(stringResource(R.string.exact_alarm_grant), color = colors.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { show = false }) {
                    Text(stringResource(R.string.backup_clear_cancel), color = colors.textSecondary)
                }
            }
        )
    }
}
