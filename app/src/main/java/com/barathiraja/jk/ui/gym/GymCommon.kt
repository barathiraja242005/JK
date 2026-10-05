package com.barathiraja.jk.ui.gym

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.barathiraja.jk.data.SetSpec
import com.barathiraja.jk.gym.AssignStatus
import com.barathiraja.jk.gym.Assignment
import com.barathiraja.jk.gym.Person
import com.barathiraja.jk.gym.Role
import com.barathiraja.jk.ui.GymState
import com.barathiraja.jk.ui.GymViewModel
import com.barathiraja.jk.ui.components.formatDuration
import com.barathiraja.jk.ui.components.shareText
import com.barathiraja.jk.ui.screens.trimZero
import com.barathiraja.jk.ui.theme.Bad
import com.barathiraja.jk.ui.theme.CodeFont
import com.barathiraja.jk.ui.theme.Good
import com.barathiraja.jk.ui.theme.Watch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.ui.semantics.Role as SemanticsRole

private val dayFmt = DateTimeFormatter.ofPattern("EEE d MMM")

fun dayLabel(epochDay: Long, today: Long): String = when (epochDay) {
    today -> "Today"
    today + 1 -> "Tomorrow"
    today - 1 -> "Yesterday"
    else -> LocalDate.ofEpochDay(epochDay).format(dayFmt)
}

/** Status of one assignment as a word in a chip: done / in progress / missed / not started. */
@Composable
fun StatusPill(a: Assignment?, today: Long, modifier: Modifier = Modifier) {
    val (text, tone) = when {
        a == null -> "Nothing assigned" to Tone.NONE
        a.status == AssignStatus.DONE -> (if (a.verified) "Done · verified" else "Done") to Tone.GOOD
        a.status == AssignStatus.IN_PROGRESS -> "${a.exercisesDone}/${a.exercises.size} in progress" to Tone.WARN
        a.epochDay < today -> "Missed" to Tone.BAD
        else -> "Not started" to Tone.NONE
    }
    OwnerChip(text, tone, modifier)
}

/** A person's photo and name in a row, with an optional line under the name and something on the right. */
@Composable
internal fun MemberLine(p: Person, modifier: Modifier = Modifier, size: Dp = 44.dp, trailing: @Composable () -> Unit = {}, sub: @Composable () -> Unit = {}) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        OwnerAvatar(p.photoUrl, p.name, size)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(p.name, style = plex(16.sp, FontWeight.SemiBold), color = Owner.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            sub()
        }
        trailing()
    }
}

/** The signed-in person's photo in a page header; tapping it opens their profile. */
@Composable
internal fun ProfileButton(p: Person, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape)
            .clickable(onClickLabel = "Open your profile", role = SemanticsRole.Button, onClick = onClick)
            .semantics { contentDescription = "Your profile" },
    ) { OwnerAvatar(p.photoUrl, p.name, 48.dp) }
}

/** Completion rate as a percentage string, or "–" when nothing was due. */
fun pct(done: Int, due: Int) = if (due == 0) "–" else "${Math.round(done * 100f / due)}%"

/** Gym section on a member's Me screen (staff have their own Me tab): who you are in the gym, leave, sign out; or join a gym if using JK alone. */
@Composable
fun GymAccountCard(gvm: GymViewModel) {
    val state by gvm.state.collectAsStateWithLifecycle()
    var confirmSignOut by remember { mutableStateOf(false) }
    var confirmLeave by remember { mutableStateOf(false) }
    when (val s = state) {
        is GymState.Ready -> OwnerCardBox {
            Column {
                Text(s.gym.name, style = plex(17.sp, FontWeight.SemiBold), color = Owner.Ink)
                val role = when (s.me.role) { Role.OWNER -> "Owner"; Role.TRAINER -> "Trainer"; Role.MEMBER -> "Member" }
                Text(role + (gvm.trainerOf(s.me)?.let { " · Coach ${it.name}" } ?: ""), style = plex(15.sp), color = Owner.Muted)
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (s.me.role != Role.OWNER) PlainButton("Leave gym", { confirmLeave = true }, ink = Owner.RedText)
                    PlainButton("Sign out", { confirmSignOut = true })
                }
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
fun SetEditorRow(index: Int, s: SetSpec, step: Float, onChange: (SetSpec) -> Unit, onDelete: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("${index + 1}", Modifier.width(22.dp), style = plex(15.sp, FontWeight.SemiBold), color = Owner.RedText)
        if (s.timed) {
            MiniStepper(formatDuration(s.seconds.toLong()), "time", Modifier.weight(1f),
                { onChange(s.copy(seconds = (s.seconds - 15).coerceAtLeast(10))) }, { onChange(s.copy(seconds = s.seconds + 15)) })
        } else {
            MiniStepper("${s.reps}", "reps", Modifier.weight(1f),
                { onChange(s.copy(reps = (s.reps - 1).coerceAtLeast(1))) }, { onChange(s.copy(reps = (s.reps + 1).coerceAtMost(100))) })
            MiniStepper(if (s.weightKg > 0f) s.weightKg.trimZero() else "BW", "kg", Modifier.weight(1f),
                { onChange(s.copy(weightKg = (s.weightKg - step).coerceAtLeast(0f))) }, { onChange(s.copy(weightKg = s.weightKg + step)) })
        }
        // IconButton keeps a 48dp target around its smaller icon.
        if (onDelete != null) IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Close, "Delete set ${index + 1}", Modifier.size(18.dp), tint = Owner.Muted)
        } else Spacer(Modifier.width(48.dp))
    }
}

@Composable
private fun MiniStepper(value: String, unit: String, modifier: Modifier, minus: () -> Unit, plus: () -> Unit) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        StepButton(Icons.Filled.Remove, "Less $unit", minus)
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = plex(16.sp, FontWeight.SemiBold), color = Owner.Ink, maxLines = 1, softWrap = false)
            Text(unit, style = plex(13.sp), color = Owner.Muted)
        }
        StepButton(Icons.Filled.Add, "More $unit", plus)
    }
}

/**
 * A ± button: a 30dp circle to look at inside a 36 × 48dp target. Two steppers and a delete button must fit one
 * row of a card, so the target is narrower than 48dp; Compose widens touches on small targets to 48dp, and the
 * value text between the buttons isn't tappable, so nothing else competes for those touches.
 */
@Composable
private fun StepButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier.width(36.dp).height(48.dp)
            .clickable(remember { MutableInteractionSource() }, ripple(bounded = false, radius = 22.dp), role = SemanticsRole.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(30.dp).clip(CircleShape).background(Owner.Well), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(16.dp), tint = Owner.Ink)
        }
    }
}

fun plural(n: Int, word: String) = "$n $word${if (n == 1) "" else "s"}"
