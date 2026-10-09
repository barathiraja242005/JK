package com.barathiraja.jk.ui.screens

import com.barathiraja.jk.ui.components.BackScreenBar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.barathiraja.jk.data.Food
import com.barathiraja.jk.data.Foods
import com.barathiraja.jk.data.Meal
import com.barathiraja.jk.data.MealPlans
import com.barathiraja.jk.domain.Health
import com.barathiraja.jk.ui.JkViewModel
import com.barathiraja.jk.ui.components.JkCard
import com.barathiraja.jk.ui.components.KeyValue
import com.barathiraja.jk.ui.components.Ring
import com.barathiraja.jk.ui.components.SectionTitle
import com.barathiraja.jk.ui.theme.Accent
import com.barathiraja.jk.ui.theme.Bad
import com.barathiraja.jk.ui.theme.Good
import com.barathiraja.jk.ui.theme.Watch
import com.barathiraja.jk.ui.theme.Calm
import androidx.compose.foundation.layout.PaddingValues
import com.barathiraja.jk.ui.components.SegmentedTabs
import com.barathiraja.jk.ui.components.SegmentTab
import com.barathiraja.jk.ui.components.ProgressBar
import com.barathiraja.jk.ui.theme.Jk

@Composable
fun DietScreen(vm: JkViewModel, nav: NavHostController) {
    var tab by rememberSaveable { mutableStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        BackScreenBar("Diet", onBack = { nav.popBackStack() })
        SegmentedTabs(listOf(SegmentTab("Food log"), SegmentTab("Meal plans")), tab, { tab = it },
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        if (tab == 0) FoodLog(vm) else MealPlanTab(vm)
    }
}

@Composable
private fun FoodLog(vm: JkViewModel) {
    val profile by vm.profile.collectAsStateWithLifecycle()
    val entries by vm.foodToday.collectAsStateWithLifecycle()
    var addTo by remember { mutableStateOf<Meal?>(null) }
    val target = Health.targetCalories(profile)
    val macros = Health.macros(profile)
    val eaten = entries.sumOf { it.kcal }

    addTo?.let { meal -> AddFoodDialog(vm, meal, onDismiss = { addTo = null }) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            JkCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Ring(eaten / target.toFloat(), if (eaten > target) Bad else Good, size = 110.dp, stroke = 11.dp) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${(target - eaten)}", style = MaterialTheme.typography.titleLarge)
                            Text(if (eaten > target) "over" else "kcal left", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        MacroBar("Protein", entries.sumOf { it.protein.toDouble() }.toFloat(), macros.proteinG, Calm)
                        MacroBar("Carbs", entries.sumOf { it.carbs.toDouble() }.toFloat(), macros.carbsG, Watch)
                        MacroBar("Fat", entries.sumOf { it.fat.toDouble() }.toFloat(), macros.fatG, Accent)
                    }
                }
                Text("$eaten of $target kcal · ${profile.goal.label}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            }
        }
        Meal.entries.forEach { meal ->
            val list = entries.filter { it.meal == meal }
            item(key = meal.name) {
                JkCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(meal.label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        Text("${list.sumOf { it.kcal }} kcal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        IconButton(onClick = { addTo = meal }) { Icon(Icons.Filled.Add, "Add to ${meal.label}") }
                    }
                    list.forEach { e ->
                        HorizontalDivider()
                        Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(e.name, style = MaterialTheme.typography.bodyMedium)
                                Text(if (e.servings != 1f) "${e.servings}× serving" else "1 serving",
                                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("${e.kcal}")
                            IconButton(onClick = { vm.deleteFood(e.id) }) { Icon(Icons.Outlined.Delete, "Delete") }
                        }
                    }
                }
            }
        }
        item {
            Text("Calorie values are typical estimates. Home portions vary.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MacroBar(label: String, value: Float, goal: Int, color: Color) {
    Column {
        Row {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
            Text("${value.toInt()} / ${goal}g", style = MaterialTheme.typography.labelMedium)
        }
        ProgressBar(value / goal.coerceAtLeast(1).toFloat(), Jk.Well, color, key = label, height = 6.dp)
    }
}

@Composable
private fun AddFoodDialog(vm: JkViewModel, meal: Meal, onDismiss: () -> Unit) {
    var q by remember { mutableStateOf("") }
    var vegOnly by remember { mutableStateOf(vm.vegOnly) }
    var picked by remember { mutableStateOf<Food?>(null) }
    var custom by remember { mutableStateOf(false) }
    val results = remember(q, vegOnly) { Foods.search(q, vegOnly) }

    picked?.let { food ->
        var servings by remember(food) { mutableFloatStateOf(1f) }
        AlertDialog(
            onDismissRequest = { picked = null },
            title = { Text(food.name) },
            text = {
                Column {
                    Text("Serving: ${food.serving}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0.5f, 1f, 1.5f, 2f, 3f).forEach { sv ->
                            FilterChip(servings == sv, { servings = sv }, label = { Text("${if (sv % 1f == 0f) sv.toInt() else sv}×") })
                        }
                    }
                    KeyValue("Calories", "${(food.kcal * servings).toInt()} kcal")
                    KeyValue("Protein · Carbs · Fat", "%.0f · %.0f · %.0f g".format(food.protein * servings, food.carbs * servings, food.fat * servings))
                }
            },
            confirmButton = { TextButton(onClick = { vm.logFood(food, meal, servings); picked = null; onDismiss() }) { Text("Add") } },
            dismissButton = { TextButton(onClick = { picked = null }) { Text("Back") } },
        )
    }

    if (custom) {
        var name by remember { mutableStateOf("") }
        var kcal by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { custom = false },
            title = { Text("Quick add") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it.take(40) }, label = { Text("Food name") }, singleLine = true)
                    NumberField(kcal, { kcal = it }, "Calories (kcal)")
                }
            },
            confirmButton = {
                TextButton(enabled = name.isNotBlank() && (kcal.toFloatOrNull() ?: 0f) > 0f, onClick = {
                    vm.logCustomFood(name.trim(), kcal.toFloat().toInt(), meal); custom = false; onDismiss()
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { custom = false }) { Text("Cancel") } },
        )
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Add to ${meal.label}", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Close") }
                }
                OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text("Search ${Foods.all.size} foods (roti, dosa, paneer…)") },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                    FilterChip(vegOnly, { vegOnly = !vegOnly; vm.vegOnly = vegOnly }, label = { Text("🟢 Veg only") })
                    FilterChip(false, { custom = true }, label = { Text("+ Quick add calories") })
                }
                LazyColumn {
                    items(results, key = { it.name }) { food ->
                        Row(Modifier.fillMaxWidth().clickable { picked = food }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(if (food.veg) "🟢" else "🔴", Modifier.width(28.dp))
                            Column(Modifier.weight(1f)) {
                                Text(food.name, style = MaterialTheme.typography.bodyLarge)
                                Text(food.serving, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("${food.kcal} kcal", style = MaterialTheme.typography.titleSmall)
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun MealPlanTab(vm: JkViewModel) {
    val profile by vm.profile.collectAsStateWithLifecycle()
    var veg by remember { mutableStateOf(vm.vegOnly) }
    val target = Health.targetCalories(profile)
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Sample days for '${profile.goal.label}' · aim for about $target kcal. Adjust portions to hit your target.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(veg, { veg = true }, label = { Text("🟢 Vegetarian") })
                FilterChip(!veg, { veg = false }, label = { Text("🔴 Non-veg") })
            }
        }
        MealPlans.plan(profile.goal, veg).forEach { (day, plan) ->
            item(key = day + veg) {
                JkCard(Modifier.fillMaxWidth()) {
                    Text(day, style = MaterialTheme.typography.titleMedium)
                    PlanLine("Breakfast", plan.breakfast)
                    PlanLine("Lunch", plan.lunch)
                    PlanLine("Snack", plan.snack)
                    PlanLine("Dinner", plan.dinner)
                }
            }
        }
        item { SectionTitle("Simple rules") }
        item {
            JkCard(Modifier.fillMaxWidth()) {
                listOf(
                    "Fill half your plate with vegetables or salad.",
                    "Include a protein source at every meal.",
                    "Prefer whole grains: roti, brown rice, oats, millets.",
                    "Limit fried snacks, sweets and sugary drinks.",
                    "Drink water before meals; it helps manage hunger.",
                ).forEach { Text("• $it", Modifier.padding(vertical = 3.dp)) }
            }
        }
    }
}

@Composable
private fun PlanLine(meal: String, text: String) {
    Row(Modifier.padding(top = 6.dp)) {
        Text(meal, Modifier.width(80.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
