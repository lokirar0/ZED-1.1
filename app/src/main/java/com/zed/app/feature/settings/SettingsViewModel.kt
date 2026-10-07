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

    // Язык: сохраняем и применяем немедленно (Activity пересоздастся сама)
    fun setLanguage(code: String) {
        viewModelScope.launch {
            settingsRepository.setLanguage(code)
            applyAppLocale(code)
        }
    }

    // Переключение уведомлений: планируем/отменяем ОБА напоминания
    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationsEnabled(enabled)
            if (enabled) ReminderScheduler.scheduleAll(context)
            else ReminderScheduler.cancelAll(context)
        }
    }

    // Мгновенная проверка канала и разрешений: пуш должен прийти сразу
    fun sendTestNotification() {
        NotificationHelper.show(
            context = context,
            notificationId = 9999,
            title = context.getString(R.string.notif_title),
            text = context.getString(R.string.notif_test_body)
        )
    }

    // --- Данные: бэкап, восстановление, очистка ---

    suspend fun exportBackup(uri: Uri): Boolean = backupManager.exportToUri(uri)

    suspend fun importBackup(uri: Uri): Boolean = backupManager.importFromUri(uri)

    suspend fun clearAll() = backupManager.clearAll()
}
