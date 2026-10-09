package com.barathiraja.jk.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.barathiraja.jk.data.ExerciseRepo
import com.barathiraja.jk.ui.gym.RestBar
import com.barathiraja.jk.ui.gym.rememberWorkoutClock
import com.barathiraja.jk.data.weightStep
import com.barathiraja.jk.data.SetSpec
import com.barathiraja.jk.data.cap
import com.barathiraja.jk.data.repsAndWeight
import com.barathiraja.jk.data.weightLabel
import com.barathiraja.jk.ui.Routes
import com.barathiraja.jk.ui.TrainingViewModel
import com.barathiraja.jk.ui.components.BackScreenBar
import com.barathiraja.jk.ui.components.BodyMap
import com.barathiraja.jk.ui.components.Confetti
import com.barathiraja.jk.ui.components.ExerciseDemo
import com.barathiraja.jk.ui.components.Pill
import com.barathiraja.jk.ui.components.Ring
import com.barathiraja.jk.ui.components.SectionTitle
import com.barathiraja.jk.ui.components.formatDuration
import com.barathiraja.jk.ui.components.openUrl
import com.barathiraja.jk.ui.theme.Good
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

private val logFmt = DateTimeFormatter.ofPattern("d MMM")

/** Guided exercise screen: work through sets, log reps/weight, rest timer, prev/next. */
@Composable
fun ExerciseSessionScreen(itemId: Long, tvm: TrainingViewModel, nav: NavHostController) {
    val selectedItems by tvm.items.collectAsStateWithLifecycle()
    val todayItems by tvm.todayItems.collectAsStateWithLifecycle()
    val list = if (selectedItems.any { it.id == itemId }) selectedItems else todayItems
    val prefs by tvm.prefs.collectAsStateWithLifecycle()
    val item = list.firstOrNull { it.id == itemId }
    if (item == null) {
        Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("This exercise was removed from the plan.")
            TextButton(onClick = { nav.popBackStack() }) { Text("Back to your plan") }
        }
        return
    }
    val ex = ExerciseRepo.get(item.exerciseId) ?: return
    val sets = item.setList
    val index = list.indexOf(item)
    val context = LocalContext.current
    val logs by remember(ex.id) { tvm.setLogs(ex.id) }.collectAsStateWithLifecycle(emptyList())
    val previous = logs.filter { it.epochDay != item.epochDay }.let { l -> l.filter { it.epochDay == l.maxOfOrNull { x -> x.epochDay } } }

    var editing by remember(itemId) { mutableStateOf(false) }
    var setTimer by remember { mutableIntStateOf(-1) } // index of the timed set running, -1 = none
    var timerLeft by remember { mutableIntStateOf(0) }
    var showMore by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    val clock = rememberWorkoutClock(prefs.restSec)
    // Timed set countdown (planks, cardio).
    LaunchedEffect(setTimer) {
        if (setTimer < 0) return@LaunchedEffect
        while (timerLeft > 0) {
            delay(1000)
            timerLeft--
            if (timerLeft in 1..3) clock.beep()
        }
        clock.beep(long = true)
        tvm.toggleSet(item.id, setTimer, true)
        setTimer = -1
        if (sets.count { !it.done } > 1) clock.start()
    }

    fun complete(i: Int) {
        tvm.toggleSet(item.id, i, true)
        if (sets.count { !it.done } > 1) clock.start()
    }

    val nextUndone = sets.indexOfFirst { !it.done }
    val allDone = sets.isNotEmpty() && nextUndone < 0
    val dayDone = list.isNotEmpty() && list.all { it.id == item.id && allDone || it.done }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            BackScreenBar(ex.name, onBack = { nav.popBackStack() })
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Pill(item.bodyPart.label.uppercase(), MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(ex.muscles, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Box {
                            IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "More") }
                            DropdownMenu(menu, { menu = false }) {
                                DropdownMenuItem({ Text("Watch video tutorial") }, { menu = false; context.openUrl(ex.tutorialUrl) })
                                DropdownMenuItem({ Text("Full exercise guide") }, { menu = false; nav.navigate(Routes.exercise(ex.id)) })
                                DropdownMenuItem({ Text("Remove from today") }, { menu = false; tvm.remove(item.id); nav.popBackStack() })
                            }
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ExerciseDemo(ex, Modifier.weight(1f).aspectRatio(1.1f).clip(RoundedCornerShape(18.dp)))
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), shape = RoundedCornerShape(18.dp)) {
                            Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                BodyMap(listOf(item.bodyPart), height = 110.dp)
                                Text(ex.primary.firstOrNull()?.cap() ?: "", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
                if (previous.isNotEmpty()) {
                    item {
                        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp)) {
                            val p = previous.sortedBy { it.setIndex }
                            Text("Last time (${LocalDate.ofEpochDay(p.first().epochDay).format(logFmt)}): " +
                                p.joinToString("  ·  ") { if (it.seconds > 0) formatDuration(it.seconds.toLong()) else repsAndWeight(it.reps, it.weightKg, ex.id) },
                                Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Sets", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                        FilledTonalButton(onClick = { editing = !editing }) { Text(if (editing) "Done" else "Edit") }
                    }
                }
                sets.forEachIndexed { i, s ->
                    item(key = "set$i") {
                        SetRow(i, s, ex.id, editing,
                            onToggle = { if (s.done) tvm.toggleSet(item.id, i, false) else if (s.timed) { timerLeft = s.seconds; setTimer = i } else complete(i) },
                            onChange = { ns -> tvm.updateSets(item.id, sets.toMutableList().also { it[i] = ns }) },
                            onDelete = { if (sets.size > 1) tvm.updateSets(item.id, sets.toMutableList().also { it.removeAt(i) }) },
                            step = ex.weightStep)
                    }
                }
                if (editing) {
                    item {
                        OutlinedButton(onClick = { tvm.updateSets(item.id, sets + (sets.lastOrNull()?.copy(done = false) ?: SetSpec(10))) },
                            modifier = Modifier.fillMaxWidth()) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(4.dp)); Text("Add set") }
                    }
                }
                item {
                    when {
                        setTimer >= 0 -> Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Ring(timerLeft / sets[setTimer].seconds.toFloat().coerceAtLeast(1f), MaterialTheme.colorScheme.primary, size = 140.dp) {
                                Text(formatDuration(timerLeft.toLong()), style = MaterialTheme.typography.headlineSmall)
                            }
                            TextButton(onClick = { timerLeft = 0 }) { Text("Finish set now") }
                        }
                        !allDone -> Button(
                            onClick = { val s = sets[nextUndone]; if (s.timed) { timerLeft = s.seconds; setTimer = nextUndone } else complete(nextUndone) },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                        ) {
                            Icon(if (sets[nextUndone].timed) Icons.Filled.PlayArrow else Icons.Filled.Check, null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (sets[nextUndone].timed) "Start set ${nextUndone + 1} timer" else "Complete set ${nextUndone + 1}")
                        }
                        else -> {
                            val next = list.drop(index + 1).firstOrNull { !it.done } ?: list.firstOrNull { !it.done && it.id != item.id }
                            if (next != null) Button(onClick = { nav.navigate(Routes.session(next.id)) { popUpTo(Routes.SESSION) { inclusive = true } } },
                                modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("Great! Next exercise →") }
                            else Surface(color = Good.copy(alpha = 0.15f), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🏆 Workout complete!", style = MaterialTheme.typography.titleLarge, color = Good)
                                    TextButton(onClick = { nav.popBackStack() }) { Text("Back to your plan") }
                                }
                            }
                        }
                    }
                }
                item { SectionTitle("How to perform") }
                item {
                    Column {
                        (if (showMore) ex.instructions else ex.instructions.take(2)).forEachIndexed { n, t ->
                            Text("${n + 1}. $t", Modifier.padding(vertical = 3.dp), style = MaterialTheme.typography.bodyMedium)
                        }
                        if (ex.instructions.size > 2) TextButton(onClick = { showMore = !showMore }) { Text(if (showMore) "Show less" else "Show more") }
                        OutlinedButton(onClick = { context.openUrl(ex.tutorialUrl) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.PlayArrow, null); Spacer(Modifier.width(4.dp)); Text("Watch video tutorial")
                        }
                        Text("Equipment: ${ex.equipment.cap()}", Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            // Previous / next navigation.
            HorizontalDivider()
            Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(enabled = index > 0, onClick = { nav.navigate(Routes.session(list[index - 1].id)) { popUpTo(Routes.SESSION) { inclusive = true } } }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null); Text("Previous")
                }
                Text("${index + 1} of ${list.size}", Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge)
                TextButton(enabled = index < list.lastIndex, onClick = { nav.navigate(Routes.session(list[index + 1].id)) { popUpTo(Routes.SESSION) { inclusive = true } } }) {
                    Text("Next"); Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                }
            }
        }

        RestBar(clock, next = "Next: set ${nextUndone + 1} of ${sets.size}", modifier = Modifier.align(Alignment.BottomCenter))
        if (dayDone && allDone) Confetti()
    }
}

