package com.zed.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

// Тактильный отклик теперь ГЛОБАЛЬНЫЙ: MainActivity.dispatchTouchEvent
// даёт тик на каждый тап и отклик на удержание по всему приложению.
// Эта обёртка оставлена как прозрачный passthrough, чтобы существующие
// вызовы rememberHapticClick(...) продолжали компилироваться без двойной вибрации.
@Composable
fun rememberHapticClick(onClick: () -> Unit): () -> Unit = remember(onClick) { onClick }
