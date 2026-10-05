package com.barathiraja.jk.ui.gym

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role as A11yRole
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barathiraja.jk.gym.Person
import com.barathiraja.jk.ui.GymViewModel

/*
 * The owner's people tools, at the bottom of a trainer's or member's page: move a member to another trainer,
 * or take someone out of the gym. Every step says in words what will happen before anything changes.
 */

/** A full-width outlined button; [danger] makes its words red, for removing. */
@Composable
internal fun ManageButton(text: String, icon: ImageVector, danger: Boolean = false, onClick: () -> Unit) =
    PlainButton(text, onClick, Modifier.fillMaxWidth(), icon, ink = if (danger) Owner.RedText else Owner.Ink)

/** A tappable trainer choice with a round tick: 56dp tall, name in full. */
@Composable
private fun TrainerOption(p: Person, note: String, selected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(if (selected) cs.secondaryContainer else Color.Transparent)
            .semantics { this.selected = selected }
            .clickable(role = A11yRole.RadioButton, onClick = onClick)
            .heightIn(min = 60.dp).padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OwnerAvatar(p.photoUrl, p.name, 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(p.name, style = plex(17.sp, FontWeight.SemiBold), color = cs.onSurface, maxLines = 1)
            Text(note, style = plex(14.sp), color = cs.onSurfaceVariant, maxLines = 1)
        }
        Box(Modifier.size(28.dp).clip(CircleShape).background(if (selected) Owner.Red else Color.Transparent)
            .border(1.5.dp, if (selected) Owner.Red else cs.outline, CircleShape), contentAlignment = Alignment.Center) {
            if (selected) Icon(Icons.Outlined.Check, null, Modifier.size(16.dp), tint = Color.White)
        }
    }
}

/** Pick which trainer a member trains with. */
@Composable
internal fun ChangeTrainerDialog(member: Person, gvm: GymViewModel, onDismiss: () -> Unit) {
    val trainers = gvm.people.value.filter { it.role == com.barathiraja.jk.gym.Role.TRAINER && it.active }
    var pick by remember { mutableStateOf(member.trainerUid?.takeIf { uid -> trainers.any { it.uid == uid } }) }
    val chosen = trainers.firstOrNull { it.uid == pick }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Who trains ${member.firstName}?") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (trainers.isEmpty()) Text("There are no trainers yet. Share your gym code so one can join.", style = plex(17.sp, line = 25.sp))
                else Text("${member.firstName}'s new workouts will come from this trainer. Past workouts stay as they are.",
                    style = plex(16.sp, line = 23.sp), modifier = Modifier.padding(bottom = 8.dp))
                trainers.forEach { t ->
                    val n = gvm.membersOf(t.uid).size
                    TrainerOption(t, if (t.uid == member.trainerUid) "Trains ${member.firstName} now" else "Trains ${plural(n, "member")}", t.uid == pick) { pick = t.uid }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = chosen != null && chosen.uid != member.trainerUid, onClick = { onDismiss(); gvm.changeTrainer(member, chosen!!) }) {
                Text(chosen?.let { "Move to ${it.firstName}" } ?: "Move", style = plex(16.sp, FontWeight.SemiBold))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", style = plex(16.sp, FontWeight.SemiBold)) } },
    )
}

/**
 * Take someone out of the gym. For a trainer with members, the owner first picks who takes over their members
 * (the best other trainer is picked already); with no other trainer, the members wait without one.
 */
@Composable
internal fun RemovePersonDialog(p: Person, gvm: GymViewModel, onDismiss: () -> Unit, onRemoved: () -> Unit) {
    val isTrainer = p.role == com.barathiraja.jk.gym.Role.TRAINER
    val theirs = if (isTrainer) gvm.membersOf(p.uid) else emptyList()
    val others = if (isTrainer) gvm.people.value.filter { it.role == com.barathiraja.jk.gym.Role.TRAINER && it.active && it.uid != p.uid } else emptyList()
    val ranked = gvm.trainerRanking.value.map { it.uid }
    var pick by remember { mutableStateOf(others.minByOrNull { t -> ranked.indexOf(t.uid).let { if (it < 0) Int.MAX_VALUE else it } }?.uid) }
    val moveTo = others.firstOrNull { it.uid == pick }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remove ${p.firstName} from the gym?") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${p.firstName} won't be able to open your gym in JK any more. Their past workouts stay in the records.",
                    style = plex(16.sp, line = 23.sp))
                if (theirs.isNotEmpty()) {
                    if (others.isEmpty()) Text(
                        "${p.firstName} trains ${plural(theirs.size, "member")}. There is no other trainer, so they will have no trainer until you choose one.",
                        style = plex(16.sp, line = 23.sp, weight = FontWeight.SemiBold), modifier = Modifier.padding(top = 10.dp),
                    ) else {
                        Text("Who should train ${p.firstName}'s ${plural(theirs.size, "member")}?", style = plex(17.sp, FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                        others.forEach { t -> TrainerOption(t, "Trains ${plural(gvm.membersOf(t.uid).size, "member")}", t.uid == pick) { pick = t.uid } }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onDismiss(); gvm.removeFromGym(p, moveTo); onRemoved() }) {
                Text("Remove ${p.firstName}", style = plex(16.sp, FontWeight.SemiBold), color = Owner.RedText)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Keep ${p.firstName}", style = plex(16.sp, FontWeight.SemiBold)) } },
    )
}

/** The owner's tools under a member's page. */
@Composable
internal fun MemberManage(member: Person, gvm: GymViewModel, onRemoved: () -> Unit) {
    var changing by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PageHeading("Manage ${member.firstName}", "Move ${member.firstName} to another trainer, or take them out of the gym.")
        Spacer(Modifier.size(2.dp))
        ManageButton("Change trainer", Icons.Outlined.SwapHoriz) { changing = true }
        ManageButton("Remove from gym", Icons.Outlined.PersonRemove, danger = true) { removing = true }
    }
    if (changing) ChangeTrainerDialog(member, gvm) { changing = false }
    if (removing) RemovePersonDialog(member, gvm, onDismiss = { removing = false }, onRemoved = onRemoved)
}

/** The owner's tools under a trainer's page. */
@Composable
internal fun TrainerManage(trainer: Person, gvm: GymViewModel, onRemoved: () -> Unit) {
    var removing by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PageHeading("Manage ${trainer.firstName}", "To move one member, open them below and tap Change trainer.")
        Spacer(Modifier.size(2.dp))
        ManageButton("Remove from gym", Icons.Outlined.PersonRemove, danger = true) { removing = true }
    }
    if (removing) RemovePersonDialog(trainer, gvm, onDismiss = { removing = false }, onRemoved = onRemoved)
}
