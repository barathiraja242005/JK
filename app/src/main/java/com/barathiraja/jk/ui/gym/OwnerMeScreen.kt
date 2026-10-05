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
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.barathiraja.jk.ui.GymViewModel
import com.barathiraja.jk.ui.JkViewModel
import com.barathiraja.jk.ui.Routes
import com.barathiraja.jk.ui.components.shareText
import com.barathiraja.jk.ui.theme.CodeFont

/**
 * The owner's Me tab: who they are, their gym and its code (the only place the code is shown), how the app looks,
 * and the account. People and awards have their own tabs, so they are not repeated here.
 */
@Composable
fun OwnerMeScreen(vm: JkViewModel, gvm: GymViewModel, nav: NavHostController) {
    val me by gvm.me.collectAsStateWithLifecycle()
    val gym by gvm.gym.collectAsStateWithLifecycle()
    val s by vm.settings.collectAsStateWithLifecycle()
    val demo by gvm.demo.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var renamingGym by remember { mutableStateOf(false) }
    var renamingMe by remember { mutableStateOf(false) }
    var signingOut by remember { mutableStateOf(false) }

    OwnerPage {
        item { PageTitle("Me", "Your gym, its code for new trainers, and the app's settings.") }

        item {
            OwnerCardBox(padding = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OwnerAvatar(me?.photoUrl, me?.name ?: "Owner", 50.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(me?.name ?: "Owner", style = plex(16.sp, FontWeight.Bold, line = 20.sp), color = Owner.Ink, maxLines = 2,
                            overflow = TextOverflow.Ellipsis)
                        Text("Owner", style = plex(14.sp), color = Owner.Muted)
                    }
                    EditDisc("Change your name") { renamingMe = true }
                }
            }
        }

        gym?.let { g ->
            item { OwnerHeading("Your gym", "New trainers join with this code. You approve each one on Home.") }
            item {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Owner.Hero).padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(g.name, Modifier.weight(1f), style = plex(20.sp, FontWeight.Bold, line = 24.sp), color = Color.White, maxLines = 2)
                        EditDisc("Rename gym", onDark = true) { renamingGym = true }
                    }
                    Text("Code for new trainers", style = plex(13.sp), color = Owner.OnDarkMuted, modifier = Modifier.padding(top = 18.dp))
                    Text(g.gymCode, style = plex(29.sp, FontWeight.SemiBold, tracking = 6.sp).copy(fontFamily = CodeFont), color = Owner.Yellow)
                    Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        RedButton("Share code", {
                            context.shareText("Join ${g.name} as a trainer on the JK app. Open JK → Sign in → I'm a trainer → enter code ${g.gymCode}")
                        }, Modifier.weight(1.4f), Icons.Outlined.Share)
                        Surface(onClick = { copy(context, g.gymCode); gvm.message.value = "Gym code copied" }, shape = RoundedCornerShape(50),
                            color = Owner.DarkStrip, contentColor = Color.White, modifier = Modifier.weight(1f).height(48.dp)) {
                            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.ContentCopy, null, Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Copy", style = plex(15.sp, FontWeight.SemiBold))
                            }
                        }
                    }
                }
            }
        }

        item { OwnerHeading("Settings") }
        item {
            OwnerCardBox(padding = 16.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Theme", style = plex(15.sp, FontWeight.SemiBold), color = Owner.Ink)
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(Owner.Well).padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        ThemeMode.entries.forEach { m ->
                            val on = m == s.theme
                            Surface(onClick = { vm.saveSettings(s.copy(theme = m)) }, Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(50),
                                color = if (on) Owner.Ink else Color.Transparent, contentColor = if (on) Owner.OnInk else Owner.Ink) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(m.name.lowercase().replaceFirstChar(Char::uppercase), style = plex(14.sp, FontWeight.SemiBold))
                                }
                            }
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Owner.Line))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Sample gym", style = plex(15.sp, FontWeight.SemiBold), color = Owner.Ink)
                            Text("Try the app with made-up trainers and members. Your real gym isn't changed.",
                                style = plex(13.sp, line = 18.sp), color = Owner.Muted)
                        }
                        Spacer(Modifier.width(12.dp))
                        Switch(checked = demo != null, onCheckedChange = { gvm.setDemo(it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = Owner.Red, checkedThumbColor = Color.White))
                    }
                }
            }
        }

        item { OwnerHeading("Account", "Signed in with Google.") }
        item { PlainButton("Help and questions", { nav.navigate(Routes.HELP) }, Modifier.fillMaxWidth(), Icons.AutoMirrored.Outlined.HelpOutline) }
        item { PlainButton("Sign out", { signingOut = true }, Modifier.fillMaxWidth(), Icons.AutoMirrored.Outlined.Logout, ink = Owner.RedText) }
    }

    if (renamingGym) NameDialog("Gym name", null, gym?.name.orEmpty(), { renamingGym = false }) { gvm.renameGym(it) }
    if (renamingMe) NameDialog("Your name", "Trainers and members see this name.", me?.name.orEmpty(), { renamingMe = false }) { gvm.renameMe(it) }
    if (signingOut) AlertDialog(onDismissRequest = { signingOut = false },
        title = { Text("Sign out?") },
        text = { Text("Your gym stays safe. Sign in again with the same Google account to come back.", style = plex(15.sp, line = 21.sp)) },
        confirmButton = { TextButton(onClick = { signingOut = false; gvm.signOut() }) {
            Text("Sign out", style = plex(14.sp, FontWeight.SemiBold), color = Owner.RedText) } },
        dismissButton = { TextButton(onClick = { signingOut = false }) { Text("Cancel", style = plex(14.sp, FontWeight.SemiBold), color = Owner.Ink) } })
}

private fun copy(context: Context, code: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("Gym code", code))
}

/** Round pencil button, 48dp. */
@Composable
private fun EditDisc(label: String, onDark: Boolean = false, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = if (onDark) Owner.DarkStrip else Owner.Well,
        contentColor = if (onDark) Color.White else Owner.Ink, modifier = Modifier.size(48.dp)) {
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
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", style = plex(14.sp, FontWeight.SemiBold), color = Owner.Ink) } })
}
