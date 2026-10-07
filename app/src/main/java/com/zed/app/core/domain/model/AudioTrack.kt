package com.zed.app.core.domain.model

// Локальный аудиофайл из MediaStore
data class AudioTrack(
    val id: Long,
    val title: String,          // пустая, если теги и имя файла нечитаемы
    val artist: String,         // пустая, если исполнитель неизвестен
    val durationMs: Long,
    val contentUri: String,
    val artUri: String?,
    val isUnknownTitle: Boolean = false // UI покажет "UNKNOWN TRACK" мелким шрифтом
)
