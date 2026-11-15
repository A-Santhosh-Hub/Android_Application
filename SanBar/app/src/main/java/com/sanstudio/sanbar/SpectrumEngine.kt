package com.sanstudio.sanbar

import android.media.audiofx.Visualizer
import android.os.SystemClock
import android.util.Log
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

/**
 * Real audio spectrum from Android's Visualizer on the output mix (session 0).
 * It reads the sound that goes to the speaker/headphones, never the microphone,
 * but Android still requires the RECORD_AUDIO permission for it.
 *
 * [onFrame] gets 24 bar heights (0..100) about 15 times a second, and `live = false`
 * when the output stays silent (some phones send zeros for offloaded music); the widget
 * then falls back to the animated bars.
 */
class SpectrumEngine(private val onFrame: (levels: IntArray?, live: Boolean) -> Unit) {

    companion object {
        const val BANDS = 24
        private const val FRAME_MS = 66L
        private const val SILENT_FRAMES_BEFORE_FALLBACK = 40
    }

    private var viz: Visualizer? = null
    private val levels = FloatArray(BANDS)
    private var silentFrames = 0
    private var live = false
    private var lastPush = 0L

    fun start(): Boolean = try {
        val v = Visualizer(0)
        v.enabled = false
        val range = Visualizer.getCaptureSizeRange()
        v.captureSize = 1024.coerceIn(range[0], range[1])
        v.setDataCaptureListener(
            object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray?, rate: Int) {}
                override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, rate: Int) {
                    if (fft != null) process(fft)
                }
            },
            Visualizer.getMaxCaptureRate().coerceAtMost(20_000),
            false,
            true,
        )
        v.enabled = true
        viz = v
        Log.i("SanBar", "Spectrum started (capture ${v.captureSize})")
        true
    } catch (t: Throwable) {
        Log.w("SanBar", "Visualizer unavailable: $t")
        stop()
        false
    }

    fun stop() {
        try {
            viz?.enabled = false
        } catch (_: Throwable) {
        }
        try {
            viz?.release()
        } catch (_: Throwable) {
        }
        viz = null
    }

    private fun process(fft: ByteArray) {
        val bins = fft.size / 2
        val maxBin = bins - 1
        var energy = 0f
        for (b in 0 until BANDS) {
            val lo = binFor(b, maxBin)
            val hi = max(lo + 1, binFor(b + 1, maxBin))
            var peak = 0f
            for (k in lo until hi) {
                val m = hypot(fft[2 * k].toFloat(), fft[2 * k + 1].toFloat())
                if (m > peak) peak = m
            }
            energy += peak
            val db = if (peak > 0f) 20f * log10(peak) else 0f
            val target = ((db - 4f) / 36f * 100f).coerceIn(0f, 100f)
            // Jump up instantly, fall smoothly.
            levels[b] = max(target, levels[b] - 9f)
        }

        silentFrames = if (energy < 1f) silentFrames + 1 else 0
        val nowLive = silentFrames < SILENT_FRAMES_BEFORE_FALLBACK
        if (nowLive != live) {
            live = nowLive
            onFrame(null, live)
        }
        val now = SystemClock.uptimeMillis()
        if (live && now - lastPush >= FRAME_MS) {
            lastPush = now
            onFrame(IntArray(BANDS) { levels[it].toInt() }, true)
        }
    }

    /** Log-spaced FFT bins, so bass, mids and highs each get a fair share of bars. */
    private fun binFor(band: Int, maxBin: Int): Int =
        maxBin.toFloat().pow(band.toFloat() / BANDS).toInt().coerceIn(1, maxBin)
}
