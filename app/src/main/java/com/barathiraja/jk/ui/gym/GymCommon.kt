package com.barathiraja.jk.ui.gym

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.barathiraja.jk.gym.Role
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barathiraja.jk.gym.AssignStatus
import com.barathiraja.jk.gym.Assignment
import com.barathiraja.jk.gym.Person
import com.barathiraja.jk.ui.components.JkCard
import com.barathiraja.jk.ui.components.Pill
import com.barathiraja.jk.ui.components.shareText
import com.barathiraja.jk.ui.components.Avatar
import com.barathiraja.jk.ui.screens.trimZero
import com.barathiraja.jk.ui.theme.CodeFont
import com.barathiraja.jk.ui.theme.Alert
import com.barathiraja.jk.ui.theme.Leaf
import com.barathiraja.jk.ui.theme.Sun
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val dayFmt = DateTimeFormatter.ofPattern("EEE d MMM")

fun dayLabel(epochDay: Long, today: Long): String = when (epochDay) {
    today -> "Today"
    today + 1 -> "Tomorrow"
    today - 1 -> "Yesterday"
    else -> LocalDate.ofEpochDay(epochDay).format(dayFmt)
}

/** Status of one assignment as a coloured pill: done / in progress / missed / to do. */
@Composable
fun StatusPill(a: Assignment?, today: Long, modifier: Modifier = Modifier) {
    val (text, color) = when {
        a == null -> "Nothing assigned" to MaterialTheme.colorScheme.outline
        a.status == AssignStatus.DONE -> (if (a.verified) "✓ Done · verified" else "✓ Done") to Leaf
        a.status == AssignStatus.IN_PROGRESS -> "● ${a.exercisesDone}/${a.exercises.size} in progress" to Sun
        a.epochDay < today -> "✗ Missed" to Alert
        else -> "Not started" to MaterialTheme.colorScheme.outline
    }
    Pill(text, color, modifier)
}

@Composable
fun PersonRow(p: Person, modifier: Modifier = Modifier, size: Dp = 44.dp, trailing: @Composable () -> Unit = {}, sub: @Composable () -> Unit = {}) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Avatar(p.photoUrl, p.name, size)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(p.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            sub()
        }
        trailing()
    }
}

/** Big, readable join code with a share button (WhatsApp etc.). */
@Composable
fun CodeCard(title: String, code: String, shareText: String) {
    val context = LocalContext.current
    JkCard(Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                Text(code, Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontSize = 32.sp, fontWeight = FontWeight.Bold,
                    fontFamily = CodeFont, letterSpacing = 4.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.weight(1f))
            FilledTonalButton(onClick = { context.shareText(shareText) }) {
                Icon(Icons.Filled.Share, null); Spacer(Modifier.width(6.dp)); Text("Share")
            }
        }
    }
}

/** Completion rate as a percentage string, or "–" when nothing was due. */
fun pct(done: Int, due: Int) = if (due == 0) "–" else "${Math.round(done * 100f / due)}%"

fun rateColor(rate: Float, due: Int): Color = when {
    due == 0 -> Color.Gray
    rate >= 0.8f -> Leaf
    rate >= 0.5f -> Sun
    else -> Alert
}

