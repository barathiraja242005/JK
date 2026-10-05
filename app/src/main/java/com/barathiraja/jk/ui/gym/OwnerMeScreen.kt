package com.barathiraja.jk.ui.gym

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.FilledTonalButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import com.barathiraja.jk.ui.components.shareText
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.barathiraja.jk.ui.switchTab
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.barathiraja.jk.data.ThemeMode
import com.barathiraja.jk.gym.GivenAward
import com.barathiraja.jk.gym.Person
import com.barathiraja.jk.gym.PersonStatus
import com.barathiraja.jk.gym.Role
import com.barathiraja.jk.ui.GymViewModel
import com.barathiraja.jk.ui.JkViewModel
import com.barathiraja.jk.ui.Routes
import com.barathiraja.jk.ui.components.JkCard
import com.barathiraja.jk.ui.components.Avatar
import com.barathiraja.jk.ui.screens.Choice
import com.barathiraja.jk.ui.components.TabScreen
import com.barathiraja.jk.ui.theme.CodeFont
import com.barathiraja.jk.ui.theme.Sun

/** The owner's Me tab: their gym, the awards they hand out, appearance and account. Nothing about personal training. */
@Composable
fun OwnerMeScreen(vm: JkViewModel, gvm: GymViewModel, nav: NavHostController) {
    val me by gvm.me.collectAsStateWithLifecycle()
    val gym by gvm.gym.collectAsStateWithLifecycle()
    val people by gvm.people.collectAsStateWithLifecycle()
    val given by gvm.givenAwards.collectAsStateWithLifecycle()
    val s by vm.settings.collectAsStateWithLifecycle()
    val demo by gvm.demo.collectAsStateWithLifecycle()
    var renaming by remember { mutableStateOf(false) }
    var renamingMe by remember { mutableStateOf(false) }
    var signingOut by remember { mutableStateOf(false) }

    val trainers = people.count { it.role == Role.TRAINER && it.active }
    val members = people.count { it.role == Role.MEMBER && it.active }
    val pending = people.count { it.role == Role.TRAINER && it.status == PersonStatus.PENDING }

    val context = LocalContext.current
    TabScreen("Me") {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                me?.let { Avatar(it.photoUrl, it.name, 64.dp) }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(me?.name ?: "Owner", style = MaterialTheme.typography.titleLarge, maxLines = 2)
                    Text("Owner", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(onClick = { renamingMe = true }, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(48.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Edit, "Change your name", Modifier.size(20.dp)) }
                }
            }
        }

        item {
            MeBento(
                gymName = gym?.name ?: "Your gym", code = gym?.gymCode,
                trainers = people.filter { it.role == Role.TRAINER && it.active },
                members = people.filter { it.role == Role.MEMBER && it.active },
                pending = pending, awards = given.size,
                onShare = gym?.let { g -> { context.shareText("Join ${g.name} as a trainer on the JK app. Open JK → Sign in → I'm a trainer → enter code ${g.gymCode}") } },
                onTrainers = { nav.navigate(Routes.people(PEOPLE_TRAINERS)) },
                onMembers = { nav.navigate(Routes.people(PEOPLE_MEMBERS)) },
                onWaiting = { nav.switchTab(Routes.GYM) },
                onAwards = { gvm.ranksTab.value = RANKS_AWARDS; nav.switchTab(Routes.RANKS) },
                onRename = { renaming = true },
            )
        }

        item {
            OwnerCard("Settings") {
                Text("Theme", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
                Choice(ThemeMode.entries, s.theme, { it.name.lowercase().replaceFirstChar(Char::uppercase) }) {
                    vm.saveSettings(s.copy(theme = it))
                }
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Sample gym", style = MaterialTheme.typography.titleMedium)
                        Text("Try the app with made-up trainers and members. Your real gym isn't changed.",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(checked = demo != null, onCheckedChange = { gvm.setDemo(it) })
                }
            }
        }

        item {
            OwnerCard("Account") {
                Text("Signed in with Google", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { signingOut = true }, Modifier.fillMaxWidth().height(52.dp)) {
                    Text("Sign out", style = MaterialTheme.typography.titleMedium)
                }
                TextButton(onClick = { nav.navigate(Routes.HELP) }, Modifier.fillMaxWidth()) { Text("Help, FAQ & credits") }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }

    if (renaming) {
        var name by remember { mutableStateOf(gym?.name.orEmpty()) }
        AlertDialog(onDismissRequest = { renaming = false },
            title = { Text("Gym name") },
            text = { OutlinedTextField(name, { name = it.take(40) }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {
                TextButton(enabled = name.isNotBlank(), onClick = { renaming = false; gvm.renameGym(name) }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renaming = false }) { Text("Cancel") } })
    }
    if (renamingMe) {
        var name by remember { mutableStateOf(me?.name.orEmpty()) }
        AlertDialog(onDismissRequest = { renamingMe = false },
            title = { Text("Your name") },
            text = {
                Column {
                    Text("Trainers and members see this name.", style = plex(16.sp, line = 23.sp), modifier = Modifier.padding(bottom = 12.dp))
                    OutlinedTextField(name, { name = it.take(40) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words))
                }
            },
            confirmButton = {
                TextButton(enabled = name.isNotBlank(), onClick = { renamingMe = false; gvm.renameMe(name) }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renamingMe = false }) { Text("Cancel") } })
    }
    if (signingOut) AlertDialog(onDismissRequest = { signingOut = false },
        title = { Text("Sign out?") },
        text = { Text("Your gym stays safe. Sign in again with the same Google account to come back.") },
        confirmButton = { TextButton(onClick = { signingOut = false; gvm.signOut() }) { Text("Sign out") } },
        dismissButton = { TextButton(onClick = { signingOut = false }) { Text("Cancel") } })
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
                Text("Take it back", style = plex(16.sp, FontWeight.SemiBold), color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { removing = null }) { Text("Keep it", style = plex(16.sp, FontWeight.SemiBold)) } })
    }
}

