package com.zed.app.feature.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zed.app.R
import com.zed.app.core.backup.BackupManager
import com.zed.app.core.notifications.NotificationHelper
import com.zed.app.core.notifications.ReminderScheduler
import com.zed.app.core.settings.Settings
import com.zed.app.core.settings.SettingsRepository
import com.zed.app.core.settings.ThemeMode
import com.zed.app.core.settings.applyAppLocale
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val backupManager: BackupManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val settings: StateFlow<Settings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Settings())

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setLanguage(code: String) {
        viewModelScope.launch {
            settingsRepository.setLanguage(code)
            applyAppLocale(code)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationsEnabled(enabled)
            if (enabled) ReminderScheduler.scheduleAll(context)
            else ReminderScheduler.cancelAll(context)
        }
    }

    // Звук уведомлений: сохраняем Uri в устойчивой форме raw/имя
    // и пересоздаём каналы с новым звуком и ритм-вибрацией
    fun setNotificationSound(uri: String) {
        viewModelScope.launch {
            val normalized = NotificationHelper.normalizeSoundUri(context, uri)
            settingsRepository.setNotificationSound(normalized)
            NotificationHelper.ensureChannels(context, normalized)
        }
    }

    // Glyph-акценты: световые вспышки на ключевых событиях (подключаются в J3)
    fun setGlyphEnabled(value: Boolean) {
        viewModelScope.launch { settingsRepository.setGlyphEnabled(value) }
    }

    // Тестовое уведомление: читаем звук честно через first()
    fun sendTestNotification() {
        viewModelScope.launch {
            val sound = settingsRepository.settings.first().notificationSoundUri
            NotificationHelper.show(
                context = context,
                notificationId = 9999,
                title = context.getString(R.string.notif_title),
                text = context.getString(R.string.notif_test_body),
                soundUri = sound
            )
        }
    }

    // --- Данные: бэкап, восстановление, очистка ---

    suspend fun exportBackup(uri: Uri): Boolean = backupManager.exportToUri(uri)

    suspend fun importBackup(uri: Uri): Boolean = backupManager.importFromUri(uri)

    suspend fun clearAll() = backupManager.clearAll()
}
