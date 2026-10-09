package com.barathiraja.jk.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.barathiraja.jk.ui.theme.Jk
import com.barathiraja.jk.ui.components.CardBox
import com.barathiraja.jk.ui.components.Heading
import com.barathiraja.jk.ui.components.JkPage
import com.barathiraja.jk.ui.components.PlainButton
import com.barathiraja.jk.ui.theme.plex
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.AlertDialog
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
import com.barathiraja.jk.data.trimZero
import com.barathiraja.jk.domain.Health
import com.barathiraja.jk.ui.JkViewModel
import com.barathiraja.jk.ui.components.BarChart
import com.barathiraja.jk.ui.components.LineChart
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavHostController
import com.barathiraja.jk.data.ExerciseRepo
import com.barathiraja.jk.gym.Assignment
import com.barathiraja.jk.gym.Lifts
import com.barathiraja.jk.gym.Scoring
import com.barathiraja.jk.ui.GymViewModel
import com.barathiraja.jk.ui.Routes
import com.barathiraja.jk.ui.member.MyAvatarButton
import com.barathiraja.jk.ui.gym.dayLabel

@Composable
fun ProgressScreen(vm: JkViewModel, nav: NavHostController, gvm: GymViewModel) {
    val profile by vm.profile.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val weekSessions by vm.weekSessions.collectAsStateWithLifecycle()
    val weekSteps by vm.weekSteps.collectAsStateWithLifecycle()
    val weekWater by vm.weekWater.collectAsStateWithLifecycle()
    val weights by vm.weights.collectAsStateWithLifecycle()
    var addWeight by remember { mutableStateOf(false) }
    val photos by vm.photos.collectAsStateWithLifecycle()
    // The coach's workouts, for the workout streak and the strength list.
    val me by gvm.me.collectAsStateWithLifecycle()
    val assignments by gvm.assignments.collectAsStateWithLifecycle()
    val mine = assignments.filter { it.memberUid == me?.uid }

    val today by vm.currentDay.collectAsStateWithLifecycle()
    val days = (today - 6..today).toList()
    val labels = days.map { LocalDate.ofEpochDay(it).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2) }
    val week = weekSessions.filter { it.epochDay in days }
    val minutes = days.map { d -> week.filter { it.epochDay == d }.sumOf { it.durationSec } / 60f }
    val steps = days.map { d -> (weekSteps.firstOrNull { it.epochDay == d }?.steps ?: 0).toFloat() }
    val water = days.map { d -> (weekWater.firstOrNull { it.epochDay == d }?.glasses ?: 0).toFloat() }
    val inARow = Scoring.workoutStreak(mine, today)

    if (addWeight) WeightDialog(profile.weightKg, onDismiss = { addWeight = false }) { vm.logWeight(it); addWeight = false }

    JkPage {
        item {
            Row(Modifier.padding(start = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Last 7 days", style = plex(14.sp, FontWeight.Medium), color = Jk.RedText)
                    Text("Progress", style = plex(24.sp, FontWeight.Bold, line = 30.sp, tracking = (-0.4).sp), color = Jk.Ink)
                }
                MyAvatarButton(vm, gvm, 48.dp) { nav.navigate(Routes.PROFILE) }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Summary("${week.size}", "workouts", Modifier.weight(1f))
                Summary("${week.sumOf { it.durationSec } / 60}", "minutes", Modifier.weight(1f))
                Summary("$inARow", "in a row", Modifier.weight(1f))
            }
        }
        item {
            ChartCard("Workout minutes", if (minutes.sum() == 0f) "No workouts in the last 7 days yet" else "${minutes.sum().toInt()} min in the last 7 days", (if (minutes.sum() > 0f) { { BarChart(minutes, labels, Jk.Red) } } else null))
        }
        item { StrengthCard(mine, today, nav) }
        item {
            CardBox(padding = 16.dp) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Body weight", style = plex(17.sp, FontWeight.Bold), color = Jk.Ink)
                            val bmi = Health.bmi(profile.weightKg, profile.heightCm)
                            Text("%.1f kg · BMI %.1f (%s)".format(profile.weightKg, bmi, Health.bmiCategory(bmi)), style = plex(14.sp), color = Jk.Muted)
                        }
                        PlainButton("Log", { addWeight = true }, icon = Icons.Filled.Add)
                    }
                    if (weights.size >= 2) {
                        Spacer(Modifier.height(12.dp))
                        LineChart(weights.takeLast(30).map { it.kg }, Jk.Ink)
                        val change = weights.last().kg - weights.first().kg
                        Text("%+.1f kg since %s".format(change, LocalDate.ofEpochDay(weights.first().epochDay).format(DateTimeFormatter.ofPattern("d MMM"))),
                            style = plex(13.sp), color = Jk.Muted)
                    } else Text("Log your weight on a few different days to see the trend.", Modifier.padding(top = 8.dp),
                        style = plex(13.sp, line = 18.sp), color = Jk.Muted)
                }
            }
        }
        item {
            ChartCard("Steps", if (steps.sum() == 0f) "No steps counted yet. Steps are counted while JK is open." else "%,d in the last 7 days".format(steps.sum().toInt()), (if (steps.sum() > 0f) { { BarChart(steps, labels, Jk.Ink, goal = settings.stepGoal.toFloat()) } } else null))
        }
        item {
            ChartCard("Water", if (water.sum() == 0f) "No water logged yet. Add glasses on Today." else "${water.sum().toInt()} glasses in the last 7 days", (if (water.sum() > 0f) { { BarChart(water, labels, Jk.Ink, goal = settings.waterGoalGlasses.toFloat()) } } else null))
        }
        item {
            NavRow(Icons.Outlined.PhotoCamera, "Transformation photos",
                when { photos.size >= 2 -> "Compare before and after"; photos.size == 1 -> "1 photo · add another to compare"; else -> "Add progress photos" }) { nav.navigate(Routes.PHOTOS) }
        }
        if (sessions.isNotEmpty()) {
            item { Heading("History") }
            item {
                CardBox(padding = 0.dp) {
                    Column {
                        sessions.take(30).forEachIndexed { i, s ->
                            if (i > 0) Box(Modifier.padding(start = 16.dp).fillMaxWidth()
                                .height(1.dp).background(Jk.Line))
                            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(s.title, style = plex(15.sp, FontWeight.SemiBold), color = Jk.Ink, maxLines = 1)
                                    Text(historyFmt.format(Instant.ofEpochMilli(s.finishedAt)), style = plex(13.sp), color = Jk.Muted)
                                }
                                Text("${s.durationSec / 60} min · ${s.calories} kcal", style = plex(13.sp, FontWeight.Medium), color = Jk.Ink)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Heaviest set per exercise from the coach's workouts, and how far it has gone up since the first time. */
@Composable
private fun StrengthCard(mine: List<Assignment>, today: Long, nav: NavHostController) {
    val bests = remember(mine) { Lifts.bests(mine) }
    CardBox(padding = 16.dp) {
        Column {
            Text("Strength", style = plex(17.sp, FontWeight.Bold), color = Jk.Ink)
            if (bests.isEmpty()) {
                Text("Your heaviest set for each exercise shows here. In a workout, tap \"Change reps / kg\" and set the weight you lift.",
                    Modifier.padding(top = 4.dp), style = plex(14.sp, line = 20.sp), color = Jk.Muted)
                return@Column
            }
            Text("Your heaviest set for each exercise, from your coach's workouts.", Modifier.padding(top = 2.dp),
                style = plex(13.sp, line = 18.sp), color = Jk.Muted)
            bests.take(8).forEach { b ->
                val name = ExerciseRepo.get(b.exerciseId)?.name ?: b.exerciseId.replace('_', ' ')
                Row(Modifier.fillMaxWidth().padding(top = 12.dp)
                    .clickable(onClickLabel = "Open $name") { nav.navigate(Routes.exercise(b.exerciseId)) },
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(name, style = plex(15.sp, FontWeight.SemiBold), color = Jk.Ink, maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                        val day = dayLabel(b.lastDay, today)
                        Text("Last done " + if (b.lastDay >= today - 1) day.lowercase() else "on $day", style = plex(13.sp), color = Jk.Muted)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${kg(b.bestKg)} × ${b.bestReps}", style = plex(16.sp, FontWeight.Bold), color = Jk.Ink)
                        if (b.gainKg > 0f) Text("+${kg(b.gainKg)}", style = plex(13.sp, FontWeight.SemiBold), color = Jk.RedText)
                    }
                }
            }
        }
    }
}

private fun kg(v: Float) = "${v.trimZero()} kg"

private val historyFmt = DateTimeFormatter.ofPattern("EEE d MMM, HH:mm").withZone(ZoneId.systemDefault())

@Composable
private fun Summary(value: String, label: String, modifier: Modifier) {
    CardBox(modifier, padding = 14.dp) {
        Column {
            Text(value, style = plex(24.sp, FontWeight.Bold), color = Jk.Ink)
            Text(label, style = plex(13.sp, FontWeight.Medium), color = Jk.Muted)
        }
    }
}

@Composable
private fun ChartCard(title: String, sub: String, chart: (@Composable () -> Unit)?) {
    CardBox(padding = 16.dp) {
        Column {
            Text(title, style = plex(17.sp, FontWeight.Bold), color = Jk.Ink)
            Text(sub, style = plex(14.sp, line = 19.sp), color = Jk.Muted)
            // No chart when there's nothing to draw; the line above says why.
            if (chart != null) Box(Modifier.padding(top = 12.dp)) { chart() }
        }
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
