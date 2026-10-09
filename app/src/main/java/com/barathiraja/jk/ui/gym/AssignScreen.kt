package com.barathiraja.jk.ui.gym

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.barathiraja.jk.data.weightStep
import com.barathiraja.jk.data.SetSpec
import com.barathiraja.jk.data.TrainingPool
import com.barathiraja.jk.data.weightLabel
import com.barathiraja.jk.gym.AssignedExercise
import com.barathiraja.jk.gym.Person
import com.barathiraja.jk.gym.PlanLibrary
import com.barathiraja.jk.gym.Role
import com.barathiraja.jk.gym.Template
import com.barathiraja.jk.ui.GymViewModel
import com.barathiraja.jk.ui.components.BackScreen
import com.barathiraja.jk.ui.components.ExerciseDemo
import com.barathiraja.jk.ui.components.formatDuration
import com.barathiraja.jk.ui.screens.ExercisePicker
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import com.barathiraja.jk.ui.components.CardBox
import com.barathiraja.jk.ui.components.Heading
import com.barathiraja.jk.ui.components.PersonAvatar
import com.barathiraja.jk.ui.components.PlainButton
import com.barathiraja.jk.ui.components.RedButton
import com.barathiraja.jk.ui.theme.Jk
import com.barathiraja.jk.ui.theme.Tone
import com.barathiraja.jk.ui.theme.plex
import com.barathiraja.jk.gym.plural

private val weekday = DateTimeFormatter.ofPattern("EEE")
private val month = DateTimeFormatter.ofPattern("MMM")

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
    // The one exercise whose sets are open for editing; the rest show as a single "4 × 6–8" line.
    var open by rememberSaveable { mutableIntStateOf(-1) }

    fun loadPlan(d: PlanLibrary.PlanDay) {
        exercises.clear(); exercises.addAll(d.exercises)
        title = "${d.title} · ${d.day}"; note = d.note; open = -1
    }

    if (picking) {
        ExercisePicker(onDismiss = { picking = false }) { id -> exercises += defaultExercise(id); open = exercises.lastIndex; picking = false }
    }
    if (showTemplates) {
        StartFromSheet(templates, onDismiss = { showTemplates = false }, onDelete = { gvm.deleteTemplate(it) },
            onPlan = { d -> loadPlan(d); showTemplates = false },
            onTemplate = { t ->
                exercises.clear(); exercises.addAll(t.exercises.filter { ExerciseRepo.get(it.exerciseId) != null })
                title = t.title; note = ""; open = -1; showTemplates = false
            })
    }

    val finalDays = (if (weekly) days.flatMap { d -> (0..3).map { d + it * 7L } } else days.toList()).distinct()
    val count = chosen.size * finalDays.size

    Column(Modifier.fillMaxSize().background(Jk.Paper)) {
        Column(Modifier.weight(1f)) {
            BackScreen("Assign workout", onBack = { nav.popBackStack() }) {
                item { StepHeading(1, "Who", if (chosen.isEmpty()) null else plural(chosen.size, "member")) }
                item { WhoPicker(members, chosen) }
                item { StepHeading(2, "When", if (finalDays.isEmpty()) null else plural(finalDays.size, "day")) }
                item { WhenPicker(today, days, weekly) { weekly = it } }
                item { StepHeading(3, "Workout", null) }
                if (exercises.isEmpty()) {
                    PlanLibrary.plans.forEach { plan ->
                        item(key = plan.name) { PlanStrip(plan, ::loadPlan) }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PlainButton("My templates", { showTemplates = true }, Modifier.weight(1f))
                            PlainButton("Build my own", { picking = true }, Modifier.weight(1f), icon = Icons.Filled.Add)
                        }
                    }
                } else {
                    item {
                        WorkoutHeader(title.ifBlank { autoTitle(exercises) }, exercises.size, exercises.sumOf { it.sets.size },
                            onRename = { title = it.take(40) },
                            onChange = { showTemplates = true }, onClear = { exercises.clear(); title = ""; note = ""; open = -1 })
                    }
                    item {
                        CardBox(padding = 14.dp) {
                            OutlinedTextField(note, { note = it.take(400) }, Modifier.fillMaxWidth(), minLines = 2, maxLines = 5,
                                label = { Text("Note for your members") }, placeholder = { Text("e.g. Slow, controlled reps today") })
                        }
                    }
                    item {
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Jk.Card)) {
                            exercises.forEachIndexed { i, e ->
                                val ex = ExerciseRepo.get(e.exerciseId) ?: return@forEachIndexed
                                if (i > 0) RowDivider(inset = 0.dp)
                                ExerciseRow(i, ex, e, expanded = open == i,
                                    onToggle = { open = if (open == i) -1 else i },
                                    onMoveUp = { exercises.add(i - 1, exercises.removeAt(i)); open = i - 1 }.takeIf { i > 0 },
                                    onRemove = { exercises.removeAt(i); open = -1 },
                                    onChange = { exercises[i] = it })
                            }
                        }
                    }
                    item { PlainButton("Add exercise", { picking = true }, Modifier.fillMaxWidth(), icon = Icons.Filled.Add) }
                    item {
                        PlainButton("Save as template", { gvm.saveTemplate(title.ifBlank { autoTitle(exercises) }, exercises.toList()) },
                            Modifier.fillMaxWidth(), icon = Icons.Outlined.BookmarkBorder)
                    }
                }
            }
        }
        RedButton(
            when {
                chosen.isEmpty() -> "Choose who"
                days.isEmpty() -> "Choose a day"
                exercises.isEmpty() -> "Pick a workout"
                else -> "Assign to ${plural(chosen.size, "member")} · ${plural(finalDays.size, "day")}"
            },
            onClick = { gvm.assign(title.ifBlank { autoTitle(exercises) }, chosen.toList(), finalDays, exercises.toList(), note) { nav.popBackStack() } },
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).height(56.dp),
            enabled = !busy && count > 0 && exercises.isNotEmpty(),
        )
    }
}

