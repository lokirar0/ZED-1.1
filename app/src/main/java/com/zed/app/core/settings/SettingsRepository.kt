package com.zed.app.core.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
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
    val language: String = "system",
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = 0,
    val fxSpeed: Float = 1f,
    val fxReverb: Int = 0,
    val fxEq: String = "0.5,0.5,0.5,0.5,0.5",
    val notificationSoundUri: String = "",
    val permissionsAsked: Boolean = false // экран первичных разрешений показан/пройден
)

fun parseEqGains(s: String): List<Float> =
    s.split(",")
        .map { it.trim().toFloatOrNull() ?: 0.5f }
        .let { if (it.size == 5) it else List(5) { 0.5f } }

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val settings: Flow<Settings> = dataStore.data.map { prefs ->
        Settings(
            themeMode = prefs[KEY_THEME]
                ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            notificationsEnabled = prefs[KEY_NOTIFICATIONS] ?: true,
            onboardingCompleted = prefs[KEY_ONBOARDING] ?: false,
            language = prefs[KEY_LANGUAGE] ?: "system",
            shuffleEnabled = prefs[KEY_SHUFFLE] ?: false,
            repeatMode = prefs[KEY_REPEAT] ?: 0,
            fxSpeed = prefs[KEY_FX_SPEED] ?: 1f,
            fxReverb = prefs[KEY_FX_REVERB] ?: 0,
            fxEq = prefs[KEY_FX_EQ] ?: "0.5,0.5,0.5,0.5,0.5",
            notificationSoundUri = prefs[KEY_NOTIF_SOUND] ?: "",
            permissionsAsked = prefs[KEY_PERMS_ASKED] ?: false
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) { dataStore.edit { it[KEY_THEME] = mode.name } }
    suspend fun setNotificationsEnabled(enabled: Boolean) { dataStore.edit { it[KEY_NOTIFICATIONS] = enabled } }
    suspend fun completeOnboarding() { dataStore.edit { it[KEY_ONBOARDING] = true } }
    suspend fun setLanguage(code: String) { dataStore.edit { it[KEY_LANGUAGE] = code } }
    suspend fun setShuffleEnabled(enabled: Boolean) { dataStore.edit { it[KEY_SHUFFLE] = enabled } }
    suspend fun setRepeatMode(mode: Int) { dataStore.edit { it[KEY_REPEAT] = mode } }
    suspend fun setPermissionsAsked(value: Boolean) { dataStore.edit { it[KEY_PERMS_ASKED] = value } }

    suspend fun setFx(speed: Float, reverb: Int, eq: String) {
        dataStore.edit {
            it[KEY_FX_SPEED] = speed
            it[KEY_FX_REVERB] = reverb
            it[KEY_FX_EQ] = eq
        }
    }

    suspend fun setNotificationSound(uri: String) {
        dataStore.edit { it[KEY_NOTIF_SOUND] = uri }
    }

    private companion object {
        val KEY_THEME = stringPreferencesKey("theme_mode")
        val KEY_NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        val KEY_ONBOARDING = booleanPreferencesKey("onboarding_completed")
        val KEY_LANGUAGE = stringPreferencesKey("language")
        val KEY_SHUFFLE = booleanPreferencesKey("shuffle_enabled")
        val KEY_REPEAT = intPreferencesKey("repeat_mode")
        val KEY_FX_SPEED = floatPreferencesKey("fx_speed")
        val KEY_FX_REVERB = intPreferencesKey("fx_reverb")
        val KEY_FX_EQ = stringPreferencesKey("fx_eq")
        val KEY_NOTIF_SOUND = stringPreferencesKey("notification_sound_uri")
        val KEY_PERMS_ASKED = booleanPreferencesKey("permissions_asked")
    }
}
