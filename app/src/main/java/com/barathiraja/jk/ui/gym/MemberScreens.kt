package com.barathiraja.jk.ui.gym

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.barathiraja.jk.data.ExerciseRepo
import com.barathiraja.jk.data.SetSpec
import com.barathiraja.jk.gym.AssignedExercise
import com.barathiraja.jk.gym.Assignment
import com.barathiraja.jk.gym.Role
import com.barathiraja.jk.ui.GymViewModel
import com.barathiraja.jk.ui.Routes
import com.barathiraja.jk.ui.components.BackScreen
import com.barathiraja.jk.ui.components.Confetti
import com.barathiraja.jk.ui.components.ExerciseDemo
import com.barathiraja.jk.ui.components.Ring
import com.barathiraja.jk.ui.components.TabScreen
import com.barathiraja.jk.ui.screens.SetRow
import com.barathiraja.jk.ui.theme.Good
import kotlinx.coroutines.delay

/** Top of a member's Today screen: the workout their coach assigned plus their rank. */
@Composable
fun AssignedTodayCard(gvm: GymViewModel, nav: NavHostController) {
    val a by gvm.myToday.collectAsStateWithLifecycle()
    val me by gvm.me.collectAsStateWithLifecycle()
    val ranking by gvm.memberRanking.collectAsStateWithLifecycle()
    val m = me ?: return
    if (m.role != Role.MEMBER) return
    val coach = gvm.trainerOf(m)
    val rank = ranking.indexOfFirst { it.uid == m.uid }
    Column {
        a?.let { w ->
            TodayWorkoutHero(w, coach?.firstName.orEmpty()) { nav.navigate(Routes.assigned(w.id)) }
            Spacer(Modifier.height(12.dp))
        }
        if (rank >= 0) {
            val s = ranking[rank]
            val ahead = ranking.getOrNull(rank - 1)
            OwnerCardBox(onClick = { nav.navigate(Routes.RANKS) }, onClickLabel = "Open leaderboard", padding = 16.dp) {
                Column {
                    Text(if (s.points == 0) "Gym leaderboard" else "You're #${rank + 1} in the gym this month",
                        style = plex(16.sp, FontWeight.SemiBold), color = Owner.Ink)
                    Text(if (s.points == 0) "Finish a workout to earn your first points"
                        else "${s.points} pts" + (ahead?.let { " · ${it.points - s.points} to #$rank" } ?: " · you're leading!"),
                        style = plex(15.sp, line = 21.sp), color = Owner.Muted)
                }
            }
        }
    }
}

/** The charcoal card with today's assigned workout, its progress and the button to start it. */
@Composable
private fun TodayWorkoutHero(w: Assignment, coachName: String, onOpen: () -> Unit) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Owner.Hero).padding(20.dp)) {
        Column {
            val from = if (coachName.isBlank()) "From your coach" else "From coach $coachName"
            Text(if (w.done) "Done · $from" else from, style = plex(14.sp, FontWeight.Medium), color = Owner.OnDarkSoft)
            Text(w.title, style = plex(20.sp, FontWeight.Bold, line = 26.sp), color = Color.White)
            Text("${plural(w.exercises.size, "exercise")} · ${plural(w.setsTotal, "set")}", style = plex(15.sp), color = Owner.OnDarkSoft)
            Spacer(Modifier.height(12.dp))
            OwnerBar(if (w.setsTotal == 0) 0f else w.setsDone / w.setsTotal.toFloat(), Owner.DarkTrack, Owner.Yellow, "today-${w.id}")
            Text("${w.exercisesDone}/${w.exercises.size} exercises done", Modifier.padding(top = 6.dp), style = plex(15.sp), color = Color.White)
            if (w.trainerNote.isNotBlank()) Text("Coach: ${w.trainerNote}", Modifier.padding(top = 6.dp), style = plex(15.sp, line = 21.sp), color = Color.White)
            Spacer(Modifier.height(14.dp))
            RedButton(when { w.done -> "View workout"; w.setsDone > 0 -> "Continue workout"; else -> "Start workout" }, onOpen, icon = Icons.Filled.PlayArrow)
        }
    }
}

