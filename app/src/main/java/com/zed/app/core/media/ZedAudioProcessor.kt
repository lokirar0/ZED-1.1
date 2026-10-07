package com.zed.app.core.media

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// Внутриприложенческая обработка звука (PCM 16-bit) в цепочке ExoPlayer:
// 5-полосный эквалайзер + реверб Шрёдера + ИЗМЕРЕНИЕ УРОВНЯ/БАСА для анимации.
// Уровень считается в том же цикле, что и обработка — почти бесплатно по CPU.
class ZedAudioProcessor : BaseAudioProcessor() {

    companion object {
        private val BAND_FREQS = floatArrayOf(60f, 230f, 910f, 3600f, 14000f)
        private const val BASS_HZ = 150f // граница «баса» для однополюсного ФНЧ
    }

    @Volatile private var eqGains: FloatArray = FloatArray(5) { 0.5f }
    @Volatile private var reverbWet: Float = 0f

    // Аудиореактивность: 0..1, атака быстрая, спад медленный (attack/decay)
    @Volatile var level: Float = 0f; private set  // общая громкость (RMS)
    @Volatile var bass: Float = 0f; private set   // энергия низких частот

    private var sampleRate: Int = 44100
    private var channelCount: Int = 2
    private var bassAlpha: Float = 0.02f
    private var lpState: Float = 0f               // состояние ФНЧ для баса
    private var chains: Array<EqChain> = emptyArray()
    private var reverbs: Array<Schroeder> = emptyArray()

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        sampleRate = inputAudioFormat.sampleRate
        channelCount = inputAudioFormat.channelCount
        rebuild()
        return inputAudioFormat
    }

    private fun rebuild() {
        chains = Array(channelCount) { EqChain(sampleRate, BAND_FREQS, eqGains) }
        reverbs = Array(channelCount) { Schroeder(sampleRate) }
        bassAlpha = (2.0 * Math.PI * BASS_HZ / sampleRate).toFloat().coerceIn(0.001f, 0.5f)
        lpState = 0f
    }

    fun setEq(gains: FloatArray) {
        eqGains = FloatArray(5) { i -> gains.getOrElse(i) { 0.5f }.coerceIn(0f, 1f) }
        chains.forEach { it.setGains(eqGains) }
    }

    fun setReverb(wet: Float) {
        reverbWet = wet.coerceIn(0f, 1f)
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return
        val out = replaceOutputBuffer(remaining)
        val wet = reverbWet
        var sampleIndex = 0
        var sumSq = 0f
        var bassSum = 0f
        while (inputBuffer.hasRemaining()) {
            val ch = sampleIndex % channelCount
            var s = inputBuffer.short.toInt() / 32768f
            if (ch < chains.size) s = chains[ch].process(s)
            if (wet > 0f && ch < reverbs.size) {
                s += reverbs[ch].process(s) * wet
            }
            // Измерение: общий RMS + RMS после ФНЧ (бас). Считаем по всем каналам
            sumSq += s * s
            lpState += bassAlpha * (s - lpState)
            bassSum += lpState * lpState
            out.putShort((s.coerceIn(-1f, 1f) * 32767f).toInt().toShort())
            sampleIndex++
        }
        out.flip()

        // Уровень: атака мгновенная, спад плавный (чтобы точки «дышали» между ударами)
        val n = sampleIndex.coerceAtLeast(1)
        val rms = sqrt(sumSq / n)
        val bassRms = sqrt(bassSum / n)
        val target = (rms * 2.5f).coerceIn(0f, 1f)
        val targetBass = (bassRms * 4.0f).coerceIn(0f, 1f)
        level = if (target > level) target else level * 0.90f + target * 0.10f
        bass = if (targetBass > bass) targetBass else bass * 0.88f + targetBass * 0.12f
    }

    override fun onFlush() { rebuild() }
    override fun onReset() { rebuild() }
    override fun onQueueEndOfStream() { /* хвост реверба не догоняем */ }
}

// Цепочка из 5 пик-фильтров (RBJ biquad), −12..+12 дБ
private class EqChain(sr: Int, freqs: FloatArray, gains: FloatArray) {
    private val filters: Array<Biquad> =
        Array(freqs.size) { i -> Biquad(freqs[i], sr, gainDb(gains.getOrElse(i) { 0.5f })) }

    fun setGains(gains: FloatArray) {
        filters.forEachIndexed { i, f -> f.update(gainDb(gains.getOrElse(i) { 0.5f })) }
    }

    fun process(x: Float): Float {
        var v = x
        for (f in filters) v = f.process(v)
        return v
    }

    private fun gainDb(g: Float): Float = (g.coerceIn(0f, 1f) - 0.5f) * 24f
}

private class Biquad(private val freq: Float, private val sr: Int, gainDb: Float, private val q: Float = 1.0f) {
    private var b0 = 1f; private var b1 = 0f; private var b2 = 0f
    private var a1 = 0f; private var a2 = 0f
    private var x1 = 0f; private var x2 = 0f
    private var y1 = 0f; private var y2 = 0f

    init { update(gainDb) }

    fun update(gainDb: Float) {
        val a = Math.pow(10.0, gainDb / 40.0).toFloat()
        val w0 = (2.0 * Math.PI * freq / sr).toFloat()
        val cosW = cos(w0)
        val sinW = sin(w0)
        val alpha = sinW / (2 * q)
        val a0 = 1 + alpha / a
        b0 = (1 + alpha * a) / a0
        b1 = (-2 * cosW) / a0
        b2 = (1 - alpha * a) / a0
        a1 = (-2 * cosW) / a0
        a2 = (1 - alpha / a) / a0
    }

    fun process(x: Float): Float {
        val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        x2 = x1; x1 = x
        y2 = y1; y1 = y
        return y
    }
}

// Реверб Шрёдера: 4 гребёнки + 2 allpass на канал
private class Schroeder(sr: Int) {
    private val combDelays: IntArray = intArrayOf(1557, 1617, 1491, 1422)
        .map { (it.toLong() * sr / 44100L).toInt().coerceAtLeast(1) }.toIntArray()
    private val combBufs: Array<FloatArray> = Array(combDelays.size) { FloatArray(combDelays[it]) }
    private val combIdx = IntArray(combDelays.size)
    private val combFeedback = 0.84f

    private val apDelays: IntArray = intArrayOf(225, 556)
        .map { (it.toLong() * sr / 44100L).toInt().coerceAtLeast(1) }.toIntArray()
    private val apX: Array<FloatArray> = Array(apDelays.size) { FloatArray(apDelays[it]) }
    private val apY: Array<FloatArray> = Array(apDelays.size) { FloatArray(apDelays[it]) }
    private val apIdx = IntArray(apDelays.size)
    private val apG = 0.5f

    fun process(x: Float): Float {
        var sum = 0f
        for (c in combBufs.indices) {
            val d = combDelays[c]
            val buf = combBufs[c]
            var i = combIdx[c]
            val delayed = buf[i]
            buf[i] = x + combFeedback * delayed
            combIdx[c] = (i + 1) % d
            sum += delayed
        }
        var v = sum / combBufs.size * 0.5f
        for (a in apX.indices) {
            val d = apDelays[a]
            var i = apIdx[a]
            val xd = apX[a][i]
            val yd = apY[a][i]
            val y = -apG * v + xd + apG * yd
            apX[a][i] = v
            apY[a][i] = y
            apIdx[a] = (i + 1) % d
            v = y
        }
        return v
    }
}
