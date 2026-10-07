package com.zed.app.feature.player

import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.zed.app.core.media.AudioEffectsManager
import com.zed.app.core.settings.SettingsRepository
import com.zed.app.core.settings.parseEqGains
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Фоновой плеер.
// Speed — кастомная команда; EQ + Reverb — AudioProcessor внутри нашего AudioSink.
// FX-настройки восстанавливаются из DataStore при старте сервиса:
// музыка с уведомления сразу играет с твоим басом/ревербом, даже без открытия UI.
@AndroidEntryPoint
class ZedPlaybackService : MediaSessionService() {

    companion object {
        const val ACTION_SPEED = "zed.set_speed"
    }

    @Inject lateinit var effectsManager: AudioEffectsManager
    @Inject lateinit var settingsRepository: SettingsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var player: ExoPlayer? = null
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        // Аудио-цепочка с нашим процессором эффектов (EQ + реверб Шрёдера)
        val audioSink = DefaultAudioSink.Builder()
            .setAudioProcessors(arrayOf(effectsManager.processor))
            .build()

        val exoPlayer = ExoPlayer.Builder(this)
            .setRenderersFactory(buildRenderersFactory(audioSink))
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        player = exoPlayer

        effectsManager.bindPlayer(exoPlayer)

        // Восстановление FX после (пере)запуска процесса
        scope.launch {
            val s = settingsRepository.settings.first()
            if (s.fxSpeed != 1f) {
                exoPlayer.playbackParameters = PlaybackParameters(s.fxSpeed, s.fxSpeed)
            }
            effectsManager.setReverbLevel(s.fxReverb)
            effectsManager.setEq(parseEqGains(s.fxEq).toFloatArray())
        }

        session = MediaSession.Builder(this, exoPlayer)
            .setCallback(SessionCallback())
            .build()
    }

    // Фабрика рендереров: стандартный набор, но аудио-рендерер заменён
    // на MediaCodecAudioRenderer с нашим sink'ом (публичный API media3 1.4.1)
    private fun buildRenderersFactory(audioSink: AudioSink): RenderersFactory {
        val service = this
        return RenderersFactory { handler, videoListener, audioListener, textOutput, metadataOutput ->
            val defaults = DefaultRenderersFactory(service)
                .createRenderers(handler, videoListener, audioListener, textOutput, metadataOutput)
            defaults.map { renderer ->
                if (renderer is MediaCodecAudioRenderer) {
                    MediaCodecAudioRenderer(
                        service,
                        MediaCodecSelector.DEFAULT,
                        true, // enableDecoderFallback
                        handler,
                        audioListener,
                        audioSink
                    )
                } else {
                    renderer
                }
            }.toTypedArray()
        }
    }

    private inner class SessionCallback : MediaSession.Callback {
        // Объявляем кастомную команду, иначе sendCustomCommand молча отклоняется
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                .buildUpon()
                .add(SessionCommand(ACTION_SPEED, Bundle.EMPTY))
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(commands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == ACTION_SPEED) {
                val speed = args.getFloat("value", 1f).coerceIn(0.5f, 1.5f)
                player?.playbackParameters = PlaybackParameters(speed, speed)
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        session

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        if (player?.playWhenReady != true) stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        scope.cancel()
        effectsManager.release()
        session?.release()
        session = null
        player?.release()
        player = null
        super.onDestroy()
    }
}