@Composable
internal fun SetRow(i: Int, s: SetSpec, exerciseId: String, editing: Boolean, onToggle: () -> Unit, onChange: (SetSpec) -> Unit, onDelete: () -> Unit, step: Float) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(enabled = !editing, onClick = onToggle), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (s.done) Good.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(if (s.done) Good else MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center) {
                if (s.done) Icon(Icons.Filled.Check, null, tint = Color.White)
                else Text("${i + 1}", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.width(12.dp))
            if (s.timed) {
                Value(formatDuration(s.seconds.toLong()), "Time", Modifier.weight(1f), editing,
                    { onChange(s.copy(seconds = (s.seconds - 15).coerceAtLeast(10))) }, { onChange(s.copy(seconds = s.seconds + 15)) })
            } else {
                Value("${s.reps}", "Reps", Modifier.weight(1f), editing,
                    { onChange(s.copy(reps = (s.reps - 1).coerceAtLeast(1))) }, { onChange(s.copy(reps = (s.reps + 1).coerceAtMost(100))) })
                Value(weightLabel(s.weightKg, exerciseId), "KG", Modifier.weight(1f), editing,
                    { onChange(s.copy(weightKg = (s.weightKg - step).coerceAtLeast(0f))) }, { onChange(s.copy(weightKg = s.weightKg + step)) })
            }
            if (editing) IconButton(onClick = onDelete) { Icon(Icons.Filled.Close, "Delete set") }
        }
    }
}

@Composable
internal fun Value(value: String, label: String, modifier: Modifier, editing: Boolean, minus: () -> Unit, plus: () -> Unit) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        if (editing) FilledTonalIconButton(onClick = minus, modifier = Modifier.size(32.dp)) { Icon(Icons.Filled.Remove, "Less", Modifier.size(16.dp)) }
        Column(Modifier.padding(horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontSize = 22.sp)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (editing) FilledTonalIconButton(onClick = plus, modifier = Modifier.size(32.dp)) { Icon(Icons.Filled.Add, "More", Modifier.size(16.dp)) }
    }
}
