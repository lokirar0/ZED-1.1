package com.zed.app.core.media

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.exoplayer.ExoPlayer
import javax.inject.Inject
import javax.inject.Singleton

// Менеджер эффектов ZED.
// Эквалайзер и реверб считаются ВНУТРИ ExoPlayer (ZedAudioProcessor) —
// никаких android.media.audiofx: OEM-HAL на части устройств молча игнорирует системные эффекты.
// Speed управляется отдельно через PlaybackParameters (работает, не трогаем).
@Singleton
class AudioEffectsManager @Inject constructor() {

    companion object {
        private const val TAG = "ZED_FX"
        private const val BANDS = 5
    }

    // Процессор встраивается в DefaultAudioSink сервисом
    val processor = ZedAudioProcessor()

    private var player: ExoPlayer? = null
    private var reverbPercent: Int = 0
    private var eqGains: FloatArray = FloatArray(BANDS) { 0.5f }

    private val handler = Handler(Looper.getMainLooper())
    private var sleepRunnable: Runnable? = null

    // Нужен только для таймера сна
    fun bindPlayer(exo: ExoPlayer) {
        player = exo
        Log.d(TAG, "bindPlayer ok")
    }

    // Reverb 0..100 → wet 0..1 в процессоре
    fun setReverbLevel(percent: Int) {
        reverbPercent = percent.coerceIn(0, 100)
        processor.setReverb(reverbPercent / 100f)
        Log.d(TAG, "setReverbLevel($reverbPercent%) → in-app Schroeder wet=${reverbPercent / 100f}")
    }

    // Эквалайзер: 5 полос, 0..1 каждая
    fun setEq(gains: FloatArray) {
        eqGains = FloatArray(BANDS) { i -> gains.getOrElse(i) { 0.5f }.coerceIn(0f, 1f) }
        processor.setEq(eqGains)
        Log.d(TAG, "setEq(${eqGains.joinToString { "%.2f".format(it) }})")
    }

    // RESET: реверб 0, эквалайзер ровно
    fun reset() {
        reverbPercent = 0
        eqGains = FloatArray(BANDS) { 0.5f }
        processor.setReverb(0f)
        processor.setEq(eqGains)
        Log.d(TAG, "reset()")
    }

    // Таймер сна: пауза через N минут
    fun startSleepTimer(minutes: Int) {
        sleepRunnable?.let { handler.removeCallbacks(it) }
        val r = Runnable {
            Log.d(TAG, "sleep timer fired → pause")
            runCatching { player?.pause() }
        }
        sleepRunnable = r
        handler.postDelayed(r, minutes * 60_000L)
        Log.d(TAG, "sleep timer set: $minutes min")
    }

    fun currentReverbPercent(): Int = reverbPercent
    fun currentEq(): List<Float> = eqGains.toList()

    fun release() {
        sleepRunnable?.let { handler.removeCallbacks(it) }
        sleepRunnable = null
        player = null
    }
}
