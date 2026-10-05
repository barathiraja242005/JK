package com.barathiraja.jk.ui.screens

import com.barathiraja.jk.ui.components.formatDuration
import com.barathiraja.jk.ui.components.BackScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.barathiraja.jk.ui.JkViewModel
import com.barathiraja.jk.ui.components.JkCard
import com.barathiraja.jk.ui.components.KeyValue
import com.barathiraja.jk.ui.components.Ring
import com.barathiraja.jk.ui.components.SectionTitle
import com.barathiraja.jk.ui.theme.Good
import com.barathiraja.jk.ui.theme.Watch
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val plans = listOf(13 to "Gentle", 16 to "16:8 Classic", 18 to "18:6 Focus", 20 to "20:4 Warrior")
private val timeFmt = DateTimeFormatter.ofPattern("EEE HH:mm").withZone(ZoneId.systemDefault())

/** Rough fasting stages by hours elapsed. */
private fun stage(h: Double) = when {
    h < 4 -> "Digesting your last meal"
    h < 12 -> "Blood sugar settling"
    h < 16 -> "Fat burning ramps up"
    else -> "Deep fasted state"
}

@Composable
fun FastingScreen(vm: JkViewModel, nav: NavHostController) {
    val fast by vm.activeFast.collectAsStateWithLifecycle()
    val past by vm.pastFasts.collectAsStateWithLifecycle()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var plan by remember { mutableIntStateOf(16) }
    LaunchedEffect(fast) {
        while (fast != null) { now = System.currentTimeMillis(); delay(1000) }
    }

    BackScreen("Fasting", onBack = { nav.popBackStack() }) {
        item {
            JkCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    val f = fast
                    if (f == null) {
                        Text("Choose a plan", style = MaterialTheme.typography.titleMedium)
                        Box(Modifier.height(12.dp))
                        Choice(plans.map { it.first }, plan, { h -> "${plans.first { it.first == h }.second} · ${h}h" }) { plan = it }
                        Box(Modifier.height(16.dp))
                        Button(onClick = { vm.startFast(plan) }, modifier = Modifier.fillMaxWidth()) { Text("Start ${plan}h fast") }
                    } else {
                        val elapsedMs = (now - f.startedAt).coerceAtLeast(0)
                        val targetMs = f.targetHours * 3_600_000L
                        val reached = elapsedMs >= targetMs
                        Ring(elapsedMs / targetMs.toFloat(), if (reached) Good else Watch, size = 220.dp, stroke = 14.dp) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(if (reached) "Goal reached!" else "Elapsed", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatDuration(elapsedMs / 1000), style = MaterialTheme.typography.headlineMedium)
                                Text("of ${f.targetHours}h", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Box(Modifier.height(12.dp))
                        Text(stage(elapsedMs / 3_600_000.0), style = MaterialTheme.typography.titleMedium)
                        KeyValue("Started", timeFmt.format(Instant.ofEpochMilli(f.startedAt)))
                        KeyValue("Goal", timeFmt.format(Instant.ofEpochMilli(f.startedAt + targetMs)))
                        Box(Modifier.height(12.dp))
                        OutlinedButton(onClick = { vm.endFast() }, modifier = Modifier.fillMaxWidth()) { Text("End fast") }
                    }
                }
            }
        }
        if (past.isNotEmpty()) {
            item { SectionTitle("History") }
            items(past, key = { it.id }) { f ->
                val hours = ((f.endedAt ?: f.startedAt) - f.startedAt) / 3_600_000.0
                JkCard(Modifier.fillMaxWidth()) {
                    KeyValue(timeFmt.format(Instant.ofEpochMilli(f.startedAt)),
                        "%.1fh / %dh %s".format(hours, f.targetHours, if (hours >= f.targetHours) "✓" else ""))
                }
            }
        }
        item {
            Text("Fasting isn't right for everyone. Check with a doctor if you're pregnant, diabetic or have a history of disordered eating.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
