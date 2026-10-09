package com.barathiraja.jk.ui.member

import com.barathiraja.jk.steps.rememberStepPermission
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role as SemanticsRole
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.barathiraja.jk.domain.Health
import com.barathiraja.jk.gym.Role
import com.barathiraja.jk.gym.Scoring
import com.barathiraja.jk.gym.firstNameOf
import com.barathiraja.jk.ui.GymViewModel
import com.barathiraja.jk.ui.JkViewModel
import com.barathiraja.jk.ui.Routes
import com.barathiraja.jk.ui.components.Avatar
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.ui.graphics.vector.ImageVector
import com.barathiraja.jk.ui.components.CardBox
import com.barathiraja.jk.ui.components.Heading
import com.barathiraja.jk.ui.components.JkPage
import com.barathiraja.jk.ui.components.PersonAvatar
import com.barathiraja.jk.ui.components.PlainButton
import com.barathiraja.jk.ui.components.ProgressRing
import com.barathiraja.jk.ui.theme.Jk
import com.barathiraja.jk.ui.theme.plex
import com.barathiraja.jk.gym.plural
import com.barathiraja.jk.ui.gym.AssignmentCard
import com.barathiraja.jk.ui.gym.RankCard
import com.barathiraja.jk.ui.gym.TodayWorkoutCard
import com.barathiraja.jk.ui.gym.greeting

/**
 * A gym member's Today: the coach's workout for today (or the next one), where they stand in the gym, and one card
 * of daily habits. Nothing else competes with the coach's plan.
 */
@Composable
fun MemberTodayScreen(vm: JkViewModel, gvm: GymViewModel, nav: NavHostController) {
    val me by gvm.me.collectAsStateWithLifecycle()
    val assignments by gvm.assignments.collectAsStateWithLifecycle()
    val ranking by gvm.memberRanking.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val m = me ?: return
    if (m.role != Role.MEMBER) return
    val today = gvm.today
    val mine = assignments.filter { it.memberUid == m.uid }
    val coach = gvm.trainerOf(m)
    val todays = mine.filter { it.epochDay == today }.minByOrNull { if (it.done) 1 else 0 }
    val next = mine.filter { it.epochDay > today }.minByOrNull { it.epochDay }
    val streak = Scoring.workoutStreak(mine, today)
    val rank = ranking.indexOfFirst { it.uid == m.uid }

    JkPage {
        item {
            Row(Modifier.padding(start = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(LocalDate.ofEpochDay(today).format(DateTimeFormatter.ofPattern("EEEE, d MMM")), style = plex(14.sp, FontWeight.Medium), color = Jk.RedText)
                    Text("${greeting()}, ${firstNameOf(if (gvm.testMode) m.name else profile.name.ifBlank { m.name })}",
                        style = plex(24.sp, FontWeight.Bold, line = 30.sp, tracking = (-0.4).sp), color = Jk.Ink)
                }
                MyAvatarButton(vm, gvm, 48.dp) { nav.navigate(Routes.PROFILE) }
            }
        }
        item { StreakLine(streak, todays?.done == true) }
        when {
            todays != null -> item { TodayWorkoutCard(todays, coach?.firstName.orEmpty(), gvm.restSec) { nav.navigate(Routes.assigned(todays.id)) } }
            else -> item {
                CardBox {
                    Column {
                        Text(if (mine.isEmpty()) "Your first workout is on its way" else "Rest day", style = plex(18.sp, FontWeight.Bold), color = Jk.Ink)
                        Text(when {
                            mine.isEmpty() -> "${coach?.let { "Coach ${it.firstName}" } ?: "Your coach"} will send it soon. Until then, Train has a plan JK made for you."
                            else -> "Nothing set for today. Rest helps you come back stronger: stretch, walk, drink water."
                        }, Modifier.padding(top = 4.dp), style = plex(15.sp, line = 21.sp), color = Jk.Muted)
                        if (mine.isEmpty()) PlainButton("Open Train", { nav.navigate(Routes.WORKOUTS) }, Modifier.padding(top = 12.dp))
                    }
                }
            }
        }
        // After today's is done (or on a rest day) the next one is a glance away.
        if (next != null && (todays == null || todays.done)) item {
            Heading("Next workout")
        }
        if (next != null && (todays == null || todays.done)) item {
            AssignmentCard(next, today, onClick = { nav.navigate(Routes.assigned(next.id)) })
        }
        if (rank >= 0) item {
            val s = ranking[rank]
            val ahead = ranking.getOrNull(rank - 1)
            RankCard(rank + 1, s.points, ahead?.let { it.points - s.points }, ahead?.let { gvm.person(it.uid)?.firstName }) {
                nav.navigate(Routes.RANKS)
            }
        }
        item { HabitsCard(vm, nav) }
    }
}