/** Member's Gym tab: coach, this week's and past workouts, awards. */
@Composable
fun MemberGymScreen(gvm: GymViewModel, nav: NavHostController) {
    val me by gvm.me.collectAsStateWithLifecycle()
    val gym by gvm.gym.collectAsStateWithLifecycle()
    val assignments by gvm.assignments.collectAsStateWithLifecycle()
    val scores by gvm.monthScores.collectAsStateWithLifecycle()
    val awards by gvm.awards.collectAsStateWithLifecycle()
    val m = me ?: return
    val today = gvm.today
    val mine = assignments.filter { it.memberUid == m.uid }
    val upcoming = mine.filter { it.epochDay >= today }.sortedBy { it.epochDay }
    val past = mine.filter { it.epochDay < today }.sortedByDescending { it.epochDay }
    val s = scores[m.uid]
    val coach = gvm.trainerOf(m)
    val myAwards = awards.flatMap { a -> a.winners.filterValues { it == m.uid }.keys.map { a.month to it } }

    TabScreen(gym?.name ?: "My gym", subtitle = "Gym", action = { ProfileButton(m) { nav.navigate(Routes.PROFILE) } }) {
        coach?.let { c ->
            item { OwnerCardBox(padding = 16.dp) { MemberLine(c, sub = { Text("Your coach", style = plex(14.sp), color = Owner.Muted) }) } }
        }
        item {
            OwnerCardBox(onClick = { nav.navigate(Routes.RANKS) }, onClickLabel = "Open leaderboard", padding = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Ring(s?.rate ?: 0f, Good, size = 72.dp, stroke = 7.dp) {
                        Text(pct(s?.completed ?: 0, s?.due ?: 0), style = plex(15.sp, FontWeight.SemiBold), color = Owner.Ink)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("This month", style = plex(14.sp, FontWeight.Medium), color = Owner.Muted)
                        Text("${s?.completed ?: 0} of ${s?.due ?: 0} workouts done", style = plex(16.sp, FontWeight.SemiBold), color = Owner.Ink)
                        Text("${s?.points ?: 0} points · see leaderboard →", style = plex(15.sp), color = Owner.Muted)
                    }
                }
            }
        }
        if (myAwards.isNotEmpty()) item {
            OwnerCardBox(padding = 16.dp) {
                Column {
                    Text("My awards", style = plex(16.sp, FontWeight.SemiBold), color = Owner.Ink)
                    myAwards.forEach { (month, award) ->
                        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            AwardBadge(award.look(), 36.dp)
                            Spacer(Modifier.width(10.dp))
                            Text("${award.label} · ${monthLabel(month)}", style = plex(15.sp, FontWeight.SemiBold), color = Owner.Ink)
                        }
                    }
                }
            }
        }
        item { OwnerHeading("Coming up") }
        if (upcoming.isEmpty()) item {
            Text("Nothing assigned yet. Your coach's workouts will show up here.", style = plex(15.sp, line = 21.sp), color = Owner.Muted)
        }
        items(upcoming, key = { it.id }) { a -> AssignmentCard(a, today, onClick = { nav.navigate(Routes.assigned(a.id)) }) }
        if (past.isNotEmpty()) item { OwnerHeading("History") }
        items(past, key = { it.id }) { a -> AssignmentCard(a, today) }
    }
}

/** Member does an assigned workout: tick sets, adjust reps/weight, rest timer between sets. */
@Composable
fun AssignedSessionScreen(id: String, gvm: GymViewModel, nav: NavHostController) {
    val assignments by gvm.assignments.collectAsStateWithLifecycle()
    val me by gvm.me.collectAsStateWithLifecycle()
    val remote = assignments.firstOrNull { it.id == id }
    // Local copy so taps feel instant; Firestore catches up (offline too).
    var a by remember(id) { mutableStateOf(remote) }
    LaunchedEffect(remote) { a = remote }
    val w = a ?: run {
        // No workouts at all means they're still loading, not that this one was removed.
        if (assignments.isEmpty()) GymLoading()
        else BackScreen("Workout", onBack = { nav.popBackStack() }) { item { Text("This workout was removed by your coach.", style = plex(15.sp)) } }
        return
    }
    val mine = w.memberUid == me?.uid
    val today = gvm.today
    var editing by rememberSaveable { mutableStateOf(false) }
    var resting by rememberSaveable { mutableIntStateOf(0) }
    var note by rememberSaveable(id) { mutableStateOf(w.memberNote) }
    val focus = LocalFocusManager.current
    val restSec = gvm.restSec
    val tone = remember { runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 80) }.getOrNull() }
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false; tone?.release() }
    }
    LaunchedEffect(resting > 0) {
        while (resting > 0) {
            delay(1000)
            resting--
            if (resting in 1..3) tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
            if (resting == 0) tone?.startTone(ToneGenerator.TONE_PROP_BEEP2, 300)
        }
    }

    fun save(updated: Assignment) { a = updated; gvm.saveProgress(updated) }
    val canEdit = mine && w.epochDay <= today

    Box(Modifier.fillMaxSize()) {
        BackScreen(w.title, onBack = { nav.popBackStack() }) {
            item {
                Column {
                    Text("${dayLabel(w.epochDay, today)} · ${w.exercisesDone}/${w.exercises.size} exercises · ${w.setsDone}/${w.setsTotal} sets",
                        style = plex(15.sp, line = 21.sp), color = Owner.Muted)
                    if (w.trainerNote.isNotBlank()) Text("Coach: ${w.trainerNote}", Modifier.padding(top = 6.dp),
                        style = plex(15.sp, FontWeight.SemiBold, line = 21.sp), color = Owner.Ink)
                    if (w.epochDay > today) Text("You can start this on ${dayLabel(w.epochDay, today)}.", Modifier.padding(top = 6.dp),
                        style = plex(15.sp), color = Owner.Ink)
                    if (canEdit) PlainButton(if (editing) "Done editing" else "Change reps / weight", { editing = !editing }, Modifier.padding(top = 10.dp))
                }
            }
            w.exercises.forEachIndexed { ei, e ->
                item(key = "ex$ei") {
                    SessionExerciseCard(e, editing, canEdit, onOpen = { nav.navigate(Routes.exercise(it)) },
                        onChange = { sets -> save(w.copy(exercises = w.exercises.toMutableList().also { it[ei] = e.copy(sets = sets) })) },
                        onTicked = { if (w.setsDone + 1 < w.setsTotal) resting = restSec })
                }
            }
            if (canEdit) item {
                Column {
                    OutlinedTextField(note, { note = it.take(300) }, Modifier.fillMaxWidth(), label = { Text("Note for your coach (optional)") })
                    if (note.trim() != w.memberNote) PlainButton("Send note", { focus.clearFocus(); note = note.trim(); save(w.copy(memberNote = note)) },
                        Modifier.padding(top = 8.dp))
                }
            }
            if (canEdit && !w.done) item {
                PlainButton("Mark everything done", {
                    save(w.copy(exercises = w.exercises.map { e -> e.copy(sets = e.sets.map { it.copy(done = true) }) }))
                }, Modifier.fillMaxWidth())
            }
            if (w.done) item { WorkoutDoneBanner() }
            item { Spacer(Modifier.height(96.dp)) }
        }

        AnimatedVisibility(resting > 0, modifier = Modifier.align(Alignment.BottomCenter)) {
            RestBar(resting, restSec, onAdd = { resting += 15 }, onSkip = { resting = 0 })
        }
        if (w.done && System.currentTimeMillis() - (w.completedAt ?: 0) < 10 * 60_000) Confetti()
    }
}