/** A step's number in a red disc, its name, and what's been chosen so far on the right. */
@Composable
private fun StepHeading(n: Int, title: String, chosen: String?) {
    Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(26.dp).clip(CircleShape).background(Jk.Red), contentAlignment = Alignment.Center) {
            Text("$n", style = plex(14.sp, FontWeight.Bold), color = Color.White)
        }
        Spacer(Modifier.width(10.dp))
        Text(title, Modifier.weight(1f), style = plex(18.sp, FontWeight.Bold), color = Jk.Ink)
        if (chosen != null) Text(chosen, style = plex(14.sp, FontWeight.SemiBold), color = Jk.Muted)
    }
}

/** One suggested plan: its name and how to run it, then its days as dark tiles to swipe through. */
@Composable
private fun PlanStrip(plan: PlanLibrary.Plan, onPlan: (PlanLibrary.PlanDay) -> Unit) {
    Column {
        Text(plan.name, Modifier.padding(horizontal = 4.dp), style = plex(15.sp, FontWeight.SemiBold), color = Jk.Ink)
        Text(plan.about, Modifier.padding(start = 4.dp, end = 4.dp, top = 2.dp, bottom = 10.dp), style = plex(13.sp, line = 18.sp), color = Jk.Muted)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(plan.days) { d -> PlanDayTile(d) { onPlan(d) } }
        }
    }
}

/** A plan day as a poster: the day in red, the split in big white letters, what it trains, and its size. */
@Composable
private fun PlanDayTile(d: PlanLibrary.PlanDay, onClick: () -> Unit) {
    Column(
        Modifier.width(148.dp).height(156.dp).clip(RoundedCornerShape(20.dp)).background(Jk.Hero)
            .clickable(onClickLabel = "Load ${d.day}, ${d.title}", onClick = onClick).padding(14.dp),
    ) {
        Text(d.day.uppercase(), style = plex(12.sp, FontWeight.Bold, tracking = 1.sp), color = Jk.RedOnDark)
        Text(d.title.uppercase(), Modifier.padding(top = 4.dp), style = plex(20.sp, FontWeight.Bold, line = 22.sp), color = Color.White,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(d.focus, Modifier.padding(top = 4.dp), style = plex(12.sp, line = 16.sp), color = Jk.OnDarkSoft, maxLines = 2,
            overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.weight(1f))
        Text("${plural(d.exercises.size, "exercise")} · ${d.exercises.sumOf { it.sets.size }} sets", style = plex(12.sp, FontWeight.SemiBold),
            color = Jk.OnDarkMuted)
    }
}

/** The loaded workout on the dark card: its name, how big it is, and ways to swap or clear it. */
@Composable
private fun WorkoutHeader(
    name: String, exerciseCount: Int, setCount: Int, onRename: (String) -> Unit, onChange: () -> Unit, onClear: () -> Unit,
) {
    var renaming by rememberSaveable { mutableStateOf(false) }
    if (renaming) {
        var draft by rememberSaveable { mutableStateOf(name) }
        AlertDialog(onDismissRequest = { renaming = false }, title = { Text("Workout name") },
            text = { OutlinedTextField(draft, { draft = it.take(40) }, singleLine = true) },
            confirmButton = { TextButton(onClick = { onRename(draft.trim()); renaming = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { renaming = false }) { Text("Cancel") } })
    }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Jk.Hero).padding(start = 18.dp, end = 6.dp, top = 8.dp, bottom = 18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name, Modifier.weight(1f).padding(top = 10.dp), style = plex(22.sp, FontWeight.Bold, line = 27.sp), color = Color.White,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            IconButton(onClick = { renaming = true }) { Icon(Icons.Outlined.Edit, "Rename workout", tint = Jk.OnDarkSoft) }
        }
        // About 2½ minutes a set, rest included.
        Text("${plural(exerciseCount, "exercise")} · $setCount sets · about ${(setCount * 2.5).roundToInt()} min",
            Modifier.padding(top = 4.dp), style = plex(14.sp), color = Jk.OnDarkSoft)
        Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DarkPill("Change", onChange)
            DarkPill("Clear", onClear)
        }
    }
}

