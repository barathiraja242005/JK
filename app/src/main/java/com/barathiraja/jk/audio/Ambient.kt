package com.barathiraja.jk.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.barathiraja.jk.data.Ambience
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Generates ambient soundscapes in real time (no audio files needed):
 * rain = softened white noise, ocean = brown noise with slow swells, drone = layered soft sines.
 */
class AmbientPlayer {
    private val rate = SAMPLE_RATE
    @Volatile var volume = 0.5f
    /** Stop flag of the current generator; each start gets its own so a quick restart can't revive an old thread. */
    private var running: AtomicBoolean? = null
    private var track: AudioTrack? = null

    fun start(kind: Ambience) {
        stop()
        if (kind == Ambience.SILENCE) return
        val minBuf = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val t = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(rate).setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(minBuf * BUFFER_MULTIPLIER)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        val alive = AtomicBoolean(true)
        track = t
        running = alive
        t.play()
        // The thread owns its track (a local, never the field) and releases it when done, so stop() never blocks.
        Thread {
            val buf = ShortArray(CHUNK_SAMPLES)
            var n = 0L
            var brown = 0.0
            var lp = 0.0
            val rnd = Random(42)
            while (alive.get()) {
                for (i in buf.indices) {
                    val time = n.toDouble() / rate
                    val white = rnd.nextDouble() * 2 - 1
                    val s = when (kind) {
                        Ambience.RAIN -> {
                            lp += 0.35 * (white - lp) // gentle low-pass
                            lp * 0.6 + (if (rnd.nextDouble() < 0.0008) white * 0.4 else 0.0) // occasional droplets
                        }
                        Ambience.OCEAN -> {
                            brown = (brown + white * 0.02).coerceIn(-1.0, 1.0) * 0.998
                            val swell = 0.35 + 0.65 * (0.5 + 0.5 * sin(2 * PI * time / 9.0))
                            brown * 3.0 * swell
                        }
                        Ambience.DRONE -> {
                            val trem = 0.8 + 0.2 * sin(2 * PI * time / 6.0)
                            (sin(2 * PI * 110.0 * time) * 0.45 + sin(2 * PI * 164.8 * time) * 0.3 +
                                sin(2 * PI * 220.0 * time) * 0.15) * trem * 0.5
                        }
                        Ambience.SILENCE -> 0.0
                    }
                    // Fade in over the first 3 seconds.
                    val fade = (time / FADE_IN_SEC).coerceAtMost(1.0)
                    buf[i] = (s.coerceIn(-1.0, 1.0) * volume * fade * Short.MAX_VALUE * 0.6).toInt().toShort()
                    n++
                }
                t.write(buf, 0, buf.size)
            }
            runCatching { t.stop() }
            t.release()
        }.apply { isDaemon = true; start() }
    }

    fun pause() = track?.pause()
    fun resume() = track?.play()

    fun stop() {
        running?.set(false)
        running = null
        // Pause and drop queued audio so a write blocked on a full (or paused) buffer returns and the thread can exit.
        track?.let { t -> runCatching { t.pause(); t.flush() } }
        track = null
    }

    private companion object {
        const val SAMPLE_RATE = 22050
        const val BUFFER_MULTIPLIER = 4
        const val CHUNK_SAMPLES = 1024
        const val FADE_IN_SEC = 3.0
    }
}
