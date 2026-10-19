package com.zed.app.feature.player

import android.content.Context
import android.graphics.Movie
import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import kotlin.math.min

// ============================================================
// Оверлей плеера: 26 встроенных GIF из res/raw (fx_01..fx_26).
// Декодер — системный android.graphics.Movie: НУЛЬ внешних
// зависимостей (coil-gif больше не нужен), потоковая декодация
// кадров без загрузки всего файла в память.
//
// ЛЮБОЕ РАЗРЕШЕНИЕ GIF: кадр вписывается в квадрат 1:1 целиком
// и центрируется (scale = min по осям + смещение к центру) —
// «выровнено под ровный квадрат, по центру», без искажений.
//
// Долгий тап по обложке листает GIF по кругу.
// Если в res/raw нет ни одного fx_* — рисуется нативный
// dot-matrix оверлей (DotMatrixOverlay), плеер не остаётся пустым.
// ============================================================

// Динамический список R.raw.fx_01..fx_99 (нулевые id пропускаются)
fun embeddedGifRes(context: Context): List<Int> {
    val res = context.resources
    val pkg = context.packageName
    return (1..99).mapNotNull { i ->
        res.getIdentifier(String.format("fx_%02d", i), "raw", pkg).takeIf { it != 0 }
    }
}

// Точка входа: GIF из APK или нативные точки, если GIF нет
@Composable
fun ZedPlayerOverlay(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val models = remember(context) { embeddedGifRes(context) }
    if (models.isEmpty()) {
        DotMatrixOverlay(modifier)
    } else {
        MovieGifOverlay(models, modifier)
    }
}

@Composable
private fun MovieGifOverlay(models: List<Int>, modifier: Modifier) {
    val context = LocalContext.current
    var index by remember { mutableStateOf(0) }
    val safeIndex = index.mod(models.size)

    // Movie из raw-ресурса; передекодируем только при смене индекса
    val movie = remember(context, safeIndex) {
        runCatching {
            context.resources.openRawResource(models[safeIndex]).use { Movie.decodeStream(it) }
        }.getOrNull()
    }

    // Длительность цикла: из метаданных GIF, иначе дефолт 3 секунды
    val durationMs = movie?.duration()?.takeIf { it > 0 } ?: 3000

    // Кадровое время: тикаем синхронно с vsync дисплея (до 120 Гц)
    var timeMs by remember(movie) { mutableStateOf(0L) }
    LaunchedEffect(movie) {
        if (movie == null) return@LaunchedEffect
        val start = SystemClock.uptimeMillis()
        while (true) {
            withFrameMillis { }
            timeMs = (SystemClock.uptimeMillis() - start) % durationMs
        }
    }

    Canvas(
        modifier = modifier.pointerInput(models) {
            detectTapGestures(onLongPress = { index = (index + 1).mod(models.size) })
        }
    ) {
        val m = movie ?: return@Canvas
        m.setTime(timeMs.toInt())
        // Вписываем целиком и центрируем: масштаб по меньшей оси + смещение
        val scale = min(size.width / m.width(), size.height / m.height())
        val dx = (size.width - m.width() * scale) / 2f
        val dy = (size.height - m.height() * scale) / 2f
        drawIntoCanvas { c ->
            val native = c.nativeCanvas
            native.save()
            native.translate(dx, dy)
            native.scale(scale, scale)
            m.draw(native, 0f, 0f)
            native.restore()
        }
    }
}
