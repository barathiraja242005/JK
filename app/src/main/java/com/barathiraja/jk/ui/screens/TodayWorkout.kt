package com.barathiraja.jk.ui.screens

import com.barathiraja.jk.ui.components.formatDuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.barathiraja.jk.data.BodyPart
import com.barathiraja.jk.data.DayMode
import com.barathiraja.jk.data.ExerciseRepo
import com.barathiraja.jk.data.PlanItem
import com.barathiraja.jk.data.cap
import com.barathiraja.jk.domain.Health
import com.barathiraja.jk.domain.TrainingEngine
import com.barathiraja.jk.ui.JkViewModel
import com.barathiraja.jk.ui.Routes
import com.barathiraja.jk.ui.TrainingViewModel
import com.barathiraja.jk.ui.components.BodyMap
import com.barathiraja.jk.ui.components.ExerciseDemo
import com.barathiraja.jk.ui.components.Pill
import com.barathiraja.jk.ui.theme.Good
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Daily plan view: the heart of training. */
@Composable
fun TodayWorkoutTab(vm: JkViewModel, tvm: TrainingViewModel, nav: NavHostController) {
    val prefs by tvm.prefs.collectAsStateWithLifecycle()
    if (!prefs.setupDone) {
        CreatePlanPrompt { nav.navigate(Routes.TRAIN_SETUP) }
        return
    }
    val selected by tvm.selectedDay.collectAsStateWithLifecycle()
    val day by tvm.day.collectAsStateWithLifecycle()
    val items by tvm.items.collectAsStateWithLifecycle()
    val weekDays by tvm.weekDays.collectAsStateWithLifecycle()
    val split by tvm.split.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    var collapsed by rememberSaveable { mutableStateOf(setOf<String>()) }
    var adding by remember { mutableStateOf<BodyPart?>(null) }
    var replacing by remember { mutableStateOf<PlanItem?>(null) }
    var confirmAll by remember { mutableStateOf(false) }
    var confirmSwitch by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { tvm.select(tvm.selectedDay.value) }

    val today = tvm.today
    val isFuture = selected > today
    val isPast = selected < today
    val parts = day?.parts?.let(BodyPart::parseList)
        ?: split[LocalDate.ofEpochDay(selected).dayOfWeek].orEmpty()
    val totalSec = items.sumOf { TrainingEngine.estimateSec(it.setList, prefs.restSec) }
    val kcal = items.sumOf { Health.caloriesBurned(ExerciseRepo.get(it.exerciseId)?.met ?: 5f, profile.weightKg, TrainingEngine.estimateSec(it.setList, prefs.restSec)) }
    val doneCount = items.count { it.done }

    adding?.let { part ->
        PartPicker(part, tvm, onDismiss = { adding = null }) { id -> tvm.addExercise(selected, part, id); adding = null }
    }
    replacing?.let { item ->
        ReplaceDialog(item, tvm, onDismiss = { replacing = null }) { id -> tvm.replace(item.id, id); replacing = null }
    }
    if (confirmAll) {
        AlertDialog(onDismissRequest = { confirmAll = false }, title = { Text("Complete all exercises?") },
            text = { Text("This marks every remaining exercise for today as complete.") },
            confirmButton = { TextButton(onClick = { tvm.completeAll(selected); confirmAll = false }) { Text("Complete all") } },
            dismissButton = { TextButton(onClick = { confirmAll = false }) { Text("Cancel") } })
    }
    if (confirmSwitch) {
        val toFat = day?.mode != DayMode.FAT_LOSS
        AlertDialog(onDismissRequest = { confirmSwitch = false },
            title = { Text(if (toFat) "Switch to a fat-burning workout?" else "Switch back to your plan?") },
            text = { Text(if (toFat) "Today's exercises are replaced with a fast cardio + full-body circuit." else "Resume your regular scheduled workout.") },
            confirmButton = { TextButton(onClick = { tvm.regenerate(selected, if (toFat) DayMode.FAT_LOSS else DayMode.PLAN); confirmSwitch = false }) { Text("Switch") } },
            dismissButton = { TextButton(onClick = { confirmSwitch = false }) { Text("Cancel") } })
    }

    LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { WeekStrip(tvm, selected, weekDays.associateBy { it.epochDay }) }
        item {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(when { selected == today -> "TODAY'S WORKOUT"; isFuture -> "UPCOMING"; else -> "PAST WORKOUT" } +
                            if (day?.mode == DayMode.FAT_LOSS) " · FAT LOSS" else "",
                            style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(TrainingEngine.title(parts), style = MaterialTheme.typography.headlineSmall)
                    }
                    if (items.isNotEmpty()) Pill("$doneCount / ${items.size}", MaterialTheme.colorScheme.primary)
                }
                if (items.isNotEmpty()) {
                    LinearProgressIndicator(progress = { doneCount / items.size.toFloat() }, Modifier.fillMaxWidth().padding(vertical = 8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Stat(Icons.Outlined.Timer, "${(totalSec + 59) / 60} min")
                        Stat(Icons.Outlined.Flag, "${items.size} exercises")
                        Stat(Icons.Filled.LocalFireDepartment, "$kcal kcal")
                    }
                }
            }
        }

        if (parts.isEmpty() && items.isEmpty()) {
            item { RestDayCard(isPast, nav) }
            return@LazyColumn
        }
        if (isPast && items.isEmpty()) {
            item { Text("No workout was logged on this day.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            return@LazyColumn
        }

        if (!isPast && items.isNotEmpty()) {
            item {
                Card(
                    onClick = { nav.navigate(Routes.player("warmup:$selected")) }, shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        val warm = tvm.engine.warmUpIds(parts).firstNotNullOfOrNull { ExerciseRepo.get(it) }
                        if (warm != null) ExerciseDemo(warm, Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)), animate = false)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Warm-Up", style = MaterialTheme.typography.titleMedium)
                            Text("~4 min · prepare your body for better performance", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(onClick = { nav.navigate(Routes.player("warmup:$selected")) }) { Text("Start") }
                    }
                }
            }
        }

        // Sections per body part, in plan order.
        val sections = items.groupBy { it.bodyPart }
        val order = (parts + sections.keys).distinct()
        order.forEach { part ->
            val list = sections[part].orEmpty()
            val isCollapsed = part.name in collapsed
            item(key = "h-${part.name}") {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Text(part.label.uppercase(), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(8.dp))
                    if (!isPast) {
                        Surface(onClick = { adding = part }, shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.secondaryContainer) {
                            Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Add, null, Modifier.size(16.dp))
                                Text("Add", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { collapsed = if (isCollapsed) collapsed - part.name else collapsed + part.name }) {
                        Icon(if (isCollapsed) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp, "Toggle")
                    }
                }
            }
            if (!isCollapsed) {
                items(list, key = { it.id }) { item ->
                    ExerciseRow(item, isPast, onOpen = { nav.navigate(Routes.session(item.id)) },
                        onReplace = { replacing = item }, onRemove = { tvm.remove(item.id) })
                }
            }
        }

        if (!isPast && !isFuture && items.isNotEmpty()) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { confirmSwitch = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.Whatshot, null); Spacer(Modifier.width(4.dp))
                        Text(if (day?.mode == DayMode.FAT_LOSS) "Back to plan" else "Fat-loss mode", maxLines = 1)
                    }
                    Button(onClick = { confirmAll = true }, enabled = doneCount < items.size, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Check, null); Spacer(Modifier.width(4.dp)); Text("Complete all")
                    }
                }
            }
            val first = items.firstOrNull { !it.done }
            if (first != null) {
                item {
                    Button(onClick = { nav.navigate(Routes.session(first.id)) }, modifier = Modifier.fillMaxWidth().height(54.dp)) {
                        Icon(Icons.Filled.PlayArrow, null); Spacer(Modifier.width(6.dp))
                        Text(if (doneCount == 0) "Start Workout" else "Continue: ${ExerciseRepo.get(first.exerciseId)?.name ?: ""}",
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            } else {
                item {
                    Surface(color = Good.copy(alpha = 0.15f), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                        Text("🏆 ALL WORKOUTS DONE! Great work today.", Modifier.padding(16.dp), color = Good,
                            style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
        if (isFuture) {
            item {
                Text("Preview — this workout unlocks on ${LocalDate.ofEpochDay(selected).dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())}. You can still edit it.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Overflow menu for the training screen (placed in the Train header). */
@Composable
fun TrainingMenu(tvm: TrainingViewModel, nav: NavHostController) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.Filled.MoreVert, "Training options") }
        DropdownMenu(open, { open = false }) {
            DropdownMenuItem({ Text("Edit Workout Days") }, { open = false; nav.navigate(Routes.EDIT_DAYS) },
                leadingIcon = { Icon(Icons.Outlined.CalendarMonth, null) })
            DropdownMenuItem({ Text("Edit Workout Preferences") }, { open = false; nav.navigate(Routes.TRAIN_SETUP) },
                leadingIcon = { Icon(Icons.Outlined.Tune, null) })
            DropdownMenuItem({ Text("Regenerate Today") }, { open = false; tvm.regenerate(tvm.selectedDay.value, DayMode.PLAN) },
                leadingIcon = { Icon(Icons.Outlined.Refresh, null) })
            DropdownMenuItem({ Text("History") }, { open = false; nav.navigate(Routes.TRAIN_HISTORY) },
                leadingIcon = { Icon(Icons.Outlined.History, null) })
            DropdownMenuItem({ Text("User Guide") }, { open = false; nav.navigate(Routes.HELP) },
                leadingIcon = { Icon(Icons.Outlined.Flag, null) })
        }
    }
}

@Composable
private fun Stat(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun WeekStrip(tvm: TrainingViewModel, selected: Long, plans: Map<Long, com.barathiraja.jk.data.PlanDay>) {
    val start = tvm.weekStart
    val split by tvm.split.collectAsStateWithLifecycle()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        (start..start + 6).forEach { d ->
            val date = LocalDate.ofEpochDay(d)
            val isSel = d == selected
            val done = plans[d]?.completedAt != null
            val rest = split[date.dayOfWeek].isNullOrEmpty()
            Column(
                Modifier.width(44.dp).clip(RoundedCornerShape(14.dp))
                    .background(if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { tvm.select(d) }.padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val fg = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                Text(date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()), color = fg.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelMedium)
                Text("${date.dayOfMonth}", color = fg, style = MaterialTheme.typography.titleMedium)
                Box(Modifier.size(6.dp).clip(CircleShape).background(
                    when { done -> Good; rest -> Color.Transparent; isSel -> fg; else -> MaterialTheme.colorScheme.outline }))
            }
        }
    }
}

@Composable
private fun ExerciseRow(item: PlanItem, readOnly: Boolean, onOpen: () -> Unit, onReplace: () -> Unit, onRemove: () -> Unit) {
    val ex = ExerciseRepo.get(item.exerciseId) ?: return
    val sets = item.setList
    var menu by remember { mutableStateOf(false) }
    Row {
        // Timeline check.
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(32.dp).padding(top = 16.dp)) {
            Box(Modifier.size(24.dp).clip(CircleShape).background(if (item.done) Good else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center) {
                if (item.done) Icon(Icons.Filled.Check, null, Modifier.size(16.dp), tint = Color.White)
            }
        }
        Card(
            onClick = onOpen, modifier = Modifier.weight(1f), shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = if (item.done) BorderStroke(1.5.dp, Good) else null,
        ) {
            Row(Modifier.padding(12.dp)) {
                ExerciseDemo(ex, Modifier.size(88.dp).clip(RoundedCornerShape(12.dp)), animate = false)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(ex.name, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (!readOnly) {
                            Box {
                                Icon(Icons.Filled.MoreVert, "More", Modifier.size(20.dp).clickable { menu = true },
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                DropdownMenu(menu, { menu = false }) {
                                    DropdownMenuItem({ Text("Replace exercise") }, { menu = false; onReplace() })
                                    DropdownMenuItem({ Text("Remove from today") }, { menu = false; onRemove() })
                                }
                            }
                        }
                    }
                    Text(ex.muscles, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    Spacer(Modifier.height(6.dp))
                    SetTable(sets)
                }
            }
        }
    }
}

/** Compact "Set · Reps · Weight" table like a gym log. */
@Composable
fun SetTable(sets: List<com.barathiraja.jk.data.SetSpec>) {
    val timed = sets.any { it.timed }
    val weighted = sets.any { it.weightKg > 0f }
    val dim = MaterialTheme.colorScheme.onSurfaceVariant
    Row {
        Text("Set", Modifier.weight(0.6f), style = MaterialTheme.typography.labelSmall, color = dim)
        Text(if (timed) "Time" else "Reps", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = dim)
        if (!timed) Text("Weight", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = dim)
    }
    sets.take(5).forEachIndexed { i, s ->
        Row {
            Text("${i + 1}", Modifier.weight(0.6f), style = MaterialTheme.typography.bodySmall,
                color = if (s.done) Good else MaterialTheme.colorScheme.onSurface)
            Text(if (s.timed) formatDuration(s.seconds.toLong()) else "${s.reps}", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            if (!timed) Text(if (s.weightKg > 0f) "%s KG".format(s.weightKg.trimZero()) else if (weighted) "—" else "BW",
                Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
        }
    }
    if (sets.size > 5) Text("+${sets.size - 5} more", style = MaterialTheme.typography.labelSmall, color = dim)
}

fun Float.trimZero(): String = if (this % 1f == 0f) toInt().toString() else "%.1f".format(this)

@Composable
private fun RestDayCard(isPast: Boolean, nav: NavHostController) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("😴", style = MaterialTheme.typography.displaySmall)
            Text("Today Is Rest Day", style = MaterialTheme.typography.titleLarge)
            Text("Recovery is part of the plan. Muscles grow while you rest.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!isPast) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { nav.navigate(Routes.workout("home_mobility")) }) { Text("Recovery stretch") }
                    OutlinedButton(onClick = { nav.navigate(Routes.WALK) }) { Text("Go for a walk") }
                }
                TextButton(onClick = { nav.navigate(Routes.EDIT_DAYS) }) { Text("Train today anyway → edit workout days") }
            }
        }
    }
}

@Composable
private fun CreatePlanPrompt(onCreate: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        BodyMap(BodyPart.entries, height = 180.dp)
        Spacer(Modifier.height(16.dp))
        Text("Know exactly what to train", style = MaterialTheme.typography.headlineSmall)
        Text("Answer a few questions and JK builds your weekly split — every day tells you what to train, with sets, reps and weights you can edit.",
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
        Button(onClick = onCreate, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("Create My Plan") }
    }
}

/** Add-exercise sheet: suggestions for the body part, plus search across the whole library. */
@Composable
fun PartPicker(part: BodyPart, tvm: TrainingViewModel, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    var q by remember { mutableStateOf("") }
    val suggested = remember(part) { tvm.available(part) }
    val results = remember(q) { if (q.isBlank()) suggested else ExerciseRepo.search(q, null, null, null).take(100) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Add ${part.label} exercise", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Close") }
                }
                OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text("Search all ${ExerciseRepo.all().size} exercises") }, leadingIcon = { Icon(Icons.Outlined.Search, null) })
                Text(if (q.isBlank()) "Suggested for your equipment" else "${results.size} results",
                    style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(vertical = 8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(results, key = { it.id }) { ex ->
                        Row(Modifier.fillMaxWidth().clickable { onPick(ex.id) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            ExerciseDemo(ex, Modifier.size(60.dp).clip(RoundedCornerShape(10.dp)), animate = false)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(ex.name, style = MaterialTheme.typography.titleSmall)
                                Text("${ex.muscles} · ${ex.equipment.cap()}", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Filled.Add, "Add")
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun ReplaceDialog(item: PlanItem, tvm: TrainingViewModel, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val options = remember(item.id) { tvm.alternatives(item.bodyPart, item.exerciseId) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Replace with…", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Close") }
                }
                Text("Similar ${item.bodyPart.label.lowercase()} exercises for your equipment", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(options, key = { it.id }) { ex ->
                        Row(Modifier.fillMaxWidth().clickable { onPick(ex.id) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            ExerciseDemo(ex, Modifier.size(60.dp).clip(RoundedCornerShape(10.dp)), animate = false)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(ex.name, style = MaterialTheme.typography.titleSmall)
                                Text("${ex.muscles} · ${ex.equipment.cap()} · ${ex.level.cap()}", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