@Composable
private fun DarkPill(text: String, onClick: () -> Unit) {
    Text(text, Modifier.clip(RoundedCornerShape(50)).border(1.5.dp, Jk.OnDarkMuted, RoundedCornerShape(50))
        .clickable(onClick = onClick).heightIn(min = 40.dp).padding(horizontal = 18.dp, vertical = 10.dp),
        style = plex(14.sp, FontWeight.SemiBold), color = Color.White)
}

private val rangeCue = Regex("""^\d+(?:–\d+)? sets of (\d+)–(\d+)""")

/**
 * The sets as one line, the way a plan poster writes them: "4 × 6–8" (the range comes from a plan's cue while the
 * reps still match it), "3 × 12", "3 × 0:30", or "4 sets" when the reps differ; plus the weight when it's the same.
 */
private fun setsLine(e: AssignedExercise): String {
    val s = e.sets
    if (s.isEmpty()) return "No sets"
    val first = s.first()
    val reps = when {
        s.all { it.timed && it.seconds == first.seconds } -> "${s.size} × ${formatDuration(first.seconds.toLong())}"
        s.any { it.timed } || s.any { it.reps != first.reps } -> plural(s.size, "set")
        else -> rangeCue.find(e.cue)?.takeIf { it.groupValues[2].toInt() == first.reps }
            ?.let { m -> if (m.groupValues[1] == m.groupValues[2]) null else "${s.size} × ${m.groupValues[1]}–${m.groupValues[2]}" }
            ?: "${s.size} × ${first.reps}"
    }
    return if (first.weightKg > 0f && s.all { it.weightKg == first.weightKg }) "$reps · ${weightLabel(first.weightKg, e.exerciseId)} kg" else reps
}

/** The cue without the "4 sets of 6–8." the pill already shows. */
private fun cueBody(cue: String): String = cue.replace(Regex("""^\d+(?:–\d+)? sets of \d+–\d+\.\s*"""), "")

/**
 * One exercise as a numbered row, like a plan poster: number, picture, name and its sets in a red pill. Tapping
 * opens it to show the how-to, edit each set, move it up or remove it.
 */
@Composable
private fun ExerciseRow(
    i: Int, ex: Exercise, e: AssignedExercise, expanded: Boolean, onToggle: () -> Unit,
    onMoveUp: (() -> Unit)?, onRemove: () -> Unit, onChange: (AssignedExercise) -> Unit,
) {
    Column(Modifier.fillMaxWidth().animateContentSize()) {
        Row(Modifier.fillMaxWidth().clickable(onClickLabel = if (expanded) "Close ${ex.name}" else "Edit ${ex.name}", onClick = onToggle)
            .heightIn(min = 76.dp).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${i + 1}", Modifier.width(30.dp), style = plex(24.sp, FontWeight.Bold), color = Jk.RedText)
            ExerciseDemo(ex, Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)), animate = false)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(ex.name, style = plex(15.sp, FontWeight.SemiBold, line = 19.sp), color = Jk.Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(setsLine(e), Modifier.padding(top = 6.dp).clip(RoundedCornerShape(8.dp)).background(Jk.Red)
                    .padding(horizontal = 8.dp, vertical = 2.dp), style = plex(13.sp, FontWeight.Bold), color = Color.White, maxLines = 1)
            }
            Icon(if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, null, Modifier.size(24.dp), tint = Jk.Muted)
        }
        if (expanded) Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
            cueBody(e.cue).takeIf { it.isNotBlank() }?.let { CueText(it) }
            val step = ex.weightStep
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
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                PlainButton("Add set", { onChange(e.copy(sets = e.sets + (e.sets.lastOrNull() ?: SetSpec(12)))) }, icon = Icons.Filled.Add)
                Spacer(Modifier.weight(1f))
                if (onMoveUp != null) IconButton(onClick = onMoveUp) { Icon(Icons.Filled.ArrowUpward, "Move ${ex.name} up", tint = Jk.Ink) }
                IconButton(onClick = onRemove) { Icon(Icons.Outlined.Delete, "Remove ${ex.name}", tint = Jk.RedText) }
            }
        }
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
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Jk.Paper) {
        LazyColumn(Modifier.fillMaxWidth().navigationBarsPadding(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Text("Start from", style = plex(21.sp, FontWeight.Bold), color = Jk.Ink, modifier = Modifier.padding(start = 4.dp)) }
            items(PlanLibrary.plans, key = { it.name }) { plan -> PlanStrip(plan, onPlan) }
            item { Heading("Your templates", if (templates.isEmpty()) "Build a workout and tap \"Save as template\" to keep it here." else null) }
            items(templates, key = { it.id }) { t ->
                CardBox(onClick = { onTemplate(t) }, onClickLabel = "Load ${t.title}", padding = 12.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(t.title, style = plex(15.sp, FontWeight.SemiBold), color = Jk.Ink, maxLines = 1)
                            Text(plural(t.exercises.size, "exercise"), style = plex(13.sp), color = Jk.Muted)
                        }
                        IconButton(onClick = { onDelete(t) }) { Icon(Icons.Filled.Close, "Delete template ${t.title}", tint = Jk.Muted) }
                    }
                }
            }
        }
    }
}

