package com.barathiraja.jk.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.barathiraja.jk.data.Equipment
import com.barathiraja.jk.data.Goal
import com.barathiraja.jk.data.Injury
import com.barathiraja.jk.data.Level
import com.barathiraja.jk.data.Place
import com.barathiraja.jk.data.TrainingPrefs
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

private const val STEPS = 7

/**
 * Step-by-step training setup. Ends with a short "building your plan" sequence, then calls
 * [onFinish] with the chosen preferences, place and goal.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrainingSetupScreen(
    initial: TrainingPrefs,
    initialPlace: Place,
    initialGoal: Goal,
    onFinish: (TrainingPrefs, Place, Goal) -> Unit,
    onBack: (() -> Unit)?,
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var place by rememberSaveable { mutableStateOf(initialPlace) }
    var level by rememberSaveable { mutableStateOf(initial.level) }
    var goal by rememberSaveable { mutableStateOf(initialGoal) }
    var days by remember { mutableStateOf(initial.activeDays) }
    var equipment by remember {
        mutableStateOf(if (initial.setupDone) initial.equipment else if (initialPlace == Place.GYM) Equipment.gymDefault else Equipment.homeDefault)
    }
    var injuries by remember { mutableStateOf(initial.injuries) }
    var rest by rememberSaveable { mutableIntStateOf(initial.restSec) }
    var building by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = step > 0 && !building) { step-- }

    if (building) {
        BuildingPlan {
            onFinish(TrainingPrefs(true, level, days, equipment, injuries, rest), place, goal)
        }
        return
    }

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (step > 0 || onBack != null) {
                IconButton(onClick = { if (step > 0) step-- else onBack?.invoke() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
            }
            LinearProgressIndicator(progress = { (step + 1) / STEPS.toFloat() }, Modifier.weight(1f).padding(horizontal = 8.dp))
            Text("${step + 1}/$STEPS", style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(16.dp))

        Box(Modifier.weight(1f)) {
            AnimatedContent(step, label = "step") { s ->
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (s) {
                        0 -> {
                            Title("Where will you train most?", "We'll pick exercises for your space.")
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                BigChoice("Gym Workout", "Full equipment", Icons.Outlined.FitnessCenter, place == Place.GYM, Modifier.weight(1f)) {
                                    place = Place.GYM; equipment = Equipment.gymDefault
                                }
                                BigChoice("Home Workout", "Little or no equipment", Icons.Outlined.Home, place == Place.HOME, Modifier.weight(1f)) {
                                    place = Place.HOME; equipment = Equipment.homeDefault
                                }
                            }
                        }
                        1 -> {
                            Title("Choose your current fitness level", "Be honest — JK adjusts as you progress.")
                            OptionRow("Beginner", "New to training or getting back into it", level == Level.BEGINNER) { level = Level.BEGINNER }
                            OptionRow("Intermediate", "Comfortable with regular workouts", level == Level.INTERMEDIATE) { level = Level.INTERMEDIATE }
                            OptionRow("Advanced", "Highly experienced and training consistently", level == Level.ADVANCED) { level = Level.ADVANCED }
                        }
                        2 -> {
                            Title("What's your goal?", "Sets, reps and cardio are tuned to it.")
                            OptionRow("Lose weight", "Burn fat with higher reps and cardio finishers", goal == Goal.LOSE) { goal = Goal.LOSE }
                            OptionRow("Stay fit", "Balanced strength and conditioning", goal == Goal.MAINTAIN) { goal = Goal.MAINTAIN }
                            OptionRow("Build muscle", "More sets in the 8–10 rep hypertrophy range", goal == Goal.GAIN) { goal = Goal.GAIN }
                        }
                        3 -> {
                            Title("Choose your active days", "Pick at least one. We'll balance training and rest days.")
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                DayOfWeek.entries.forEach { d ->
                                    FilterChip(d in days, { days = if (d in days) days - d else days + d },
                                        label = { Text(d.getDisplayName(TextStyle.FULL, Locale.getDefault())) })
                                }
                            }
                            Text("${days.size} days a week · suggested: 3 for beginners, 4–5 intermediate, 5–6 advanced",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        4 -> {
                            Title("Do you have any workout equipment?", "Select all equipment you have access to.")
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Equipment.entries.forEach { e ->
                                    FilterChip(e in equipment, {
                                        if (e != Equipment.BODY_ONLY) equipment = if (e in equipment) equipment - e else equipment + e
                                    }, label = { Text(e.label) })
                                }
                            }
                            Text("Bodyweight exercises are always included.", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        5 -> {
                            Title("Any injuries or conditions?", "We'll flag and skip exercises that could aggravate them.")
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(injuries.isEmpty(), { injuries = emptySet() }, label = { Text("No injuries") })
                                Injury.entries.forEach { i ->
                                    FilterChip(i in injuries, { injuries = if (i in injuries) injuries - i else injuries + i }, label = { Text(i.label) })
                                }
                            }
                            Text("This isn't medical advice. If something hurts, stop and consult a professional.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        else -> {
                            Title("Rest between sets", "Used by the rest timer during your workout.")
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(30, 45, 60, 90, 120, 180).forEach { r ->
                                    FilterChip(rest == r, { rest = r }, label = { Text(if (r < 60) "${r}s" else "${r / 60}${if (r % 60 == 30) ".5" else ""} min") })
                                }
                            }
                            Text("Tip: 60–90s for muscle, 2–3 min for heavy strength, 30–45s for fat loss.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        Button(
            onClick = { if (step < STEPS - 1) step++ else building = true },
            enabled = step != 3 || days.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(54.dp),
        ) { Text(if (step < STEPS - 1) "Continue" else "Create my plan") }
        if (step == 0 && onBack == null) {
            TextButton(onClick = { building = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Skip — use recommended settings")
            }
        }
    }
}

@Composable
private fun Title(title: String, sub: String) {
    Text(title, style = MaterialTheme.typography.headlineSmall)
    Text(sub, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun BigChoice(title: String, sub: String, icon: ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick, modifier = modifier.height(190.dp), shape = RoundedCornerShape(20.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            if (selected) Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun OptionRow(title: String, sub: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(Modifier.size(22.dp), shape = CircleShape,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant) {}
        }
    }
}

private val buildSteps = listOf(
    "Reviewing your goals...",
    "Balancing training and rest days...",
    "Matching exercises to your selected days...",
    "Flagging exercises that could aggravate injuries...",
    "Personalizing your training intensity...",
    "Almost ready! Finalizing your workout plan...",
)

@Composable
private fun BuildingPlan(onDone: () -> Unit) {
    var i by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (i < buildSteps.size) { delay(550); i++ }
        onDone()
    }
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator(Modifier.size(64.dp), strokeWidth = 6.dp)
        Spacer(Modifier.height(24.dp))
        Text("Building Your Workout Plan", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        buildSteps.forEachIndexed { n, t ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CheckCircle, null, Modifier.size(18.dp),
                    tint = if (n < i) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.width(10.dp))
                Text(t, color = if (n <= i) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
