package com.zed.app.feature.player

import android.content.Context
import android.graphics.Canvas
import android.graphics.Movie
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
// Декодер — системный android.graphics.Movie (нуль внешних зависимостей).
//
// 1) ПОСЛЕДНИЙ ВЫБРАННЫЙ GIF: индекс хранится в GifIndexStore
//    (SharedPreferences) — переживает выход из плеера, переключение
//    вкладок и перезапуск приложения.
// 2) В ТАКТ С МУЗЫКОЙ: скорость кадров пропорциональна уровню звука
//    (audioLevel), бас даёт лёгкий пульс масштаба (audioBass),
//    на паузе GIF замирает на текущем кадре.
// 3) ЛЮБОЕ РАЗРЕШЕНИЕ: кадр вписывается в квадрат 1:1 целиком и
//    центрируется — без искажений и обрезки.
// Долгий тап — следующий GIF по кругу.
// Нет fx_* в res/raw — рисуется нативный dot-matrix оверлей.
// ============================================================

// Хранилище последнего выбранного GIF (переживает перезапуски)
object GifIndexStore {
    private const val NAME = "zed_gif_prefs"
    private const val KEY = "gif_index"

    fun get(context: Context): Int =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).getInt(KEY, 0)

    fun set(context: Context, index: Int) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY, index).apply()
    }
}

// Динамический список R.raw.fx_01..fx_99 (нулевые id пропускаются)
fun embeddedGifRes(context: Context): List<Int> {
    val res = context.resources
    val pkg = context.packageName
    return (1..99).mapNotNull { i ->
        res.getIdentifier(String.format("fx_%02d", i), "raw", pkg).takeIf { it != 0 }
    }
}

// Точка входа: GIF из APK или нативные точки, если GIF нет.
// Провайдеры звука приходят из PlayerViewModel (аудиопроцессор).
@Composable
fun ZedPlayerOverlay(
    modifier: Modifier = Modifier,
    level: () -> Float = { 0f },
    bass: () -> Float = { 0f },
    playing: () -> Boolean = { false }
) {
    val context = LocalContext.current
    val models = remember(context) { embeddedGifRes(context) }
    if (models.isEmpty()) {
        DotMatrixOverlay(modifier)
    } else {
        MovieGifOverlay(models, modifier, level, bass, playing)
    }
}

@Composable
private fun MovieGifOverlay(
    models: List<Int>,
    modifier: Modifier,
    level: () -> Float,
    bass: () -> Float,
    playing: () -> Boolean
) {
    val context = LocalContext.current
    // Стартуем с ПОСЛЕДНЕГО ВЫБРАННОГО GIF, а не с первого
    var index by remember { mutableStateOf(GifIndexStore.get(context).mod(models.size)) }
    val safeIndex = index.mod(models.size)

    // Movie из raw-ресурса; передекодируем только при смене индекса
    val movie = remember(context, safeIndex) {
        runCatching {
            context.resources.openRawResource(models[safeIndex]).use { Movie.decodeStream(it) }
        }.getOrNull()
    }

    AndroidView(
        modifier = modifier.pointerInput(models) {
            detectTapGestures(onLongPress = {
                index = (index + 1).mod(models.size)
                GifIndexStore.set(context, index)
            })
        },
        factory = { ctx -> MovieView(ctx) },
        update = { view -> view.bind(movie, level, bass, playing) }
    )
}

// View, которое крутит Movie В ТАКТ С МУЗЫКОЙ:
// — скорость кадров = f(уровень звука): тише → медленнее, бит → быстрее;
// — бас слегка пульсирует масштабом;
// — пауза = стоп-кадр (время не течёт).
private class MovieView(context: Context) : View(context) {

    private var movie: Movie? = null
    private var levelProvider: () -> Float = { 0f }
    private var bassProvider: () -> Float = { 0f }
    private var playingProvider: () -> Boolean = { false }

    private var movieTimeMs = 0f
    private var lastFrameMs = SystemClock.uptimeMillis()

    fun bind(
        m: Movie?,
        level: () -> Float,
        bass: () -> Float,
        playing: () -> Boolean
    ) {
        if (m !== movie) {
            movie = m
            movieTimeMs = 0f
        }
        levelProvider = level
        bassProvider = bass
        playingProvider = playing
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val m = movie ?: return
        val now = SystemClock.uptimeMillis()
        val dt = (now - lastFrameMs).coerceAtMost(100L)
        lastFrameMs = now

        val dur = if (m.duration() > 0) m.duration().toFloat() else 3000f

        // Играет музыка → время GIF течёт со скоростью, зависящей от уровня;
        // пауза → кадр замирает (в такт тишине)
        if (playingProvider()) {
            movieTimeMs = (movieTimeMs + dt * (0.35f + levelProvider() * 2.6f)) % dur
        }
        m.setTime(movieTimeMs.toInt())

        // Вписываем целиком и центрируем + лёгкий пульс от баса
        val pulse = 1f + bassProvider() * 0.06f
        val scale = minOf(width.toFloat() / m.width(), height.toFloat() / m.height()) * pulse
        val dx = (width - m.width() * scale) / 2f
        val dy = (height - m.height() * scale) / 2f

        canvas.save()
        canvas.translate(dx, dy)
        canvas.scale(scale, scale)
        m.draw(canvas, 0f, 0f)
        canvas.restore()

        invalidate()
    }
}
