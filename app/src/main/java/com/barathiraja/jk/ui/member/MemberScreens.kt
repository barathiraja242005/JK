package com.barathiraja.jk.ui.member

import com.barathiraja.jk.ui.gym.look

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.barathiraja.jk.data.ExerciseRepo
import com.barathiraja.jk.data.weightStep
import com.barathiraja.jk.data.SetSpec
import com.barathiraja.jk.gym.Assignment
import com.barathiraja.jk.ui.GymViewModel
import com.barathiraja.jk.ui.Routes
import com.barathiraja.jk.ui.components.BackScreen
import com.barathiraja.jk.ui.components.Confetti
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.text.style.TextOverflow
import com.barathiraja.jk.gym.Award
import com.barathiraja.jk.gym.Person
import com.barathiraja.jk.gym.Scoring
import com.barathiraja.jk.ui.JkViewModel
import com.barathiraja.jk.ui.components.CardBox
import com.barathiraja.jk.ui.components.Heading
import com.barathiraja.jk.ui.components.JkPage
import com.barathiraja.jk.ui.components.PlainButton
import com.barathiraja.jk.ui.components.ProgressRing
import com.barathiraja.jk.ui.theme.Jk
import com.barathiraja.jk.ui.theme.plex
import com.barathiraja.jk.ui.gym.AwardBadge
import com.barathiraja.jk.ui.gym.ExerciseCard
import com.barathiraja.jk.ui.gym.GymLoading
import com.barathiraja.jk.ui.gym.MemberLine
import com.barathiraja.jk.ui.gym.RestBar
import com.barathiraja.jk.ui.gym.SessionSummary
import com.barathiraja.jk.ui.gym.SetEditorRow
import com.barathiraja.jk.ui.gym.awardLook
import com.barathiraja.jk.ui.gym.dayLabel
import com.barathiraja.jk.ui.gym.leaderboard
import com.barathiraja.jk.ui.gym.monthLabel
import com.barathiraja.jk.ui.gym.pct
import com.barathiraja.jk.ui.gym.rememberWorkoutClock

/**
 * Member's Gym tab: the people and the competition. Their coach (and the coach's latest note), how this month is
 * going and where they stand, and the awards they can win. Their workouts live on Train.
 */
@Composable
fun MemberGymScreen(gvm: GymViewModel, vm: JkViewModel, nav: NavHostController) {
    val me by gvm.me.collectAsStateWithLifecycle()
    val gym by gvm.gym.collectAsStateWithLifecycle()
    val assignments by gvm.assignments.collectAsStateWithLifecycle()
    val scores by gvm.monthScores.collectAsStateWithLifecycle()
    val ranking by gvm.memberRanking.collectAsStateWithLifecycle()
    val awards by gvm.awards.collectAsStateWithLifecycle()
    val given by gvm.givenAwards.collectAsStateWithLifecycle()
    val m = me ?: return
    val mine = assignments.filter { it.memberUid == m.uid }
    val s = scores[m.uid]
    val coach = gvm.trainerOf(m)
    val rank = ranking.indexOfFirst { it.uid == m.uid }
    val myAwards = awards.flatMap { a -> a.winners.filterValues { it == m.uid }.keys.map { a.month to it } }
    // Awards the owner handed out by hand count too, newest first.
    val myGiven = given.filter { it.uid == m.uid }
    // Only workouts already open can be replied on, so the latest note comes from today or before.
    val lastNote = mine.filter { it.trainerNote.isNotBlank() && it.epochDay <= gvm.today }.maxByOrNull { it.epochDay }

    JkPage {
        item {
            Row(Modifier.padding(start = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Your gym", style = plex(14.sp, FontWeight.Medium), color = Jk.RedText)
                    Text(gym?.name ?: "My gym", style = plex(24.sp, FontWeight.Bold, line = 30.sp, tracking = (-0.4).sp), color = Jk.Ink)
                }
                MyAvatarButton(vm, gvm, 48.dp) { nav.navigate(Routes.PROFILE) }
            }
        }
        item { CoachCard(coach, lastNote, gvm.today) { a -> nav.navigate(Routes.assigned(a.id)) } }
        item {
            MonthCard(s, rank, ranking.size) { gvm.showRanksTab(0); nav.navigate(Routes.RANKS) }
        }
        item { Heading("Awards", "Winners are picked on the 1st of every month from the workouts you finish.") }
        if (myAwards.isNotEmpty() || myGiven.isNotEmpty()) item {
            CardBox(padding = 16.dp) {
                Column {
                    Text("You've won", style = plex(16.sp, FontWeight.Bold), color = Jk.Ink)
                    myGiven.forEach { a ->
                        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            AwardBadge(awardLook(a.emoji, a.title), 36.dp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("${a.title} · ${monthLabel(a.month)}", style = plex(15.sp, FontWeight.SemiBold), color = Jk.Ink)
                                if (a.note.isNotBlank()) Text(a.note, style = plex(14.sp, line = 18.sp), color = Jk.Muted)
                            }
                        }
                    }
                    myAwards.forEach { (month, award) ->
                        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            AwardBadge(award.look(), 36.dp)
                            Spacer(Modifier.width(10.dp))
                            Text("${award.label} · ${monthLabel(month)}", style = plex(15.sp, FontWeight.SemiBold), color = Jk.Ink)
                        }
                    }
                }
            }
        }
        item { AwardGuide { gvm.showRanksTab(2); nav.navigate(Routes.RANKS) } }
    }
}

