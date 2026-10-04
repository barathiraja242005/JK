package com.barathiraja.jk.ui.gym

import com.barathiraja.jk.ui.theme.HeroBlue
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.barathiraja.jk.data.ExerciseRepo
import com.barathiraja.jk.gym.Assignment
import com.barathiraja.jk.gym.Role
import com.barathiraja.jk.ui.GymViewModel
import com.barathiraja.jk.ui.Routes
import com.barathiraja.jk.ui.components.Confetti
import com.barathiraja.jk.ui.components.ExerciseDemo
import com.barathiraja.jk.ui.components.JkCard
import com.barathiraja.jk.ui.components.Ring
import com.barathiraja.jk.ui.components.SectionTitle
import com.barathiraja.jk.ui.components.Avatar
import com.barathiraja.jk.ui.components.BackScreen
import com.barathiraja.jk.ui.screens.SetRow
import com.barathiraja.jk.ui.components.TabScreen
import com.barathiraja.jk.ui.theme.Leaf
import kotlinx.coroutines.delay

/** Top of a member's Today screen: the workout their coach assigned plus their rank. */
@Composable
fun AssignedTodayCard(gvm: GymViewModel, nav: NavHostController) {
    val a by gvm.myToday.collectAsStateWithLifecycle()
    val me by gvm.me.collectAsStateWithLifecycle()
    val ranking by gvm.memberRanking.collectAsStateWithLifecycle()
    val m = me ?: return
    if (m.role != Role.MEMBER) return
    val coach = gvm.person(m.trainerUid)
    val rank = ranking.indexOfFirst { it.uid == m.uid }
    Column {
        a?.let { w ->
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                .background(HeroBlue).padding(20.dp)) {
                Column {
                    Text(if (w.done) "DONE ✓ · FROM COACH ${coach?.firstName?.uppercase() ?: ""}" else "🏋 FROM COACH ${coach?.firstName?.uppercase() ?: ""}",
                        style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.85f))
                    Text(w.title, style = MaterialTheme.typography.headlineSmall, color = Color.White)
                    Text("${plural(w.exercises.size, "exercise")} · ${plural(w.setsTotal, "set")}", color = Color.White.copy(alpha = 0.85f))
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator({ if (w.setsTotal == 0) 0f else w.setsDone / w.setsTotal.toFloat() },
                        Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = Color.White, trackColor = Color.White.copy(alpha = 0.3f))
                    Text("${w.exercisesDone}/${w.exercises.size} exercises done", Modifier.padding(top = 4.dp), color = Color.White)
                    if (w.trainerNote.isNotBlank()) Text("💬 ${w.trainerNote}", Modifier.padding(top = 6.dp), color = Color.White)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { nav.navigate(Routes.assigned(w.id)) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = HeroBlue)) {
                        Icon(Icons.Filled.PlayArrow, null); Spacer(Modifier.width(4.dp))
                        Text(when { w.done -> "View workout"; w.setsDone > 0 -> "Continue workout"; else -> "Start workout" })
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        if (rank >= 0) {
            val s = ranking[rank]
            val ahead = ranking.getOrNull(rank - 1)
            JkCard(Modifier.fillMaxWidth(), onClick = { nav.navigate(Routes.RANKS) }) {
                if (s.points == 0) {
                    Text("🏆 Gym leaderboard", style = MaterialTheme.typography.titleMedium)
                    Text("Finish a workout to earn your first points", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text("🏆 You're #${rank + 1} in the gym this month", style = MaterialTheme.typography.titleMedium)
                    Text("${s.points} pts" + (ahead?.let { " · ${it.points - s.points} to #$rank" } ?: " · you're leading!"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
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
    val coach = gvm.person(m.trainerUid)
    val myAwards = awards.flatMap { a -> a.winners.filterValues { it == m.uid }.keys.map { a.month to it } }

    TabScreen(gym?.name ?: "My gym", subtitle = "Gym", action = { Avatar(m.photoUrl, m.name, 44.dp) { nav.navigate(Routes.PROFILE) } }) {
        coach?.let { c -> item { JkCard(Modifier.fillMaxWidth()) { PersonRow(c, sub = { Text("Your coach", color = MaterialTheme.colorScheme.onSurfaceVariant) }) } } }
        item {
            JkCard(Modifier.fillMaxWidth(), onClick = { nav.navigate(Routes.RANKS) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Ring(s?.rate ?: 0f, Leaf, size = 72.dp, stroke = 7.dp) { Text(pct(s?.completed ?: 0, s?.due ?: 0), style = MaterialTheme.typography.titleSmall) }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("This month", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text("${s?.completed ?: 0} of ${s?.due ?: 0} workouts done", style = MaterialTheme.typography.titleMedium)
                        Text("${s?.points ?: 0} points · see leaderboard →", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        if (myAwards.isNotEmpty()) item {
            JkCard(Modifier.fillMaxWidth()) {
                Text("My awards", style = MaterialTheme.typography.titleMedium)
                myAwards.forEach { (month, award) -> Text("${award.emoji} ${award.label} · ${monthLabel(month)}", Modifier.padding(top = 4.dp)) }
            }
        }
        item { SectionTitle("Coming up") }
        if (upcoming.isEmpty()) item { Text("Nothing assigned yet. Your coach's workouts will show up here.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(upcoming, key = { it.id }) { a -> AssignmentCard(a, today, onClick = { nav.navigate(Routes.assigned(a.id)) }) }
        if (past.isNotEmpty()) item { SectionTitle("History") }
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
        BackScreen("Workout", onBack = { nav.popBackStack() }) { item { Text("This workout was removed by your coach.") } }
        return
    }
    val mine = w.memberUid == me?.uid
    val today = gvm.today
    var editing by remember { mutableStateOf(false) }
    var resting by remember { mutableIntStateOf(0) }
    var note by remember(id) { mutableStateOf(w.memberNote) }
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
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
                Text("${dayLabel(w.epochDay, today)} · ${w.exercisesDone}/${w.exercises.size} exercises · ${w.setsDone}/${w.setsTotal} sets",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (w.trainerNote.isNotBlank()) Text("💬 Coach: ${w.trainerNote}", Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.primary)
                if (w.epochDay > today) Text("You can start this on ${dayLabel(w.epochDay, today)}.", Modifier.padding(top = 6.dp))
                if (canEdit) TextButton(onClick = { editing = !editing }) { Text(if (editing) "Done editing" else "Change reps / weight") }
            }
            w.exercises.forEachIndexed { ei, e ->
                item(key = "ex$ei") {
                    val ex = ExerciseRepo.get(e.exerciseId)
                    JkCard(Modifier.fillMaxWidth(), onClick = { ex?.let { nav.navigate(Routes.exercise(it.id)) } }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ex?.let { ExerciseDemo(it, Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)), animate = false) }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text((if (e.done) "✓ " else "") + (ex?.name ?: e.exerciseId), style = MaterialTheme.typography.titleMedium)
                                ex?.let { Text(it.muscles, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                        }
                        val step = if (ex?.equipment == "dumbbell" || ex?.equipment == "kettlebells") 1f else 2.5f
                        e.sets.forEachIndexed { si, s ->
                            Spacer(Modifier.height(6.dp))
                            if (editing) SetEditorRow(si, s, step, onChange = { ns ->
                                save(w.copy(exercises = w.exercises.toMutableList().also { it[ei] = e.copy(sets = e.sets.toMutableList().also { l -> l[si] = ns }) }))
                            }, onDelete = null)
                            else SetRow(si, s, false, onToggle = {
                                if (!canEdit) return@SetRow
                                val ns = e.sets.toMutableList().also { it[si] = s.copy(done = !s.done) }
                                save(w.copy(exercises = w.exercises.toMutableList().also { it[ei] = e.copy(sets = ns) }))
                                if (!s.done && w.setsDone + 1 < w.setsTotal) resting = restSec
                            }, onChange = { ns ->
                                save(w.copy(exercises = w.exercises.toMutableList().also { it[ei] = e.copy(sets = e.sets.toMutableList().also { l -> l[si] = ns }) }))
                            }, onDelete = {}, step = step)
                        }
                    }
                }
            }
            if (canEdit) item {
                OutlinedTextField(note, { note = it.take(300) }, Modifier.fillMaxWidth(), label = { Text("Note for your coach (optional)") })
                if (note.trim() != w.memberNote) FilledTonalButton(onClick = { focus.clearFocus(); note = note.trim(); save(w.copy(memberNote = note)) },
                    Modifier.padding(top = 6.dp)) { Text("Send note") }
            }
            if (canEdit && !w.done) item {
                TextButton(onClick = {
                    save(w.copy(exercises = w.exercises.map { e -> e.copy(sets = e.sets.map { it.copy(done = true) }) }))
                }, Modifier.fillMaxWidth()) { Text("Mark everything done") }
            }
            if (w.done) item {
                Card(colors = CardDefaults.cardColors(containerColor = Leaf.copy(alpha = 0.15f)), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🏆 Workout complete!", style = MaterialTheme.typography.titleLarge, color = Leaf)
                        Text("Your coach can see it now.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item { Spacer(Modifier.height(96.dp)) }
        }

        AnimatedVisibility(resting > 0, modifier = Modifier.align(Alignment.BottomCenter)) {
            Card(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.inverseSurface)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Ring(resting / restSec.toFloat(), Leaf, size = 64.dp, stroke = 6.dp) {
                        Text("$resting", color = MaterialTheme.colorScheme.inverseOnSurface, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.width(14.dp))
                    Text("Rest", Modifier.weight(1f), color = MaterialTheme.colorScheme.inverseOnSurface, style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { resting += 15 }) { Text("+15s", color = MaterialTheme.colorScheme.inverseOnSurface) }
                    TextButton(onClick = { resting = 0 }) { Text("Skip", color = Leaf) }
                }
            }
        }
        if (w.done && System.currentTimeMillis() - (w.completedAt ?: 0) < 10 * 60_000) Confetti()
    }
}
