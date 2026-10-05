package com.barathiraja.jk.ui.gym

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.barathiraja.jk.data.BodyPart
import com.barathiraja.jk.data.Exercise
import com.barathiraja.jk.data.ExerciseRepo
import com.barathiraja.jk.data.SetSpec
import com.barathiraja.jk.data.TrainingPool
import com.barathiraja.jk.gym.AssignedExercise
import com.barathiraja.jk.gym.PlanLibrary
import com.barathiraja.jk.gym.Role
import com.barathiraja.jk.gym.Template
import com.barathiraja.jk.ui.GymViewModel
import com.barathiraja.jk.ui.components.BackScreen
import com.barathiraja.jk.ui.components.ExerciseDemo
import com.barathiraja.jk.ui.screens.ExercisePicker
import com.barathiraja.jk.ui.theme.HeroFill
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val chipDay = DateTimeFormatter.ofPattern("EEE d")

/** Default prescription for a newly added exercise; the trainer adjusts it. */
private fun defaultExercise(id: String): AssignedExercise {
    val ex = ExerciseRepo.require(id)
    val timed = id in TrainingPool.timedIds || ex.category == "cardio" || ex.category == "stretching"
    val sets = if (timed) List(3) { SetSpec(0, seconds = 30) } else List(3) { SetSpec(12) }
    return AssignedExercise(id, TrainingPool.partOf(id)?.name.orEmpty(), sets)
}

private fun autoTitle(list: List<AssignedExercise>): String =
    list.mapNotNull { e -> BodyPart.entries.firstOrNull { it.name == e.bodyPart }?.label }.distinct().take(3).joinToString(" + ").ifBlank { "Workout" }

/** Keeps a list of plain values (strings, numbers) across rotation. */
private fun <T : Any> stateListSaver(): Saver<SnapshotStateList<T>, Any> =
    listSaver(save = { it.toList() }, restore = { it.toMutableStateList() })

/** Keeps the exercises being built across rotation: three strings each (id, body part, sets as [SetSpec.encode]). */
private val exerciseListSaver: Saver<SnapshotStateList<AssignedExercise>, Any> = listSaver(
    save = { list -> list.flatMap { listOf(it.exerciseId, it.bodyPart, SetSpec.encode(it.sets), it.cue) } },
    restore = { flat -> flat.chunked(4).map { (id, part, sets, cue) -> AssignedExercise(id, part, SetSpec.decode(sets), cue) }.toMutableStateList() },
)

