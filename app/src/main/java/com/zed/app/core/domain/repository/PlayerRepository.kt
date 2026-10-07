package com.zed.app.core.domain.repository

import kotlinx.coroutines.flow.Flow

// Плейлист для UI: id, имя, id треков в порядке добавления
data class PlaylistUi(
    val id: Int,
    val name: String,
    val trackIds: List<Long>
)

interface PlayerRepository {
    fun observeFavorites(): Flow<Set<Long>>
    suspend fun setFavorite(trackId: Long, favorite: Boolean)
    fun observePlaylistsWithTracks(): Flow<List<PlaylistUi>>
    suspend fun createPlaylist(name: String): Long
    suspend fun deletePlaylist(id: Int)
    suspend fun addToPlaylist(playlistId: Int, trackId: Long)
}