/**
 * The gym at a glance as a bento grid: an ink card with the gym's name and join code, two pastel tiles that show
 * the trainers' and members' faces and open their lists, and two short tiles for who is waiting and awards.
 * Every tile is a button and says in words what it is.
 */
@Composable
private fun MeBento(
    gymName: String, code: String?, trainers: List<Person>, members: List<Person>, pending: Int, awards: Int,
    onShare: (() -> Unit)?, onTrainers: () -> Unit, onMembers: () -> Unit, onWaiting: () -> Unit, onAwards: () -> Unit, onRename: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Gym card
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(com.barathiraja.jk.ui.theme.HeroBlue).padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(start = 2.dp)) {
                    Text("Your gym", style = plex(14.sp), color = Owner.OnDarkMuted)
                    Text(gymName, style = plex(24.sp, FontWeight.SemiBold, line = 28.sp), color = Color.White, maxLines = 2)
                }
                Surface(onClick = onRename, shape = CircleShape, color = Owner.DarkStrip, contentColor = Color.White, modifier = Modifier.size(48.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Edit, "Rename gym", Modifier.size(20.dp)) }
                }
            }
            if (code != null) Row(
                Modifier.padding(top = 16.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Owner.DarkStrip)
                    .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Code for new trainers", style = plex(13.sp), color = Owner.OnDarkMuted)
                    Text(code, style = plex(24.sp, FontWeight.SemiBold, tracking = 4.sp).copy(fontFamily = CodeFont), color = Color.White, maxLines = 1)
                }
                if (onShare != null) Surface(onClick = onShare, shape = RoundedCornerShape(50), color = com.barathiraja.jk.ui.theme.Mustard,
                    contentColor = Color.Black, modifier = Modifier.heightIn(min = 48.dp)) {
                    Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Share, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Share", style = plex(15.sp, FontWeight.SemiBold))
                    }
                }
            }
        }
        // People tiles
        Row(Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PeopleTile(members, if (members.size == 1) "Member" else "Members", Owner.Lavender, onMembers, Modifier.weight(1f).fillMaxHeight())
            PeopleTile(trainers, if (trainers.size == 1) "Trainer" else "Trainers", Owner.Mint, onTrainers, Modifier.weight(1f).fillMaxHeight())
        }
        // Small tiles
        Row(Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LineTile(Icons.Outlined.HourglassEmpty, if (pending == 0) "No one" else "$pending", "waiting to join",
                if (pending > 0) Owner.Butter else cs.surfaceContainer, onWaiting, Modifier.weight(1f).fillMaxHeight())
            LineTile(Icons.Outlined.EmojiEvents, "$awards", if (awards == 1) "award given" else "awards given",
                cs.surfaceContainer, onAwards, Modifier.weight(1f).fillMaxHeight())
        }
    }
}

