package com.zed.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zed.app.core.settings.Settings
import com.zed.app.core.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MainViewModel @Inject constructor(
    settingsRepository: SettingsRepository
) : ViewModel() {

    // null = настройки ещё не прочитаны (показываем чёрный экран, без вспышки онбординга)
    val settings: StateFlow<Settings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
