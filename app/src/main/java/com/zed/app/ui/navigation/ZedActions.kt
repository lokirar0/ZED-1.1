package com.zed.app.ui.navigation

import androidx.compose.runtime.staticCompositionLocalOf

// Глобальные навигационные действия: доступны любому компоненту
// без пробрасывания параметров через десятки экранов
data class ZedActions(
    val openSettings: () -> Unit = {},
    val openSearch: () -> Unit = {}
)

val LocalZedActions = staticCompositionLocalOf { ZedActions() }
