package com.barathiraja.jk.ui.screens

import com.barathiraja.jk.ui.components.Avatar
import com.barathiraja.jk.ui.components.TabScreen
import com.barathiraja.jk.ui.theme.HeroFill
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.barathiraja.jk.data.BodyPart
import com.barathiraja.jk.data.ExerciseRepo
import com.barathiraja.jk.domain.TrainingEngine
import com.barathiraja.jk.ui.TrainingViewModel
import com.barathiraja.jk.ui.components.Ring
import com.barathiraja.jk.data.Articles
import com.barathiraja.jk.data.Catalog
import com.barathiraja.jk.domain.Health
import com.barathiraja.jk.ui.JkViewModel
import com.barathiraja.jk.ui.Routes
import com.barathiraja.jk.ui.components.JkCard
import com.barathiraja.jk.ui.components.SectionTitle
import com.barathiraja.jk.ui.components.StatRing
import com.barathiraja.jk.ui.theme.Accent
import com.barathiraja.jk.ui.theme.Good
import com.barathiraja.jk.ui.theme.Watch
import com.barathiraja.jk.ui.theme.Calm
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun TodayScreen(vm: JkViewModel, tvm: TrainingViewModel, nav: NavHostController, gvm: com.barathiraja.jk.ui.GymViewModel? = null) {
    // A workout from the member's coach replaces the auto-generated plan for that day.
    val assigned = gvm?.myToday?.collectAsStateWithLifecycle()?.value
    val profile by vm.profile.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val steps by vm.stepsToday.collectAsStateWithLifecycle()
    val water by vm.waterToday.collectAsStateWithLifecycle()
    val weekSessions by vm.weekSessions.collectAsStateWithLifecycle()
    val streak by vm.streak.collectAsStateWithLifecycle()
    val avatar by vm.avatar.collectAsStateWithLifecycle()
    val food by vm.foodToday.collectAsStateWithLifecycle()
    val challengeDays by vm.challengeDays.collectAsStateWithLifecycle()
    val tPrefs by tvm.prefs.collectAsStateWithLifecycle()
    val planDay by tvm.todayDay.collectAsStateWithLifecycle()
    val planItems by tvm.todayItems.collectAsStateWithLifecycle()
    val weekPlans by tvm.weekDays.collectAsStateWithLifecycle()
    val weekItems by tvm.weekItems.collectAsStateWithLifecycle()
    val split by tvm.split.collectAsStateWithLifecycle()
    var showPlanDialog by remember { mutableStateOf(false) }

    val todayDay = LocalDate.now().toEpochDay()
    val doneToday = weekSessions.filter { it.epochDay == todayDay }
    val workout = Catalog.todayFor(profile.place, tPrefs.level, todayDay)
    val planParts = planDay?.parts?.let(BodyPart::parseList) ?: split[java.time.LocalDate.now().dayOfWeek].orEmpty()
    val planSec = planItems.sumOf { TrainingEngine.estimateSec(it.setList, tPrefs.restSec) }
    val planKcal = planItems.sumOf { Health.caloriesBurned(ExerciseRepo.get(it.exerciseId)?.met ?: 5f, profile.weightKg, TrainingEngine.estimateSec(it.setList, tPrefs.restSec)) }
    val planDone = planItems.count { it.done }

    // "Today's plan" popup once per day, like a coach greeting you.
    LaunchedEffect(planItems.isNotEmpty()) {
        if (assigned == null && planItems.isNotEmpty() && planDone < planItems.size && tvm.planDialogDay != todayDay) {
            tvm.planDialogDay = todayDay
            showPlanDialog = true
        }
    }
    if (showPlanDialog) {
        TodayPlanDialog(profile.name, planParts, planItems.size, (planSec + 59) / 60, planKcal,
            onStart = { showPlanDialog = false; tvm.select(todayDay); nav.navigate(Routes.WORKOUTS) },
            onDismiss = { showPlanDialog = false })
    }
    val burned = doneToday.sumOf { it.calories } + Health.stepCalories(steps, profile.weightKg)
    val target = Health.targetCalories(profile)

    val greeting = when (LocalTime.now().hour) { in 5..11 -> "Good morning"; in 12..16 -> "Good afternoon"; else -> "Good evening" }
    val date = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMM"))

    TabScreen(
        title = "$greeting, ${profile.name.split(' ').firstOrNull { it.trimEnd('.').length > 1 } ?: profile.name}", subtitle = date,
        action = { Avatar(avatar, profile.name, 44.dp) { nav.navigate(Routes.PROFILE) } },
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocalFireDepartment, null, tint = Accent)
                Spacer(Modifier.width(6.dp))
                Text(
                    if (streak > 0) "$streak-day streak — keep it alive!" else "Start a streak today",
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }

        if (gvm != null) item { com.barathiraja.jk.ui.gym.AssignedTodayCard(gvm, nav) }

        // Daily fitness report: this week's training days with completion rings.
        if (tPrefs.setupDone && assigned == null) {
            item {
                JkCard(Modifier.fillMaxWidth(), onClick = { nav.navigate(Routes.WORKOUTS) }) {
                    Text("Daily Fitness Report", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        (tvm.weekStart..tvm.weekStart + 6).forEach { d ->
                            val date = java.time.LocalDate.ofEpochDay(d)
                            val its = weekItems.filter { it.epochDay == d }
                            val rest = split[date.dayOfWeek].isNullOrEmpty()
                            val prog = if (its.isEmpty()) 0f else its.count { it.done } / its.size.toFloat()
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault()),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (d == todayDay) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(4.dp))
                                Ring(if (weekPlans.any { it.epochDay == d && it.completedAt != null }) 1f else prog,
                                    if (rest) MaterialTheme.colorScheme.outline else Good, size = 36.dp, stroke = 4.dp) {
                                    Text(if (rest) "R" else "${date.dayOfMonth}", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (assigned == null) item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(HeroFill)
                    .padding(20.dp),
            ) {
                Column {
                    when {
                        !tPrefs.setupDone -> {
                            Text("PERSONAL PLAN", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.8f))
                            Text("Know exactly what to train", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                            Text("Answer 7 quick questions and JK builds your weekly split.", color = Color.White.copy(alpha = 0.85f))
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { nav.navigate(Routes.TRAIN_SETUP) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = HeroFill)) { Text("Create My Plan") }
                        }
                        planParts.isEmpty() -> {
                            Text("TODAY", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.8f))
                            Text("Rest Day 😴", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                            Text("Recovery is part of the plan. Try a light stretch or a walk.", color = Color.White.copy(alpha = 0.85f))
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { nav.navigate(Routes.workout("home_mobility")) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = HeroFill)) { Text("Recovery stretch") }
                        }
                        else -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(if (planDone == planItems.size && planItems.isNotEmpty()) "COMPLETED ✓" else "TODAY'S WORKOUT",
                                    Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.8f))
                                Text("$planDone / ${planItems.size}", color = Color.White, style = MaterialTheme.typography.titleMedium)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(TrainingEngine.title(planParts), style = MaterialTheme.typography.headlineSmall, color = Color.White)
                            Text("${(planSec + 59) / 60} min · ${planItems.size} exercises · ${tPrefs.level.label}",
                                color = Color.White.copy(alpha = 0.85f))
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { tvm.select(todayDay); nav.navigate(Routes.WORKOUTS) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = HeroFill),
                            ) {
                                Icon(Icons.Filled.PlayArrow, null)
                                Spacer(Modifier.width(4.dp))
                                Text(if (planDone == 0) "Start Workout" else if (planDone < planItems.size) "Continue" else "View workout")
                            }
                        }
                    }
                }
            }
        }

        item {
            JkCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatRing("Steps", "%,d".format(steps), "/ ${settings.stepGoal / 1000}k",
                        steps / settings.stepGoal.toFloat(), Good)
                    StatRing("Water", "$water", "/ ${settings.waterGoalGlasses} gl",
                        water / settings.waterGoalGlasses.toFloat(), Accent)
                    StatRing("Burned", "$burned", "kcal", burned / 500f, Accent)
                }
            }
        }

        item { SectionTitle("Quick actions") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickAction("Water", Icons.Outlined.WaterDrop, Accent, Modifier.weight(1f)) { vm.addWater(1) }
                QuickAction("Diet", Icons.Outlined.Restaurant, Accent, Modifier.weight(1f)) { nav.navigate(Routes.DIET) }
                QuickAction("Meditate", Icons.Outlined.SelfImprovement, Calm, Modifier.weight(1f)) { nav.navigate(Routes.MEDITATE) }
                QuickAction("Fast", Icons.Outlined.Timer, Watch, Modifier.weight(1f)) { nav.navigate(Routes.FASTING) }
            }
        }

        // Active challenge (most progressed, not finished) or a nudge to start one.
        item {
            val active = Catalog.challenges
                .map { c -> c to (challengeDays[c.id]?.size ?: 0) }
                .filter { (c, n) -> n > 0 && n < (1..c.days).count { c.plan(it) != null } }
                .maxByOrNull { it.second }
            JkCard(Modifier.fillMaxWidth(), onClick = {
                nav.navigate(if (active != null) Routes.challenge(active.first.id) else Routes.challenge("transform30"))
            }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.EmojiEvents, null, tint = Watch, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        if (active != null) {
                            val (c, n) = active
                            Text(c.title, style = MaterialTheme.typography.titleMedium)
                            Text("$n days done · keep going!", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Text("Take the 30-Day Transformation", style = MaterialTheme.typography.titleMedium)
                            Text("A guided month of training. Tap to start day 1.", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        item {
            val a = Articles.all[(todayDay % Articles.all.size).toInt()]
            JkCard(Modifier.fillMaxWidth(), onClick = { nav.navigate(Routes.article(a.id)) }) {
                Text("TODAY'S READ · ${a.minutes} MIN", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(a.title, style = MaterialTheme.typography.titleMedium)
            }
        }

        item {
            val m = Health.macros(profile)
            JkCard(Modifier.fillMaxWidth(), onClick = { nav.navigate(Routes.DIET) }) {
                Text("Daily fuel target", style = MaterialTheme.typography.titleMedium)
                Text("${profile.goal.label} · ${food.sumOf { it.kcal }} kcal logged today",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Macro("$target", "kcal", Accent)
                    Macro("${m.proteinG}g", "protein", Calm)
                    Macro("${m.carbsG}g", "carbs", Watch)
                    Macro("${m.fatG}g", "fat", Accent)
                }
            }
        }
    }
}

@Composable
private fun QuickAction(label: String, icon: ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick, modifier = modifier, shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = color) }
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
        }
    }
}

