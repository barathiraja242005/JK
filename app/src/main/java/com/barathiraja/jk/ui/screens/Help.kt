package com.barathiraja.jk.ui.screens

import com.barathiraja.jk.ui.components.BackScreen
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.barathiraja.jk.ui.components.JkCard
import com.barathiraja.jk.ui.components.SectionTitle
import com.barathiraja.jk.ui.components.openUrl

private val faq = listOf(
    "How does JK pick today's workout?" to "It rotates through programs that match where you train (home or gym) and your goal, with a recovery session every 7th day for home users.",
    "Why aren't my steps counting?" to "Tap Enable on the Health tab to allow activity access. JK reads your phone's built-in step sensor while the app is open and catches up on steps when you reopen it.",
    "Do I need internet?" to "No for the main features. Built-in workouts, tracking, diet and meditation work offline. Photos for the common exercises are built in; the rest of the library loads online the first time, then is cached. Tutorial videos open on YouTube.",
    "Where is my data stored?" to "Only on this phone. JK has no account and sends nothing to a server. Uninstalling the app deletes your data.",
    "How are calories calculated?" to "Workout calories use MET values × your weight × time. Daily targets use the Mifflin–St Jeor equation. Treat them as estimates.",
    "How do challenges work?" to "Each day unlocks a workout that gets slightly harder. Finish it in the player and the day is ticked off automatically. 💤 days are for recovery.",
    "Can I make my own workouts?" to "Yes — Train → My workouts → New workout. Add any exercise from the library, choose reps or time, rounds and rest.",
)

private val guide = listOf(
    "Today" to "Your daily plan: workout of the day, streak, steps, water and calorie burn, and quick actions.",
    "Train" to "Programs for home and gym, 30-day challenges, the exercise library with photos and video tutorials, and your custom workouts.",
    "Health" to "Water, steps and walks, diet log and meal plans, fasting, meditation, breathing, calculators, timers and guides.",
    "Progress" to "Weekly charts, weight trend, workout history and transformation photos. Your profile and settings live under the avatar.",
)

@Composable
fun HelpScreen(nav: NavHostController) {
    val context = LocalContext.current
    BackScreen("Help & about", onBack = { nav.popBackStack() }) {
        item { SectionTitle("User guide") }
        items(guide) { (t, d) ->
            JkCard(Modifier.fillMaxWidth()) {
                Text(t, style = MaterialTheme.typography.titleMedium)
                Text(d, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { SectionTitle("FAQ") }
        items(faq) { (q, a) ->
            var open by remember { mutableStateOf(false) }
            JkCard(Modifier.fillMaxWidth().clickable { open = !open }) {
                Text(q, style = MaterialTheme.typography.titleSmall)
                AnimatedVisibility(open) { Text(a, Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        item {
            OutlinedButton(onClick = {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).putExtra(Intent.EXTRA_SUBJECT, "JK app feedback"))
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Send feedback by email") }
        }
        item { SectionTitle("Credits & licences") }
        item {
            JkCard(Modifier.fillMaxWidth()) {
                Text("Exercise data & photos", style = MaterialTheme.typography.titleSmall)
                Text("free-exercise-db by yuhonas — released into the public domain (Unlicense).",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("github.com/yuhonas/free-exercise-db", color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { context.openUrl("https://github.com/yuhonas/free-exercise-db") })
                Text("Typeface", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                Text("IBM Plex Sans & Mono by IBM — SIL Open Font License 1.1.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Icons", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                Text("Material Symbols by Google — Apache License 2.0.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Sounds & animations", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                Text("Generated in-app by JK.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Text("JK is for general fitness and wellbeing. It is not medical advice — consult a professional before starting a new programme if you have health conditions.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp))
        }
    }
}