/** The member's coach, with the last note they wrote on a workout (tap to open that workout and reply). */
@Composable
private fun CoachCard(coach: Person?, lastNote: Assignment?, today: Long, onOpen: (Assignment) -> Unit) {
    CardBox(padding = 16.dp, onClick = lastNote?.let { { onOpen(it) } }, onClickLabel = "Open the workout with this note") {
        Column {
            if (coach == null) {
                Text("No coach yet", style = plex(16.sp, FontWeight.Bold), color = Jk.Ink)
                Text("The gym will give you a coach soon. Until then, Train has a plan JK made for you.",
                    Modifier.padding(top = 4.dp), style = plex(15.sp, line = 21.sp), color = Jk.Muted)
                return@Column
            }
            MemberLine(coach, sub = { Text("Your coach", style = plex(14.sp), color = Jk.Muted) })
            if (lastNote != null) {
                Column(Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Jk.Well).padding(12.dp)) {
                    Text("Latest note · ${lastNote.title} · ${dayLabel(lastNote.epochDay, today)}", style = plex(12.sp, FontWeight.SemiBold), color = Jk.Muted)
                    Text(lastNote.trainerNote, Modifier.padding(top = 4.dp), style = plex(15.sp, line = 21.sp), color = Jk.Ink, maxLines = 4,
                        overflow = TextOverflow.Ellipsis)
                    Text("Reply on that workout →", Modifier.padding(top = 6.dp), style = plex(13.sp, FontWeight.SemiBold), color = Jk.RedText)
                }
            } else Text("Notes from ${coach.firstName} on your workouts will show here.", Modifier.padding(top = 10.dp),
                style = plex(14.sp, line = 19.sp), color = Jk.Muted)
        }
    }
}

