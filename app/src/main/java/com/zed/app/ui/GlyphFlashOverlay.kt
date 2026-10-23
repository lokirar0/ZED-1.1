package com.zed.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// ============================================================
// Оверлей световой вспышки в стиле Glyph: квадратные точки
// по периметру экрана загораются и гаснут волной за 700 мс.
//
// ВАЖНО: оверлей НЕ перехватывает касания — у Canvas нет
// pointerInput, поэтому клики проходят сквозь него к контенту.
// Рисуется поверх всего (в MainActivity), включая bottom-sheet.
// Цвет — красный акцент ZED (#D71921), как световые интерфейсы
// Nothing: монохром + один неоновый акцент.
// ============================================================
@Composable
fun GlyphFlashOverlay() {
    val ts by GlyphBus.flash.collectAsStateWithLifecycle()
    var alpha by remember { mutableFloatStateOf(0f) }

    // Новая вспышка: альфа 1 → 0 за 700 мс
    LaunchedEffect(ts) {
        if (ts == 0L) return@LaunchedEffect
        val anim = Animatable(1f)
        anim.animateTo(
            targetValue = 0f,
            animationSpec = androidx.compose.animation.core.tween(700)
        ) {
            alpha = value
        }
        alpha = 0f
    }

    if (alpha > 0.01f) {
        Canvas(Modifier.fillMaxSize()) {
            val dot = 6.dp.toPx()
            val inset = 10.dp.toPx()
            val step = 26.dp.toPx()
            val color = Color(0xFFD71921).copy(alpha = alpha)

            // Верхняя и нижняя кромки
            var x = inset
            while (x < size.width - inset) {
                drawRect(color, Offset(x, inset), size = androidx.compose.ui.size.Size(dot, dot))
                drawRect(color, Offset(x, size.height - inset - dot), size = androidx.compose.ui.size.Size(dot, dot))
                x += step
            }
            // Левая и правая кромки
            var y = inset + step
            while (y < size.height - inset - step) {
                drawRect(color, Offset(inset, y), size = androidx.compose.ui.size.Size(dot, dot))
                drawRect(color, Offset(size.width - inset - dot, y), size = androidx.compose.ui.size.Size(dot, dot))
                y += step
            }
        }
    }
}
