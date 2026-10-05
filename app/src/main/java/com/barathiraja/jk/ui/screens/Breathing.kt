package com.barathiraja.jk.ui.screens

import com.barathiraja.jk.ui.components.BackScreen
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import com.barathiraja.jk.ui.components.JkCard
import com.barathiraja.jk.ui.theme.Calm

/** A breathing pattern as (label, seconds, target scale) steps. */
private data class Pattern(val name: String, val about: String, val steps: List<Triple<String, Int, Float>>)

private val patterns = listOf(
    Pattern("Box", "Focus & composure · 4-4-4-4",
        listOf(Triple("Inhale", 4, 1f), Triple("Hold", 4, 1f), Triple("Exhale", 4, 0.5f), Triple("Hold", 4, 0.5f))),
    Pattern("4-7-8", "Wind down for sleep",
        listOf(Triple("Inhale", 4, 1f), Triple("Hold", 7, 1f), Triple("Exhale", 8, 0.5f))),
    Pattern("Relax", "Long exhale to calm · 4-6",
        listOf(Triple("Inhale", 4, 1f), Triple("Exhale", 6, 0.5f))),
)

@Composable
fun BreathingScreen(nav: NavHostController) {
    var pattern by remember { mutableStateOf(patterns.first()) }
    var running by remember { mutableStateOf(false) }
    var label by remember { mutableStateOf("Ready") }
    var count by remember { mutableIntStateOf(0) }
    var cycles by remember { mutableIntStateOf(0) }
    val scale = remember { Animatable(0.5f) }

    LaunchedEffect(running, pattern) {
        if (!running) { label = "Ready"; scale.animateTo(0.5f); return@LaunchedEffect }
        cycles = 0
        while (true) {
            for ((name, secs, target) in pattern.steps) {
                label = name
                count = secs
                // Animate the circle over the full step while counting down each second.
                val anim = launch { scale.animateTo(target, tween(secs * 1000, easing = LinearEasing)) }
                repeat(secs) { kotlinx.coroutines.delay(1000); count-- }
                anim.join()
            }
            cycles++
        }
    }

    BackScreen("Breathe", onBack = { nav.popBackStack() }) {
        item { Choice(patterns, pattern, { it.name }) { pattern = it; running = false } }
        item { Text(pattern.about, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item {
            JkCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(260.dp), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(260.dp).scale(scale.value).background(Calm.copy(alpha = 0.18f), CircleShape))
                        Box(Modifier.size(180.dp).scale(scale.value).background(Calm.copy(alpha = 0.35f), CircleShape))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(label, style = MaterialTheme.typography.headlineSmall)
                            if (running) Text("$count", style = MaterialTheme.typography.displaySmall)
                        }
                    }
                    Text(if (running) "Cycles: $cycles" else "Sit comfortably and follow the circle.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box(Modifier.height(16.dp))
                    Button(onClick = { running = !running }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (running) "Stop" else "Begin")
                    }
                }
            }
        }
    }
}
