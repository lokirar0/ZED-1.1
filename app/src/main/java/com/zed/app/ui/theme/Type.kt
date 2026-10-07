package com.zed.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.zed.app.R
import java.util.Locale

// ============================================================
// ZED — Typography Tokens (Nothing OS style)
//
// МУЛЬТИЯЗЫЧНОСТЬ (детерминированная):
// Doto и Space Mono содержат только латиницу, поэтому НЕ используем
// погодовый фолбэк внутри FontFamily (нестабилен с variable-шрифтами).
// Вместо этого семейства переключаются ЦЕЛИКОМ по локали приложения:
//   EN → Doto (точки) / Space Mono (моно)
//   RU → Handjet (точки с кириллицей) / JetBrains Mono (моно с кириллицей)
// Токены читаются на каждой композиции → смена языка применяется
// мгновенно и ко всем пунктам UI без перезапуска.
// ============================================================

// Текущая локаль приложения (учитывает per-app language через AppCompatDelegate)
private fun isRu(): Boolean = Locale.getDefault().language == "ru"

// --- Точечная матрица ---
private val dotLatin by lazy { FontFamily(Font(R.font.doto_regular, FontWeight.Normal)) }
private val dotCyr by lazy { FontFamily(Font(R.font.handjet_regular, FontWeight.Normal)) }

val DotoFont: FontFamily
    get() = if (isRu()) dotCyr else dotLatin

// --- Гротеск (латиница; кириллица уходит в системный фолбэк, как раньше) ---
val SpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk_light, FontWeight.Light),
    Font(R.font.space_grotesk_regular, FontWeight.Normal),
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_bold, FontWeight.Bold)
)

// --- Моно ---
private val monoLatin by lazy {
    FontFamily(
        Font(R.font.space_mono_regular, FontWeight.Normal),
        Font(R.font.space_mono_bold, FontWeight.Bold)
    )
}
private val monoCyr by lazy {
    FontFamily(
        Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
        Font(R.font.jetbrains_mono_bold, FontWeight.Bold)
    )
}

val SpaceMono: FontFamily
    get() = if (isRu()) monoCyr else monoLatin

// --- Typography: два готовых набора, выбираются по локали ---
private val typLatin by lazy { buildTypography(dotLatin, monoLatin) }
private val typCyr by lazy { buildTypography(dotCyr, monoCyr) }

val ZedTypography: Typography
    get() = if (isRu()) typCyr else typLatin

private fun buildTypography(dot: FontFamily, mono: FontFamily) = Typography(
    displayLarge = TextStyle(
        fontFamily = dot,
        fontWeight = FontWeight.Normal,
        fontSize = 72.sp,
        lineHeight = 72.sp,
        letterSpacing = (-0.03).em
    ),
    displayMedium = TextStyle(
        fontFamily = dot,
        fontWeight = FontWeight.Normal,
        fontSize = 48.sp,
        lineHeight = 50.sp,
        letterSpacing = (-0.02).em
    ),
    displaySmall = TextStyle(
        fontFamily = dot,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.02).em
    ),
    headlineLarge = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 29.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 23.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp
    ),
    labelLarge = TextStyle(
        fontFamily = mono,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.04.em
    ),
    labelMedium = TextStyle(
        fontFamily = mono,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.08.em
    ),
    labelSmall = TextStyle(
        fontFamily = mono,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        letterSpacing = 0.08.em
    )
)

// --- Цифры данных (балансы, суммы, тайминги, артисты) ---
private val dataLatin by lazy {
    TextStyle(
        fontFamily = monoLatin,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.04.em
    )
}
private val dataCyr by lazy {
    TextStyle(
        fontFamily = monoCyr,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.04.em
    )
}

val ZedDataNumber: TextStyle
    get() = if (isRu()) dataCyr else dataLatin

// --- Glyph-подписи (жирное моно) ---
private val glyphLatin by lazy {
    TextStyle(
        fontFamily = monoLatin,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 0.08.em
    )
}
private val glyphCyr by lazy {
    TextStyle(
        fontFamily = monoCyr,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 0.08.em
    )
}

val ZedGlyphLabel: TextStyle
    get() = if (isRu()) glyphCyr else glyphLatin