/**
 * One exercise in an assigned workout: its picture and name (tap for how-to), then each set. Ticking a set
 * reports the new sets through [onChange] and calls [onTicked] when a set was just completed, to start the rest timer.
 */
@Composable
private fun SessionExerciseCard(
    e: AssignedExercise, editing: Boolean, canEdit: Boolean,
    onOpen: (String) -> Unit, onChange: (List<SetSpec>) -> Unit, onTicked: () -> Unit,
) {
    val ex = ExerciseRepo.get(e.exerciseId)
    val name = ex?.name ?: e.exerciseId
    OwnerCardBox(onClick = ex?.let { { onOpen(it.id) } }, onClickLabel = "How to do $name", padding = 16.dp) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ex?.let { ExerciseDemo(it, Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)), animate = false) }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(name, style = plex(16.sp, FontWeight.SemiBold), color = Owner.Ink)
                    Text(if (e.done) "Done" + (ex?.let { " · ${it.muscles}" } ?: "") else ex?.muscles.orEmpty(),
                        style = plex(13.sp), color = Owner.Muted)
                }
            }
            if (e.cue.isNotBlank()) CueText(e.cue)
            val step = if (ex?.equipment == "dumbbell" || ex?.equipment == "kettlebells") 1f else 2.5f
            e.sets.forEachIndexed { si, s ->
                Spacer(Modifier.height(6.dp))
                val replace = { ns: SetSpec -> onChange(e.sets.toMutableList().also { it[si] = ns }) }
                if (editing) SetEditorRow(si, s, step, onChange = replace, onDelete = null)
                else SetRow(si, s, false, onToggle = {
                    if (!canEdit) return@SetRow
                    replace(s.copy(done = !s.done))
                    if (!s.done) onTicked()
                }, onChange = replace, onDelete = {}, step = step)
            }
        }
    }
}

/** Shown under a finished workout. Yellow, because it's a reward. */
@Composable
private fun WorkoutDoneBanner() {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Owner.Yellow).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Workout complete!", style = plex(18.sp, FontWeight.Bold), color = Owner.Black)
        Text("Your coach can see it now.", style = plex(15.sp), color = Owner.Black)
    }
}

/** The rest countdown over the bottom of the workout: charcoal bar, yellow ring, white text (15:1), yellow Skip (10:1). */
@Composable
private fun RestBar(resting: Int, restSec: Int, onAdd: () -> Unit, onSkip: () -> Unit) {
    val surface = MaterialTheme.colorScheme.inverseSurface
    val onSurface = MaterialTheme.colorScheme.inverseOnSurface
    Row(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).clip(RoundedCornerShape(24.dp)).background(surface).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Ring(resting / restSec.toFloat(), Owner.Yellow, size = 64.dp, stroke = 6.dp, track = Owner.DarkTrack) {
            Text("$resting", style = plex(17.sp, FontWeight.SemiBold), color = onSurface)
        }
        Spacer(Modifier.width(10.dp))
        Text("Rest", Modifier.weight(1f), style = plex(17.sp, FontWeight.SemiBold), color = onSurface)
        TextButton(onClick = onAdd) { Text("+15s", style = plex(15.sp, FontWeight.SemiBold), color = onSurface) }
        TextButton(onClick = onSkip) { Text("Skip", style = plex(15.sp, FontWeight.SemiBold), color = Owner.Yellow) }
    }
}
