package com.zed.app.feature.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import com.zed.app.ui.theme.LocalZedColors
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// 5 режимов dot-matrix оверлея. Долгий тап по обложке переключает режим.
private enum class DotFx { RING, EQ, BREATH, WAVE, GLYPH }

@Composable
fun DotMatrixOverlay(modifier: Modifier = Modifier) {
    val colors = LocalZedColors.current
    val accent = colors.accent
    val bright = colors.textPrimary   // #E8E8E8
    val dim = colors.textDisabled     // #666666
    val off = colors.border           // #222222
    var mode by remember { mutableStateOf(DotFx.RING) }

    val t = rememberInfiniteTransition(label = "dotfx")
    val phase by t.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, LinearEasing), RepeatMode.Restart),
        label = "phase"
    )
    val fast by t.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800, LinearEasing), RepeatMode.Restart),
        label = "fast"
    )

    Box(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(onLongPress = { mode = DotFx.entries[(mode.ordinal + 1) % DotFx.entries.size] })
        }
    ) {
        Canvas(Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            val dot = w / 48f
            val gap = dot * 1.6f
            val twoPi = (Math.PI * 2).toFloat()

            when (mode) {
                // 1. Вращающиеся кольца + красный бегунок
                DotFx.RING -> {
                    listOf(w * 0.46f to 32, w * 0.40f to 28).forEachIndexed { ri, (r, count) ->
                        for (i in 0 until count) {
                            val a = i * twoPi / count + phase * twoPi * if (ri == 0) 1f else -1f
                            val pulse = 0.5f + 0.5f * sin(a * 3 + phase * twoPi)
                            drawSquare(cx + cos(a) * r, cy + sin(a) * r, dot, if (pulse > 0.6f) bright else dim, 0.35f + 0.65f * pulse)
                        }
                    }
                    val ra = phase * twoPi
                    drawSquare(cx + cos(ra) * w * 0.46f, cy + sin(ra) * w * 0.46f, dot * 1.4f, accent, 1f)
                }

                // 2. Круговой эквалайзер
                DotFx.EQ -> {
                    val bars = 32
                    for (i in 0 until bars) {
                        val a = i * twoPi / bars
                        val level = 0.25f + 0.75f * kotlin.math.abs(sin(i * 1.7f + fast * twoPi) * sin(i * 0.53f + phase * twoPi))
                        val steps = (level * 6).toInt().coerceIn(1, 6)
                        for (s in 0 until steps) {
                            val r = w * 0.34f + s * gap
                            val col = if (s == steps - 1 && s >= 4) accent else if (s % 2 == 0) bright else dim
                            drawSquare(cx + cos(a) * r, cy + sin(a) * r, dot, col, 1f)
                        }
                    }
                }

                // 3. Дыхание: волна яркости по кольцу + красный бегунок
                DotFx.BREATH -> {
                    val count = 40
                    for (i in 0 until count) {
                        val a = i * twoPi / count
                        val wave = 0.5f + 0.5f * sin(phase * twoPi - i * 0.35f)
                        val col = when {
                            wave > 0.75f -> bright
                            wave > 0.35f -> dim
                            else -> off
                        }
                        drawSquare(cx + cos(a) * w * 0.44f, cy + sin(a) * w * 0.44f, dot, col, 1f)
                    }
                    val ra = (phase * count).toInt() * twoPi / count
                    drawSquare(cx + cos(ra) * w * 0.44f, cy + sin(ra) * w * 0.44f, dot * 1.3f, accent, 1f)
                }

                // 4. Матрица-волна с прозрачным центром
                DotFx.WAVE -> {
                    val n = 16
                    val step = w / n
                    for (x in 0 until n) for (y in 0 until n) {
                        val px = x * step + step / 2
                        val py = y * step + step / 2
                        val d = sqrt((px - cx) * (px - cx) + (py - cy) * (py - cy))
                        if (d < w * 0.30f) continue // прозрачный центр
                        val wave = 0.5f + 0.5f * sin(phase * twoPi - d / (w * 0.08f))
                        val col = if (wave > 0.85f) accent else if (wave > 0.5f) bright else dim
                        drawSquare(px, py, dot * 0.8f, col, 0.25f + 0.75f * wave)
                    }
                }

                // 5. Glyph-полосы по бокам + красный скан-ряд
                DotFx.GLYPH -> {
                    val cols = listOf(w * 0.08f, w * 0.16f, w * 0.84f, w * 0.92f)
                    val rows = 14
                    val activeRow = (phase * rows).toInt()
                    cols.forEachIndexed { ci, x ->
                        for (r in 0 until rows) {
                            val y = h * 0.1f + r * (h * 0.8f / rows)
                            val moving = (r + ci * 3) % rows == activeRow
                            val col = if (moving) accent else if ((r + ci) % 4 == 0) bright else dim
                            drawSquare(x, y, dot, col, 1f)
                        }
                    }
                }
            }
        }
    }
}

// Квадратный «пиксель» + ступенчатое свечение (второй квадрат с низкой альфой, без градиентов)
private fun DrawScope.drawSquare(x: Float, y: Float, size: Float, color: Color, alpha: Float) {
    if (alpha > 0.6f) {
        drawRect(
            color = color,
            alpha = 0.16f,
            topLeft = Offset(x - size * 0.9f, y - size * 0.9f),
            size = Size(size * 2.8f, size * 2.8f)
        )
    }
    drawRect(
        color = color,
        alpha = alpha,
        topLeft = Offset(x - size / 2f, y - size / 2f),
        size = Size(size, size)
    )
}
