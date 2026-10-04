package com.barathiraja.jk.ui.screens

import com.barathiraja.jk.ui.components.BackScreen
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.barathiraja.jk.data.Block
import com.barathiraja.jk.data.Catalog
import com.barathiraja.jk.data.Challenge
import com.barathiraja.jk.data.Exercise
import com.barathiraja.jk.data.ExerciseRepo
import com.barathiraja.jk.data.Level
import com.barathiraja.jk.data.Place
import com.barathiraja.jk.data.Workout
import com.barathiraja.jk.data.cap
import com.barathiraja.jk.ui.JkViewModel
import com.barathiraja.jk.ui.Routes
import com.barathiraja.jk.ui.components.ExerciseDemo
import com.barathiraja.jk.ui.components.JkCard
import com.barathiraja.jk.ui.components.Pill
import com.barathiraja.jk.ui.components.SectionTitle
import com.barathiraja.jk.ui.components.openUrl
import com.barathiraja.jk.ui.theme.Ember
import com.barathiraja.jk.ui.theme.Leaf
import com.barathiraja.jk.ui.theme.Sun

fun Level.color(): Color = when (this) { Level.BEGINNER -> Leaf; Level.INTERMEDIATE -> Sun; Level.ADVANCED -> Ember }
fun levelColor(level: String) = when (level) { "beginner" -> Leaf; "intermediate" -> Sun; else -> Ember }

private val trainTabs = listOf("Today", "Programs", "Challenges", "Exercises", "My workouts")

@Composable
fun WorkoutsScreen(vm: JkViewModel, tvm: com.barathiraja.jk.ui.TrainingViewModel, nav: NavHostController) {
    var tab by rememberSaveable { mutableStateOf(0) }
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
        Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 20.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy")),
                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(if (tab == 0) "Today's Workout" else "Train", style = MaterialTheme.typography.headlineMedium)
            }
            if (tab == 0) TrainingMenu(tvm, nav)
        }
        PrimaryScrollableTabRow(selectedTabIndex = tab, edgePadding = 16.dp, containerColor = MaterialTheme.colorScheme.background) {
            trainTabs.forEachIndexed { i, t -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) }) }
        }
        when (tab) {
            0 -> TodayWorkoutTab(vm, tvm, nav)
            1 -> ProgramsTab(vm, nav)
            2 -> ChallengesTab(vm, nav)
            3 -> LibraryTab(vm, nav)
            else -> MyWorkoutsTab(vm, nav)
        }
    }
}

@Composable
private fun ProgramsTab(vm: JkViewModel, nav: NavHostController) {
    val profile by vm.profile.collectAsStateWithLifecycle()
    var place by rememberSaveable { mutableStateOf(profile.place) }
    var level by rememberSaveable { mutableStateOf<Level?>(null) }
    val list = Catalog.workouts.filter { it.place == place && (level == null || it.level == level) }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Choice(Place.entries, place, { it.label }) { place = it } }
        item { Choice(listOf<Level?>(null) + Level.entries, level, { it?.label ?: "All levels" }) { level = it } }
        items(list, key = { it.id }) { w -> WorkoutCard(w) { nav.navigate(Routes.workout(w.id)) } }
    }
}