/** This month in one card: share of workouts done (yellow ring), points and place, and what was lifted. */
@Composable
private fun MonthCard(s: Scoring.MemberScore?, rank: Int, of: Int, onClick: () -> Unit) {
    CardBox(onClick = onClick, onClickLabel = "Open leaderboard", padding = 16.dp) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProgressRing(s?.rate ?: 0f, 72.dp, 7.dp, Jk.Well, Jk.Yellow, key = "month") {
                    Text(pct(s?.completed ?: 0, s?.due ?: 0), style = plex(16.sp, FontWeight.Bold), color = Jk.Ink)
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("This month", style = plex(14.sp, FontWeight.Medium), color = Jk.Muted)
                    Text(if ((s?.due ?: 0) == 0) "No workouts due yet" else "${s!!.completed} of ${s.due} workouts done",
                        style = plex(16.sp, FontWeight.Bold), color = Jk.Ink)
                    Text(if (rank >= 0 && (s?.points ?: 0) > 0) "${s!!.points} points · #${rank + 1} of $of" else "${s?.points ?: 0} points",
                        style = plex(15.sp), color = Jk.Muted)
                }
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, Modifier.size(22.dp), tint = Jk.Muted)
            }
            Row(Modifier.padding(top = 14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniStat("${s?.setsDone ?: 0}", "sets done", Modifier.weight(1f))
                val kg = Math.round(s?.volumeKg ?: 0.0)
                // Bodyweight-only sets carry no weight; say how kilos get counted rather than show a bare 0.
                MiniStat(if (kg > 0) "%,d".format(kg) else "–", if (kg > 0) "kg lifted" else "kg lifted · log weights to count", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MiniStat(value: String, label: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(Jk.Well).padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text(value, style = plex(18.sp, FontWeight.Bold), color = Jk.Ink)
        Text(label, style = plex(13.sp), color = Jk.Muted)
    }
}

/** The monthly awards a member can win and, in one line each, how. */
@Composable
private fun AwardGuide(onSeeWinners: () -> Unit) {
    val how = listOf(
        Award.BEST_MEMBER to "Most points this month",
        Award.MOST_CONSISTENT to "Highest share of workouts done (8 or more set)",
        Award.MOST_IMPROVED to "Biggest rise in share of workouts done since last month",
        Award.IRON_LIFTER to "Most kilos lifted this month",
    )
    CardBox(padding = 16.dp, onClick = onSeeWinners, onClickLabel = "See past winners") {
        Column {
            how.forEachIndexed { i, (award, text) ->
                Row(Modifier.padding(top = if (i == 0) 0.dp else 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    AwardBadge(award.look(), 36.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(award.label, style = plex(15.sp, FontWeight.SemiBold), color = Jk.Ink)
                        Text(text, style = plex(13.sp, line = 18.sp), color = Jk.Muted)
                    }
                }
            }
            Text("See past winners →", Modifier.padding(top = 14.dp), style = plex(14.sp, FontWeight.SemiBold), color = Jk.RedText)
        }
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
    var note by rememberSaveable(id) { mutableStateOf(w.memberNote) }
    val focus = LocalFocusManager.current
    val restSec = gvm.restSec
    val clock = rememberWorkoutClock(restSec)

    fun save(updated: Assignment) { a = updated; gvm.saveProgress(updated) }
    // Ticking every set at once is for logging a workout done without the phone; ask first so it's never a slip.
    var confirmAll by rememberSaveable { mutableStateOf(false) }
    if (confirmAll) AlertDialog(onDismissRequest = { confirmAll = false },
        title = { Text("Mark the whole workout done?") },
        text = { Text("Every set left (${w.setsTotal - w.setsDone}) is ticked as done. Your coach sees it, and it earns points, so only do this if you really did them.") },
        confirmButton = { TextButton(onClick = {
            confirmAll = false
            save(w.copy(exercises = w.exercises.map { e -> e.copy(sets = e.sets.map { it.copy(done = true) }) }))
        }) { Text("Mark all done") } },
        dismissButton = { TextButton(onClick = { confirmAll = false }) { Text("Cancel") } })
    // Once the coach has checked it the sets are final (the server refuses changes too); a note can still be sent.
    val canEdit = mine && w.epochDay <= today && !w.verified
    val canNote = mine && w.epochDay <= today

    Box(Modifier.fillMaxSize()) {
        BackScreen(w.title, onBack = { nav.popBackStack() }) {
            item { SessionSummary(w, dayLabel(w.epochDay, today), restSec, locked = w.epochDay > today) }
            if (canEdit) item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlainButton(if (editing) "Done changing" else "Change reps / kg", { editing = !editing },
                        icon = if (editing) Icons.Filled.Check else Icons.Outlined.Edit)
                    if (!w.done && !editing) PlainButton("All done", { confirmAll = true }, icon = Icons.Outlined.DoneAll)
                }
            }
            // The exercise being done now: the first with a set left, or the last one ticked if they skip around.
            val current = w.exercises.indexOfFirst { e -> e.sets.any { it.done } && !e.done }.takeIf { it >= 0 }
                ?: w.exercises.indexOfFirst { !it.done }
            w.exercises.forEachIndexed { ei, e ->
                item(key = "ex$ei") {
                    val replace = { sets: List<SetSpec> -> save(w.copy(exercises = w.exercises.toMutableList().also { it[ei] = e.copy(sets = sets) })) }
                    val ex = ExerciseRepo.get(e.exerciseId)
                    val step = ex?.weightStep ?: 2.5f
                    ExerciseCard(ei, e, canEdit, current = ei == current, onHowTo = { nav.navigate(Routes.exercise(it)) },
                        onToggle = { si ->
                            val s = e.sets[si]
                            replace(e.sets.toMutableList().also { it[si] = s.copy(done = !s.done) })
                            if (!s.done && w.setsDone + 1 < w.setsTotal) clock.start()
                        },
                        editor = if (!editing) null else { {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                e.sets.forEachIndexed { si, s ->
                                    SetEditorRow(si, s, e.exerciseId, step, onChange = { ns -> replace(e.sets.toMutableList().also { it[si] = ns }) }, onDelete = null)
                                }
                            }
                        } })
                }
            }
            if (mine && w.verified) item {
                Text("Checked by your coach, so the sets can't be changed now.", style = plex(14.sp), color = Jk.Muted)
            }
            if (canNote) item {
                Column {
                    OutlinedTextField(note, { note = it.take(300) }, Modifier.fillMaxWidth(), label = { Text("Note for your coach (optional)") })
                    if (note.trim() != w.memberNote) PlainButton("Send note", { focus.clearFocus(); note = note.trim(); save(w.copy(memberNote = note)) },
                        Modifier.padding(top = 8.dp))
                }
            }
            if (w.done) item { WorkoutDoneBanner() }
            item { Spacer(Modifier.height(96.dp)) }
        }

        RestBar(clock, next = null, modifier = Modifier.align(Alignment.BottomCenter))
        if (w.done && System.currentTimeMillis() - (w.completedAt ?: 0) < 10 * 60_000) Confetti()
    }
}

/** Shown under a finished workout. Yellow, because it's a reward. */
@Composable
private fun WorkoutDoneBanner() {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Jk.Yellow).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Workout complete!", style = plex(18.sp, FontWeight.Bold), color = Jk.Black)
        Text("Your coach can see it now.", style = plex(15.sp), color = Jk.Black)
    }
}

