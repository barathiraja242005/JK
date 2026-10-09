package com.barathiraja.jk.ui.gym

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.barathiraja.jk.data.ThemeMode
import com.barathiraja.jk.gym.Person
import com.barathiraja.jk.gym.Role
import com.barathiraja.jk.ui.GymViewModel
import com.barathiraja.jk.ui.JkViewModel
import com.barathiraja.jk.ui.Routes
import com.barathiraja.jk.ui.components.shareText
import com.barathiraja.jk.ui.theme.CodeFont
import com.barathiraja.jk.ui.components.CardBox
import com.barathiraja.jk.ui.components.Heading
import com.barathiraja.jk.ui.components.JkPage
import com.barathiraja.jk.ui.components.PageTitle
import com.barathiraja.jk.ui.components.PersonAvatar
import com.barathiraja.jk.ui.components.PlainButton
import com.barathiraja.jk.ui.components.RedButton
import com.barathiraja.jk.ui.components.SegmentTab
import com.barathiraja.jk.ui.components.SegmentedTabs
import com.barathiraja.jk.ui.theme.Jk
import com.barathiraja.jk.ui.theme.plex

/**
 * The Me tab for gym staff (owner and trainers): who they are, their gym, how the app looks, and the account.
 * The owner's gym card carries the trainer code (the only place it shows); a trainer's member code lives on their
 * Members tab instead. Nothing about personal training.
 */
@Composable
fun StaffMeScreen(vm: JkViewModel, gvm: GymViewModel, nav: NavHostController) {
    val me by gvm.me.collectAsStateWithLifecycle()
    val gym by gvm.gym.collectAsStateWithLifecycle()
    val s by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val owner = me?.role == Role.OWNER
    var renamingGym by remember { mutableStateOf(false) }
    var renamingMe by remember { mutableStateOf(false) }
    var signingOut by remember { mutableStateOf(false) }
    var leaving by remember { mutableStateOf(false) }

    JkPage {
        item { PageTitle("Me", if (owner) "Your gym, its code for new trainers, and the app's settings." else "Your name, your gym and the app's settings.") }

        item {
            CardBox(padding = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PersonAvatar(me?.photoUrl, me?.name ?: "Owner", 50.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(me?.name ?: "Owner", style = plex(16.sp, FontWeight.Bold, line = 20.sp), color = Jk.Ink, maxLines = 2,
                            overflow = TextOverflow.Ellipsis)
                        Text(if (owner) "Owner" else "Trainer", style = plex(14.sp), color = Jk.Muted)
                    }
                    EditDisc("Change your name") { renamingMe = true }
                }
            }
        }

        gym?.let { g ->
            if (owner) {
                item { Heading("Your gym", "New trainers join with this code. You approve each one on Home.") }
                item {
                    CodeHeroCard(g.name, "Code for new trainers", g.gymCode, onRename = { renamingGym = true },
                        onShare = { context.shareText("Join ${g.name} as a trainer on the JK app. Open JK → Sign in → I'm a trainer → enter code ${g.gymCode}") },
                        onCopy = { copyText(context, "Gym code", g.gymCode); gvm.showMessage("Gym code copied") })
                }
            } else {
                item { Heading("Your gym", "Your member code is on the Members tab.") }
                item {
                    CardBox(padding = 16.dp) {
                        Column {
                            Text(g.name, style = plex(17.sp, FontWeight.Bold), color = Jk.Ink)
                            Text("You train members here", style = plex(14.sp), color = Jk.Muted)
                        }
                    }
                }
            }
        }

        item { Heading("Settings") }
        item {
            CardBox(padding = 16.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Theme", style = plex(15.sp, FontWeight.SemiBold), color = Jk.Ink)
                    SegmentedTabs(ThemeMode.entries.map { SegmentTab(it.name.lowercase().replaceFirstChar(Char::uppercase)) },
                        ThemeMode.entries.indexOf(s.theme), { vm.saveSettings(s.copy(theme = ThemeMode.entries[it])) }, track = Jk.Well)
                }
            }
        }

        item { Heading("Account", "Signed in with Google.") }
        item { PlainButton("Help and questions", { nav.navigate(Routes.HELP) }, Modifier.fillMaxWidth(), Icons.AutoMirrored.Outlined.HelpOutline) }
        if (!gvm.testMode) item {
            PlainButton("Test mode (made-up gym)", { gvm.setTestMode(true) }, Modifier.fillMaxWidth(), Icons.Outlined.Science)
        }
        if (!owner) item {
            PlainButton("Leave this gym", { leaving = true }, Modifier.fillMaxWidth(), Icons.AutoMirrored.Outlined.ExitToApp, ink = Jk.RedText)
        }
        item { PlainButton("Sign out", { signingOut = true }, Modifier.fillMaxWidth(), Icons.AutoMirrored.Outlined.Logout, ink = Jk.RedText) }
        if (!owner) item { DeleteAccountButton(gvm, Modifier.fillMaxWidth()) }
    }

    if (renamingGym) NameDialog("Gym name", null, gym?.name.orEmpty(), { renamingGym = false }) { gvm.renameGym(it) }
    if (leaving) AlertDialog(onDismissRequest = { leaving = false },
        title = { Text("Leave ${gym?.name ?: "this gym"}?") },
        text = { Text("Your members will need a new trainer, and you'll stop seeing them. You can join again with the gym code.",
            style = plex(15.sp, line = 21.sp)) },
        confirmButton = { TextButton(onClick = { leaving = false; gvm.leaveGym() }) {
            Text("Leave", style = plex(14.sp, FontWeight.SemiBold), color = Jk.RedText) } },
        dismissButton = { TextButton(onClick = { leaving = false }) { Text("Stay", style = plex(14.sp, FontWeight.SemiBold), color = Jk.Ink) } })
    if (renamingMe) NameDialog("Your name", if (owner) "Trainers and members see this name." else "Your members and the owner see this name.", me?.name.orEmpty(), { renamingMe = false }) { gvm.renameMe(it) }
    if (signingOut) AlertDialog(onDismissRequest = { signingOut = false },
        title = { Text("Sign out?") },
        text = { Text("Your gym stays safe. Sign in again with the same Google account to come back.", style = plex(15.sp, line = 21.sp)) },
        confirmButton = { TextButton(onClick = { signingOut = false; gvm.signOut() }) {
            Text("Sign out", style = plex(14.sp, FontWeight.SemiBold), color = Jk.RedText) } },
        dismissButton = { TextButton(onClick = { signingOut = false }) { Text("Cancel", style = plex(14.sp, FontWeight.SemiBold), color = Jk.Ink) } })
}

