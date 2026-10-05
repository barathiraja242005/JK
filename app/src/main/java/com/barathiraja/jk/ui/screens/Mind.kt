package com.barathiraja.jk.ui.screens

import com.barathiraja.jk.ui.components.formatDuration
import com.barathiraja.jk.ui.components.BackScreen
import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import com.barathiraja.jk.audio.AmbientPlayer
import com.barathiraja.jk.data.Meditation
import com.barathiraja.jk.data.Meditations
import com.barathiraja.jk.ui.JkViewModel
import com.barathiraja.jk.ui.Routes
import com.barathiraja.jk.ui.components.JkCard
import com.barathiraja.jk.ui.components.SectionTitle
import com.barathiraja.jk.ui.theme.Calm
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun MeditateScreen(vm: JkViewModel, nav: NavHostController) {
    val history by vm.mindSessions.collectAsStateWithLifecycle()
    BackScreen("Meditate", onBack = { nav.popBackStack() }) {
        item {
            Text("Guided sessions with generated calming soundscapes. Use headphones for the best experience.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(Meditations.all, key = { it.id }) { m ->
            JkCard(Modifier.fillMaxWidth(), onClick = { nav.navigate(Routes.meditation(m.id)) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Outlined.SelfImprovement, Calm)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(m.title, style = MaterialTheme.typography.titleMedium)
                        Text(m.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("${m.minutes}m", style = MaterialTheme.typography.titleSmall)
                }
            }
        }
        if (history.isNotEmpty()) {
            item { SectionTitle("Mindful minutes: ${history.sumOf { it.durationSec } / 60}") }
        }
    }
}

data class MindState(val elapsed: Int = 0, val paused: Boolean = false, val caption: String = "", val done: Boolean = false)

class MeditationViewModel(app: Application, val m: Meditation) : ViewModel(), TextToSpeech.OnInitListener {
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
        ambient.stop()
        tts.shutdown()
        bell?.release()
    }
}

@Composable
fun MeditationPlayerScreen(id: String, vm: JkViewModel, nav: NavHostController) {
    val m = Meditations.byId(id) ?: return
    val app = LocalContext.current.applicationContext as Application
    val mvm: MeditationViewModel = viewModel(key = "med-$id", factory = viewModelFactory { initializer { MeditationViewModel(app, m) } })
    val s by mvm.state.collectAsStateWithLifecycle()
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        0.85f, 1.05f, infiniteRepeatable(tween(5000), RepeatMode.Reverse), label = "p",
    )
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false; vm.saveMind(m.title, mvm.state.value.elapsed) }
    }
    var volume by remember { mutableFloatStateOf(0.5f) }

    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF1B1446), Color(0xFF0B0B1A)))),
    ) {
        Column(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Filled.Close, "Close", tint = Color.White) }
                Text(m.title, Modifier.weight(1f), color = Color.White, style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center)
                Spacer(Modifier.width(48.dp))
            }
            Spacer(Modifier.weight(1f))
            Box(contentAlignment = Alignment.Center) {
                Box(Modifier.size(260.dp).scale(if (s.paused || s.done) 0.9f else pulse).background(Calm.copy(alpha = 0.18f), CircleShape))
                Box(Modifier.size(190.dp).scale(if (s.paused || s.done) 0.9f else pulse).background(Calm.copy(alpha = 0.32f), CircleShape))
                Text(formatDuration((mvm.total - s.elapsed).coerceAtLeast(0).toLong()), color = Color.White,
                    style = MaterialTheme.typography.displaySmall)
            }
            Spacer(Modifier.height(32.dp))
            Text(s.caption, color = Color.White.copy(alpha = 0.9f), textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium, modifier = Modifier.height(96.dp))
            Spacer(Modifier.weight(1f))
            Text("${m.ambience.label} volume", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
            Slider(volume, { volume = it; mvm.setVolume(it) })
            Spacer(Modifier.height(8.dp))
            if (!s.done) {
                FilledIconButton(onClick = mvm::togglePause, modifier = Modifier.size(72.dp)) {
                    Icon(if (s.paused) Icons.Filled.PlayArrow else Icons.Filled.Pause, if (s.paused) "Resume" else "Pause")
                }
            } else {
                androidx.compose.material3.Button(onClick = { nav.popBackStack() }, modifier = Modifier.fillMaxWidth()) { Text("Finish") }
            }
        }
    }
}
