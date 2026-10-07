package com.zed.app.core.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

// Принудительный язык приложения: "system" | "ru" | "en".
// Пустой список локалей = следуем за системой.
fun applyAppLocale(code: String) {
    val locales = when (code) {
        "ru" -> LocaleListCompat.forLanguageTags("ru")
        "en" -> LocaleListCompat.forLanguageTags("en")
        else -> LocaleListCompat.getEmptyLocaleList()
    }
    AppCompatDelegate.setApplicationLocales(locales)
}
