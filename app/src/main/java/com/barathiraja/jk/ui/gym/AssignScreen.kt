package com.barathiraja.jk.ui.gym

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.barathiraja.jk.data.BodyPart
import com.barathiraja.jk.data.ExerciseRepo
import com.barathiraja.jk.data.SetSpec
import com.barathiraja.jk.data.TrainingPool
import com.barathiraja.jk.gym.AssignedExercise
import com.barathiraja.jk.gym.Role
import com.barathiraja.jk.ui.GymViewModel
import com.barathiraja.jk.ui.components.ExerciseDemo
import com.barathiraja.jk.ui.components.JkCard
import com.barathiraja.jk.ui.components.SectionTitle
import com.barathiraja.jk.ui.components.BackScreen
import com.barathiraja.jk.ui.screens.ExercisePicker
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

/** Trainer builds a workout and assigns it to one or more members on one or more days. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AssignScreen(memberUid: String, gvm: GymViewModel, nav: NavHostController) {
    val me by gvm.me.collectAsStateWithLifecycle()
    val people by gvm.people.collectAsStateWithLifecycle()
    val templates by gvm.templates.collectAsStateWithLifecycle()
    val busy by gvm.busy.collectAsStateWithLifecycle()
    val today = gvm.today
    val members = people.filter { it.role == Role.MEMBER && it.active && it.trainerUid == me?.uid }.sortedBy { it.name }

    val exercises = remember { mutableStateListOf<AssignedExercise>() }
    val chosen = remember { mutableStateListOf<String>().apply { if (memberUid.isNotBlank()) add(memberUid) } }
    val days = remember { mutableStateListOf(today) }
    var title by remember { mutableStateOf("") }
    var weekly by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf(false) }
    var showTemplates by remember { mutableStateOf(false) }

    if (picking) {
        ExercisePicker(onDismiss = { picking = false }) { id -> exercises += defaultExercise(id); picking = false }
    }
    if (showTemplates) {
        AlertDialog(onDismissRequest = { showTemplates = false }, title = { Text("Load a template") },
            text = {
                Column {
                    if (templates.isEmpty()) Text("No templates yet. Build a workout and tap \"Save as template\".")
                    templates.forEach { t ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = {
                                exercises.clear(); exercises.addAll(t.exercises.filter { ExerciseRepo.get(it.exerciseId) != null })
                                title = t.title; showTemplates = false
                            }, Modifier.weight(1f)) { Text("${t.title} (${t.exercises.size})", Modifier.fillMaxWidth()) }
                            IconButton(onClick = { gvm.deleteTemplate(t) }) { Icon(Icons.Filled.Close, "Delete template") }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showTemplates = false }) { Text("Close") } })
    }

    val finalDays = (if (weekly) days.flatMap { d -> (0..3).map { d + it * 7L } } else days.toList()).distinct()
    val count = chosen.size * finalDays.size

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f)) {
            BackScreen("Assign workout", onBack = { nav.popBackStack() }) {
                item { SectionTitle("1. Who") }
                item {
                    if (members.isEmpty()) Text("You don't have members yet. Share your member code from the Members tab.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (members.size > 1) FilterChip(chosen.size == members.size, {
                            if (chosen.size == members.size) chosen.clear() else { chosen.clear(); chosen.addAll(members.map { it.uid }) }
                        }, label = { Text("Everyone") })
                        members.forEach { m ->
                            FilterChip(m.uid in chosen, { if (m.uid in chosen) chosen.remove(m.uid) else chosen.add(m.uid) }, label = { Text(m.name) })
                        }
                    }
                }
                item { SectionTitle("2. When") }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(14) { i ->
                            val d = today + i
                            FilterChip(d in days, { if (d in days) days.remove(d) else days.add(d) },
                                label = { Text(if (i == 0) "Today" else LocalDate.ofEpochDay(d).format(chipDay)) })
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                        Text("Repeat every week for 4 weeks", Modifier.weight(1f))
                        Switch(weekly, { weekly = it })
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionTitle("3. Workout", Modifier.weight(1f))
                        TextButton(onClick = { showTemplates = true }) { Text("Templates") }
                    }
                }
                item {
                    OutlinedTextField(title, { title = it.take(40) }, Modifier.fillMaxWidth(), singleLine = true,
                        label = { Text("Name") }, placeholder = { Text(autoTitle(exercises)) })
                }
                exercises.forEachIndexed { i, e ->
                    item(key = "e$i${e.exerciseId}") {
                        val ex = ExerciseRepo.get(e.exerciseId) ?: return@item
                        JkCard(Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ExerciseDemo(ex, Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)), animate = false)
                                Spacer(Modifier.width(10.dp))
                                Text(ex.name, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, maxLines = 2)
                                if (i > 0) IconButton(onClick = { exercises.add(i - 1, exercises.removeAt(i)) }) { Icon(Icons.Filled.ArrowUpward, "Move up") }
                                IconButton(onClick = { exercises.removeAt(i) }) { Icon(Icons.Filled.Close, "Remove") }
                            }
                            val step = if (ex.equipment == "dumbbell" || ex.equipment == "kettlebells") 1f else 2.5f
                            Spacer(Modifier.height(6.dp))
                            e.sets.forEachIndexed { si, s ->
                                SetEditorRow(si, s, step,
                                    onChange = { ns ->
                                        // Editing a set also updates the sets after it that were the same, so
                                        // "3 × 12 @ 60kg" is one change instead of three.
                                        exercises[i] = e.copy(sets = e.sets.mapIndexed { k, x -> if (k == si || (k > si && x == s)) ns else x })
                                    },
                                    onDelete = { exercises[i] = e.copy(sets = e.sets.toMutableList().also { it.removeAt(si) }) }.takeIf { e.sets.size > 1 })
                            }
                            TextButton(onClick = { exercises[i] = e.copy(sets = e.sets + (e.sets.lastOrNull() ?: SetSpec(12))) }) {
                                Icon(Icons.Filled.Add, null); Text("Add set")
                            }
                        }
                    }
                }
                item {
                    OutlinedButton(onClick = { picking = true }, Modifier.fillMaxWidth().height(52.dp)) {
                        Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("Add exercise")
                    }
                }
                if (exercises.isNotEmpty()) item {
                    TextButton(onClick = { gvm.saveTemplate(title.ifBlank { autoTitle(exercises) }, exercises.toList()) }, Modifier.fillMaxWidth()) {
                        Text("Save as template")
                    }
                }
            }
        }
        Button(
            onClick = { gvm.assign(title.ifBlank { autoTitle(exercises) }, chosen.toList(), finalDays, exercises.toList()) { nav.popBackStack() } },
            enabled = !busy && count > 0 && exercises.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).height(56.dp),
        ) {
            Text(when {
                chosen.isEmpty() -> "Choose who"
                days.isEmpty() -> "Choose a day"
                exercises.isEmpty() -> "Add exercises"
                else -> "Assign to ${chosen.size} member${if (chosen.size == 1) "" else "s"} · ${finalDays.size} day${if (finalDays.size == 1) "" else "s"}"
            }, style = MaterialTheme.typography.titleMedium)
        }
    }
}
