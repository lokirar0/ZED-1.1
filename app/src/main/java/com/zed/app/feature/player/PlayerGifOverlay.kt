package com.zed.app.feature.player

import android.content.Context
import android.graphics.Canvas
import android.graphics.Movie
import android.graphics.Paint
import android.os.SystemClock
import android.view.View
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

// ============================================================
// Оверлей плеера: 26 встроенных GIF из res/raw (fx_01..fx_26).
// Декодер — системный android.graphics.Movie: НУЛЬ внешних
// зависимостей, потоковая декодация кадров без загрузки всего
// файла в память. Отрисовка — обычный Android View через
// AndroidView: только родной android.graphics.Canvas, без
// экзотических Compose graphics API.
//
// ЛЮБОЕ РАЗРЕШЕНИЕ GIF: кадр вписывается в квадрат 1:1 целиком
// и центрируется (scale по меньшей оси + смещение к центру) —
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

    AndroidView(
        modifier = modifier.pointerInput(models) {
            detectTapGestures(onLongPress = { index = (index + 1).mod(models.size) })
        },
        factory = { ctx -> MovieView(ctx) },
        update = { view -> view.setMovie(movie) }
    )
}

// View, которое крутит Movie: кадр по времени + invalidate() для
// следующего кадра (синхронно с vsync). Масштаб/центрирование — в onDraw.
private class MovieView(context: Context) : View(context) {

    private var movie: Movie? = null
    private var startMs = SystemClock.uptimeMillis()

    @Suppress("unused")
    private val paint = Paint().apply { isFilterBitmap = false }

    fun setMovie(m: Movie?) {
        movie = m
        startMs = SystemClock.uptimeMillis()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val m = movie ?: return
        // Длительность цикла: из метаданных GIF, иначе дефолт 3 секунды
        val dur = if (m.duration() > 0) m.duration() else 3000
        m.setTime(((SystemClock.uptimeMillis() - startMs) % dur).toInt())

        // Вписываем целиком и центрируем: масштаб по меньшей оси + смещение
        val scale = minOf(width.toFloat() / m.width(), height.toFloat() / m.height())
        val dx = (width - m.width() * scale) / 2f
        val dy = (height - m.height() * scale) / 2f

        canvas.save()
        canvas.translate(dx, dy)
        canvas.scale(scale, scale)
        m.draw(canvas, 0f, 0f)
        canvas.restore()

        // Запрашиваем следующий кадр
        invalidate()
    }
}