@Composable
fun WorkoutCard(w: Workout, onClick: () -> Unit) {
    JkCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ExerciseDemo(w.cover, Modifier.size(72.dp).clip(RoundedCornerShape(14.dp)), animate = false)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(w.title, style = MaterialTheme.typography.titleMedium)
                Text(w.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill(w.level.label, w.level.color())
                    Pill("${w.estimatedMin} min", MaterialTheme.colorScheme.onSurfaceVariant)
                    Pill(w.focus, MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ---------------- Challenges ----------------

@Composable
private fun ChallengesTab(vm: JkViewModel, nav: NavHostController) {
    val done by vm.challengeDays.collectAsStateWithLifecycle()
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(Catalog.challenges, key = { it.id }) { c ->
            val count = done[c.id]?.size ?: 0
            val workDays = (1..c.days).count { c.plan(it) != null }
            JkCard(Modifier.fillMaxWidth(), onClick = { nav.navigate(Routes.challenge(c.id)) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(56.dp).clip(RoundedCornerShape(16.dp))
                            .background(c.level.color()),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.EmojiEvents, null, tint = Color.White) }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.title, style = MaterialTheme.typography.titleMedium)
                        Text(c.tagline, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(progress = { count / workDays.toFloat() }, modifier = Modifier.fillMaxWidth())
                        Text(if (count == 0) "${c.days} days · ${c.level.label}" else "$count / $workDays workouts done",
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ChallengeDetailScreen(id: String, vm: JkViewModel, nav: NavHostController) {
    val c: Challenge = Catalog.challenge(id) ?: return
    val doneMap by vm.challengeDays.collectAsStateWithLifecycle()
    val done = doneMap[c.id].orEmpty()
    val next = (1..c.days).firstOrNull { c.plan(it) != null && it !in done }
    val workDays = (1..c.days).count { c.plan(it) != null }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            BackScreen(c.title, onBack = { nav.popBackStack() }) {
                item {
                    Text(c.about, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Pill(c.level.label, c.level.color())
                        Pill("${c.days} days", MaterialTheme.colorScheme.primary)
                        Pill("${done.size}/$workDays done", MaterialTheme.colorScheme.primary)
                    }
                }
                if (next == null) {
                    item {
                        JkCard(Modifier.fillMaxWidth()) {
                            Text("🏆 Challenge complete!", style = MaterialTheme.typography.titleLarge)
                            Text("You finished all $workDays workouts. Legendary.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Button(onClick = { vm.resetChallenge(c.id) }, modifier = Modifier.padding(top = 8.dp)) { Text("Start again") }
                        }
                    }
                }
                item { SectionTitle("Your ${c.days} days") }
                item {
                    // Calendar-style grid of days, 6 per row.
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        (1..c.days).chunked(6).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { d ->
                                    val plan = c.plan(d)
                                    val isDone = d in done
                                    val isNext = d == next
                                    val bg = when {
                                        isDone -> Leaf
                                        isNext -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.surfaceContainerHigh
                                    }
                                    Box(
                                        Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(bg)
                                            .clickable(enabled = plan != null) { nav.navigate(Routes.workout(plan!!.id)) },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            when { isDone -> "✓"; plan == null -> "💤"; else -> "$d" },
                                            color = if (isDone || isNext) Color.White else MaterialTheme.colorScheme.onSurface,
                                            style = MaterialTheme.typography.titleSmall,
                                        )
                                    }
                                }
                                repeat(6 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
                item {
                    Text("💤 = rest day. Tap any day to preview it.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (next != null) {
            Button(
                onClick = { nav.navigate(Routes.player(c.plan(next)!!.id)) },
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).height(54.dp),
            ) {
                Icon(Icons.Filled.PlayArrow, null)
                Spacer(Modifier.width(6.dp))
                Text("Start day $next")
            }
        }
    }
}

// ---------------- Exercise library ----------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryTab(vm: JkViewModel, nav: NavHostController) {
    var query by rememberSaveable { mutableStateOf("") }
    var muscle by rememberSaveable { mutableStateOf<String?>(null) }
    var equipment by rememberSaveable { mutableStateOf<String?>(null) }
    var savedOnly by rememberSaveable { mutableStateOf(false) }
    val flags by vm.flags.collectAsStateWithLifecycle()
    val results = remember(query, muscle, equipment, savedOnly, flags) {
        ExerciseRepo.search(query, muscle, equipment, null).filter { !savedOnly || flags[it.id]?.saved == true }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text("Search 870+ exercises") }, leadingIcon = { Icon(Icons.Outlined.Search, null) })
                ChipRow(listOf(null) + ExerciseRepo.muscles, muscle, { it?.cap() ?: "All muscles" }) { muscle = it }
                ChipRow(listOf(null) + ExerciseRepo.equipment, equipment, { it?.cap() ?: "Any equipment" }) { equipment = it }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(savedOnly, { savedOnly = !savedOnly }, label = { Text("Saved only") },
                        leadingIcon = { Icon(Icons.Filled.Bookmark, null, Modifier.size(18.dp)) })
                    Spacer(Modifier.weight(1f))
                    Text("${results.size} results", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        items(results, key = { it.id }) { ex -> ExerciseTile(ex) { nav.navigate(Routes.exercise(ex.id)) } }
    }
}

@Composable
fun <T> ChipRow(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options) { FilterChip(it == selected, { onSelect(it) }, label = { Text(label(it)) }) }
    }
}

@Composable
fun ExerciseTile(ex: Exercise, onClick: () -> Unit) {
    JkCard(Modifier.fillMaxWidth(), onClick = onClick) {
        ExerciseDemo(ex, Modifier.fillMaxWidth().aspectRatio(1.2f).clip(RoundedCornerShape(12.dp)), animate = false)
        Spacer(Modifier.height(8.dp))
        Text(ex.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(ex.muscles, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
fun ExerciseDetailScreen(id: String, vm: JkViewModel, nav: NavHostController) {
    val ex = ExerciseRepo.get(id) ?: return
    val flags by vm.flags.collectAsStateWithLifecycle()
    val saved = flags[id]?.saved == true
    val context = LocalContext.current
    BackScreen(ex.name, onBack = { nav.popBackStack() }) {
        item { ExerciseDemo(ex, Modifier.fillMaxWidth().aspectRatio(1.3f).clip(RoundedCornerShape(20.dp))) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Pill(ex.level.cap(), levelColor(ex.level))
                Pill(ex.equipment.cap(), MaterialTheme.colorScheme.primary)
                Pill(ex.category.cap(), MaterialTheme.colorScheme.primary)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { context.openUrl(ex.tutorialUrl) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.PlayArrow, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Watch tutorial")
                }
                androidx.compose.material3.OutlinedButton(onClick = { vm.toggleSave(id) }) {
                    Icon(if (saved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, null)
                    Spacer(Modifier.width(4.dp))
                    Text(if (saved) "Saved" else "Save")
                }
            }
        }
        item {
            JkCard(Modifier.fillMaxWidth()) {
                Text("Muscles", style = MaterialTheme.typography.titleMedium)
                Text("Primary: ${ex.primary.joinToString { it.cap() }}")
                if (ex.secondary.isNotEmpty()) {
                    Text("Secondary: ${ex.secondary.joinToString { it.cap() }}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { SectionTitle("How to do it") }
        items(ex.instructions.withIndex().toList()) { (i, step) ->
            Row {
                Text("${i + 1}", Modifier.width(28.dp), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                Text(step, style = MaterialTheme.typography.bodyMedium)
            }
        }
        item {
            Text("Photos: free-exercise-db (public domain). Videos open on YouTube.",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp))
        }
    }
}

// ---------------- Custom workouts ----------------

@Composable
private fun MyWorkoutsTab(vm: JkViewModel, nav: NavHostController) {
    val list by vm.customWorkouts.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (list.isEmpty()) {
                item {
                    JkCard(Modifier.fillMaxWidth()) {
                        Text("Build your own workout", style = MaterialTheme.typography.titleMedium)
                        Text("Pick any exercises from the library, set reps or time, rounds and rest, then train with the guided player.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            items(list, key = { it.id }) { cw ->
                val blocks = Block.decodeAll(cw.blocks).filter { ExerciseRepo.get(it.exerciseId) != null }
                JkCard(Modifier.fillMaxWidth(), onClick = { nav.navigate(Routes.workout("custom:${cw.id}")) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        blocks.firstOrNull()?.let {
                            ExerciseDemo(it.exercise, Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)), animate = false)
                            Spacer(Modifier.width(12.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(cw.name, style = MaterialTheme.typography.titleMedium)
                            Text("${blocks.size} exercises · ${cw.rounds} rounds · ${cw.restSec}s rest",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { nav.navigate(Routes.builder(cw.id)) }) { Icon(Icons.Outlined.Edit, "Edit") }
                        IconButton(onClick = { vm.deleteCustom(cw.id) }) { Icon(Icons.Outlined.Delete, "Delete") }
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { nav.navigate(Routes.builder(0)) },
            icon = { Icon(Icons.Filled.Add, null) }, text = { Text("New workout") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }
}

// ---------------- Workout detail ----------------

@Composable
fun WorkoutDetailScreen(id: String, vm: JkViewModel, nav: NavHostController) {
    var w by remember { mutableStateOf<Workout?>(null) }
    LaunchedEffect(id) { w = vm.resolveWorkout(id) }
    val workout = w ?: return
    var expanded by rememberSaveable { mutableStateOf<Int?>(null) }
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            BackScreen(workout.title, onBack = { nav.popBackStack() }) {
                item {
                    Text(workout.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Pill(workout.level.label, workout.level.color())
                        Pill("${workout.estimatedMin} min", MaterialTheme.colorScheme.primary)
                        Pill("${workout.rounds} rounds", MaterialTheme.colorScheme.primary)
                        Pill("${workout.restSec}s rest", MaterialTheme.colorScheme.primary)
                    }
                }
                item { SectionTitle("Exercises · repeat ${workout.rounds}×") }
                items(workout.blocks.withIndex().toList(), key = { it.index }) { (i, b) ->
                    JkCard(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().clickable { expanded = if (expanded == i) null else i },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ExerciseDemo(b.exercise, Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)), animate = expanded == i)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(b.exercise.name, style = MaterialTheme.typography.titleMedium)
                                Text(b.exercise.muscles, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(b.label, style = MaterialTheme.typography.titleMedium)
                        }
                        AnimatedVisibility(expanded == i) {
                            Column(Modifier.padding(top = 10.dp)) {
                                HorizontalDivider()
                                b.exercise.instructions.take(3).forEachIndexed { n, s ->
                                    Text("${n + 1}. $s", Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodyMedium)
                                }
                                androidx.compose.material3.TextButton(onClick = { nav.navigate(Routes.exercise(b.exerciseId)) }) {
                                    Text("Full guide & video tutorial")
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
        Button(
            onClick = { nav.navigate(Routes.player(workout.id)) },
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).height(54.dp),
        ) {
            Icon(Icons.Filled.PlayArrow, null)
            Spacer(Modifier.width(6.dp))
            Text("Start workout")
        }
    }
}
