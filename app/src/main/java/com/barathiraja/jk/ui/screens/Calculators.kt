package com.barathiraja.jk.ui.screens

import com.barathiraja.jk.ui.components.BackScreen
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.barathiraja.jk.data.Activity
import com.barathiraja.jk.data.Goal
import com.barathiraja.jk.data.Sex
import com.barathiraja.jk.domain.Health
import com.barathiraja.jk.ui.JkViewModel
import com.barathiraja.jk.ui.components.JkCard
import com.barathiraja.jk.ui.components.KeyValue
import com.barathiraja.jk.ui.components.SectionTitle
import java.util.Locale

/** What-if calculators. Prefilled from the profile but edits here don't change it. */
@Composable
fun CalculatorsScreen(vm: JkViewModel, nav: NavHostController) {
    val p0 = vm.profile.value
    var sex by remember { mutableStateOf(p0.sex) }
    var age by remember { mutableStateOf(p0.age.toString()) }
    var height by remember { mutableStateOf(p0.heightCm.toInt().toString()) }
    var weight by remember { mutableStateOf("%.1f".format(Locale.US, p0.weightKg)) }
    var activity by remember { mutableStateOf(p0.activity) }
    var goal by remember { mutableStateOf(p0.goal) }

    val a = age.toIntOrNull()
    val h = height.toFloatOrNull()
    val w = weight.toFloatOrNull()
    val p = if (a != null && h != null && w != null && h > 0 && w > 0)
        p0.copy(sex = sex, age = a, heightCm = h, weightKg = w, activity = activity, goal = goal) else null

    BackScreen("Health calculators", onBack = { nav.popBackStack() }) {
        item { Choice(Sex.entries, sex, { if (it == Sex.MALE) "Male" else "Female" }) { sex = it } }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(age, { age = it }, "Age", Modifier.weight(1f))
                NumberField(height, { height = it }, "Height cm", Modifier.weight(1f))
                NumberField(weight, { weight = it }, "Weight kg", Modifier.weight(1f))
            }
        }
        item { Choice(Activity.entries, activity, { it.label }) { activity = it } }
        item { Choice(Goal.entries, goal, { it.label }) { goal = it } }

        if (p == null) {
            item { Text("Enter valid numbers to see results.", color = MaterialTheme.colorScheme.error) }
        } else {
            val bmi = Health.bmi(p.weightKg, p.heightCm)
            val ideal = Health.idealWeight(p.sex, p.heightCm)
            val m = Health.macros(p)
            item { SectionTitle("Results") }
            item {
                JkCard(Modifier.fillMaxWidth()) {
                    Text("Body", style = MaterialTheme.typography.titleMedium)
                    KeyValue("BMI", "%.1f · %s".format(bmi, Health.bmiCategory(bmi)))
                    KeyValue("Healthy BMI range", "%.0f–%.0f kg".format(18.5f * (p.heightCm / 100) * (p.heightCm / 100),
                        24.9f * (p.heightCm / 100) * (p.heightCm / 100)))
                    KeyValue("Ideal weight (Devine)", "%.1f kg / %.0f lb".format(ideal, Health.kgToLb(ideal)))
                }
            }
            item {
                JkCard(Modifier.fillMaxWidth()) {
                    Text("Energy", style = MaterialTheme.typography.titleMedium)
                    KeyValue("BMR (at rest)", "${Health.bmr(p.sex, p.weightKg, p.heightCm, p.age)} kcal")
                    KeyValue("TDEE (maintenance)", "${Health.tdee(p)} kcal")
                    KeyValue("Target for '${p.goal.label}'", "${Health.targetCalories(p)} kcal")
                }
            }
            item {
                JkCard(Modifier.fillMaxWidth()) {
                    Text("Macros", style = MaterialTheme.typography.titleMedium)
                    KeyValue("Protein", "${m.proteinG} g")
                    KeyValue("Carbs", "${m.carbsG} g")
                    KeyValue("Fat", "${m.fatG} g")
                    KeyValue("Water", "%.1f L".format(p.weightKg * 0.035f))
                }
            }
            item {
                Text("Estimates only — not medical advice.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