/** Puts [text] on the clipboard under [label]. */
internal fun copyText(context: Context, label: String, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText(label, text))
}

/**
 * A charcoal card holding a join code: [title] (with a rename button when [onRename] is set), the code in big
 * yellow letters, and Share (red, the page's main action) with Copy beside it.
 */
@Composable
internal fun CodeHeroCard(title: String, codeLabel: String, code: String, onRename: (() -> Unit)?, onShare: () -> Unit, onCopy: () -> Unit,
                          onNewCode: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Jk.Hero).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), style = plex(20.sp, FontWeight.Bold, line = 24.sp), color = Color.White, maxLines = 2)
            if (onRename != null) EditDisc("Rename", onDark = true, onClick = onRename)
        }
        Text(codeLabel, style = plex(13.sp), color = Jk.OnDarkMuted, modifier = Modifier.padding(top = 18.dp))
        Text(code, style = plex(29.sp, FontWeight.SemiBold, tracking = 6.sp).copy(fontFamily = CodeFont), color = Jk.Yellow)
        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RedButton("Share code", onShare, Modifier.weight(1.4f), Icons.Outlined.Share)
            Surface(onClick = onCopy, shape = RoundedCornerShape(50), color = Jk.DarkStrip, contentColor = Color.White,
                modifier = Modifier.weight(1f).height(48.dp)) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.ContentCopy, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Copy", style = plex(15.sp, FontWeight.SemiBold))
                }
            }
        }
        if (onNewCode != null) TextButton(onClick = onNewCode, modifier = Modifier.padding(top = 6.dp)) {
            Text("Make a new code (the old one stops working)", style = plex(14.sp, FontWeight.SemiBold), color = Jk.OnDarkSoft)
        }
    }
}

/** Round pencil button, 48dp. */
@Composable
private fun EditDisc(label: String, onDark: Boolean = false, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = if (onDark) Jk.DarkStrip else Jk.Well,
        contentColor = if (onDark) Color.White else Jk.Ink, modifier = Modifier.size(48.dp)) {
        Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Edit, label, Modifier.size(20.dp)) }
    }
}

/** One text box to rename something; Save is off until there is a name. */
@Composable
private fun NameDialog(title: String, explain: String?, initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                if (explain != null) Text(explain, style = plex(14.sp, line = 19.sp), modifier = Modifier.padding(bottom = 12.dp))
                OutlinedTextField(name, { name = it.take(40) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words))
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onDismiss(); onSave(name) }) { Text("Save", style = plex(14.sp, FontWeight.SemiBold)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", style = plex(14.sp, FontWeight.SemiBold), color = Jk.Ink) } })
}

/** One trainer's member code on a home page: whose it is, the code itself, and Share / Copy. */
internal data class MemberCode(val trainer: Person, val code: String)

/**
 * The codes new members join with, right on the home page: the trainer's own (one row), or every trainer's for the
 * owner. Members join a trainer, so the code also decides who coaches them.
 */
@Composable
internal fun MemberCodesCard(codes: List<MemberCode>, gymName: String, gvm: GymViewModel, mine: Boolean) {
    val context = LocalContext.current
    CardBox(padding = 14.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            codes.forEachIndexed { i, (t, code) ->
                if (i > 0) HorizontalDivider(color = Jk.Line)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!mine) { PersonAvatar(t.photoUrl, t.name, 36.dp); Spacer(Modifier.width(10.dp)) }
                    Column(Modifier.weight(1f)) {
                        Text(if (mine) "Your member code" else t.name, style = plex(13.sp, line = 17.sp), color = Jk.Muted,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(code.ifBlank { "—" }, style = plex(22.sp, FontWeight.SemiBold, tracking = 4.sp).copy(fontFamily = CodeFont),
                            color = Jk.Ink, maxLines = 1)
                    }
                    if (code.isNotBlank()) Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SmallAction(Icons.Outlined.ContentCopy, "Copy ${t.firstName}'s member code", red = false) {
                            copyText(context, "Member code", code); gvm.showMessage("Member code copied")
                        }
                        SmallAction(Icons.Outlined.Share, "Share ${t.firstName}'s member code", red = mine) {
                            context.shareText(
                                if (mine) "Join me on the JK app at $gymName! Open JK → Sign in with Google → I'm a member → enter code $code"
                                else "Join ${t.firstName}'s members at $gymName on the JK app. Open JK → Sign in with Google → I'm a member → enter code $code")
                        }
                    }
                }
            }
        }
    }
}
