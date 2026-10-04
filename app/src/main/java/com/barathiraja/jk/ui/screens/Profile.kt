package com.barathiraja.jk.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Share
import com.barathiraja.jk.ui.components.shareText
import com.barathiraja.jk.ui.theme.Aqua
import com.barathiraja.jk.ui.theme.Ember
import com.barathiraja.jk.ui.theme.Leaf
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.barathiraja.jk.data.Settings
import com.barathiraja.jk.data.ThemeMode
import com.barathiraja.jk.domain.Health
import com.barathiraja.jk.ui.JkViewModel
import com.barathiraja.jk.ui.Routes
import com.barathiraja.jk.ui.components.JkCard
import com.barathiraja.jk.ui.components.KeyValue
import com.barathiraja.jk.ui.components.SectionTitle

@Composable
fun ProfileScreen(vm: JkViewModel, nav: NavHostController) {
    val p by vm.profile.collectAsStateWithLifecycle()
    val s by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Ask for notification permission (Android 13+) before turning a reminder on.
    var pending by remember { mutableStateOf<Settings?>(null) }
    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) pending?.let(vm::saveSettings)
    }
    fun enableReminder(next: Settings) {
        if (Build.VERSION.SDK_INT >= 33) {
            pending = next
            notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else vm.saveSettings(next)
    }

    val avatar by vm.avatar.collectAsStateWithLifecycle()
    val pickAvatar = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            vm.setAvatar(uri.toString())
        }
    }

    BackScreen("Me", onBack = { nav.popBackStack() }) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(avatar, p.name, 80.dp) {
                    pickAvatar.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
                androidx.compose.foundation.layout.Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.name.ifBlank { "You" }, style = MaterialTheme.typography.headlineSmall)
                    Text("Tap the photo to change it", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            JkCard(Modifier.fillMaxWidth()) {
                val bmi = Health.bmi(p.weightKg, p.heightCm)
                KeyValue("Goal", p.goal.label)
                KeyValue("Trains at", p.place.label)
                KeyValue("Age", "${p.age}")
                KeyValue("Height", "${p.heightCm.toInt()} cm")
                KeyValue("Weight", "%.1f kg".format(p.weightKg))
                KeyValue("BMI", "%.1f (%s)".format(bmi, Health.bmiCategory(bmi)))
                KeyValue("Daily calories", "${Health.targetCalories(p)} kcal")
                OutlinedButton(onClick = { nav.navigate(Routes.EDIT_PROFILE) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text("Edit profile")
                }
            }
        }

        item { SectionTitle("Appearance") }
        item {
            Choice(ThemeMode.entries, s.theme, { it.name.lowercase().replaceFirstChar(Char::uppercase) }) {
                vm.saveSettings(s.copy(theme = it))
            }
        }

        item { SectionTitle("Goals") }
        item {
            JkCard(Modifier.fillMaxWidth()) {
                Stepper("Daily steps", "%,d".format(s.stepGoal),
                    onMinus = { vm.saveSettings(s.copy(stepGoal = (s.stepGoal - 1000).coerceAtLeast(2000))) },
                    onPlus = { vm.saveSettings(s.copy(stepGoal = (s.stepGoal + 1000).coerceAtMost(30000))) })
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Stepper("Water (glasses)", "${s.waterGoalGlasses}",
                    onMinus = { vm.saveSettings(s.copy(waterGoalGlasses = (s.waterGoalGlasses - 1).coerceAtLeast(4))) },
                    onPlus = { vm.saveSettings(s.copy(waterGoalGlasses = (s.waterGoalGlasses + 1).coerceAtMost(20))) })
            }
        }

        item { SectionTitle("Workout & reminders") }
        item {
            JkCard(Modifier.fillMaxWidth()) {
                Toggle("Voice coach", "Spoken cues during workouts", s.voiceCues) { vm.saveSettings(s.copy(voiceCues = it)) }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Toggle("Daily workout reminder", "Every day at %02d:%02d".format(s.reminderHour, s.reminderMinute), s.workoutReminder) {
                    if (it) enableReminder(s.copy(workoutReminder = true)) else vm.saveSettings(s.copy(workoutReminder = false))
                }
                TextButton(onClick = {
                    TimePickerDialog(context, { _, h, m ->
                        vm.saveSettings(vm.settings.value.copy(reminderHour = h, reminderMinute = m))
                    }, s.reminderHour, s.reminderMinute, true).show()
                }) { Text("Change time") }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Toggle("Hydration reminders", "Every ~2 hours, 8am–10pm", s.waterReminder) {
                    if (it) enableReminder(s.copy(waterReminder = true)) else vm.saveSettings(s.copy(waterReminder = false))
                }
            }
        }

        item { SectionTitle("More") }
        item { NavRow(Icons.Outlined.PhotoCamera, "Transformation photos", "Before/after progress pictures", Ember) { nav.navigate(Routes.PHOTOS) } }
        item { NavRow(Icons.AutoMirrored.Outlined.HelpOutline, "Help & about", "User guide, FAQ, credits", Aqua) { nav.navigate(Routes.HELP) } }
        item {
            NavRow(Icons.Outlined.Share, "Share JK", "Invite a friend to train with you", Leaf) {
                context.shareText("I'm training with JK — workouts, challenges, diet and meditation in one app. Join me!")
            }
        }
        item {
            Text("JK v2.0 · Your data stays on this device.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 16.dp))
        }
    }
}

@Composable
private fun Toggle(title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked, onChange)
    }
}

@Composable
private fun Stepper(title: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
        FilledTonalIconButton(onClick = onMinus) { Icon(Icons.Filled.Remove, "Decrease") }
        Text(value, Modifier.width(64.dp), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        FilledTonalIconButton(onClick = onPlus) { Icon(Icons.Filled.Add, "Increase") }
    }
}
