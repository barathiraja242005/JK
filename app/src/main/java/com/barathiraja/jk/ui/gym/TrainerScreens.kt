package com.barathiraja.jk.ui.gym

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.barathiraja.jk.data.ExerciseRepo
import com.barathiraja.jk.gym.Assignment
import com.barathiraja.jk.gym.Role
import com.barathiraja.jk.ui.GymViewModel
import com.barathiraja.jk.ui.Routes
import com.barathiraja.jk.ui.components.TabScreen
import com.barathiraja.jk.ui.components.formatDuration
import com.barathiraja.jk.ui.components.shareText
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

    TabScreen("My members", subtitle = gym?.name, action = { ProfileButton(trainer) { nav.navigate(Routes.PROFILE) } }) {
        item {
            OwnerCardBox {
                Column {
                    Text(if (todays.isEmpty()) "No workouts assigned for today" else "Today: ${todays.count { it.done }} of ${todays.size} done",
                        style = plex(17.sp, FontWeight.SemiBold, line = 24.sp), color = Owner.Ink)
                    Text(if (monthDue == 0) "Completion this month shows here once workouts are due"
                        else "This month: ${pct(monthDone, monthDue)} of workouts completed", style = plex(15.sp, line = 21.sp), color = Owner.Muted)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RedButton("Assign workout", { nav.navigate(Routes.assign("")) }, Modifier.weight(1f).height(52.dp), icon = Icons.Filled.Add)
                PlainButton("Add member", { context.shareText(invite) }, Modifier.weight(1f).height(52.dp), icon = Icons.Filled.PersonAdd)
            }
        }
        if (members.isEmpty()) {
            item { CodeCard("Your member code", code, invite) }
            item { Text("Members sign in to JK and enter this code to join you. They'll appear here.", style = plex(15.sp, line = 21.sp), color = Owner.Muted) }
        }
        items(members, key = { it.uid }) { m ->
            val a = todays.filter { it.memberUid == m.uid }.minByOrNull { if (it.done) 1 else 0 }
            val s = scores[m.uid]
            OwnerCardBox(onClick = { nav.navigate(Routes.gymMember(m.uid)) }, onClickLabel = "Open ${m.firstName}", padding = 16.dp) {
                MemberLine(m, sub = {
                    Text(a?.title ?: "No workout today", style = plex(13.sp), color = Owner.Muted, maxLines = 1)
                    Text("Month ${pct(s?.completed ?: 0, s?.due ?: 0)} · ${s?.points ?: 0} pts", style = plex(13.sp),
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
    val member = people.firstOrNull { it.uid == uid }?.takeIf { it.active }
    val isTrainer = me?.role == Role.TRAINER && member?.trainerUid == me?.uid
    val today = gvm.today
    var confirmRemove by remember { mutableStateOf(false) }
    var noteFor by remember { mutableStateOf<Assignment?>(null) }
    var deleteFor by remember { mutableStateOf<Assignment?>(null) }
    var showAllWorkouts by rememberSaveable { mutableStateOf(false) }

    if (member == null) {
        // An empty list means the gym's people haven't loaded yet, not that the member left.
        if (people.isEmpty()) GymLoading()
        else PersonPage("Back", onBack = { nav.popBackStack() }) {
            item { Text("This member isn't in the gym anymore.", style = plex(15.sp), color = Owner.Ink) }
        }
        return
    }
    val list = assignments.filter { it.memberUid == uid }.sortedByDescending { it.epochDay }
    val s = scores[uid]
    val rank = ranking.indexOfFirst { it.uid == uid } + 1

    PersonPage("Back", onBack = { nav.popBackStack() }) {
        memberOverview(member, s, rank, digest?.idle?.firstOrNull { it.member.uid == uid }, gvm.trainerOf(member), list, today,
            actions = if (me?.role == Role.OWNER) ({ OwnerPersonActions(member, gvm, onRemoved = { nav.popBackStack() }) }) else null)
        if (isTrainer) item {
            RedButton("Assign workout to ${member.firstName}", { nav.navigate(Routes.assign(uid)) }, Modifier.fillMaxWidth().height(52.dp), icon = Icons.Filled.Add)
        }
        item { PageHeading("Workouts", "Newest first.") }
        if (list.isEmpty()) item { Text("No workouts assigned yet.", style = plex(15.sp), color = Owner.Muted) }
        val shown = if (showAllWorkouts) list else list.take(WORKOUTS_PREVIEW)
        items(shown, key = { it.id }) { a ->
            AssignmentCard(a, today, expandedByDefault = a.epochDay == today) {
                if (isTrainer) TrainerAssignmentActions(a, onVerify = { gvm.verify(a, !a.verified, a.trainerNote) },
                    onNote = { noteFor = a }, onDelete = { deleteFor = a })
            }
        }
        if (list.size > WORKOUTS_PREVIEW) item {
            PlainButton(if (showAllWorkouts) "Show fewer" else "Show all ${list.size} workouts", { showAllWorkouts = !showAllWorkouts }, Modifier.fillMaxWidth())
        }
        if (isTrainer) item {
            PlainButton("Remove ${member.firstName} from my members", { confirmRemove = true },
                Modifier.fillMaxWidth().padding(top = 16.dp), ink = Owner.RedText)
        }
    }

    noteFor?.let { a ->
        var note by rememberSaveable(a.id) { mutableStateOf(a.trainerNote) }
        AlertDialog(onDismissRequest = { noteFor = null },
            title = { Text("Note for ${member.firstName}") },
            text = { OutlinedTextField(note, { note = it.take(300) }, Modifier.fillMaxWidth(), placeholder = { Text("Great form today! Add 2.5kg next time.") }) },
            confirmButton = { TextButton(onClick = { gvm.verify(a, a.verified, note); noteFor = null }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { noteFor = null }) { Text("Cancel") } })
    }
    deleteFor?.let { a ->
        AlertDialog(onDismissRequest = { deleteFor = null },
            title = { Text("Delete this workout?") },
            text = { Text("\"${a.title}\" (${dayLabel(a.epochDay, today)}) will be removed from ${member.firstName}'s workouts.") },
            confirmButton = { TextButton(onClick = { deleteFor = null; gvm.deleteAssignment(a) }) { Text("Delete", color = Owner.RedText) } },
            dismissButton = { TextButton(onClick = { deleteFor = null }) { Text("Cancel", color = Owner.Ink) } })
    }
    if (confirmRemove) {
        AlertDialog(onDismissRequest = { confirmRemove = false },
            title = { Text("Remove ${member.firstName}?") },
            text = { Text("They'll lose access to the gym in JK. Their past workouts stay in the records.") },
            confirmButton = { TextButton(onClick = { confirmRemove = false; gvm.removeMember(member); nav.popBackStack() }) { Text("Remove") } },
            dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text("Cancel") } })
    }
}

/** The trainer's buttons under one of their member's workouts: verify (once done), note, delete (if not started). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TrainerAssignmentActions(a: Assignment, onVerify: () -> Unit, onNote: () -> Unit, onDelete: () -> Unit) {
    FlowRow(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (a.done) PlainButton(if (a.verified) "Unverify" else "Verify", onVerify)
        PlainButton(if (a.trainerNote.isBlank()) "Add note" else "Edit note", onNote)
        if (!a.done && a.setsDone == 0) PlainButton("Delete", onDelete, ink = Owner.RedText)
    }
}

/** One assigned workout: title, status and (expanded) every exercise with its logged sets. Tapping toggles the list unless [onClick] is given. */
@Composable
fun AssignmentCard(a: Assignment, today: Long, expandedByDefault: Boolean = false, onClick: (() -> Unit)? = null, actions: @Composable () -> Unit = {}) {
    var expanded by rememberSaveable(a.id) { mutableStateOf(expandedByDefault) }
    OwnerCardBox(
        onClick = onClick ?: { expanded = !expanded },
        onClickLabel = if (onClick != null) "Open workout" else if (expanded) "Hide exercises" else "Show exercises",
        padding = 16.dp,
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(dayLabel(a.epochDay, today), style = plex(14.sp, FontWeight.Medium), color = Owner.Muted)
                    Text(a.title, style = plex(16.sp, FontWeight.SemiBold), color = Owner.Ink)
                    Text("${plural(a.exercises.size, "exercise")} · ${a.setsDone}/${a.setsTotal} sets", style = plex(13.sp), color = Owner.Muted)
                }
                StatusPill(a, today)
            }
            if (expanded) AssignmentDetails(a)
            actions()
        }
    }
}

/** Every exercise in a workout with its sets; sets not done yet are in brackets. */
@Composable
private fun AssignmentDetails(a: Assignment) {
    Spacer(Modifier.height(8.dp))
    a.exercises.forEach { e ->
        val name = ExerciseRepo.get(e.exerciseId)?.name ?: e.exerciseId.replace('_', ' ')
        Text((if (e.done) "✓ " else "• ") + name, style = plex(15.sp), color = Owner.Ink)
        Text(e.sets.joinToString("  ") { st ->
            val v = if (st.timed) formatDuration(st.seconds.toLong()) else "${st.reps}×${if (st.weightKg > 0) st.weightKg.trimZero() + "kg" else "BW"}"
            if (st.done) v else "($v)"
        }, Modifier.padding(start = 14.dp, bottom = 4.dp), style = plex(13.sp), color = Owner.Muted)
    }
    if (a.memberNote.isNotBlank()) Text("Member: ${a.memberNote}", style = plex(13.sp), color = Owner.Ink)
    if (a.trainerNote.isNotBlank()) Text("Coach: ${a.trainerNote}", style = plex(13.sp, FontWeight.SemiBold), color = Owner.Ink)
}
