package com.zed.app.feature.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.zed.app.core.domain.model.AudioTrack
import com.zed.app.core.domain.repository.PlayerRepository
import com.zed.app.core.domain.repository.PlaylistUi
import com.zed.app.core.media.AudioEffectsManager
import com.zed.app.core.media.LocalAudioScanner
import com.zed.app.core.settings.SettingsRepository
import com.zed.app.core.settings.parseEqGains
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerUiState(
    val tracks: List<AudioTrack> = emptyList(),
    val loaded: Boolean = false,
    val current: AudioTrack? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queue: List<AudioTrack> = emptyList(),
    val queueIndex: Int = 0,
    val favoriteIds: Set<Long> = emptySet(),
    val playlists: List<PlaylistUi> = emptyList(),
    val shuffle: Boolean = false,
    val repeatMode: Int = 0,
    val speed: Float = 1f,
    val reverb: Int = 0,
    val eqGains: List<Float> = List(5) { 0.5f }
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val scanner: LocalAudioScanner,
    private val playerRepository: PlayerRepository,
    private val settingsRepository: SettingsRepository,
    private val effectsManager: AudioEffectsManager,
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private var tickerJob: Job? = null

    // Трек из поиска (player_screen?play=id)
    private var pendingPlayId: Long = savedStateHandle.get<Long>("play") ?: -1L

    // Отложенные запросы: библиотека не готова / контроллер не подключён
    private var deferredPlayId: Long = -1L
    private var pendingPlayIndex: Int = -1

    init {
        viewModelScope.launch { refresh() }

        viewModelScope.launch {
            val s = settingsRepository.settings.first()
            effectsManager.setReverbLevel(s.fxReverb)
            effectsManager.setEq(parseEqGains(s.fxEq).toFloatArray())
            _state.update {
                it.copy(speed = s.fxSpeed, reverb = s.fxReverb, eqGains = parseEqGains(s.fxEq))
            }
        }

        viewModelScope.launch {
            combine(
                playerRepository.observeFavorites(),
                playerRepository.observePlaylistsWithTracks()
            ) { favs, pls -> Pair(favs, pls) }.collect { (favs, pls) ->
                _state.update { it.copy(favoriteIds = favs, playlists = pls) }
            }
        }

        val token = SessionToken(context, ComponentName(context, ZedPlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            // ВАЖНО: сначала присваиваем controller, ПОТОМ любая работа с ним.
            // Раньше playTrackById вызывался до присвоения поля и молча выходил
            // на `controller ?: return` — запрос из поиска терялся навсегда.
            val c = runCatching { future.get() }.getOrNull()
            if (c != null) {
                controller = c
                c.addListener(playerListener)
                syncFrom(c)
                viewModelScope.launch {
                    val s = settingsRepository.settings.first()
                    c.shuffleModeEnabled = s.shuffleEnabled
                    c.repeatMode = s.repeatMode.toPlayerRepeat()
                    _state.update { it.copy(shuffle = s.shuffleEnabled, repeatMode = s.repeatMode) }
                }
                if (_state.value.queue.isNotEmpty() && c.mediaItemCount == 0) {
                    pushQueueToPlayer()
                }
                // Трек из поиска: библиотека может быть ещё не готова —
                // playTrackById сам отложится (deferredPlayId) и доиграет в refresh()
                if (pendingPlayId != -1L) {
                    val id = pendingPlayId
                    pendingPlayId = -1L
                    playTrackById(id)
                }
                // Страховка: индекс, отложенный до подключения контроллера
                if (pendingPlayIndex >= 0) {
                    val idx = pendingPlayIndex
                    pendingPlayIndex = -1
                    pushQueueToPlayer(resetPosition = true)
                    playAt(idx)
                }
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun audioLevel(): Float = effectsManager.processor.level
    fun audioBass(): Float = effectsManager.processor.bass

    // --- Очередь = вся библиотека ---

    fun refresh() {
        viewModelScope.launch {
            val list = scanner.scan()
            _state.update { st ->
                val sameSet = st.queue.map { it.id }.toSet() == list.map { it.id }.toSet()
                st.copy(
                    tracks = list,
                    loaded = true,
                    queue = if (st.queue.isEmpty() || !sameSet) list else st.queue
                )
            }
            val st = _state.value
            if (controller != null && !st.isPlaying && st.current == null && st.queue.isNotEmpty()) {
                pushQueueToPlayer()
            }
            // Отложенный запрос из поиска: библиотека готова — играем
            if (deferredPlayId != -1L) {
                val id = deferredPlayId
                deferredPlayId = -1L
                playTrackById(id)
            }
        }
    }

    // Воспроизвести трек по id (поиск, шорткаты).
    // Не готова библиотека → откладываем id. Нет контроллера → откладываем индекс.
    // Оба отложенных запроса выполняются автоматически, потеря невозможна.
    fun playTrackById(id: Long) {
        val st = _state.value
        if (st.tracks.isEmpty()) {
            deferredPlayId = id
            return
        }
        val queue = if (st.queue.isNotEmpty()) st.queue else st.tracks
        val idx = queue.indexOfFirst { it.id == id }
        if (idx < 0) return
        _state.update { it.copy(queue = queue) }
        if (controller == null) {
            pendingPlayIndex = idx
            return
        }
        pushQueueToPlayer(resetPosition = true)
        playAt(idx)
    }

    fun playTrack(index: Int) {
        _state.update { it.copy(queue = it.tracks) }
        pushQueueToPlayer(resetPosition = true)
        playAt(index)
    }

    fun playQueue(list: List<AudioTrack>, startIndex: Int) {
        if (list.isEmpty()) return
        _state.update { it.copy(queue = list) }
        pushQueueToPlayer(resetPosition = true)
        playAt(startIndex)
    }

    fun playAt(index: Int) {
        val c = controller ?: return
        c.seekToDefaultPosition(index)
        c.play()
        _state.update { it.copy(queueIndex = index) }
    }

    // --- Управление очередью (позиция воспроизведения сохраняется) ---

    fun moveBySteps(trackId: Long, steps: Int) {
        val st = _state.value
        val from = st.queue.indexOfFirst { it.id == trackId }
        if (from < 0) return
        val to = (from + steps).coerceIn(0, st.queue.size - 1)
        if (to == from) return
        reorder(from, to)
    }

    fun playNextById(trackId: Long) {
        val st = _state.value
        val from = st.queue.indexOfFirst { it.id == trackId }
        if (from < 0) return
        val curId = st.queue.getOrNull(st.queueIndex)?.id
        val q = st.queue.toMutableList()
        val item = q.removeAt(from)
        val curNow = q.indexOfFirst { it.id == curId }
        q.add(curNow + 1, item)
        applyQueue(q, curId)
    }

    private fun reorder(from: Int, to: Int) {
        val st = _state.value
        val curId = st.queue.getOrNull(st.queueIndex)?.id
        val q = st.queue.toMutableList()
        val item = q.removeAt(from)
        q.add(to, item)
        applyQueue(q, curId)
    }

    private fun applyQueue(q: List<AudioTrack>, curId: Long?) {
        val newIdx = curId?.let { id -> q.indexOfFirst { it.id == id } } ?: 0
        _state.update { it.copy(queue = q, queueIndex = newIdx.coerceAtLeast(0)) }
        pushQueueToPlayer(resetPosition = false)
    }

    // Заливка очереди в плеер. resetPosition=true — новый список с 0:00
    private fun pushQueueToPlayer(resetPosition: Boolean = false) {
        val c = controller ?: return
        val st = _state.value
        if (st.queue.isEmpty()) return
        c.setMediaItems(
            st.queue.map { it.toMediaItem() },
            st.queueIndex.coerceIn(0, st.queue.size - 1),
            if (resetPosition) 0L else c.currentPosition.coerceAtLeast(0L)
        )
        c.prepare()
    }

    // --- Воспроизведение ---

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) {
            c.pause()
        } else {
            if (_state.value.current == null && _state.value.queue.isNotEmpty()) {
                if (c.mediaItemCount == 0) pushQueueToPlayer(resetPosition = true)
                c.seekToDefaultPosition(0)
                c.play()
                _state.update { it.copy(queueIndex = 0) }
            } else {
                c.play()
            }
        }
    }

    fun next() = controller?.seekToNextMediaItem()
    fun previous() = controller?.seekToPreviousMediaItem()

    fun seekTo(ms: Long) {
        controller?.seekTo(ms)
        _state.update { it.copy(positionMs = ms) }
    }

    fun toggleShuffle() {
        val value = !_state.value.shuffle
        controller?.shuffleModeEnabled = value
        _state.update { it.copy(shuffle = value) }
        viewModelScope.launch { settingsRepository.setShuffleEnabled(value) }
    }

    fun cycleRepeat() {
        val next = (_state.value.repeatMode + 1) % 3
        controller?.repeatMode = next.toPlayerRepeat()
        _state.update { it.copy(repeatMode = next) }
        viewModelScope.launch { settingsRepository.setRepeatMode(next) }
    }

    // --- Лайки и плейлисты ---

    fun toggleFavorite() {
        val id = _state.value.current?.id ?: return
        val add = id !in _state.value.favoriteIds
        viewModelScope.launch { playerRepository.setFavorite(id, add) }
    }

    fun playFavorites() {
        val favs = _state.value.favoriteIds
        val list = _state.value.tracks.filter { it.id in favs }
        if (list.isNotEmpty()) playQueue(list, 0)
    }

    fun createPlaylist(name: String) { viewModelScope.launch { playerRepository.createPlaylist(name) } }
    fun deletePlaylist(id: Int) { viewModelScope.launch { playerRepository.deletePlaylist(id) } }

    fun addToPlaylist(playlistId: Int, trackId: Long) {
        viewModelScope.launch { playerRepository.addToPlaylist(playlistId, trackId) }
    }

    fun playPlaylist(playlist: PlaylistUi) {
        val byId = _state.value.tracks.associateBy { it.id }
        val list = playlist.trackIds.mapNotNull { byId[it] }
        if (list.isNotEmpty()) playQueue(list, 0)
    }

    // --- Эффекты ---

    fun setSpeed(speed: Float) {
        val v = speed.coerceIn(0.5f, 1.5f)
        _state.update { it.copy(speed = v) }
        sendSpeed(v)
        persistFx()
    }

    fun setReverb(percent: Int) {
        val p = percent.coerceIn(0, 100)
        _state.update { it.copy(reverb = p) }
        effectsManager.setReverbLevel(p)
        persistFx()
    }

    fun setEqBand(index: Int, gain: Float) {
        val gains = _state.value.eqGains.toMutableList()
        if (index !in gains.indices) return
        gains[index] = gain.coerceIn(0f, 1f)
        _state.update { it.copy(eqGains = gains) }
        effectsManager.setEq(gains.toFloatArray())
        persistFx()
    }

    fun applyEqPreset(preset: List<Float>) {
        _state.update { it.copy(eqGains = preset) }
        effectsManager.setEq(preset.toFloatArray())
        persistFx()
    }

    fun resetFx() {
        _state.update { it.copy(speed = 1f, reverb = 0, eqGains = List(5) { 0.5f }) }
        effectsManager.reset()
        sendSpeed(1f)
        persistFx()
    }

    private fun persistFx() {
        val s = _state.value
        viewModelScope.launch {
            settingsRepository.setFx(
                speed = s.speed,
                reverb = s.reverb,
                eq = s.eqGains.joinToString(",") { String.format(Locale.US, "%.3f", it) }
            )
        }
    }

    private fun sendSpeed(speed: Float) {
        val c = controller ?: return
        c.sendCustomCommand(
            SessionCommand(ZedPlaybackService.ACTION_SPEED, Bundle.EMPTY),
            Bundle().apply { putFloat("value", speed) }
        )
    }

    // --- Синхронизация ---

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
            startStopTicker(isPlaying)
        }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            syncFrom(controller ?: return)
        }
        override fun onPlaybackStateChanged(playbackState: Int) {
            syncFrom(controller ?: return)
        }
        override fun onShuffleModeEnabledChanged(enabled: Boolean) {
            _state.update { it.copy(shuffle = enabled) }
        }
        override fun onRepeatModeChanged(repeatMode: Int) {
            _state.update { it.copy(repeatMode = repeatMode.toAppRepeat()) }
        }
    }

    private fun syncFrom(c: Player) {
        val mediaId = c.currentMediaItem?.mediaId?.toLongOrNull()
        val current = _state.value.queue.firstOrNull { it.id == mediaId }
            ?: _state.value.tracks.firstOrNull { it.id == mediaId }
        _state.update {
            it.copy(
                current = current ?: it.current,
                queueIndex = c.currentMediaItemIndex.coerceAtLeast(0),
                isPlaying = c.isPlaying,
                positionMs = c.currentPosition.coerceAtLeast(0L),
                durationMs = c.duration.coerceAtLeast(0L)
            )
        }
    }

    // Тикер позиции: 2 раза в секунду и ТОЛЬКО во время воспроизведения
    private fun startStopTicker(playing: Boolean) {
        if (playing && tickerJob == null) {
            tickerJob = viewModelScope.launch {
                while (isActive) {
                    controller?.let { c ->
                        _state.update {
                            it.copy(
                                positionMs = c.currentPosition.coerceAtLeast(0L),
                                durationMs = c.duration.coerceAtLeast(0L)
                            )
                        }
                    }
                    delay(500)
                }
            }
        } else if (!playing) {
            tickerJob?.cancel()
            tickerJob = null
        }
    }

    override fun onCleared() {
        controller?.removeListener(playerListener)
        controller?.release()
        controller = null
        super.onCleared()
    }
}

private fun Int.toPlayerRepeat(): Int = when (this) {
    1 -> Player.REPEAT_MODE_ONE
    2 -> Player.REPEAT_MODE_ALL
    else -> Player.REPEAT_MODE_OFF
}

private fun Int.toAppRepeat(): Int = when (this) {
    Player.REPEAT_MODE_ONE -> 1
    Player.REPEAT_MODE_ALL -> 2
    else -> 0
}

private fun AudioTrack.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(id.toString())
    .setUri(Uri.parse(contentUri))
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(if (isUnknownTitle) null else title)
            .setArtist(artist.ifBlank { null })
            .setArtworkUri(artUri?.let(Uri::parse))
            .build()
    )
    .build()
