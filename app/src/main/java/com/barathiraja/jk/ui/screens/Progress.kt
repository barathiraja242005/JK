package com.barathiraja.jk.ui.screens

import com.barathiraja.jk.ui.components.Avatar
import com.barathiraja.jk.ui.components.formatDuration
import com.barathiraja.jk.ui.components.TabScreen
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.barathiraja.jk.domain.Health
import com.barathiraja.jk.ui.JkViewModel
import com.barathiraja.jk.ui.components.BarChart
import com.barathiraja.jk.ui.components.JkCard
import com.barathiraja.jk.ui.components.KeyValue
import com.barathiraja.jk.ui.components.LineChart
import com.barathiraja.jk.ui.components.SectionTitle
import com.barathiraja.jk.ui.theme.Aqua
import com.barathiraja.jk.ui.theme.Ember
import com.barathiraja.jk.ui.theme.Leaf
import com.barathiraja.jk.ui.theme.Violet
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun ProgressScreen(vm: JkViewModel, nav: androidx.navigation.NavHostController) {
    val profile by vm.profile.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val weekSessions by vm.weekSessions.collectAsStateWithLifecycle()
    val weekSteps by vm.weekSteps.collectAsStateWithLifecycle()
    val weekWater by vm.weekWater.collectAsStateWithLifecycle()
    val weights by vm.weights.collectAsStateWithLifecycle()
    val streak by vm.streak.collectAsStateWithLifecycle()
    var addWeight by remember { mutableStateOf(false) }
    val avatar by vm.avatar.collectAsStateWithLifecycle()
    val walks by vm.walks.collectAsStateWithLifecycle()
    val mind by vm.mindSessions.collectAsStateWithLifecycle()
    val photos by vm.photos.collectAsStateWithLifecycle()

    val today = LocalDate.now().toEpochDay()
    val days = (today - 6..today).toList()
    val labels = days.map { LocalDate.ofEpochDay(it).dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()) }
    val minutes = days.map { d -> weekSessions.filter { it.epochDay == d }.sumOf { it.durationSec } / 60f }
    val steps = days.map { d -> (weekSteps.firstOrNull { it.epochDay == d }?.steps ?: 0).toFloat() }
    val water = days.map { d -> (weekWater.firstOrNull { it.epochDay == d }?.glasses ?: 0).toFloat() }

    if (addWeight) WeightDialog(profile.weightKg, onDismiss = { addWeight = false }) { vm.logWeight(it); addWeight = false }

    TabScreen("Progress", subtitle = "Last 7 days",
        action = { Avatar(avatar, profile.name, 44.dp) { nav.navigate(com.barathiraja.jk.ui.Routes.PROFILE) } }) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Summary("${sessions.size}", "workouts", Ember, Modifier.weight(1f))
                Summary("${sessions.sumOf { it.durationSec } / 60}", "minutes", Violet, Modifier.weight(1f))
                Summary("$streak", "day streak", Leaf, Modifier.weight(1f))
            }
        }
        item { ChartCard("Workout minutes", "${minutes.sum().toInt()} min this week") { BarChart(minutes, labels, Ember) } }
        item {
            ChartCard("Steps", "%,d this week".format(steps.sum().toInt())) {
                BarChart(steps, labels, Leaf, goal = settings.stepGoal.toFloat())
            }
        }
        item {
            ChartCard("Water", "${water.sum().toInt()} glasses this week") {
                BarChart(water, labels, Aqua, goal = settings.waterGoalGlasses.toFloat())
            }
        }
        item {
            JkCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Body weight", style = MaterialTheme.typography.titleMedium)
                        val bmi = Health.bmi(profile.weightKg, profile.heightCm)
                        Text("%.1f kg · BMI %.1f (%s)".format(profile.weightKg, bmi, Health.bmiCategory(bmi)),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    FilledTonalButton(onClick = { addWeight = true }) {
                        Icon(Icons.Filled.Add, null)
                        Text("Log")
                    }
                }
                if (weights.size >= 2) {
                    LineChart(weights.takeLast(30).map { it.kg }, Violet)
                    val change = weights.last().kg - weights.first().kg
                    Text("%+.1f kg since %s".format(change, LocalDate.ofEpochDay(weights.first().epochDay)),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text("Log your weight on different days to see a trend.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Summary("${walks.size}", "walks", Leaf, Modifier.weight(1f))
                Summary("${mind.sumOf { it.durationSec } / 60}", "mindful min", Violet, Modifier.weight(1f))
                Summary("${photos.size}", "photos", Aqua, Modifier.weight(1f))
            }
        }
        item {
            NavRow(androidx.compose.material.icons.Icons.Outlined.PhotoCamera, "Transformation photos",
                if (photos.size >= 2) "Compare before and after" else "Add progress photos", Ember) {
                nav.navigate(com.barathiraja.jk.ui.Routes.PHOTOS)
            }
        }
        if (sessions.isNotEmpty()) {
            item { SectionTitle("History") }
            items(sessions.take(30), key = { it.id }) { s ->
                JkCard(Modifier.fillMaxWidth()) {
                    Text(s.title, style = MaterialTheme.typography.titleSmall)
                    KeyValue(
                        historyFmt.format(Instant.ofEpochMilli(s.finishedAt)),
                        "${formatDuration(s.durationSec.toLong())} · ${s.calories} kcal",
                    )
                }
            }
        }
    }
}

private val historyFmt = DateTimeFormatter.ofPattern("d MMM, HH:mm").withZone(ZoneId.systemDefault())

@Composable
private fun Summary(value: String, label: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    JkCard(modifier) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = color)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ChartCard(title: String, sub: String, chart: @Composable () -> Unit) {
    JkCard(Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 12.dp))
        chart()
    }
}

@Composable
fun WeightDialog(current: Float, onDismiss: () -> Unit, onSave: (Float) -> Unit) {
    var text by remember { mutableStateOf("%.1f".format(Locale.US, current)) }
    val value = text.toFloatOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log today's weight") },
        text = { NumberField(text, { text = it }, "Weight (kg)") },
        confirmButton = {
            TextButton(onClick = { onSave(value!!) }, enabled = value != null && value in 25f..300f) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
