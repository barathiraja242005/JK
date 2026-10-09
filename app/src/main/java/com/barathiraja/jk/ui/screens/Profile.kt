package com.barathiraja.jk.ui.screens

import com.barathiraja.jk.ui.components.Avatar
import com.barathiraja.jk.ui.components.BackScreen
import android.Manifest
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.barathiraja.jk.ui.theme.Jk
import com.barathiraja.jk.ui.components.PersonAvatar
import com.barathiraja.jk.ui.components.CardBox
import com.barathiraja.jk.ui.components.Heading
import com.barathiraja.jk.ui.components.PlainButton
import com.barathiraja.jk.ui.theme.plex
import android.app.TimePickerDialog
import android.content.Intent
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material3.HorizontalDivider
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
import com.barathiraja.jk.domain.Health
import com.barathiraja.jk.ui.JkViewModel
import com.barathiraja.jk.ui.Routes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CircleShape
import com.barathiraja.jk.ui.GymViewModel
import com.barathiraja.jk.ui.gym.GymAccountCard

@Composable
fun ProfileScreen(vm: JkViewModel, nav: NavHostController, gvm: GymViewModel) {
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
                val pick = { pickAvatar.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                val gymPhoto = gvm.me.collectAsStateWithLifecycle().value?.photoUrl
                // The photo picked on this phone wins; else the Google photo the gym shows; else the initial.
                if (avatar == null) Box(
                    Modifier.clip(CircleShape).clickable(onClickLabel = "Change photo", onClick = pick),
                ) { PersonAvatar(gymPhoto, p.name, 80.dp) }
                else Avatar(avatar, p.name, 80.dp, pick)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.name.ifBlank { "You" }, style = plex(22.sp, FontWeight.Bold, line = 28.sp), color = Jk.Ink)
                    Text("Tap the photo to change it", style = plex(14.sp), color = Jk.Muted)
                }
            }
        }
        item {
            CardBox(padding = 16.dp) {
                Column {
                    Text("Your details", style = plex(17.sp, FontWeight.Bold), color = Jk.Ink)
                    val bmi = Health.bmi(p.weightKg, p.heightCm)
                    Detail("Goal", p.goal.label)
                    Detail("Trains at", p.place.label)
                    Detail("Age", "${p.age}")
                    Detail("Height", "${p.heightCm.toInt()} cm")
                    Detail("Weight", "%.1f kg".format(p.weightKg))
                    Detail("BMI", "%.1f (%s)".format(bmi, Health.bmiCategory(bmi)))
                    Detail("Daily calories", "${Health.targetCalories(p)} kcal")
                    PlainButton("Edit details", { nav.navigate(Routes.EDIT_PROFILE) }, Modifier.fillMaxWidth().padding(top = 12.dp))
                }
            }
        }

        item { Heading("Reminders") }
        item {
            CardBox(padding = 16.dp) {
                Column {
                    Toggle("Daily workout reminder", "Every day at %02d:%02d".format(s.reminderHour, s.reminderMinute), s.workoutReminder) {
                        if (it) enableReminder(s.copy(workoutReminder = true)) else vm.saveSettings(s.copy(workoutReminder = false))
                    }
                    TextButton(onClick = {
                        TimePickerDialog(context, { _, h, m ->
                            vm.saveSettings(vm.settings.value.copy(reminderHour = h, reminderMinute = m))
                        }, s.reminderHour, s.reminderMinute, true).show()
                    }) { Text("Change time", style = plex(15.sp, FontWeight.SemiBold), color = Jk.RedText) }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Jk.Line)
                    Toggle("Voice coach", "Spoken cues during guided workouts", s.voiceCues) { vm.saveSettings(s.copy(voiceCues = it)) }
                }
            }
        }

        item { NavRow(Icons.AutoMirrored.Outlined.HelpOutline, "Help", "User guide and questions") { nav.navigate(Routes.HELP) } }
        // Leaving and signing out go last, away from everyday settings.
        run {
            item { Heading("Account") }
            item { GymAccountCard(gvm) }
            // For trying the app out: a quiet link, last on the page, so members don't mistake it for a setting.
            if (!gvm.testMode) item {
                TextButton(onClick = { gvm.setTestMode(true) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Try JK on a made-up gym (test mode)", style = plex(14.sp), color = Jk.Muted)
                }
            }
        }
    }
}

@Composable
private fun Detail(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Text(label, Modifier.weight(1f), style = plex(15.sp), color = Jk.Muted)
        Text(value, style = plex(15.sp, FontWeight.SemiBold), color = Jk.Ink)
    }
}

@Composable
private fun Toggle(title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = plex(15.sp, FontWeight.SemiBold), color = Jk.Ink)
            Text(sub, style = plex(13.sp), color = Jk.Muted)
        }
        Switch(checked, onChange)
    }
}