/** Faces of the first few people (initials on pastels), the count in words, and a round arrow to open the list. */
@Composable
private fun PeopleTile(people: List<Person>, label: String, fill: Color, onClick: () -> Unit, modifier: Modifier) {
    Surface(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(26.dp), color = fill, contentColor = Color.Black) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f)) {
                    people.take(3).forEachIndexed { i, p ->
                        Box(Modifier.padding(start = if (i == 0) 0.dp else 0.dp).offset(x = (-10 * i).dp)) {
                            OwnerAvatar(p.photoUrl, p.name, p.uid, 34.dp, ring = Owner.Black)
                        }
                    }
                    if (people.size > 3) Box(
                        Modifier.offset(x = (-30).dp).size(34.dp).clip(CircleShape).background(Color.Black),
                        contentAlignment = Alignment.Center,
                    ) { Text("+${people.size - 3}", style = plex(12.sp, FontWeight.SemiBold), color = Color.White) }
                    if (people.isEmpty()) Text("None yet", style = plex(14.sp), color = Owner.Warm)
                }
                Box(Modifier.size(34.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(17.dp), tint = Color.Black)
                }
            }
            Spacer(Modifier.height(22.dp))
            Text("${people.size}", style = plex(34.sp, FontWeight.SemiBold, tracking = (-1).sp, line = 38.sp), color = Color.Black)
            Text(label, style = plex(15.sp, FontWeight.SemiBold), color = Color.Black)
        }
    }
}

/** A short tile: icon disc, then a bold value with a few words after it. */
@Composable
private fun LineTile(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, words: String, fill: Color, onClick: () -> Unit, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    val pastel = fill != cs.surfaceContainer
    val ink = if (pastel) Color.Black else cs.onSurface
    Surface(onClick = onClick, modifier = modifier.heightIn(min = 76.dp), shape = RoundedCornerShape(24.dp), color = fill, contentColor = ink) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(if (pastel) Color.White else cs.surfaceContainerHigh), contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(20.dp), tint = if (pastel) Color.Black else cs.onSurface)
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(value, style = plex(18.sp, FontWeight.SemiBold), color = ink, maxLines = 1)
                Text(words, style = plex(13.sp, line = 17.sp), color = if (pastel) Owner.Warm else cs.onSurfaceVariant)
            }
        }
    }
}

/** Bottom sheet for one given award: rename it, change the gift, or take it back. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AwardSheet(a: GivenAward, person: Person?, onDismiss: () -> Unit, onSave: (String, String) -> Unit, onRemove: () -> Unit) {
    var title by rememberSaveable(a.id) { mutableStateOf(a.title) }
    var note by rememberSaveable(a.id) { mutableStateOf(a.note) }
    val changed = title.trim() != a.title || note.trim() != a.note
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 16.dp).navigationBarsPadding().imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AwardBadge(awardLook(a.emoji, title), 52.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Edit award", style = MaterialTheme.typography.titleLarge)
                    Text("For ${person?.name ?: "a former member"} · ${monthLabel(a.month)}", style = MaterialTheme.typography.bodyMedium, maxLines = 2,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            OutlinedTextField(title, { title = it.take(32) }, Modifier.fillMaxWidth(), label = { Text("Award name") }, singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words))
            OutlinedTextField(note, { note = it.take(60) }, Modifier.fillMaxWidth(), label = { Text("Gift (optional)") },
                placeholder = { Text("e.g. Free PT session") }, singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences))
            Button(onClick = { onSave(title, note) }, Modifier.fillMaxWidth().height(56.dp), enabled = title.isNotBlank() && changed) {
                Text("Save changes", style = MaterialTheme.typography.titleMedium)
            }
            TextButton(onClick = onRemove, Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.Outlined.Close, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error); Spacer(Modifier.width(8.dp))
                Text("Take back award", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

/** One hand-given award: medal, award, who and when, and the gift. [onClick] (owner only) opens the edit sheet. */
@Composable
fun GivenAwardRow(a: GivenAward, person: Person?, onClick: (() -> Unit)? = null) {
    val cs = MaterialTheme.colorScheme
    JkCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AwardBadge(awardLook(a.emoji, a.title), 58.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("${a.title} · ${monthLabel(a.month)}", style = plex(14.sp), color = cs.onSurfaceVariant)
                Text(person?.name ?: "Former member", style = plex(18.sp, FontWeight.SemiBold), color = cs.onSurface, maxLines = 1)
                if (a.note.isNotBlank()) OwnerChip("Gift · ${a.note}", Owner.Cream, modifier = Modifier.padding(top = 3.dp))
            }
            if (onClick != null) Icon(Icons.Outlined.Edit, "Edit or take back", Modifier.padding(start = 8.dp).size(22.dp), tint = cs.onSurfaceVariant)
        }
    }
}
