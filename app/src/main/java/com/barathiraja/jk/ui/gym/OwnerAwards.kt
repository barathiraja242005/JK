package com.barathiraja.jk.ui.gym

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.barathiraja.jk.gym.GivenAward
import com.barathiraja.jk.gym.Person
import com.barathiraja.jk.ui.GymViewModel
import com.barathiraja.jk.ui.Routes

/**
 * The owner's Awards tab: everything about awards in one place. Give one (the page's red button), see who is
 * leading this month (good picks), change or take back the awards already given, and the app's monthly awards.
 */
@Composable
fun OwnerAwardsScreen(gvm: GymViewModel, nav: NavHostController) {
    val given by gvm.givenAwards.collectAsStateWithLifecycle()
    val awards by gvm.awards.collectAsStateWithLifecycle()
    val digest by gvm.ownerDigest.collectAsStateWithLifecycle()
    val ranking by gvm.memberRanking.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<GivenAward?>(null) }

    val topMember = ranking.firstOrNull { it.points > 0 }?.let { s -> gvm.person(s.uid)?.let { it to s } }
    val topTrainer = digest?.trainers?.filter { it.due > 0 }?.maxByOrNull { it.rate }

    OwnerPage {
        item { PageTitle("Awards", "Reward your best members and trainers. Everyone in the gym sees the awards you give.") }
        item { RedButton("Give an award", { nav.navigate(Routes.GIVE_AWARD) }, Modifier.fillMaxWidth().padding(top = 4.dp), Icons.Outlined.EmojiEvents) }

        if (topMember != null || topTrainer != null) {
            item { OwnerHeading("Leading this month", "Who is ahead right now. Good picks for an award.") }
            topMember?.let { (p, s) ->
                item {
                    LeaderRow(p, "Top member", "Finished ${s.completed} of ${plural(s.due, "workout")}") { nav.navigate(Routes.gymMember(p.uid)) }
                }
            }
            topTrainer?.let { t ->
                item {
                    LeaderRow(t.trainer, "Top trainer", "Members finished ${t.done} of ${plural(t.due, "workout")}") { nav.navigate(Routes.gymTrainer(t.trainer.uid)) }
                }
            }
        }

        item { OwnerHeading("Awards you gave", if (given.isEmpty()) "None yet. Tap Give an award to start." else "Tap one to change it or take it back.") }
        items(given, key = { "g" + it.id }) { a -> GivenAwardRow(a, gvm.person(a.uid), onClick = { editing = a }) }

        item { OwnerHeading("Monthly awards", "The app picks these on the 1st of each month from everyone's workouts.") }
        if (awards.isEmpty()) item {
            OwnerCardBox {
                Text("The first ones come on the 1st of next month: Best Member, Best Trainer, Most Consistent, Most Improved and Iron Lifter.",
                    style = plex(16.sp, line = 23.sp), color = Owner.Muted)
            }
        }
        awards.sortedByDescending { it.month }.forEach { m -> item(key = m.month) { AwardsCard(m, gvm) } }
    }
    GivenAwardEditor(editing, gvm) { editing = null }
}

/** This month's leader: face, a yellow "Top …" chip and their numbers. Opens their page. */
@Composable
private fun LeaderRow(p: Person, chip: String, line: String, onClick: () -> Unit) {
    OwnerCardBox(onClick = onClick, onClickLabel = "Open ${p.firstName}", padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OwnerAvatar(p.photoUrl, p.name, 52.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OwnerChip(chip, Tone.TOP)
                Text(p.name, style = plex(18.sp, FontWeight.SemiBold), color = Owner.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(line, style = plex(15.sp), color = Owner.Muted)
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, Modifier.size(26.dp), tint = Owner.Muted)
        }
    }
}

/** One hand-given award: medal, award, who and when, and the gift. [onClick] (owner only) opens the edit sheet. */
@Composable
fun GivenAwardRow(a: GivenAward, person: Person?, onClick: (() -> Unit)? = null) {
    OwnerCardBox(onClick = onClick, onClickLabel = "Change or take back", padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AwardBadge(awardLook(a.emoji, a.title), 56.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("${a.title} · ${monthLabel(a.month)}", style = plex(14.sp), color = Owner.Muted)
                Text(person?.name ?: "Former member", style = plex(18.sp, FontWeight.SemiBold), color = Owner.Ink, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                if (a.note.isNotBlank()) OwnerChip("Gift · ${a.note}", Tone.WARN, Modifier.padding(top = 3.dp))
            }
            if (onClick != null) Icon(Icons.Outlined.Edit, null, Modifier.padding(start = 8.dp).size(22.dp), tint = Owner.Muted)
        }
    }
}

/** Edit sheet for a given award, then a confirm step before taking it back. Shown while [editing] is set. */
@Composable
internal fun GivenAwardEditor(editing: GivenAward?, gvm: GymViewModel, onClose: () -> Unit) {
    var removing by remember { mutableStateOf<GivenAward?>(null) }
    editing?.let { a ->
        AwardSheet(a, gvm.person(a.uid), onDismiss = onClose,
            onSave = { title, note -> onClose(); gvm.editGivenAward(a, title, note) },
            onRemove = { onClose(); removing = a })
    }
    removing?.let { a ->
        AlertDialog(onDismissRequest = { removing = null },
            title = { Text("Take back this award?") },
            text = { Text("${a.title} for ${gvm.person(a.uid)?.name ?: "this person"} will disappear for everyone.", style = plex(17.sp, line = 25.sp)) },
            confirmButton = { TextButton(onClick = { removing = null; gvm.removeGivenAward(a) }) {
                Text("Take it back", style = plex(16.sp, FontWeight.SemiBold), color = Owner.RedText) } },
            dismissButton = { TextButton(onClick = { removing = null }) { Text("Keep it", style = plex(16.sp, FontWeight.SemiBold)) } })
    }
}

/** Bottom sheet for one given award: rename it, change the gift, or take it back. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AwardSheet(a: GivenAward, person: Person?, onDismiss: () -> Unit, onSave: (String, String) -> Unit, onRemove: () -> Unit) {
    var title by rememberSaveable(a.id) { mutableStateOf(a.title) }
    var note by rememberSaveable(a.id) { mutableStateOf(a.note) }
    val changed = title.trim() != a.title || note.trim() != a.note
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Owner.Card) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 16.dp).navigationBarsPadding().imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AwardBadge(awardLook(a.emoji, title), 52.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Change award", style = plex(21.sp, FontWeight.Bold), color = Owner.Ink)
                    Text("For ${person?.name ?: "a former member"} · ${monthLabel(a.month)}", style = plex(15.sp), maxLines = 2, color = Owner.Muted)
                }
            }
            OutlinedTextField(title, { title = it.take(32) }, Modifier.fillMaxWidth(), label = { Text("Award name") }, singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words))
            OutlinedTextField(note, { note = it.take(60) }, Modifier.fillMaxWidth(), label = { Text("Gift (optional)") },
                placeholder = { Text("e.g. Free PT session") }, singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences))
            RedButton("Save changes", { onSave(title, note) }, Modifier.fillMaxWidth(), enabled = title.isNotBlank() && changed)
            PlainButton("Take back award", onRemove, Modifier.fillMaxWidth(), Icons.Outlined.Close, ink = Owner.RedText)
            Spacer(Modifier.height(4.dp))
        }
    }
}
