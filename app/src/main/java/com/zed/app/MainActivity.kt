package com.zed.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.zed.app.core.settings.Settings
import com.zed.app.core.settings.SettingsRepository
import com.zed.app.core.settings.ThemeMode
import com.zed.app.core.settings.applyAppLocale
import com.zed.app.navigation.ZedNavHost
import com.zed.app.ui.theme.ZedBlack
import com.zed.app.ui.theme.ZedTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// AppCompatActivity нужна для AppCompatDelegate.setApplicationLocales (переключатель языка)
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Восстанавливаем сохранённый язык до отрисовки контента
        lifecycleScope.launch {
            applyAppLocale(settingsRepository.settings.first().language)
        }

        setContent {
            val viewModel: MainViewModel = hiltViewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()

            // Чёрный экран-заглушка, пока DataStore не отдал настройки
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
                    ZedNavHost(
                        startDestination = if (s.onboardingCompleted) "habits" else "onboarding"
                    )
                }
            }
        }
    }
}
