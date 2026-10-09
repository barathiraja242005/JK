package com.barathiraja.jk.ui.screens

import com.barathiraja.jk.ui.components.formatDuration
import com.barathiraja.jk.ui.components.BackScreenBar
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.barathiraja.jk.ui.components.JkCard
import com.barathiraja.jk.ui.components.KeyValue
import com.barathiraja.jk.ui.components.Ring
import com.barathiraja.jk.ui.theme.Accent
import com.barathiraja.jk.ui.theme.Good
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.PaddingValues
import com.barathiraja.jk.ui.components.SegmentedTabs
import com.barathiraja.jk.ui.components.SegmentTab

@Composable
fun ToolsScreen(nav: NavHostController) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val view = LocalView.current
    DisposableEffect(Unit) { view.keepScreenOn = true; onDispose { view.keepScreenOn = false } }
    Column(Modifier.fillMaxSize()) {
        BackScreenBar("Timers", onBack = { nav.popBackStack() })
        SegmentedTabs(listOf(SegmentTab("Stopwatch"), SegmentTab("Interval")), tab, { tab = it },
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        if (tab == 0) Stopwatch() else IntervalTimer()
    }
}

private fun fmtMs(ms: Long) = "%02d:%02d.%02d".format(ms / 60000, (ms / 1000) % 60, (ms / 10) % 100)

@Composable
private fun Stopwatch() {
    var running by remember { mutableStateOf(false) }
    var base by remember { mutableLongStateOf(0L) }      // accumulated ms before current run
    var startedAt by remember { mutableLongStateOf(0L) }
    var now by remember { mutableLongStateOf(0L) }
    val laps = remember { mutableStateListOf<Long>() }
    LaunchedEffect(running) { while (running) { now = System.currentTimeMillis(); delay(30) } }
    val elapsed = base + if (running) now - startedAt else 0

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(fmtMs(elapsed), fontSize = 64.sp, style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(vertical = 32.dp)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { if (running) laps.add(0, elapsed) else { base = 0; laps.clear() } },
                    modifier = Modifier.width(140.dp),
                ) { Text(if (running) "Lap" else "Reset") }
                Button(onClick = {
                    if (running) { base += System.currentTimeMillis() - startedAt; running = false }
                    else { startedAt = System.currentTimeMillis(); now = startedAt; running = true }
                }, modifier = Modifier.width(140.dp)) { Text(if (running) "Stop" else "Start") }
            }
        }
        itemsIndexed(laps) { i, t ->
            val prev = laps.getOrNull(i + 1) ?: 0L
            KeyValue("Lap ${laps.size - i}", "${fmtMs(t - prev)}   ${fmtMs(t)}", Modifier.padding(horizontal = 8.dp))
        }
    }
}

@Composable
private fun IntervalTimer() {
    var work by rememberSaveable { mutableIntStateOf(20) }
    var rest by rememberSaveable { mutableIntStateOf(10) }
    var rounds by rememberSaveable { mutableIntStateOf(8) }
    var running by remember { mutableStateOf(false) }
    var round by remember { mutableIntStateOf(1) }
    var isWork by remember { mutableStateOf(true) }
    var left by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }
    val tone = remember { runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 80) }.getOrNull() }
    DisposableEffect(Unit) { onDispose { tone?.release() } }

    LaunchedEffect(running) {
        while (running) {
            delay(1000)
            left--
            if (left in 1..3) tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
            if (left <= 0) {
                tone?.startTone(ToneGenerator.TONE_PROP_BEEP2, 300)
                if (isWork && rest > 0) { isWork = false; left = rest }
                else if (round < rounds) { round++; isWork = true; left = work }
                else { running = false; finished = true }
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!running && left == 0 || finished) {
            Text("Tabata = 20s work / 10s rest × 8", color = MaterialTheme.colorScheme.onSurfaceVariant)
            JkCard(Modifier.fillMaxWidth()) {
                Adjust("Work", "${work}s", { work = (work - 5).coerceAtLeast(5) }, { work = (work + 5).coerceAtMost(600) })
                Adjust("Rest", "${rest}s", { rest = (rest - 5).coerceAtLeast(0) }, { rest = (rest + 5).coerceAtMost(600) })
                Adjust("Rounds", "$rounds", { rounds = (rounds - 1).coerceAtLeast(1) }, { rounds = (rounds + 1).coerceAtMost(50) })
            }
            if (finished) Text("Done! 💪 ${rounds} rounds complete", style = MaterialTheme.typography.titleMedium)
            Button(onClick = { round = 1; isWork = true; left = work; finished = false; running = true }, Modifier.fillMaxWidth()) {
                Text("Start · ${formatDuration(((work + rest) * rounds).toLong())}")
            }
        } else {
            Text(if (isWork) "WORK" else "REST", style = MaterialTheme.typography.headlineMedium, color = if (isWork) Accent else Good)
            Ring(left / (if (isWork) work else rest).coerceAtLeast(1).toFloat(), if (isWork) Accent else Good, size = 260.dp, stroke = 16.dp) {
                Text("$left", fontSize = 84.sp, style = MaterialTheme.typography.displaySmall)
            }
            Text("Round $round of $rounds", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { running = false; left = 0 }) { Text("Reset") }
                Button(onClick = { running = !running }) { Text(if (running) "Pause" else "Resume") }
            }
        }
    }
}

@Composable
private fun Adjust(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, Modifier.weight(1f))
        FilledTonalIconButton(onClick = onMinus) { Icon(Icons.Filled.Remove, "Decrease") }
        Text(value, Modifier.width(56.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
        FilledTonalIconButton(onClick = onPlus) { Icon(Icons.Filled.Add, "Increase") }
    }
}
