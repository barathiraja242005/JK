package com.barathiraja.jk.ui.gym

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.barathiraja.jk.gym.Gym
import com.barathiraja.jk.ui.GymViewModel
import com.barathiraja.jk.ui.theme.CodeFont

/*
 * Getting in: sign in with Google, say who you are at the gym, then create the gym (owner) or enter a code.
 * One question per screen, one red button per screen, big type. The black card at the top is the brand moment.
 */

@Composable
fun GymLoading() {
    Box(Modifier.fillMaxSize().background(Owner.Paper), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Owner.Red) }
}

@Composable
private fun JoinColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Owner.Paper).statusBarsPadding().navigationBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) { content() }
}

/** The JK mark: a red K after the J, on a black tile ([onDark]: a white tile, for black cards). */
@Composable
private fun Logo(size: Int = 56, onDark: Boolean = false) {
    Box(Modifier.size(size.dp).clip(RoundedCornerShape((size * 0.28f).dp)).background(if (onDark) Color.White else Owner.Black),
        contentAlignment = Alignment.Center) {
        Row {
            Text("J", style = plex((size * 0.42f).sp, FontWeight.Bold), color = if (onDark) Owner.Black else Color.White)
            Text("K", style = plex((size * 0.42f).sp, FontWeight.Bold), color = if (onDark) Owner.Red else Color(0xFFE5212B))
        }
    }
}

/** First screen: what JK does in three lines, then sign in with the Google account on the phone. */
@Composable
fun SignInScreen(gvm: GymViewModel) {
    val busy by gvm.busy.collectAsStateWithLifecycle()
    val message by gvm.message.collectAsStateWithLifecycle()
    val context = LocalContext.current
    JoinColumn {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(Owner.Hero).padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Logo(56, onDark = true)
                Spacer(Modifier.width(12.dp))
                Text("JK Gym", style = plex(20.sp, FontWeight.Bold), color = Color.White)
            }
            Spacer(Modifier.height(36.dp))
            Text("Your whole gym,\nin one place.", style = plex(36.sp, FontWeight.Bold, line = 42.sp, tracking = (-1).sp), color = Color.White)
            Box(Modifier.padding(top = 14.dp).width(56.dp).height(6.dp).clip(RoundedCornerShape(50)).background(Owner.Red))
            Spacer(Modifier.height(24.dp))
            listOf(
                "Owners see who trained today",
                "Trainers send workouts to members",
                "Members log every set and earn awards",
            ).forEach { Point(it) }
        }
        Spacer(Modifier.height(4.dp))
        Surface(
            onClick = { gvm.message.value = null; gvm.signIn(context) }, enabled = !busy, shape = RoundedCornerShape(50),
            color = Owner.Red, contentColor = Color.White, modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
        ) {
            Row(Modifier.padding(horizontal = 22.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                if (busy) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                else {
                    Box(Modifier.size(30.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                        Text("G", style = plex(17.sp, FontWeight.Bold), color = Owner.Black)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Continue with Google", style = plex(18.sp, FontWeight.SemiBold))
                }
            }
        }
        Text("We use your Google name and photo so your gym knows it's you.", Modifier.fillMaxWidth(),
            style = plex(15.sp, line = 21.sp), color = Owner.Muted, textAlign = TextAlign.Center)
        message?.let { ErrorText(it) }
    }
}

/** A yellow tick and a short line, on the black card. */
@Composable
private fun Point(text: String) {
    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(26.dp).clip(CircleShape).background(Owner.Yellow), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Check, null, Modifier.size(16.dp), tint = Owner.Black)
        }
        Spacer(Modifier.width(12.dp))
        Text(text, style = plex(17.sp, line = 23.sp), color = Owner.OnDarkSoft)
    }
}

@Composable
private fun ErrorText(text: String) {
    Text(text, Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Tone.BAD.fill).padding(14.dp),
        style = plex(16.sp, line = 22.sp), color = Tone.BAD.ink)
}

private enum class JoinMode { CHOOSE, OWNER, TRAINER, MEMBER }

