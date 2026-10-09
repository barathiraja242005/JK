package com.barathiraja.jk.ui.gym

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role as A11yRole
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.barathiraja.jk.gym.Person
import com.barathiraja.jk.gym.Role
import com.barathiraja.jk.ui.GymViewModel
import com.barathiraja.jk.ui.components.PersonAvatar
import com.barathiraja.jk.ui.components.PlainButton
import com.barathiraja.jk.ui.components.RedButton
import com.barathiraja.jk.ui.theme.Jk
import com.barathiraja.jk.ui.theme.plex
import com.barathiraja.jk.gym.plural

/*
 * The owner's people tools, at the bottom of a trainer's or member's page: move a member to another trainer,
 * or take someone out of the gym. Every step says in words what will happen before anything changes.
 */

/** A tappable trainer choice with a round tick: 56dp tall, name in full. */
@Composable
private fun TrainerOption(p: Person, note: String, selected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(if (selected) cs.secondaryContainer else Color.Transparent)
            .semantics { this.selected = selected }
            .clickable(role = A11yRole.RadioButton, onClick = onClick)
            .heightIn(min = 50.dp).padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PersonAvatar(p.photoUrl, p.name, 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(p.name, style = plex(15.sp, FontWeight.SemiBold), color = cs.onSurface, maxLines = 1)
            Text(note, style = plex(13.sp), color = cs.onSurfaceVariant, maxLines = 1)
        }
        Box(Modifier.size(24.dp).clip(CircleShape).background(if (selected) Jk.Red else Color.Transparent)
            .border(1.5.dp, if (selected) Jk.Red else cs.outline, CircleShape), contentAlignment = Alignment.Center) {
            if (selected) Icon(Icons.Outlined.Check, null, Modifier.size(16.dp), tint = Color.White)
        }
    }
}

/** Pick which trainer a member trains with. */
@Composable
internal fun ChangeTrainerDialog(member: Person, gvm: GymViewModel, onDismiss: () -> Unit) {
    val people by gvm.people.collectAsStateWithLifecycle()
    val trainers = people.filter { it.role == Role.TRAINER && it.active }
    var pick by remember { mutableStateOf(member.trainerUid?.takeIf { uid -> trainers.any { it.uid == uid } }) }
    val chosen = trainers.firstOrNull { it.uid == pick }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Who trains ${member.firstName}?") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (trainers.isEmpty()) Text("There are no trainers yet. Share your gym code so one can join.", style = plex(15.sp, line = 21.sp))
                else Text("${member.firstName}'s new workouts will come from this trainer. Past workouts stay as they are.",
                    style = plex(14.sp, line = 19.sp), modifier = Modifier.padding(bottom = 8.dp))
                trainers.forEach { t ->
                    val n = gvm.membersOf(t.uid).size
                    TrainerOption(t, if (t.uid == member.trainerUid) "Trains ${member.firstName} now" else "Trains ${plural(n, "member")}", t.uid == pick) { pick = t.uid }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = chosen != null && chosen.uid != member.trainerUid, onClick = { onDismiss(); gvm.changeTrainer(member, chosen!!) }) {
                Text(chosen?.let { "Move to ${it.firstName}" } ?: "Move", style = plex(14.sp, FontWeight.SemiBold))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", style = plex(14.sp, FontWeight.SemiBold), color = Jk.Ink) } },
    )
}

/**
 * Take someone out of the gym. For a trainer with members, the owner first picks who takes over their members
 * (the best other trainer is picked already); with no other trainer, the members wait without one.
 */
@Composable
internal fun RemovePersonDialog(p: Person, gvm: GymViewModel, onDismiss: () -> Unit, onRemoved: () -> Unit) {
    val isTrainer = p.role == Role.TRAINER
    val theirs = if (isTrainer) gvm.membersOf(p.uid) else emptyList()
    val people by gvm.people.collectAsStateWithLifecycle()
    val ranking by gvm.trainerRanking.collectAsStateWithLifecycle()
    val others = if (isTrainer) people.filter { it.role == Role.TRAINER && it.active && it.uid != p.uid } else emptyList()
    val ranked = ranking.map { it.uid }
    var pick by remember { mutableStateOf(others.minByOrNull { t -> ranked.indexOf(t.uid).let { if (it < 0) Int.MAX_VALUE else it } }?.uid) }
    val moveTo = others.firstOrNull { it.uid == pick }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remove ${p.firstName} from the gym?") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${p.firstName} won't be able to open your gym in JK any more. Their past workouts stay in the records.",
                    style = plex(14.sp, line = 19.sp))
                if (theirs.isNotEmpty()) {
                    if (others.isEmpty()) Text(
                        "${p.firstName} trains ${plural(theirs.size, "member")}. There is no other trainer, so they will have no trainer until you choose one.",
                        style = plex(14.sp, line = 19.sp, weight = FontWeight.SemiBold), modifier = Modifier.padding(top = 10.dp),
                    ) else {
                        Text("Who should train ${p.firstName}'s ${plural(theirs.size, "member")}?", style = plex(15.sp, FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                        others.forEach { t -> TrainerOption(t, "Trains ${plural(gvm.membersOf(t.uid).size, "member")}", t.uid == pick) { pick = t.uid } }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onDismiss(); gvm.removeFromGym(p, moveTo); onRemoved() }) {
                Text("Remove ${p.firstName}", style = plex(14.sp, FontWeight.SemiBold), color = Jk.RedText)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Keep ${p.firstName}", style = plex(14.sp, FontWeight.SemiBold), color = Jk.Ink) } },
    )
}

/**
 * The owner's actions for one person, in one row right under their summary card: Message (red, the usual next
 * step), Change trainer for members, and Remove (asks first). Nothing is hidden at the bottom of a long page.
 */
@Composable
internal fun OwnerPersonActions(p: Person, gvm: GymViewModel, onRemoved: () -> Unit) {
    val context = LocalContext.current
    var changing by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf(false) }
    val member = p.role == Role.MEMBER
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        RedButton("Message", { context.whatsApp("Hi ${p.firstName}, ") }, Modifier.weight(1f),
            Icons.AutoMirrored.Outlined.Send)
        if (member) PlainButton("Trainer", { changing = true }, Modifier.weight(1f), Icons.Outlined.SwapHoriz)
        Surface(onClick = { removing = true }, shape = CircleShape, color = Jk.Card, contentColor = Jk.RedText,
            border = BorderStroke(1.5.dp, Jk.Line), modifier = Modifier.size(48.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.PersonRemove, "Remove ${p.firstName} from the gym", Modifier.size(20.dp)) }
        }
    }
    if (changing) ChangeTrainerDialog(p, gvm) { changing = false }
    if (removing) RemovePersonDialog(p, gvm, onDismiss = { removing = false }, onRemoved = onRemoved)
}

/** A trainer's actions for one of their members, under the summary card: Assign (red), Message, and Remove. */
@Composable
internal fun TrainerPersonActions(member: Person, onAssign: () -> Unit, onRemove: () -> Unit) {
    val context = LocalContext.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        RedButton("Assign", onAssign, Modifier.weight(1f), Icons.Outlined.Add)
        PlainButton("Message", { context.whatsApp("Hi ${member.firstName}, ") }, Modifier.weight(1f), Icons.AutoMirrored.Outlined.Send)
        Surface(onClick = onRemove, shape = CircleShape, color = Jk.Card, contentColor = Jk.RedText,
            border = BorderStroke(1.5.dp, Jk.Line), modifier = Modifier.size(48.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.PersonRemove, "Remove ${member.firstName} from your members", Modifier.size(20.dp)) }
        }
    }
}
