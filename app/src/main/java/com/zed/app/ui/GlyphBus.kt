package com.zed.app.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// ============================================================
// Глобальная шина Glyph-событий (эмуляция Glyph Interface).
// Любой модуль в момент ключевого успеха вызывает fire():
// привычка отмечена, приём курса принят, кредит оплачен,
// трек лайкнут. Оверлей GlyphFlashOverlay рисует световую
// точечную волну по периметру экрана (700 мс, красным акцентом).
// Singleton без DI: доступен из любой ViewModel без зависимостей.
// ============================================================
object GlyphBus {

    // Timestamp последней вспышки (0 = ещё не было)
    private val _flash = MutableStateFlow(0L)
    val flash: StateFlow<Long> = _flash.asStateFlow()

    fun fire() {
        _flash.value = System.currentTimeMillis()
    }
}
