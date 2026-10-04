package com.barathiraja.jk.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barathiraja.jk.data.Activity
import com.barathiraja.jk.data.Goal
import com.barathiraja.jk.data.Place
import com.barathiraja.jk.data.Profile
import com.barathiraja.jk.data.Sex
import com.barathiraja.jk.ui.components.SectionTitle

@Composable
fun OnboardingScreen(initial: Profile = Profile(), onDone: (Profile) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text(
            buildAnnotatedString {
                append("J")
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append("K") }
            },
            fontSize = 56.sp,
            style = MaterialTheme.typography.displaySmall,
        )
        Text("Train smarter. Live stronger.", style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        Text("Tell us about you so JK can personalise your plan, calorie target and hydration goal.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        ProfileForm(initial = initial, cta = "Start training", onSubmit = onDone)
    }
}

@Composable
fun EditProfileScreen(profile: Profile, onSave: (Profile) -> Unit, onBack: () -> Unit) {
    BackScreen("Edit profile", onBack) {
        item { ProfileForm(initial = profile, cta = "Save", onSubmit = onSave) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileForm(initial: Profile, cta: String, onSubmit: (Profile) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial.name) }
    var sex by rememberSaveable { mutableStateOf(initial.sex) }
    var age by rememberSaveable { mutableStateOf(initial.age.toString()) }
    var height by rememberSaveable { mutableStateOf(initial.heightCm.clean()) }
    var weight by rememberSaveable { mutableStateOf(initial.weightKg.clean()) }
    var goal by rememberSaveable { mutableStateOf(initial.goal) }
    var place by rememberSaveable { mutableStateOf(initial.place) }
    var activity by rememberSaveable { mutableStateOf(initial.activity) }

    val ageN = age.toIntOrNull()
    val heightN = height.toFloatOrNull()
    val weightN = weight.toFloatOrNull()
    val valid = name.isNotBlank() && ageN in 10..100 && (heightN ?: 0f) in 100f..250f && (weightN ?: 0f) in 25f..300f

    Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
        OutlinedTextField(name, { name = it.take(30) }, label = { Text("Your name") }, singleLine = true,
            modifier = Modifier.fillMaxWidth())

        SectionTitle("Sex")
        Choice(Sex.entries, sex, { if (it == Sex.MALE) "Male" else "Female" }) { sex = it }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(age, { age = it }, "Age", Modifier.weight(1f))
            NumberField(height, { height = it }, "Height (cm)", Modifier.weight(1f))
            NumberField(weight, { weight = it }, "Weight (kg)", Modifier.weight(1f))
        }

        SectionTitle("Goal")
        Choice(Goal.entries, goal, { it.label }) { goal = it }
        SectionTitle("Where do you train?")
        Choice(Place.entries, place, { it.label }) { place = it }
        SectionTitle("Daily activity")
        Choice(Activity.entries, activity, { it.label }) { activity = it }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                onSubmit(initial.copy(name = name.trim(), sex = sex, age = ageN!!, heightCm = heightN!!,
                    weightKg = weightN!!, goal = goal, place = place, activity = activity))
            },
            enabled = valid,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text(cta) }
        if (!valid) {
            Text("Enter your name, age 10–100, height 100–250 cm and weight 25–300 kg.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> Choice(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { FilterChip(selected = it == selected, onClick = { onSelect(it) }, label = { Text(label(it)) }) }
    }
}

@Composable
fun NumberField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value, { v -> onChange(v.filter { it.isDigit() || it == '.' }.take(5)) },
        label = { Text(label, maxLines = 1) }, singleLine = true, modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}

private fun Float.clean() = if (this % 1f == 0f) toInt().toString() else toString()
