package com.zed.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch

// Каскадное появление карточек в стиле Nothing: альфа + сдвиг снизу,
// задержка = index * 28 мс. Используется на экранах аналитики.
@Composable
fun Modifier.zedAppear(index: Int): Modifier {
    val alpha = remember { Animatable(0f) }
    val offsetDp = remember { Animatable(16f) }
    val density = LocalDensity.current

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 28L)
        launch { alpha.animateTo(1f, tween(240, easing = FastOutSlowInEasing)) }
        launch { offsetDp.animateTo(0f, tween(300, easing = FastOutSlowInEasing)) }
    }

    return this.graphicsLayer {
        this.alpha = alpha.value
        translationY = offsetDp.value * density.density
    }
}
