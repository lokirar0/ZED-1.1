package com.zed.app.feature.player

import android.content.Context
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.gif.GifDecoder

// ============================================================
// Оверлей плеера: 26 встроенных GIF из res/raw (fx_01..fx_26).
// Список обнаруживается динамически через getIdentifier — код
// не зависит от точного количества файлов.
//
// ЛЮБОЕ РАЗРЕШЕНИЕ GIF: оверлей — квадрат 1:1, каждый GIF
// вписывается целиком (Fit) и центрируется (Center) —
// «выровнено под ровный квадрат, по центру», без искажений.
//
// Долгий тап по обложке листает GIF по кругу.
// Если в res/raw нет ни одного fx_* — рисуется нативный
// dot-matrix оверлей (DotMatrixOverlay), плеер не остаётся пустым.
// ============================================================

// Динамический список R.raw.fx_01..fx_99 (0 пропускаются)
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
        EmbeddedGifOverlay(models, modifier)
    }
}

@Composable
private fun EmbeddedGifOverlay(
    models: List<Int>,
    modifier: Modifier
) {
    val context = LocalContext.current
    var index by remember { mutableStateOf(0) }

    // ImageLoader с GIF-декодером создаём один раз
    val gifLoader = remember(context) {
        ImageLoader.Builder(context)
            .components { add(GifDecoder) }
            .build()
    }

    val safeIndex = index.mod(models.size)

    Box(
        modifier = modifier.pointerInput(models) {
            detectTapGestures(onLongPress = { index = (index + 1).mod(models.size) })
        }
    ) {
        AsyncImage(
            model = models[safeIndex],
            imageLoader = gifLoader,
            contentDescription = null,
            // Любое разрешение → вписываем в квадрат и центрируем
            contentScale = ContentScale.Fit,
            alignment = Alignment.Center,
            modifier = Modifier.matchParentSize()
        )
    }
}