/** Trainer builds a workout and assigns it to one or more members on one or more days. */
@Composable
fun AssignScreen(memberUid: String, gvm: GymViewModel, nav: NavHostController) {
    val me by gvm.me.collectAsStateWithLifecycle()
    val people by gvm.people.collectAsStateWithLifecycle()
    val templates by gvm.templates.collectAsStateWithLifecycle()
    val busy by gvm.busy.collectAsStateWithLifecycle()
    val today = gvm.today
    val members = people.filter { it.role == Role.MEMBER && it.active && it.trainerUid == me?.uid }.sortedBy { it.name }

    val exercises = rememberSaveable(saver = exerciseListSaver) { SnapshotStateList() }
    val chosen = rememberSaveable(saver = stateListSaver()) { SnapshotStateList<String>().apply { if (memberUid.isNotBlank()) add(memberUid) } }
    val days = rememberSaveable(saver = stateListSaver()) { SnapshotStateList<Long>().apply { add(today) } }
    var title by rememberSaveable { mutableStateOf("") }
    var weekly by rememberSaveable { mutableStateOf(false) }
    var picking by rememberSaveable { mutableStateOf(false) }
    var showTemplates by rememberSaveable { mutableStateOf(false) }
    // The coach's note sent with the workout; a suggested plan fills it with its tempo and warm-up advice.
    var note by rememberSaveable { mutableStateOf("") }

    if (picking) {
        ExercisePicker(onDismiss = { picking = false }) { id -> exercises += defaultExercise(id); picking = false }
    }
    if (showTemplates) {
        StartFromSheet(templates, onDismiss = { showTemplates = false }, onDelete = { gvm.deleteTemplate(it) },
            onPlan = { d ->
                exercises.clear(); exercises.addAll(d.exercises)
                title = "${d.title} · ${d.day}"; note = d.note; showTemplates = false
            },
            onTemplate = { t ->
                exercises.clear(); exercises.addAll(t.exercises.filter { ExerciseRepo.get(it.exerciseId) != null })
                title = t.title; showTemplates = false
            })
    }

    val finalDays = (if (weekly) days.flatMap { d -> (0..3).map { d + it * 7L } } else days.toList()).distinct()
    val count = chosen.size * finalDays.size

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f)) {
            BackScreen("Assign workout", onBack = { nav.popBackStack() }) {
                item { OwnerHeading("1. Who") }
                item { WhoPicker(members.map { it.uid to it.name }, chosen) }
                item { OwnerHeading("2. When") }
                item { WhenPicker(today, days, weekly) { weekly = it } }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { OwnerHeading("3. Workout") }
                        PlainButton("Plans & templates", { showTemplates = true }, Modifier.padding(top = 10.dp))
                    }
                }
                item {
                    OutlinedTextField(title, { title = it.take(40) }, Modifier.fillMaxWidth(), singleLine = true,
                        label = { Text("Name") }, placeholder = { Text(autoTitle(exercises)) })
                }
                item {
                    OutlinedTextField(note, { note = it.take(400) }, Modifier.fillMaxWidth(), minLines = 2,
                        label = { Text("Note for your members (optional)") }, placeholder = { Text("e.g. Slow, controlled reps today") })
                }
                exercises.forEachIndexed { i, e ->
                    item(key = "e$i${e.exerciseId}") {
                        val ex = ExerciseRepo.get(e.exerciseId) ?: return@item
                        ExerciseEditorCard(ex, e, canMoveUp = i > 0,
                            onMoveUp = { exercises.add(i - 1, exercises.removeAt(i)) },
                            onRemove = { exercises.removeAt(i) },
                            onChange = { exercises[i] = it })
                    }
                }
                item { PlainButton("Add exercise", { picking = true }, Modifier.fillMaxWidth().height(52.dp), icon = Icons.Filled.Add) }
                if (exercises.isNotEmpty()) item {
                    PlainButton("Save as template", { gvm.saveTemplate(title.ifBlank { autoTitle(exercises) }, exercises.toList()) }, Modifier.fillMaxWidth())
                }
            }
        }
        RedButton(
            when {
                chosen.isEmpty() -> "Choose who"
                days.isEmpty() -> "Choose a day"
                exercises.isEmpty() -> "Add exercises"
                else -> "Assign to ${plural(chosen.size, "member")} · ${plural(finalDays.size, "day")}"
            },
            onClick = { gvm.assign(title.ifBlank { autoTitle(exercises) }, chosen.toList(), finalDays, exercises.toList(), note) { nav.popBackStack() } },
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).height(56.dp),
            enabled = !busy && count > 0 && exercises.isNotEmpty(),
        )
    }
}

/**
 * Where a workout can start from: the ready-made plans (each training day loads its exercises, sets, how-to cues
 * and a note for the member), then the trainer's own saved templates.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartFromSheet(
    templates: List<Template>, onDismiss: () -> Unit, onDelete: (Template) -> Unit,
    onPlan: (PlanLibrary.PlanDay) -> Unit, onTemplate: (Template) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Owner.Paper) {
        LazyColumn(Modifier.fillMaxWidth().navigationBarsPadding(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text("Start from", style = plex(21.sp, FontWeight.Bold), color = Owner.Ink, modifier = Modifier.padding(start = 4.dp)) }
            PlanLibrary.all.groupBy { it.plan }.forEach { (plan, days) ->
                item {
                    OwnerHeading("Suggested plan", "$plan. Each muscle twice a week; rest after Day 3 and Day 6. Load one day at a time.")
                }
                item {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Owner.Card)) {
                        days.forEachIndexed { i, d ->
                            if (i > 0) RowDivider()
                            PlanDayRow(d) { onPlan(d) }
                        }
                    }
                }
            }
            item { OwnerHeading("Your templates", if (templates.isEmpty()) "Build a workout and tap \"Save as template\" to keep it here." else null) }
            items(templates, key = { it.id }) { t ->
                OwnerCardBox(onClick = { onTemplate(t) }, onClickLabel = "Load ${t.title}", padding = 12.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(t.title, style = plex(15.sp, FontWeight.SemiBold), color = Owner.Ink, maxLines = 1)
                            Text(plural(t.exercises.size, "exercise"), style = plex(13.sp), color = Owner.Muted)
                        }
                        IconButton(onClick = { onDelete(t) }) { Icon(Icons.Filled.Close, "Delete template ${t.title}", tint = Owner.Muted) }
                    }
                }
            }
        }
    }
}

/** One day of a suggested plan: its number in a charcoal disc, its name and what it trains. */
@Composable
private fun PlanDayRow(d: PlanLibrary.PlanDay, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClickLabel = "Load ${d.day}, ${d.title}", onClick = onClick)
        .heightIn(min = 64.dp).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(HeroFill), contentAlignment = Alignment.Center) {
            Text(d.day.removePrefix("Day ").trim(), style = plex(16.sp, FontWeight.Bold), color = Color.White)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(d.title, style = plex(15.sp, FontWeight.SemiBold), color = Owner.Ink, maxLines = 1)
            Text("${d.focus} · ${plural(d.exercises.size, "exercise")}", style = plex(13.sp), color = Owner.Muted, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(22.dp), tint = Owner.Muted)
    }
}

