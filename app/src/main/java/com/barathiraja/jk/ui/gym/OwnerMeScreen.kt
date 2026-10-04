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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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

private const val AWARDS_PREVIEW = 5

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
    var removing by remember { mutableStateOf<GivenAward?>(null) }
    var showAll by rememberSaveable { mutableStateOf(false) }
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
            }
        }

        item {
            OwnerCard(gym?.name.orEmpty(), explain = "Waiting: trainers who asked to join and need your approval on the Gym tab.") {
                Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Count("$trainers", "Trainers", Modifier.weight(1f))
                    Count("$members", "Members", Modifier.weight(1f))
                    Count("$pending", "Waiting", Modifier.weight(1f))
                }
                gym?.let { g ->
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Text("Code for new trainers", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(g.gymCode, Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold, fontFamily = CodeFont, letterSpacing = 3.sp)
                        FilledTonalButton(onClick = {
                            context.shareText("Join ${g.name} as a trainer on the JK app. Open JK → Sign in → I'm a trainer → enter code ${g.gymCode}")
                        }) { Icon(Icons.Filled.Share, null); Spacer(Modifier.width(6.dp)); Text("Share") }
                    }
                    TextButton(onClick = { renaming = true }, contentPadding = PaddingValues(0.dp)) {
                        Icon(Icons.Outlined.Edit, null); Spacer(Modifier.width(6.dp)); Text("Rename gym")
                    }
                }
            }
        }

        item {
            OwnerCard("Awards", explain = "Reward your best members and trainers. Everyone in the gym sees them on the Ranks tab.") {
                Spacer(Modifier.height(8.dp))
                Button(onClick = { nav.navigate(Routes.GIVE_AWARD) }, Modifier.fillMaxWidth().height(56.dp)) {
                    Text("🏆  Give an award", style = MaterialTheme.typography.titleMedium)
                }
                (if (showAll) given else given.take(AWARDS_PREVIEW)).forEach { a ->
                    HorizontalDivider(Modifier.padding(top = 12.dp))
                    AwardLine(a, gvm.person(a.uid), onRemove = { removing = a })
                }
                if (given.size > AWARDS_PREVIEW) {
                    TextButton(onClick = { showAll = !showAll }, Modifier.fillMaxWidth()) {
                        Text(if (showAll) "Show less" else "Show all ${given.size}", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
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
    if (signingOut) AlertDialog(onDismissRequest = { signingOut = false },
        title = { Text("Sign out?") },
        text = { Text("Your gym stays safe. Sign in again with the same Google account to come back.") },
        confirmButton = { TextButton(onClick = { signingOut = false; gvm.signOut() }) { Text("Sign out") } },
        dismissButton = { TextButton(onClick = { signingOut = false }) { Text("Cancel") } })
    removing?.let { a ->
        AlertDialog(onDismissRequest = { removing = null },
            title = { Text("Take back this award?") },
            text = { Text("${a.emoji} ${a.title} for ${gvm.person(a.uid)?.name ?: "this person"} will disappear for everyone.") },
            confirmButton = { TextButton(onClick = { removing = null; gvm.removeGivenAward(a) }) { Text("Remove") } },
            dismissButton = { TextButton(onClick = { removing = null }) { Text("Keep") } })
    }
}

@Composable
private fun Count(value: String, label: String, modifier: Modifier) {
    Surface(modifier, color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** A given award as one row inside the owner's Awards card. */
@Composable
private fun AwardLine(a: GivenAward, person: Person?, onRemove: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(a.emoji, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.width(40.dp))
        Column(Modifier.weight(1f)) {
            Text(person?.name ?: "Former member", style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Text(a.title + if (a.note.isNotBlank()) " · ${a.note}" else "", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        }
        IconButton(onClick = onRemove) { Icon(Icons.Outlined.Close, "Take back award") }
    }
}

/** One hand-given award; [onRemove] shows a remove button (owner only). */
@Composable
fun GivenAwardRow(a: GivenAward, person: Person?, onRemove: (() -> Unit)? = null) {
    JkCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(a.emoji, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(a.title, style = MaterialTheme.typography.labelLarge, color = Sun)
                Text(person?.name ?: "Former member", style = MaterialTheme.typography.titleMedium)
                if (a.note.isNotBlank()) Text("🎁 ${a.note}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(monthLabel(a.month), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (person != null) Avatar(person.photoUrl, person.name, 40.dp)
            if (onRemove != null) IconButton(onClick = onRemove) { Icon(Icons.Outlined.Close, "Remove award") }
        }
    }
}