/** Gym section on the Me screen: who you are in the gym, leave, sign out; or join a gym if using JK alone. */
@Composable
fun GymAccountCard(gvm: com.barathiraja.jk.ui.GymViewModel) {
    val state by gvm.state.collectAsStateWithLifecycle()
    var confirmSignOut by remember { mutableStateOf(false) }
    var confirmLeave by remember { mutableStateOf(false) }
    when (val s = state) {
        is com.barathiraja.jk.ui.GymState.Ready -> JkCard(Modifier.fillMaxWidth()) {
            Text(s.gym.name, style = MaterialTheme.typography.titleLarge)
            val role = when (s.me.role) { Role.OWNER -> "Owner"; Role.TRAINER -> "Trainer"; Role.MEMBER -> "Member" }
            Text(role + (gvm.trainerOf(s.me)?.let { " · Coach ${it.name}" } ?: ""), color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (s.me.role == Role.TRAINER && !s.me.trainerCode.isNullOrBlank()) Text("Member code: ${s.me.trainerCode}", Modifier.padding(top = 4.dp))
            if (s.me.role == Role.OWNER) Text("Gym code: ${s.gym.gymCode}", Modifier.padding(top = 4.dp))
            Row(Modifier.padding(top = 8.dp)) {
                if (s.me.role != Role.OWNER) TextButton(onClick = { confirmLeave = true }) { Text("Leave gym") }
                TextButton(onClick = { confirmSignOut = true }) { Text("Sign out") }
            }
        }
        else -> {}
    }
    if (confirmSignOut) AlertDialog(onDismissRequest = { confirmSignOut = false },
        title = { Text("Sign out?") },
        text = { Text("Your gym workouts stay safe in the gym. Personal data on this phone (water, diet, history) will be cleared.") },
        confirmButton = { TextButton(onClick = { confirmSignOut = false; gvm.signOut() }) { Text("Sign out") } },
        dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("Cancel") } })
    if (confirmLeave) AlertDialog(onDismissRequest = { confirmLeave = false },
        title = { Text("Leave the gym?") },
        text = { Text("You'll stop getting workouts from your trainer here. You can join again with a code.") },
        confirmButton = { TextButton(onClick = { confirmLeave = false; gvm.leaveGym() }) { Text("Leave") } },
        dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text("Cancel") } })
}

/** One set as "reps × kg" (or seconds) with small steppers; fits inside a card. */
@Composable
fun SetEditorRow(index: Int, s: com.barathiraja.jk.data.SetSpec, step: Float, onChange: (com.barathiraja.jk.data.SetSpec) -> Unit, onDelete: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("${index + 1}", Modifier.width(22.dp), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        if (s.timed) {
            MiniStepper(com.barathiraja.jk.ui.components.formatDuration(s.seconds.toLong()), "time", Modifier.weight(1f),
                { onChange(s.copy(seconds = (s.seconds - 15).coerceAtLeast(10))) }, { onChange(s.copy(seconds = s.seconds + 15)) })
        } else {
            MiniStepper("${s.reps}", "reps", Modifier.weight(1f),
                { onChange(s.copy(reps = (s.reps - 1).coerceAtLeast(1))) }, { onChange(s.copy(reps = (s.reps + 1).coerceAtMost(100))) })
            MiniStepper(if (s.weightKg > 0f) s.weightKg.trimZero() else "BW", "kg", Modifier.weight(1f),
                { onChange(s.copy(weightKg = (s.weightKg - step).coerceAtLeast(0f))) }, { onChange(s.copy(weightKg = s.weightKg + step)) })
        }
        if (onDelete != null) androidx.compose.material3.IconButton(onClick = onDelete, Modifier.size(36.dp)) {
            Icon(Icons.Filled.Close, "Delete set", Modifier.size(18.dp))
        } else Spacer(Modifier.width(36.dp))
    }
}

@Composable
private fun MiniStepper(value: String, unit: String, modifier: Modifier, minus: () -> Unit, plus: () -> Unit) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
        androidx.compose.material3.FilledTonalIconButton(onClick = minus, Modifier.size(30.dp)) {
            Icon(Icons.Filled.Remove, "Less $unit", Modifier.size(16.dp))
        }
        Column(Modifier.width(52.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Text(unit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        androidx.compose.material3.FilledTonalIconButton(onClick = plus, Modifier.size(30.dp)) {
            Icon(Icons.Filled.Add, "More $unit", Modifier.size(16.dp))
        }
    }
}

fun plural(n: Int, word: String) = "$n $word${if (n == 1) "" else "s"}"
