package com.zed.app.core.data.repository

import com.zed.app.core.data.local.FavoriteDao
import com.zed.app.core.data.local.FavoriteTrackEntity
import com.zed.app.core.data.local.PlaylistDao
import com.zed.app.core.data.local.PlaylistEntity
import com.zed.app.core.data.local.PlaylistTrackEntity
import com.zed.app.core.domain.repository.PlayerRepository
import com.zed.app.core.domain.repository.PlaylistUi
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

@Singleton
class PlayerRepositoryImpl @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val playlistDao: PlaylistDao
) : PlayerRepository {

    override fun observeFavorites(): Flow<Set<Long>> =
        favoriteDao.observe().map { list -> list.map { it.trackId }.toSet() }

    override suspend fun setFavorite(trackId: Long, favorite: Boolean) {
        if (favorite) favoriteDao.insert(FavoriteTrackEntity(trackId, System.currentTimeMillis()))
        else favoriteDao.delete(trackId)
    }

    override fun observePlaylistsWithTracks(): Flow<List<PlaylistUi>> =
        combine(playlistDao.observePlaylists(), playlistDao.observeTracks()) { playlists, rows ->
            playlists.map { p ->
                PlaylistUi(
                    id = p.id,
                    name = p.name,
                    trackIds = rows.filter { it.playlistId == p.id }.map { it.trackId }
                )
            }
        }

    override suspend fun createPlaylist(name: String): Long =
        playlistDao.insertPlaylist(PlaylistEntity(name = name))

    override suspend fun deletePlaylist(id: Int) = playlistDao.deletePlaylist(id)

    override suspend fun addToPlaylist(playlistId: Int, trackId: Long) =
        playlistDao.addTrack(PlaylistTrackEntity(playlistId = playlistId, trackId = trackId))
}
