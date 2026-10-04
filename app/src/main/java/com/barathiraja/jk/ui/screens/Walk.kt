package com.barathiraja.jk.ui.screens

import com.barathiraja.jk.ui.components.formatDuration
import com.barathiraja.jk.ui.components.BackScreen
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.barathiraja.jk.data.WalkSession
import com.barathiraja.jk.domain.Health
import com.barathiraja.jk.ui.JkViewModel
import com.barathiraja.jk.ui.components.JkCard
import com.barathiraja.jk.ui.components.KeyValue
import com.barathiraja.jk.ui.components.Ring
import com.barathiraja.jk.ui.components.SectionTitle
import com.barathiraja.jk.ui.theme.Leaf
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val walkFmt = DateTimeFormatter.ofPattern("d MMM, HH:mm").withZone(ZoneId.systemDefault())

/** Timed walk/run session using the step counter; distance and calories are estimates. */
@Composable
fun WalkScreen(vm: JkViewModel, nav: NavHostController) {
    val profile by vm.profile.collectAsStateWithLifecycle()
    val stepsToday by vm.stepsToday.collectAsStateWithLifecycle()
    val history by vm.walks.collectAsStateWithLifecycle()
    var startedAt by rememberSaveable { mutableLongStateOf(0L) }
    var startSteps by rememberSaveable { mutableIntStateOf(0) }
    var pausedTotal by rememberSaveable { mutableLongStateOf(0L) }
    var pausedAt by rememberSaveable { mutableLongStateOf(0L) }
    var now by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var saved by rememberSaveable { mutableStateOf(false) }

    val active = startedAt > 0
    val paused = pausedAt > 0
    LaunchedEffect(active) { while (active) { now = System.currentTimeMillis(); delay(1000) } }

    val elapsedSec = if (!active) 0 else (((if (paused) pausedAt else now) - startedAt - pausedTotal) / 1000).toInt().coerceAtLeast(0)
    val steps = if (active) (stepsToday - startSteps).coerceAtLeast(0) else 0
    val km = Health.stepKm(steps, profile.heightCm)
    val kcal = Health.stepCalories(steps, profile.weightKg)
    val pace = if (km > 0.05f) elapsedSec / 60f / km else 0f

    BackScreen("Walk & run", onBack = { nav.popBackStack() }) {
        item {
            JkCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Ring(if (active) (elapsedSec % 60) / 60f else 0f, Leaf, size = 200.dp, stroke = 12.dp) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(formatDuration(elapsedSec.toLong()), style = MaterialTheme.typography.headlineMedium)
                            Text("%,d steps".format(steps), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Stat("%.2f".format(km), "km")
                        Stat("$kcal", "kcal")
                        Stat(if (pace > 0) "%.1f".format(pace) else "–", "min/km")
                    }
                    androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))
                    if (!active) {
                        Button(onClick = {
                            startedAt = System.currentTimeMillis(); startSteps = stepsToday; pausedTotal = 0; pausedAt = 0; saved = false
                            vm.startSteps()
                        }, modifier = Modifier.fillMaxWidth()) { Text("Start") }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = {
                                if (paused) { pausedTotal += System.currentTimeMillis() - pausedAt; pausedAt = 0 }
                                else pausedAt = System.currentTimeMillis()
                            }, modifier = Modifier.weight(1f)) { Text(if (paused) "Resume" else "Pause") }
                            Button(onClick = {
                                if (!saved && elapsedSec >= 30) {
                                    saved = true
                                    vm.saveWalk(WalkSession(epochDay = LocalDate.now().toEpochDay(), startedAt = startedAt,
                                        durationSec = elapsedSec, steps = steps, calories = kcal))
                                }
                                startedAt = 0
                            }, modifier = Modifier.weight(1f)) { Text("Finish") }
                        }
                    }
                }
            }
        }
        item {
            Text("Keep JK open or check back after your walk — steps are read from your phone's step sensor.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (history.isNotEmpty()) {
            item { SectionTitle("History") }
            items(history, key = { it.id }) { w ->
                JkCard(Modifier.fillMaxWidth()) {
                    KeyValue(walkFmt.format(Instant.ofEpochMilli(w.startedAt)),
                        "${formatDuration(w.durationSec.toLong())} · %,d steps · ${w.calories} kcal".format(w.steps))
                }
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
