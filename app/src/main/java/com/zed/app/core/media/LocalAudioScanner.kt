package com.zed.app.core.media

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.zed.app.core.domain.model.AudioTrack
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Сканирование локальной библиотеки через MediaStore + очистка метаданных:
// никаких "<unknown>" и сырых имён вида "2_5408905757577980147" в UI.
@Singleton
class LocalAudioScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun scan(): List<AudioTrack> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<AudioTrack>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DISPLAY_NAME
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val displayCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val albumId = cursor.getLong(albumCol)
                val (title, unknown) = cleanTitle(
                    rawTitle = cursor.getString(titleCol),
                    displayName = cursor.getString(displayCol)
                )
                tracks += AudioTrack(
                    id = id,
                    title = title,
                    artist = cleanArtist(cursor.getString(artistCol)),
                    durationMs = cursor.getLong(durationCol),
                    contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                    ).toString(),
                    artUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"), albumId
                    ).toString(),
                    isUnknownTitle = unknown
                )
            }
        }
        tracks
    }

    // Название: тег → имя файла (без расширения, "_" и "-" → пробелы) → "неизвестно"
    private fun cleanTitle(rawTitle: String?, displayName: String?): Pair<String, Boolean> {
        val fromTag = rawTitle?.trim().orEmpty()
        val fromFile = (displayName ?: "")
            .substringBeforeLast('.')
            .replace('_', ' ')
            .replace('-', ' ')
            .trim()
        return when {
            fromTag.hasLetters() && !fromTag.equals("<unknown>", true) && !fromTag.equals("unknown", true) ->
                fromTag to false
            fromFile.hasLetters() ->
                fromFile to false
            else ->
                "" to true // набор цифр/символов → UI покажет UNKNOWN TRACK
        }
    }

    // Исполнитель: "<unknown>" и пустота → пустая строка (UI скроет строку)
    private fun cleanArtist(raw: String?): String {
        val v = raw?.trim().orEmpty()
        return if (v.isEmpty() || v.equals("<unknown>", true) || v.equals("unknown", true)) "" else v
    }

    private fun String.hasLetters(): Boolean = isNotEmpty() && any { it.isLetter() }
}
