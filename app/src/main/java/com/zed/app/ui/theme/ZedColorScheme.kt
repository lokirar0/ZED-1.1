package com.zed.app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ============================================================
// Расширенная схема цветов ZED.
// MaterialTheme не знает про textSecondary / border / glass —
// пробрасываем их через CompositionLocal, чтобы компоненты
// были темо-независимыми (dark ↔ light).
// ============================================================

data class ZedColorScheme(
    val background: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val border: Color,
    val borderVisible: Color,
    val textDisplay: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textDisabled: Color,
    val accent: Color,
    val accentSubtle: Color,
    val success: Color,
    val warning: Color,
    val scrim: Color,
    val glassOverlay: Color,
    val habitActive: Color,
    val financeIncome: Color,
    val financeExpense: Color,
    val creditDue: Color,
    val playerWave: Color
)

// --- Тёмная схема (True Black, OLED) ---
val ZedDarkColors = ZedColorScheme(
    background = ZedBlack,
    surface = ZedSurface,
    surfaceRaised = ZedSurfaceRaised,
    border = ZedBorder,
    borderVisible = ZedBorderVisible,
    textDisplay = ZedTextDisplay,
    textPrimary = ZedTextPrimary,
    textSecondary = ZedTextSecondary,
    textDisabled = ZedTextDisabled,
    accent = ZedAccent,
    accentSubtle = ZedAccentSubtle,
    success = ZedSuccess,
    warning = ZedWarning,
    scrim = ZedScrim,
    glassOverlay = ZedGlassOverlay,
    habitActive = ZedHabitActive,
    financeIncome = ZedFinanceIncome,
    financeExpense = ZedFinanceExpense,
    creditDue = ZedCreditDue,
    playerWave = ZedPlayerWave
)

// --- Светлая схема (off-white, «бумажная») ---
val ZedLightColors = ZedColorScheme(
    background = ZedLightBackground,
    surface = ZedLightSurface,
    surfaceRaised = ZedLightSurfaceRaised,
    border = ZedLightBorder,
    borderVisible = ZedLightBorderVisible,
    textDisplay = ZedLightTextDisplay,
    textPrimary = ZedLightTextPrimary,
    textSecondary = ZedLightTextSecondary,
    textDisabled = ZedLightTextDisabled,
    accent = ZedLightAccent,
    accentSubtle = ZedLightAccentSubtle,
    success = ZedLightSuccess,
    warning = ZedLightWarning,
    scrim = ZedLightScrim,
    glassOverlay = ZedLightGlassOverlay,
    habitActive = ZedLightHabitActive,
    financeIncome = ZedLightFinanceIncome,
    financeExpense = ZedLightFinanceExpense,
    creditDue = ZedLightCreditDue,
    playerWave = ZedLightPlayerWave
)

// --- Доступ к токенам из любого компонента: LocalZedColors.current.accent ---
val LocalZedColors = staticCompositionLocalOf { ZedDarkColors }
