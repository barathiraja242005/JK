package com.barathiraja.jk.ui.screens

import com.barathiraja.jk.ui.components.formatDuration
import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import com.barathiraja.jk.ui.components.Confetti
import com.barathiraja.jk.ui.components.ExerciseDemo
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import com.barathiraja.jk.data.Workout
import com.barathiraja.jk.ui.JkViewModel
import com.barathiraja.jk.ui.components.Ring
import com.barathiraja.jk.ui.theme.Accent
import com.barathiraja.jk.ui.theme.Good
import com.barathiraja.jk.ui.components.ProgressBar
import com.barathiraja.jk.ui.theme.Jk

@Composable
fun WorkoutPlayerScreen(id: String, vm: JkViewModel, nav: NavHostController) {
    var workout by remember { mutableStateOf<Workout?>(null) }
    var missing by remember { mutableStateOf(false) }
    LaunchedEffect(id) {
        workout = vm.resolveWorkout(id)
        missing = workout == null
    }
    val w = workout
    when {
        w != null -> PlayerContent(w, vm, nav)
        missing -> Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text("This workout is no longer available.")
            TextButton(onClick = { nav.popBackStack() }) { Text("Go back") }
        }
    }
}

@Composable
private fun PlayerContent(workout: Workout, vm: JkViewModel, nav: NavHostController) {
    val app = LocalContext.current.applicationContext as Application
    val profile = vm.profile.value
    val voice = vm.settings.value.voiceCues
    val pvm: PlayerViewModel = viewModel(key = "player-${workout.id}", factory = viewModelFactory {
        initializer { PlayerViewModel(app, workout, profile.weightKg, voice) }
    })
    val s by pvm.state.collectAsStateWithLifecycle()
    var confirmQuit by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    // Keep the screen on while training.
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    LaunchedEffect(s.phase) {
        if (s.phase == Phase.DONE && !saved) {
            saved = true
            vm.completeWorkout(pvm.session())
        }
    }

    BackHandler(enabled = s.phase != Phase.DONE) { confirmQuit = true }

    if (confirmQuit) {
        AlertDialog(
            onDismissRequest = { confirmQuit = false },
            title = { Text("Quit workout?") },
            text = { Text("Your progress for this session won't be saved.") },
            confirmButton = { TextButton(onClick = { nav.popBackStack() }) { Text("Quit") } },
            dismissButton = { TextButton(onClick = { confirmQuit = false }) { Text("Keep going") } },
        )
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if (s.phase == Phase.DONE) nav.popBackStack() else confirmQuit = true }) {
                    Icon(Icons.Filled.Close, "Close")
                }
                Text(workout.title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center,
                    maxLines = 1)
                Text(formatDuration(s.elapsed.toLong()), style = MaterialTheme.typography.titleMedium)
            }
            Box(Modifier.padding(vertical = 8.dp)) {
                ProgressBar(if (s.phase == Phase.DONE) 1f else s.index / pvm.sequence.size.toFloat(), Jk.Well, Jk.Red, key = "player", height = 6.dp)
            }

            if (s.phase == Phase.DONE) {
                DoneContent(s, onFinish = { nav.popBackStack() })
                return@Column
            }

            val block = pvm.sequence[s.index]
            val shown = if (s.phase == Phase.REST) pvm.sequence[s.index + 1] else block
            val (label, color) = when (s.phase) {
                Phase.READY -> "GET READY" to Accent
                Phase.REST -> "REST · UP NEXT" to Good
                else -> "ROUND ${s.index / workout.blocks.size + 1} OF ${workout.rounds}" to Accent
            }
            ExerciseDemo(
                shown.exercise,
                Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.height(12.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = color)
            Text(shown.exercise.name, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, maxLines = 2)
            Spacer(Modifier.height(8.dp))

            val isReps = s.phase == Phase.WORK && !block.isTimed
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Ring(
                    progress = if (isReps) 1f else s.remaining / s.phaseTotal.coerceAtLeast(1).toFloat(),
                    color = color, size = 120.dp, stroke = 10.dp,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (isReps) "×${block.reps}" else "${s.remaining}", fontSize = 40.sp,
                            style = MaterialTheme.typography.displaySmall)
                        Text(if (isReps) "reps" else "sec", color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium)
                    }
                }
                Column(Modifier.weight(1f)) {
                    if (isReps) {
                        Button(onClick = pvm::advance, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Done") }
                    } else if (s.phase == Phase.REST) {
                        OutlinedButton(onClick = { pvm.addRest(10) }, modifier = Modifier.fillMaxWidth()) { Text("+10s") }
                        Button(onClick = pvm::advance, modifier = Modifier.fillMaxWidth()) { Text("Skip rest") }
                    } else {
                        Text(shown.exercise.instructions.firstOrNull().orEmpty(), maxLines = 4,
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = pvm::previous, enabled = s.phase != Phase.READY) { Icon(Icons.Filled.SkipPrevious, "Previous") }
                FilledIconButton(onClick = pvm::togglePause, modifier = Modifier.size(64.dp)) {
                    Icon(if (s.paused) Icons.Filled.PlayArrow else Icons.Filled.Pause, if (s.paused) "Resume" else "Pause")
                }
                IconButton(onClick = pvm::advance) { Icon(Icons.Filled.SkipNext, "Skip") }
            }
        }
        if (s.phase == Phase.DONE) Confetti()
    }
}

@Composable
private fun DoneContent(s: PlayerState, onFinish: () -> Unit) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("🏆", fontSize = 72.sp)
        Text("Workout complete!", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(formatDuration(s.elapsed.toLong()), style = MaterialTheme.typography.headlineSmall)
                Text("duration", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${s.calories.toInt()}", style = MaterialTheme.typography.headlineSmall, color = Accent)
                Text("kcal", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(32.dp))
        Button(onClick = onFinish, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("Finish") }
    }
}