/** The member's photo: the one they picked on this phone, else their Google photo, else their initial. */
@Composable
internal fun MyAvatarButton(vm: JkViewModel, gvm: GymViewModel, size: Dp, onClick: () -> Unit) {
    val local by vm.avatar.collectAsStateWithLifecycle()
    val me by gvm.me.collectAsStateWithLifecycle()
    val p = me ?: return
    if (local != null) Avatar(local, p.name, size, onClick)
    else Box(Modifier.size(maxOf(size, 48.dp)).clip(CircleShape)
        .clickable(onClickLabel = "Open your profile", role = SemanticsRole.Button, onClick = onClick)
        .semantics { contentDescription = "Your profile" }, contentAlignment = Alignment.Center) { PersonAvatar(p.photoUrl, p.name, size) }
}

/** Workouts done in a row, counted in workouts (not days), so the coach's rest days never break it. */
@Composable
private fun StreakLine(streak: Int, doneToday: Boolean) {
    Row(Modifier.padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.LocalFireDepartment, null, Modifier.size(22.dp), tint = if (streak > 0) Jk.RedText else Jk.Muted)
        Spacer(Modifier.width(6.dp))
        Text(when {
            streak == 0 -> "Finish a workout to start a streak"
            doneToday -> "${plural(streak, "workout")} in a row. Nice work today!"
            else -> "${plural(streak, "workout")} in a row. Keep it going"
        }, style = plex(15.sp, FontWeight.SemiBold), color = Jk.Ink)
    }
}

/** Steps, water and food for today in one card; water has its own +/− so it's logged in one tap. */
@Composable
private fun HabitsCard(vm: JkViewModel, nav: NavHostController) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val steps by vm.stepsToday.collectAsStateWithLifecycle()
    val water by vm.waterToday.collectAsStateWithLifecycle()
    val food by vm.foodToday.collectAsStateWithLifecycle()
    val sessions by vm.weekSessions.collectAsStateWithLifecycle()
    val stepPerm = rememberStepPermission { vm.startSteps() }
    val today by vm.currentDay.collectAsStateWithLifecycle()
    val burned = sessions.filter { it.epochDay == today }.sumOf { it.calories } + Health.stepCalories(steps, profile.weightKg)
    val eaten = food.sumOf { it.kcal }
    val target = Health.targetCalories(profile)

    CardBox(padding = 16.dp) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Today's habits", Modifier.weight(1f), style = plex(16.sp, FontWeight.Bold), color = Jk.Ink)
                Text("$burned kcal burned", style = plex(13.sp, FontWeight.Medium), color = Jk.Muted)
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Top) {
                when {
                    !vm.stepSensorAvailable -> Habit("–", "steps", "No step sensor", 0f, onClick = null)
                    !stepPerm.granted -> Habit("Off", "steps", "Tap to count", 0f, onClick = stepPerm.ask)
                    else -> Habit("%,d".format(steps), "steps", "of %,d".format(settings.stepGoal), steps / settings.stepGoal.toFloat()) {
                        nav.navigate(Routes.TRACK)
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Habit("$water", "glasses", "of ${settings.waterGoalGlasses}", water / settings.waterGoalGlasses.toFloat(), onClick = null)
                    Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RoundStep(Icons.Filled.Remove, "Remove a glass of water", enabled = water > 0) { vm.addWater(-1) }
                        RoundStep(Icons.Filled.Add, "Add a glass of water") { vm.addWater(1) }
                    }
                }
                Habit("$eaten", "kcal eaten", "of $target", eaten / target.toFloat().coerceAtLeast(1f)) { nav.navigate(Routes.DIET) }
            }
        }
    }
}

/** One habit: a ring with the number in it, its unit and goal under it. Full rings turn yellow. */
@Composable
private fun Habit(value: String, unit: String, goal: String, fraction: Float, onClick: (() -> Unit)?) {
    Column(
        Modifier.width(96.dp).clip(RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val full = fraction >= 1f
        ProgressRing(fraction.coerceIn(0f, 1f), 74.dp, 7.dp, Jk.Well, if (full) Jk.Yellow else Jk.Ink, key = unit) {
            Text(value, style = plex(17.sp, FontWeight.Bold), color = Jk.Ink, maxLines = 1)
        }
        Text(unit, Modifier.padding(top = 6.dp), style = plex(14.sp, FontWeight.SemiBold), color = Jk.Ink, textAlign = TextAlign.Center)
        Text(goal, style = plex(13.sp), color = Jk.Muted, textAlign = TextAlign.Center)
    }
}

@Composable
private fun RoundStep(icon: ImageVector, label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(onClick = onClick, enabled = enabled, shape = CircleShape, color = if (enabled) Jk.Well else Jk.Card,
        modifier = Modifier.size(40.dp).semantics { contentDescription = label }) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(18.dp), tint = if (enabled) Jk.Ink else Jk.Line) }
    }
}
