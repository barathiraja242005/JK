package com.barathiraja.jk.ui.gym

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.barathiraja.jk.data.ExerciseRepo
import com.barathiraja.jk.gym.Assignment
import com.barathiraja.jk.gym.Role
import com.barathiraja.jk.ui.GymViewModel
import com.barathiraja.jk.ui.Routes
import com.barathiraja.jk.ui.components.JkCard
import com.barathiraja.jk.ui.components.KeyValue
import com.barathiraja.jk.ui.components.SectionTitle
import com.barathiraja.jk.ui.components.shareText
import com.barathiraja.jk.ui.components.Avatar
import com.barathiraja.jk.ui.components.BackScreen
import com.barathiraja.jk.ui.components.TabScreen
import com.barathiraja.jk.ui.components.formatDuration
import com.barathiraja.jk.ui.screens.trimZero

/** Trainer's home: every member and whether they've done today's workout. */
@Composable
fun TrainerMembersScreen(gvm: GymViewModel, nav: NavHostController) {
    val me by gvm.me.collectAsStateWithLifecycle()
    val gym by gvm.gym.collectAsStateWithLifecycle()
    val people by gvm.people.collectAsStateWithLifecycle()
    val assignments by gvm.assignments.collectAsStateWithLifecycle()
    val scores by gvm.monthScores.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val trainer = me ?: return
    val today = gvm.today
    val members = people.filter { it.role == Role.MEMBER && it.active && it.trainerUid == trainer.uid }.sortedBy { it.name }
    val todays = assignments.filter { it.trainerUid == trainer.uid && it.epochDay == today }
    val month = members.mapNotNull { scores[it.uid] }
    val monthDue = month.sumOf { it.due }
    val monthDone = month.sumOf { it.completed }
    val code = trainer.trainerCode.orEmpty()
    val invite = "Join me on the JK app at ${gym?.name ?: "the gym"}! Open JK → Sign in with Google → I'm a member → enter code $code"

    TabScreen("My members", subtitle = gym?.name, action = { Avatar(trainer.photoUrl, trainer.name, 44.dp) { nav.navigate(Routes.PROFILE) } }) {
        item {
            JkCard(Modifier.fillMaxWidth()) {
                Text(if (todays.isEmpty()) "No workouts assigned for today" else "Today: ${todays.count { it.done }} of ${todays.size} done",
                    style = MaterialTheme.typography.titleLarge)
                Text(if (monthDue == 0) "Completion this month shows here once workouts are due"
                    else "This month: ${pct(monthDone, monthDue)} of workouts completed", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { nav.navigate(Routes.assign("")) }, Modifier.weight(1f).height(52.dp)) {
                    Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("Assign workout")
                }
                FilledTonalButton(onClick = { context.shareText(invite) }, Modifier.weight(1f).height(52.dp)) {
                    Icon(Icons.Filled.PersonAdd, null); Spacer(Modifier.width(6.dp)); Text("Add member")
                }
            }
        }
        if (members.isEmpty()) {
            item { CodeCard("Your member code", code, invite) }
            item { Text("Members sign in to JK and enter this code to join you. They'll appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(members, key = { it.uid }) { m ->
            val a = todays.filter { it.memberUid == m.uid }.minByOrNull { if (it.done) 1 else 0 }
            val s = scores[m.uid]
            JkCard(Modifier.fillMaxWidth(), onClick = { nav.navigate(Routes.gymMember(m.uid)) }) {
                PersonRow(m, sub = {
                    Text(a?.title ?: "No workout today", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    Text("Month ${pct(s?.completed ?: 0, s?.due ?: 0)} · ${s?.points ?: 0} pts", style = MaterialTheme.typography.bodySmall,
                        color = rateColor(s?.rate ?: 0f, s?.due ?: 0))
                }, trailing = { StatusPill(a, today) })
            }
        }
        if (members.isNotEmpty()) item { CodeCard("Your member code", code, invite) }
    }
}

/** How many workouts a member's page lists before "Show all". */
private const val WORKOUTS_PREVIEW = 5

/** A member's month and workout history. Their trainer can verify sessions, add notes and assign more. */
@Composable
fun MemberDetailScreen(uid: String, gvm: GymViewModel, nav: NavHostController) {
    val me by gvm.me.collectAsStateWithLifecycle()
    val people by gvm.people.collectAsStateWithLifecycle()
    val assignments by gvm.assignments.collectAsStateWithLifecycle()
    val scores by gvm.monthScores.collectAsStateWithLifecycle()
    val ranking by gvm.memberRanking.collectAsStateWithLifecycle()
    val digest by gvm.ownerDigest.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val member = people.firstOrNull { it.uid == uid }?.takeIf { it.active }
    val isTrainer = me?.role == Role.TRAINER && member?.trainerUid == me?.uid
    val today = gvm.today
    var confirmRemove by remember { mutableStateOf(false) }
    var noteFor by remember { mutableStateOf<Assignment?>(null) }
    var showAllWorkouts by remember { mutableStateOf(false) }

    if (member == null) {
        BackScreen("Member", onBack = { nav.popBackStack() }) { item { Text("This member isn't in the gym anymore.") } }
        return
    }
    val list = assignments.filter { it.memberUid == uid }.sortedByDescending { it.epochDay }
    val s = scores[uid]
    val rank = ranking.indexOfFirst { it.uid == uid } + 1

    PersonPage("Back", onBack = { nav.popBackStack() }) {
        memberOverview(member, s, rank, digest?.idle?.firstOrNull { it.member.uid == uid }, gvm.trainerOf(member), list, today,
            actions = if (me?.role == Role.OWNER) ({ OwnerPersonActions(member, gvm, onRemoved = { nav.popBackStack() }) }) else null)
        if (isTrainer) item {
            Button(onClick = { nav.navigate(Routes.assign(uid)) }, Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("Assign workout to ${member.firstName}")
            }
        }
        item { PageHeading("Workouts", "Newest first.") }
        if (list.isEmpty()) item { Text("No workouts assigned yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        val shown = if (showAllWorkouts) list else list.take(WORKOUTS_PREVIEW)
        items(shown, key = { it.id }) { a ->
            AssignmentCard(a, today, expandedByDefault = a.epochDay == today) {
                if (isTrainer) Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (a.done) FilledTonalButton(onClick = { gvm.verify(a, !a.verified, a.trainerNote) }) { Text(if (a.verified) "Unverify" else "✓ Verify") }
                    TextButton(onClick = { noteFor = a }) { Text(if (a.trainerNote.isBlank()) "Add note" else "Edit note") }
                    if (!a.done && a.setsDone == 0) TextButton(onClick = { gvm.deleteAssignment(a) }) { Text("Delete") }
                }
            }
        }
        if (list.size > WORKOUTS_PREVIEW) item {
            PlainButton(if (showAllWorkouts) "Show fewer" else "Show all ${list.size} workouts", { showAllWorkouts = !showAllWorkouts }, Modifier.fillMaxWidth())
        }
        if (isTrainer) item {
            TextButton(onClick = { confirmRemove = true }, Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Text("Remove ${member.firstName} from my members", color = MaterialTheme.colorScheme.error)
            }
        }
    }

    noteFor?.let { a ->
        var note by remember(a.id) { mutableStateOf(a.trainerNote) }
        AlertDialog(onDismissRequest = { noteFor = null },
            title = { Text("Note for ${member.firstName}") },
            text = { OutlinedTextField(note, { note = it.take(300) }, Modifier.fillMaxWidth(), placeholder = { Text("Great form today! Add 2.5kg next time.") }) },
            confirmButton = { TextButton(onClick = { gvm.verify(a, a.verified, note); noteFor = null }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { noteFor = null }) { Text("Cancel") } })
    }
    if (confirmRemove) {
        AlertDialog(onDismissRequest = { confirmRemove = false },
            title = { Text("Remove ${member.firstName}?") },
            text = { Text("They'll lose access to the gym in JK. Their past workouts stay in the records.") },
            confirmButton = { TextButton(onClick = { confirmRemove = false; gvm.removeMember(member); nav.popBackStack() }) { Text("Remove") } },
            dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text("Cancel") } })
    }
}

/** One assigned workout: title, status and (expanded) every exercise with its logged sets. */
@Composable
fun AssignmentCard(a: Assignment, today: Long, expandedByDefault: Boolean = false, onClick: (() -> Unit)? = null, actions: @Composable () -> Unit = {}) {
    var expanded by remember(a.id) { mutableStateOf(expandedByDefault) }
    JkCard(Modifier.fillMaxWidth(), onClick = onClick ?: { expanded = !expanded }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(dayLabel(a.epochDay, today), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(a.title, style = MaterialTheme.typography.titleMedium)
                Text("${plural(a.exercises.size, "exercise")} · ${a.setsDone}/${a.setsTotal} sets", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StatusPill(a, today)
        }
        if (expanded) {
            Spacer(Modifier.height(8.dp))
            a.exercises.forEach { e ->
                val name = ExerciseRepo.get(e.exerciseId)?.name ?: e.exerciseId.replace('_', ' ')
                Text((if (e.done) "✓ " else "• ") + name, style = MaterialTheme.typography.bodyMedium)
                Text(e.sets.joinToString("  ") { st ->
                    val v = if (st.timed) formatDuration(st.seconds.toLong()) else "${st.reps}×${if (st.weightKg > 0) st.weightKg.trimZero() + "kg" else "BW"}"
                    if (st.done) v else "($v)"
                }, Modifier.padding(start = 14.dp, bottom = 4.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (a.memberNote.isNotBlank()) Text("Member: ${a.memberNote}", style = MaterialTheme.typography.bodySmall)
            if (a.trainerNote.isNotBlank()) Text("Coach: ${a.trainerNote}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
        actions()
    }
}
