package com.barathiraja.jk.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.barathiraja.jk.data.Ambience
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Generates ambient soundscapes in real time (no audio files needed):
 * rain = softened white noise, ocean = brown noise with slow swells, drone = layered soft sines.
 */
class AmbientPlayer {
    private val rate = 22050
    @Volatile private var running = false
    @Volatile var volume = 0.5f
    private var thread: Thread? = null
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
            .setBufferSizeInBytes(minBuf * 4)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        track = t
        running = true
        t.play()
        thread = Thread {
            val buf = ShortArray(1024)
            var n = 0L
            var brown = 0.0
            var lp = 0.0
            val rnd = Random(42)
            while (running) {
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
                    val fade = (time / 3.0).coerceAtMost(1.0)
                    buf[i] = (s.coerceIn(-1.0, 1.0) * volume * fade * Short.MAX_VALUE * 0.6).toInt().toShort()
                    n++
                }
                track?.write(buf, 0, buf.size)
            }
        }.apply { isDaemon = true; start() }
    }

    fun pause() = track?.pause()
    fun resume() = track?.play()

    fun stop() {
        running = false
        thread?.join(300)
        thread = null
        track?.run { runCatching { stop() }; release() }
        track = null
    }
}