/** The trainer's members as photos with first names, plus "All" when there are several; picked ones get a red ring. */
@Composable
private fun WhoPicker(members: List<Person>, chosen: SnapshotStateList<String>) {
    if (members.isEmpty()) {
        Text("You don't have members yet. Share your member code from the Members tab.",
            Modifier.padding(horizontal = 4.dp), style = plex(15.sp, line = 21.sp), color = Jk.Muted)
        return
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (members.size > 1) item {
            val all = chosen.size == members.size
            PersonToggle("All", all, {
                if (all) chosen.clear() else { chosen.clear(); chosen.addAll(members.map { it.uid }) }
            }) {
                Box(Modifier.size(52.dp).clip(CircleShape).background(if (all) Jk.Hero else Tone.GOOD.fill), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Groups, null, Modifier.size(26.dp), tint = if (all) Color.White else Jk.Ink)
                }
            }
        }
        items(members, key = { it.uid }) { m ->
            PersonToggle(m.firstName, m.uid in chosen, { if (m.uid in chosen) chosen.remove(m.uid) else chosen.add(m.uid) }) {
                PersonAvatar(m.photoUrl, m.name, 52.dp)
            }
        }
    }
}

@Composable
private fun PersonToggle(label: String, on: Boolean, onClick: () -> Unit, avatar: @Composable () -> Unit) {
    Column(Modifier.width(68.dp).clip(RoundedCornerShape(14.dp)).toggleable(on, onValueChange = { onClick() }).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            Box(Modifier.border(2.5.dp, if (on) Jk.Red else Color.Transparent, CircleShape).padding(4.dp)) { avatar() }
            if (on) Box(Modifier.align(Alignment.BottomEnd).size(20.dp).clip(CircleShape).background(Jk.Red), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Check, null, Modifier.size(14.dp), tint = Color.White)
            }
        }
        Text(label, Modifier.padding(top = 4.dp), style = plex(13.sp, if (on) FontWeight.SemiBold else FontWeight.Normal),
            color = Jk.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** The next two weeks as calendar tiles (picked ones in black), and whether to repeat weekly for four weeks. */
@Composable
private fun WhenPicker(today: Long, days: SnapshotStateList<Long>, weekly: Boolean, onWeekly: (Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(14) { i ->
                val d = today + i
                val on = d in days
                val date = LocalDate.ofEpochDay(d)
                Column(
                    Modifier.width(56.dp).clip(RoundedCornerShape(16.dp)).background(if (on) Jk.Hero else Jk.Card)
                        .toggleable(on, onValueChange = { if (on) days.remove(d) else days.add(d) }).padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(if (i == 0) "TODAY" else date.format(weekday).uppercase(), style = plex(11.sp, FontWeight.Bold, tracking = 0.5.sp),
                        color = if (on) Jk.RedOnDark else Jk.Muted)
                    Text("${date.dayOfMonth}", style = plex(20.sp, FontWeight.Bold), color = if (on) Color.White else Jk.Ink)
                    Text(date.format(month), style = plex(11.sp), color = if (on) Jk.OnDarkSoft else Jk.Muted)
                }
            }
        }
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Jk.Card)
            .toggleable(weekly, onValueChange = onWeekly).padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Repeat, null, Modifier.size(22.dp), tint = Jk.Ink)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Repeat weekly", style = plex(15.sp, FontWeight.SemiBold), color = Jk.Ink)
                Text("Same days for the next 4 weeks", style = plex(13.sp), color = Jk.Muted)
            }
            Switch(weekly, onCheckedChange = null)
        }
    }
}