/** Chips for each of the trainer's members ([members] as uid to name), plus "Everyone" when there are several. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WhoPicker(members: List<Pair<String, String>>, chosen: SnapshotStateList<String>) {
    Column {
        if (members.isEmpty()) Text("You don't have members yet. Share your member code from the Members tab.",
            style = plex(15.sp, line = 21.sp), color = Owner.Muted)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (members.size > 1) FilterChip(chosen.size == members.size, {
                if (chosen.size == members.size) chosen.clear() else { chosen.clear(); chosen.addAll(members.map { it.first }) }
            }, label = { Text("Everyone") })
            members.forEach { (uid, name) ->
                FilterChip(uid in chosen, { if (uid in chosen) chosen.remove(uid) else chosen.add(uid) }, label = { Text(name) })
            }
        }
    }
}

/** The next two weeks as day chips, and whether to repeat weekly for four weeks. */
@Composable
private fun WhenPicker(today: Long, days: SnapshotStateList<Long>, weekly: Boolean, onWeekly: (Boolean) -> Unit) {
    Column {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(14) { i ->
                val d = today + i
                FilterChip(d in days, { if (d in days) days.remove(d) else days.add(d) },
                    label = { Text(if (i == 0) "Today" else LocalDate.ofEpochDay(d).format(chipDay)) })
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            Text("Repeat every week for 4 weeks", Modifier.weight(1f), style = plex(15.sp), color = Owner.Ink)
            Switch(weekly, onWeekly)
        }
    }
}

/** One exercise being prescribed: reorder or remove it, and edit its sets. */
@Composable
private fun ExerciseEditorCard(
    ex: Exercise, e: AssignedExercise, canMoveUp: Boolean,
    onMoveUp: () -> Unit, onRemove: () -> Unit, onChange: (AssignedExercise) -> Unit,
) {
    OwnerCardBox(padding = 16.dp) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ExerciseDemo(ex, Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)), animate = false)
                Spacer(Modifier.width(10.dp))
                Text(ex.name, Modifier.weight(1f), style = plex(15.sp, FontWeight.SemiBold), color = Owner.Ink, maxLines = 2)
                if (canMoveUp) IconButton(onClick = onMoveUp) { Icon(Icons.Filled.ArrowUpward, "Move ${ex.name} up", tint = Owner.Ink) }
                IconButton(onClick = onRemove) { Icon(Icons.Filled.Close, "Remove ${ex.name}", tint = Owner.Ink) }
            }
            if (e.cue.isNotBlank()) CueText(e.cue)
            val step = if (ex.equipment == "dumbbell" || ex.equipment == "kettlebells") 1f else 2.5f
            Spacer(Modifier.height(6.dp))
            e.sets.forEachIndexed { si, s ->
                SetEditorRow(si, s, ex.id, step,
                    onChange = { ns ->
                        // Editing a set also updates the sets after it that were the same, so
                        // "3 × 12 @ 60kg" is one change instead of three.
                        onChange(e.copy(sets = e.sets.mapIndexed { k, x -> if (k == si || (k > si && x == s)) ns else x }))
                    },
                    onDelete = { onChange(e.copy(sets = e.sets.toMutableList().also { it.removeAt(si) })) }.takeIf { e.sets.size > 1 })
            }
            PlainButton("Add set", { onChange(e.copy(sets = e.sets + (e.sets.lastOrNull() ?: SetSpec(12)))) }, Modifier.padding(top = 6.dp), icon = Icons.Filled.Add)
        }
    }
}
