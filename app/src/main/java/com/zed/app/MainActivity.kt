package com.zed.app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import com.zed.app.ui.PermissionsGate
import com.zed.app.ui.theme.ZedBlack
import com.zed.app.ui.theme.ZedTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val vm: MainViewModel by viewModels()

    @Inject lateinit var settingsRepository: SettingsRepository

    // Замер сессии для экрана «Активность»
    private var sessionStartMs = 0L

    // Глобальный haptic-слой: тик на тап, отклик на удержание, скроллы молчат
    private val hapticHandler = Handler(Looper.getMainLooper())
    private val slop by lazy { ViewConfiguration.get(this).scaledTouchSlop }
    private var downX = 0f
    private var downY = 0f
    private var longFired = false
    private val longPressRunnable = Runnable {
        longFired = true
        window?.decorView?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.pointerCount == 1) {
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = ev.x
                    downY = ev.y
                    longFired = false
                    hapticHandler.removeCallbacks(longPressRunnable)
                    hapticHandler.postDelayed(longPressRunnable, 500L)
                }
                MotionEvent.ACTION_MOVE -> {
                    if (abs(ev.x - downX) > slop * 2 || abs(ev.y - downY) > slop * 2) {
                        hapticHandler.removeCallbacks(longPressRunnable)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    hapticHandler.removeCallbacks(longPressRunnable)
                    val dx = abs(ev.x - downX)
                    val dy = abs(ev.y - downY)
                    if (!longFired && dx <= slop && dy <= slop) {
                        window?.decorView?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    }
                }
                MotionEvent.ACTION_CANCEL -> hapticHandler.removeCallbacks(longPressRunnable)
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setupShortcuts()

        lifecycleScope.launch {
            applyAppLocale(settingsRepository.settings.first().language)
        }

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
                    // После онбординга и до прохождения гейта разрешений —
                    // показываем экран выдачи разрешений вместо приложения
                    if (s.onboardingCompleted && !s.permissionsAsked) {
                        PermissionsGate(onDone = { viewModel.markPermissionsAsked() })
                    } else {
                        ZedNavHost(
                            startDestination = if (s.onboardingCompleted) "habits" else "onboarding",
                            startAction = startAction
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sessionStartMs = SystemClock.elapsedRealtime()
        vm.ensureSchedules()
    }

    override fun onPause() {
        super.onPause()
        val seconds = (SystemClock.elapsedRealtime() - sessionStartMs) / 1000
        vm.addSession(seconds)
        ZedWidgetRefresher.update(this)
    }

    // Шорткаты на иконке приложения
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
