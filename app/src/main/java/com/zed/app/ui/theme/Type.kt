package com.zed.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.zed.app.R

// ============================================================
// ZED — Typography Tokens (Nothing OS style)
//
// МУЛЬТИЯЗЫЧНОСТЬ: Doto и Space Mono не содержат кириллицу.
// Поэтому каждое семейство = основной шрифт (латиница) +
// фолбэк с кириллицей в том же стиле. Compose подставляет
// фолбэк погодово: EN выглядит как раньше, RU получает точки/моно.
// Правка здесь автоматически применяется ко ВСЕМ пунктам UI,
// потому что вся типографика ссылается на эти семейства.
// ============================================================

// Точечная матрица: Doto (EN) + Handjet (RU, квадратные LED-точки)
val DotoFont = FontFamily(
    Font(R.font.doto_regular, FontWeight.Normal),
    Font(R.font.handjet_regular, FontWeight.Normal)
)

// Гротеск: Space Grotesk (латиница); кириллица уходит в системный фолбэк
val SpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk_light, FontWeight.Light),
    Font(R.font.space_grotesk_regular, FontWeight.Normal),
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_bold, FontWeight.Bold)
)

// Моно: Space Mono (EN) + JetBrains Mono (RU) — цифры и лейблы всегда моно
val SpaceMono = FontFamily(
    Font(R.font.space_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.space_mono_bold, FontWeight.Bold),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold)
)

val ZedTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = DotoFont,
        fontWeight = FontWeight.Normal,
        fontSize = 72.sp,
        lineHeight = 72.sp,
        letterSpacing = (-0.03).em
    ),
    displayMedium = TextStyle(
        fontFamily = DotoFont,
        fontWeight = FontWeight.Normal,
        fontSize = 48.sp,
        lineHeight = 50.sp,
        letterSpacing = (-0.02).em
    ),
    displaySmall = TextStyle(
        fontFamily = DotoFont,
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
        fontFamily = SpaceMono,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.04.em
    ),
    labelMedium = TextStyle(
        fontFamily = SpaceMono,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.08.em
    ),
    labelSmall = TextStyle(
        fontFamily = SpaceMono,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        letterSpacing = 0.08.em
    )
)

// Цифры данных (балансы, суммы, тайминги) — моно с кириллическим фолбэком
val ZedDataNumber = TextStyle(
    fontFamily = SpaceMono,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    letterSpacing = 0.04.em
)

// Glyph-подписи
val ZedGlyphLabel = TextStyle(
    fontFamily = SpaceMono,
    fontWeight = FontWeight.Bold,
    fontSize = 11.sp,
    letterSpacing = 0.08.em
)
