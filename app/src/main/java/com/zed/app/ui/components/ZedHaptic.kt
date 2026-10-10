package com.zed.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

// Обёртка клика с тактильным откликом в стиле Nothing:
// короткий «текстовый» щелчок на каждое действие пользователя.
@Composable
fun rememberHapticClick(onClick: () -> Unit): () -> Unit {
    val haptic = LocalHapticFeedback.current
    return remember(onClick) {
        {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        }
    }
}
