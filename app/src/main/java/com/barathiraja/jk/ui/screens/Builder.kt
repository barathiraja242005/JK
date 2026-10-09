package com.barathiraja.jk.ui.screens

import com.barathiraja.jk.ui.components.BackScreen
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import com.barathiraja.jk.data.Block
import com.barathiraja.jk.data.CustomWorkout
import com.barathiraja.jk.data.ExerciseRepo
import com.barathiraja.jk.ui.JkViewModel
import com.barathiraja.jk.ui.components.ExerciseDemo
import com.barathiraja.jk.ui.components.JkCard
import com.barathiraja.jk.ui.components.SectionTitle
import androidx.compose.material3.Surface

@Composable
fun BuilderScreen(editId: Long, vm: JkViewModel, nav: NavHostController) {
    // Everything typed survives rotation and the app being put away; the saved workout is loaded only once.
    var name by rememberSaveable { mutableStateOf("My workout") }
    var rounds by rememberSaveable { mutableIntStateOf(3) }
    var rest by rememberSaveable { mutableIntStateOf(30) }
    val blocks = rememberSaveable(saver = listSaver<SnapshotStateList<Block>, String>(
        save = { listOf(Block.encodeAll(it)) }, restore = { mutableStateListOf<Block>().apply { addAll(Block.decodeAll(it.first())) } },
    )) { mutableStateListOf() }
    var loaded by rememberSaveable { mutableStateOf(false) }
    var picking by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(editId) {
        if (editId != 0L && !loaded) {
            loaded = true
            vm.customWorkouts.value.firstOrNull { it.id == editId }?.let { cw ->
                name = cw.name; rounds = cw.rounds; rest = cw.restSec
                blocks.clear(); blocks.addAll(Block.decodeAll(cw.blocks).filter { ExerciseRepo.get(it.exerciseId) != null })
            }
        }
    }

    if (picking) {
        ExercisePicker(onDismiss = { picking = false }) { id ->
            val ex = ExerciseRepo.require(id)
            blocks += if (ex.category == "stretching" || ex.category == "cardio") Block(id, seconds = 40) else Block(id, reps = 12)
            picking = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            BackScreen(if (editId == 0L) "New workout" else "Edit workout", onBack = { nav.popBackStack() }) {
                item {
                    OutlinedTextField(name, { name = it.take(40) }, Modifier.fillMaxWidth(), label = { Text("Name") }, singleLine = true)
                }
                item {
                    JkCard(Modifier.fillMaxWidth()) {
                        Counter("Rounds", "$rounds", { rounds = (rounds - 1).coerceAtLeast(1) }, { rounds = (rounds + 1).coerceAtMost(10) })
                        Counter("Rest between exercises", "${rest}s", { rest = (rest - 5).coerceAtLeast(0) }, { rest = (rest + 5).coerceAtMost(180) })
                    }
                }
                item { SectionTitle("Exercises (${blocks.size})") }
                items(blocks.size) { i ->
                    val b = blocks[i]
                    JkCard(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ExerciseDemo(b.exercise, Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)), animate = false)
                            Spacer(Modifier.width(10.dp))
                            Text(b.exercise.name, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, maxLines = 2)
                            if (i > 0) IconButton(onClick = { blocks.add(i - 1, blocks.removeAt(i)) }) { Icon(Icons.Filled.ArrowUpward, "Move up") }
                            IconButton(onClick = { blocks.removeAt(i) }) { Icon(Icons.Filled.Close, "Remove") }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(!b.isTimed, { blocks[i] = Block(b.exerciseId, reps = 12) }, label = { Text("Reps") })
                            FilterChip(b.isTimed, { blocks[i] = Block(b.exerciseId, seconds = 30) }, label = { Text("Time") })
                            Spacer(Modifier.weight(1f))
                            val step = if (b.isTimed) 5 else 1
                            FilledTonalIconButton(onClick = {
                                blocks[i] = if (b.isTimed) b.copy(seconds = (b.seconds - step).coerceAtLeast(5)) else b.copy(reps = (b.reps - step).coerceAtLeast(1))
                            }) { Icon(Icons.Filled.Remove, "Less") }
                            Text(b.label, Modifier.width(48.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
                            FilledTonalIconButton(onClick = {
                                blocks[i] = if (b.isTimed) b.copy(seconds = (b.seconds + step).coerceAtMost(600)) else b.copy(reps = (b.reps + step).coerceAtMost(100))
                            }) { Icon(Icons.Filled.Add, "More") }
                        }
                    }
                }
                item {
                    OutlinedButton(onClick = { picking = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("Add exercise")
                    }
                }
            }
        }
        Button(
            onClick = {
                vm.saveCustom(CustomWorkout(id = editId, name = name.ifBlank { "My workout" }, restSec = rest, rounds = rounds,
                    blocks = Block.encodeAll(blocks))) { nav.popBackStack() }
            },
            enabled = blocks.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).height(54.dp),
        ) { Text("Save workout") }
    }
}

@Composable
private fun Counter(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, Modifier.weight(1f))
        FilledTonalIconButton(onClick = onMinus) { Icon(Icons.Filled.Remove, "Decrease") }
        Text(value, Modifier.width(48.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
        FilledTonalIconButton(onClick = onPlus) { Icon(Icons.Filled.Add, "Increase") }
    }
}

@Composable
fun ExercisePicker(onDismiss: () -> Unit, onPick: (String) -> Unit) {
    var q by remember { mutableStateOf("") }
    val results = remember(q) { ExerciseRepo.search(q, null, null, null).take(150) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Add exercise", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Close") }
                }
                OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text("Search by name or muscle") }, leadingIcon = { Icon(Icons.Outlined.Search, null) })
                Spacer(Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(results, key = { it.id }) { ex ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onPick(ex.id) }.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ExerciseDemo(ex, Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)), animate = false)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(ex.name, style = MaterialTheme.typography.titleSmall)
                                Text("${ex.muscles} · ${ex.equipment}", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Filled.Add, "Add")
                        }
                    }
                }
            }
        }
    }
}
