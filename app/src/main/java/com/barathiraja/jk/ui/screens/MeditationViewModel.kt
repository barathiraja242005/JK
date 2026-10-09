package com.barathiraja.jk.ui.screens

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barathiraja.jk.audio.AmbientPlayer
import com.barathiraja.jk.data.Meditation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

data class MindState(val elapsed: Int = 0, val paused: Boolean = false, val caption: String = "", val done: Boolean = false)

/** Plays one meditation. [onEnd] gets the seconds sat, once, when the session is closed for good (not on rotation). */
class MeditationViewModel(app: Application, val m: Meditation, private val onEnd: (Int) -> Unit) : ViewModel(), TextToSpeech.OnInitListener {
    val state = MutableStateFlow(MindState())
    private val ambient = AmbientPlayer()
    private val tts = TextToSpeech(app, this)
    private val bell = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 60) }.getOrNull()
    private var ttsReady = false
    val total = m.minutes * 60

    init {
        ambient.start(m.ambience)
        bell?.startTone(ToneGenerator.TONE_SUP_CONFIRM, 300)
        viewModelScope.launch {
            var spoken = -1
            while (isActive) {
                val s = state.value
                if (!s.paused && !s.done) {
                    val line = m.script.lastOrNull { it.first <= s.elapsed }
                    val idx = m.script.indexOf(line)
                    if (line != null && idx > spoken && ttsReady) {
                        spoken = idx
                        tts.speak(line.second, TextToSpeech.QUEUE_ADD, null, "m$idx")
                        state.update { it.copy(caption = line.second) }
                    }
                    if (s.elapsed >= total) finish() else state.update { it.copy(elapsed = it.elapsed + 1) }
                }
                delay(1000)
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.getDefault()
            tts.setSpeechRate(0.85f)
            ttsReady = true
        }
    }

    fun setVolume(v: Float) { ambient.volume = v }

    fun togglePause() {
        val p = !state.value.paused
        state.update { it.copy(paused = p) }
        if (p) { ambient.pause(); tts.stop() } else ambient.resume()
    }

    private fun finish() {
        state.update { it.copy(done = true, caption = "Session complete. Take this calm with you.") }
        bell?.startTone(ToneGenerator.TONE_SUP_CONFIRM, 600)
        ambient.stop()
    }

    override fun onCleared() {
        onEnd(state.value.elapsed)
        ambient.stop()
        tts.shutdown()
        bell?.release()
    }
}