/** Signed in but not in a gym yet: owner creates one, trainers and members join with a code. */
@Composable
fun ChooseRoleScreen(gvm: GymViewModel, userName: String) {
    var mode by remember { mutableStateOf(JoinMode.CHOOSE) }
    var text by remember { mutableStateOf("") }
    val busy by gvm.busy.collectAsStateWithLifecycle()
    val message by gvm.message.collectAsStateWithLifecycle()

    JoinColumn {
        if (mode == JoinMode.CHOOSE) {
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Logo(44)
                Spacer(Modifier.weight(1f))
                Text("Use another account", style = plex(15.sp, FontWeight.SemiBold), color = Owner.Ink,
                    modifier = Modifier.clip(RoundedCornerShape(50)).clickable { gvm.signOut() }.padding(horizontal = 12.dp, vertical = 14.dp))
            }
            Column(Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
                Text("Hi ${GymViewModel.tidyName(userName).substringBefore(' ').ifBlank { "there" }}", style = plex(32.sp, FontWeight.Bold, tracking = (-0.6).sp),
                    color = Owner.Ink)
                Text("Who are you at the gym?", style = plex(18.sp, line = 25.sp), color = Owner.Muted)
            }
            RoleCard(Icons.Outlined.Storefront, "I own the gym", "Set up your gym and see how everyone is doing.") { mode = JoinMode.OWNER; text = "" }
            RoleCard(Icons.Outlined.Badge, "I'm a trainer", "I coach members. The owner gave me the gym code.") { mode = JoinMode.TRAINER; text = "" }
            RoleCard(Icons.Outlined.FitnessCenter, "I'm a member", "I train here. My trainer gave me a code.") { mode = JoinMode.MEMBER; text = "" }
        } else {
            Surface(onClick = { mode = JoinMode.CHOOSE; gvm.message.value = null }, shape = CircleShape, color = Owner.Card,
                contentColor = Owner.Ink, modifier = Modifier.padding(top = 8.dp).size(52.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", Modifier.size(24.dp)) }
            }
            val isCode = mode != JoinMode.OWNER
            PageTitle(
                when (mode) { JoinMode.OWNER -> "Name your gym"; JoinMode.TRAINER -> "Join as a trainer"; else -> "Join your trainer" },
                when (mode) {
                    JoinMode.OWNER -> "Members and trainers will see this name. You'll get a code to give your trainers."
                    JoinMode.TRAINER -> "Type the 6-letter gym code from the owner. The owner then says yes with one tap."
                    else -> "Type the 6-letter code your trainer gave you."
                },
            )
            OutlinedTextField(
                text, { text = if (isCode) it.uppercase().filter(Char::isLetterOrDigit).take(6) else it.take(40) },
                Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(18.dp),
                label = { Text(if (isCode) "Code" else "Gym name", style = plex(16.sp)) },
                placeholder = { Text(if (isCode) "ABC123" else "e.g. Iron Temple Fitness", style = plex(if (isCode) 26.sp else 20.sp)) },
                textStyle = if (isCode) plex(28.sp, FontWeight.SemiBold, tracking = 6.sp).copy(fontFamily = CodeFont, color = Owner.Ink)
                    else plex(20.sp, FontWeight.SemiBold).copy(color = Owner.Ink),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Owner.Ink, focusedLabelColor = Owner.Ink, cursorColor = Owner.Red,
                    focusedContainerColor = Owner.Card, unfocusedContainerColor = Owner.Card),
                keyboardOptions = KeyboardOptions(capitalization = if (isCode) KeyboardCapitalization.Characters else KeyboardCapitalization.Words),
            )
            message?.let { ErrorText(it) }
            val ready = !busy && (if (isCode) text.length == 6 else text.isNotBlank())
            RedButton(
                if (busy) "Please wait…" else if (mode == JoinMode.OWNER) "Create my gym" else "Join",
                {
                    gvm.message.value = null
                    when (mode) {
                        JoinMode.OWNER -> gvm.createGym(text)
                        JoinMode.TRAINER -> gvm.joinAsTrainer(text)
                        else -> gvm.joinAsMember(text)
                    }
                },
                Modifier.fillMaxWidth().padding(top = 4.dp), Icons.AutoMirrored.Outlined.ArrowForward, enabled = ready,
            )
        }
    }
}

@Composable
private fun RoleCard(icon: ImageVector, title: String, sub: String, onClick: () -> Unit) {
    OwnerCardBox(onClick = onClick, onClickLabel = title, padding = 18.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).clip(CircleShape).background(Owner.Ink), contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(26.dp), tint = Owner.OnInk)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = plex(20.sp, FontWeight.Bold), color = Owner.Ink)
                Text(sub, style = plex(16.sp, line = 22.sp), color = Owner.Muted)
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, Modifier.size(28.dp), tint = Owner.Muted)
        }
    }
}

/** Trainer waiting for the owner, or someone removed from the gym. */
@Composable
fun WaitingScreen(gvm: GymViewModel, gym: Gym?, removed: Boolean) {
    JoinColumn {
        Spacer(Modifier.height(48.dp))
        Box(Modifier.size(88.dp).clip(CircleShape).background(if (removed) Tone.BAD.fill else Owner.Yellow).align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.HourglassTop, null, Modifier.size(40.dp), tint = if (removed) Tone.BAD.ink else Owner.Black)
        }
        Text(if (removed) "You're no longer part of ${gym?.name ?: "this gym"}" else "Waiting for the owner",
            Modifier.fillMaxWidth(), style = plex(26.sp, FontWeight.Bold, line = 32.sp), color = Owner.Ink, textAlign = TextAlign.Center)
        Text(
            if (removed) "Ask the gym owner if this is a mistake, or join another gym."
            else "We've asked the owner of ${gym?.name ?: "the gym"} to let you in. This screen moves on by itself once they say yes.",
            Modifier.fillMaxWidth(), style = plex(17.sp, line = 25.sp), textAlign = TextAlign.Center, color = Owner.Muted,
        )
        Spacer(Modifier.height(8.dp))
        PlainButton(if (removed) "Join another gym" else "Use a different code", { gvm.leaveGym() }, Modifier.fillMaxWidth())
        PlainButton("Sign out", { gvm.signOut() }, Modifier.fillMaxWidth(), ink = Owner.RedText)
    }
}