@Composable
private fun Macro(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}


/** Morning greeting with today's plan at a glance (shown once a day). */
@Composable
fun TodayPlanDialog(name: String, parts: List<BodyPart>, exercises: Int, minutes: Int, kcal: Int, onStart: () -> Unit, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        androidx.compose.material3.Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text("● Today's plan", style = MaterialTheme.typography.labelLarge, color = Good)
                        Text("Ready to train, ${name.substringBefore(' ').uppercase()}! 💪", style = MaterialTheme.typography.headlineSmall)
                        Text("Here's what's lined up for you today. Let's make it count.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    androidx.compose.material3.IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Close") }
                }
                Spacer(Modifier.height(12.dp))
                Text("Today at a glance", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Glance("$exercises", "Exercises", Modifier.weight(1f))
                    Glance("$minutes", "mins", Modifier.weight(1f))
                    Glance("$kcal", "kcal", Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                Text("Workout focus", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                    com.barathiraja.jk.ui.components.BodyMap(parts, height = 120.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(TrainingEngine.title(parts).uppercase(), style = MaterialTheme.typography.titleSmall)
                        Text("Build strength and definition in your ${parts.joinToString(", ") { it.label.lowercase() }}.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(54.dp)) {
                    Icon(Icons.Filled.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Start Workout")
                }
            }
        }
    }
}

@Composable
private fun Glance(value: String, label: String, modifier: Modifier) {
    androidx.compose.material3.Surface(modifier, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
