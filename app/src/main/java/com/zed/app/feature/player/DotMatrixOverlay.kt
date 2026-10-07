package com.zed.app.feature.player

import android.content.Context
import android.os.PowerManager
import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zed.app.ui.theme.LocalZedColors
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// Аудиореактивный dot-matrix оверлей поверх обложки.
// 5 режимов: RING → EQ → BREATH → WAVE → GLYPH (долгий тап переключает).
//
// БАТАРЕЙНАЯ ДИСЦИПЛИНА:
// — 30 fps вместо 120 Гц дисплея (15 fps в режиме энергосбережения);
// — на паузе кадр статичный: ноль рекомпозиций, ноль отрисовок;
// — в фоне / при выключенном экране тикер полностью остановлен (Lifecycle ON_STOP);
// — уровень звука читается из процессора без StateFlow — без аллокаций в кадре.
private enum class DotFx { RING, EQ, BREATH, WAVE, GLYPH }

@Composable
fun DotMatrixOverlay(modifier: Modifier = Modifier) {
    val colors = LocalZedColors.current
    val accent = colors.accent
    val bright = colors.textPrimary
    val dim = colors.textDisabled
    val off = colors.border

    // VM той же nav-записи, что и у PlayerScreen — общий инстанс
    val vm: PlayerViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val isPlaying = state.isPlaying

    var mode by remember { mutableStateOf(DotFx.RING) }

    // Кадровые переменные: тикер их меняет → Canvas перерисовывается
    var phase by remember { mutableStateOf(0f) }
    var level by remember { mutableStateOf(0f) }
    var bass by remember { mutableStateOf(0f) }

    // Видимость: стоп тикера в фоне и при выключенном экране
    val lifecycleOwner = LocalLifecycleOwner.current
    var visible by remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> visible = true
                Lifecycle.Event.ON_STOP -> visible = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Режим энергосбережения системы: режем fps вдвое
    val context = LocalContext.current
    var powerSave by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        powerSave = runCatching {
            context.getSystemService(Context.POWER_SERVICE)?.let {
                (it as PowerManager).isPowerSaveMode
            } ?: false
        }.getOrDefault(false)
    }

    // Тикер: 30 fps (15 в power save). На паузе или в фоне не крутится вовсе
    LaunchedEffect(isPlaying, visible, powerSave) {
        if (!isPlaying || !visible) {
            // Пауза: гасим уровень — останется статичный тусклый кадр
            level = 0f
            bass = 0f
            return@LaunchedEffect
        }
        val frameDelay = if (powerSave) 66L else 33L
        val start = SystemClock.elapsedRealtime()
        while (true) {
            val t = SystemClock.elapsedRealtime() - start
            phase = (t % 3000L) / 3000f
            // Собственное сглаживание: держим пик, плавно отпускаем
            level = maxOf(vm.audioLevel(), level * 0.88f)
            bass = maxOf(vm.audioBass(), bass * 0.85f)
            kotlinx.coroutines.delay(frameDelay)
        }
    }

    Box(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(onLongPress = {
                mode = DotFx.entries[(mode.ordinal + 1) % DotFx.entries.size]
            })
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
                // 1. КОЛЬЦО: встречное вращение, яркость и размер точек пульсируют от баса
                DotFx.RING -> {
                    listOf(w * 0.46f to 32, w * 0.40f to 28).forEachIndexed { ri, (r, count) ->
                        for (i in 0 until count) {
                            val a = i * twoPi / count + phase * twoPi * if (ri == 0) 1f else -1f
                            val pulse = 0.5f + 0.5f * sin(a * 3 + phase * twoPi)
                            // Порог яркости опускается с уровнем: на бите горит больше точек
                            val isBright = pulse > (0.75f - 0.45f * level)
                            drawSquare(
                                cx + cos(a) * r, cy + sin(a) * r,
                                dot * (1f + 0.45f * bass),
                                if (isBright) bright else dim,
                                0.35f + 0.65f * pulse
                            )
                        }
                    }
                    val ra = phase * twoPi
                    drawSquare(
                        cx + cos(ra) * w * 0.46f, cy + sin(ra) * w * 0.46f,
                        dot * (1.4f + 0.8f * bass), accent, 1f
                    )
                }

                // 2. ЭКВАЛАЙЗЕР ПО КРУГУ: высота столбиков = реальный уровень звука
                DotFx.EQ -> {
                    val bars = 32
                    for (i in 0 until bars) {
                        val a = i * twoPi / bars
                        val pattern = 0.35f + 0.65f * kotlin.math.abs(sin(i * 1.7f + phase * twoPi * 2))
                        val lvl = ((0.15f + 0.85f * level) * pattern).coerceIn(0f, 1f)
                        val steps = (lvl * 6).toInt().coerceIn(1, 6)
                        for (s in 0 until steps) {
                            val r = w * 0.34f + s * gap
                            val isPeak = s == steps - 1 && s >= 4 && bass > 0.35f
                            val col = if (isPeak) accent else if (s % 2 == 0) bright else dim
                            drawSquare(cx + cos(a) * r, cy + sin(a) * r, dot, col, 1f)
                        }
                    }
                }

                // 3. ДЫХАНИЕ: волна яркости, амплитуда и скорость бегунка от уровня
                DotFx.BREATH -> {
                    val count = 40
                    for (i in 0 until count) {
                        val a = i * twoPi / count
                        val wave = (0.5f + 0.5f * sin(phase * twoPi - i * 0.35f)) * (0.35f + 0.65f * level + 0.15f)
                        val col = when {
                            wave > 0.75f -> bright
                            wave > 0.35f -> dim
                            else -> off
                        }
                        drawSquare(
                            cx + cos(a) * w * 0.44f, cy + sin(a) * w * 0.44f,
                            dot * (1f + 0.3f * bass), col, 1f
                        )
                    }
                    val runnerPhase = (phase * (1f + 2f * bass)) % 1f
                    val ra = (runnerPhase * count).toInt() * twoPi / count
                    drawSquare(cx + cos(ra) * w * 0.44f, cy + sin(ra) * w * 0.44f, dot * 1.3f, accent, 1f)
                }

                // 4. МАТРИЦА-ВОЛНА: видны только точки выше порога, порог опускает уровень
                DotFx.WAVE -> {
                    val n = 16
                    val step = w / n
                    for (x in 0 until n) for (y in 0 until n) {
                        val px = x * step + step / 2
                        val py = y * step + step / 2
                        val d = sqrt((px - cx) * (px - cx) + (py - cy) * (py - cy))
                        if (d < w * 0.30f) continue
                        val wave = 0.5f + 0.5f * sin(phase * twoPi - d / (w * 0.08f))
                        if (wave < 0.55f - 0.45f * level) continue // тишина гасит матрицу
                        val col = if (wave > 0.85f && bass > 0.3f) accent
                                  else if (wave > 0.5f) bright else dim
                        drawSquare(px, py, dot * 0.8f, col, 0.25f + 0.75f * wave)
                    }
                }

                // 5. GLYPH: скорость скан-ряда и свечение колонок от уровня
                DotFx.GLYPH -> {
                    val cols = listOf(w * 0.08f, w * 0.16f, w * 0.84f, w * 0.92f)
                    val rows = 14
                    val scanPhase = (phase * (1f + 2f * level)) % 1f
                    val activeRow = (scanPhase * rows).toInt()
                    cols.forEachIndexed { ci, x ->
                        for (r in 0 until rows) {
                            val y = h * 0.1f + r * (h * 0.8f / rows)
                            val moving = (r + ci * 3) % rows == activeRow
                            val col = if (moving) accent
                                      else if ((r + ci) % 4 == 0 && level > 0.25f) bright
                                      else dim
                            drawSquare(x, y, dot * (if (moving) 1f + 0.5f * bass else 1f), col, 1f)
                        }
                    }
                }
            }
        }
    }
}

// Квадратный «пиксель» + ступенчатое свечение (без градиентов)
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
