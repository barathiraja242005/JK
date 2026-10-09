package com.barathiraja.jk.ui.screens

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barathiraja.jk.data.Block
import com.barathiraja.jk.data.Workout
import com.barathiraja.jk.data.WorkoutSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

enum class Phase { READY, WORK, REST, DONE }

data class PlayerState(
    val phase: Phase = Phase.READY,
    val index: Int = 0,
    val remaining: Int = PlayerViewModel.READY_SEC,
    val phaseTotal: Int = PlayerViewModel.READY_SEC,
    val paused: Boolean = false,
    val elapsed: Int = 0,
    val calories: Float = 0f,
)

class PlayerViewModel(app: Application, val workout: Workout, private val weightKg: Float, private val voice: Boolean) :
    ViewModel(), TextToSpeech.OnInitListener {

    companion object { const val READY_SEC = 10 }

    /** Workout flattened into one block per round: [A, B, C, A, B, C, ...]. */
    val sequence: List<Block> = List(workout.rounds) { workout.blocks }.flatten()

    private val _state = MutableStateFlow(PlayerState())
    val state = _state.asStateFlow()

    private val tts: TextToSpeech? = if (voice) TextToSpeech(app, this) else null
    private val tone = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 70) }.getOrNull()
    private var ticker: Job? = null

    init { start() }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.getDefault()
            say("Get ready. First up, ${sequence.first().spoken()}")
        }
    }

    private fun Block.spoken() = if (isTimed) "${exercise.name}, $seconds seconds" else "${exercise.name}, $reps reps"

    private fun start() {
        ticker = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                val s = _state.value
                if (s.paused || s.phase == Phase.DONE) continue
                val met = if (s.phase == Phase.WORK) sequence[s.index].exercise.met else 2f
                val counting = s.phase != Phase.WORK || sequence[s.index].isTimed
                val next = s.copy(
                    elapsed = s.elapsed + 1,
                    calories = s.calories + met * weightKg / 3600f,
                    remaining = if (counting) s.remaining - 1 else s.remaining,
                )
                _state.value = next
                if (counting && next.remaining in 1..3) tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
                if (counting && next.remaining <= 0) advance()
            }
        }
    }

    fun togglePause() = _state.update { it.copy(paused = !it.paused) }

    fun addRest(sec: Int) = _state.update {
        if (it.phase == Phase.REST) it.copy(remaining = it.remaining + sec, phaseTotal = it.phaseTotal + sec) else it
    }

    /** Move forward: READY→WORK, WORK→REST (or DONE), REST→WORK. Also used for "Done"/"Skip". */
    fun advance() {
        val s = _state.value
        when (s.phase) {
            Phase.READY -> goWork(0)
            Phase.WORK -> {
                if (s.index == sequence.lastIndex) finish()
                else if (workout.restSec > 0) {
                    _state.update { it.copy(phase = Phase.REST, remaining = workout.restSec, phaseTotal = workout.restSec) }
                    say("Rest. Next, ${sequence[s.index + 1].spoken()}")
                } else goWork(s.index + 1)
            }
            Phase.REST -> goWork(s.index + 1)
            Phase.DONE -> Unit
        }
    }

    fun previous() {
        val s = _state.value
        when {
            s.phase == Phase.REST -> goWork(s.index)
            s.phase == Phase.WORK && s.index > 0 -> goWork(s.index - 1)
            s.phase == Phase.WORK -> goWork(0)
        }
    }

    private fun goWork(i: Int) {
        val b = sequence[i]
        _state.update { it.copy(phase = Phase.WORK, index = i, remaining = b.seconds, phaseTotal = b.seconds) }
        say(if (b.isTimed) "Go! ${b.exercise.name}" else "${b.exercise.name}. ${b.reps} reps. Tap done when finished.")
    }

    private fun finish() {
        _state.update { it.copy(phase = Phase.DONE) }
        tone?.startTone(ToneGenerator.TONE_PROP_ACK, 400)
        say("Workout complete. Great job!")
    }

    fun session(): WorkoutSession {
        val s = _state.value
        return WorkoutSession(
            workoutId = workout.id, title = workout.title, finishedAt = System.currentTimeMillis(),
            epochDay = LocalDate.now().toEpochDay(), durationSec = s.elapsed, calories = s.calories.toInt(),
        )
    }

    private fun say(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jk")
    }

    override fun onCleared() {
        ticker?.cancel()
        tts?.shutdown()
        tone?.release()
    }
}
