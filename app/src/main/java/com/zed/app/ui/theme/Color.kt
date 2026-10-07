package com.zed.app.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================
// ZED — Color Tokens (Dark Mode / Nothing OS style / True Black)
// ============================================================

// --- Primary Background (OLED) ---
val ZedBlack = Color(0xFF000000)
val ZedSurface = Color(0xFF111111)
val ZedSurfaceRaised = Color(0xFF1A1A1A)

// --- Borders & Dividers ---
val ZedBorder = Color(0xFF222222)
val ZedBorderVisible = Color(0xFF333333)

// --- Text Colors ---
val ZedTextDisplay = Color(0xFFFFFFFF)
val ZedTextPrimary = Color(0xFFE8E8E8)
val ZedTextSecondary = Color(0xFF999999)
val ZedTextDisabled = Color(0xFF666666)

// --- Accent & Signal ---
val ZedAccent = Color(0xFFD71921)
val ZedAccentSubtle = Color(0x33D71921)
val ZedSuccess = Color(0xFF4CAF50)
val ZedWarning = Color(0xFFFF9800)

// --- Overlay & Scrim ---
val ZedScrim = Color(0xD9000000)
val ZedGlassOverlay = Color(0x1AFFFFFF)

// --- Специфичные для модулей ---
val ZedHabitActive = ZedAccent
val ZedFinanceIncome = Color(0xFF4CAF50)
val ZedFinanceExpense = ZedAccent
val ZedCreditDue = ZedAccent
val ZedPlayerWave = ZedTextPrimary
