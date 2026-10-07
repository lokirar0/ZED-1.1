package com.zed.app.core.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class Settings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val notificationsEnabled: Boolean = true,
    val onboardingCompleted: Boolean = false,
    val language: String = "system",       // "system" | "ru" | "en"
    val shuffleEnabled: Boolean = false,   // плеер: перемешивание
    val repeatMode: Int = 0                // плеер: 0=OFF, 1=ONE, 2=ALL
)

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    // Поток настроек из DataStore (локально, без облака)
    val settings: Flow<Settings> = dataStore.data.map { prefs ->
        Settings(
            themeMode = prefs[KEY_THEME]
                ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            notificationsEnabled = prefs[KEY_NOTIFICATIONS] ?: true,
            onboardingCompleted = prefs[KEY_ONBOARDING] ?: false,
            language = prefs[KEY_LANGUAGE] ?: "system",
            shuffleEnabled = prefs[KEY_SHUFFLE] ?: false,
            repeatMode = prefs[KEY_REPEAT] ?: 0
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) { dataStore.edit { it[KEY_THEME] = mode.name } }
    suspend fun setNotificationsEnabled(enabled: Boolean) { dataStore.edit { it[KEY_NOTIFICATIONS] = enabled } }
    suspend fun completeOnboarding() { dataStore.edit { it[KEY_ONBOARDING] = true } }
    suspend fun setLanguage(code: String) { dataStore.edit { it[KEY_LANGUAGE] = code } }
    suspend fun setShuffleEnabled(enabled: Boolean) { dataStore.edit { it[KEY_SHUFFLE] = enabled } }
    suspend fun setRepeatMode(mode: Int) { dataStore.edit { it[KEY_REPEAT] = mode } }

    private companion object {
        val KEY_THEME = stringPreferencesKey("theme_mode")
        val KEY_NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        val KEY_ONBOARDING = booleanPreferencesKey("onboarding_completed")
        val KEY_LANGUAGE = stringPreferencesKey("language")
        val KEY_SHUFFLE = booleanPreferencesKey("shuffle_enabled")
        val KEY_REPEAT = intPreferencesKey("repeat_mode")
    }
}
