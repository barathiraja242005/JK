package com.barathiraja.jk.ui.gym

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.barathiraja.jk.gym.Gym
import com.barathiraja.jk.ui.GymViewModel
import com.barathiraja.jk.ui.components.JkCard
import com.barathiraja.jk.ui.theme.Ember

@Composable
fun GymLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
private fun JoinColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) { content() }
}

/** First screen: sign in with the Google account on the phone. */
@Composable
fun SignInScreen(gvm: GymViewModel) {
    val busy by gvm.busy.collectAsStateWithLifecycle()
    val message by gvm.message.collectAsStateWithLifecycle()
    val context = LocalContext.current
    JoinColumn {
        Spacer(Modifier.height(48.dp))
        Text("JK", style = MaterialTheme.typography.displayLarge, color = Ember)
        Text("Train smarter. Live stronger.", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Text("Sign in to join your gym", style = MaterialTheme.typography.headlineSmall)
        Text("Your trainer sends your workouts here, and you can see how you rank in the gym.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = { gvm.message.value = null; gvm.signIn(context) }, enabled = !busy,
            modifier = Modifier.fillMaxWidth().height(60.dp)) {
            if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            else Text("Continue with Google", style = MaterialTheme.typography.titleMedium)
        }
        message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
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
            Text("Hi ${userName.substringBefore(' ')} 👋", style = MaterialTheme.typography.headlineMedium)
            Text("Who are you at the gym?", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            RoleCard(Icons.Outlined.FitnessCenter, "I'm a member", "I train at the gym. My trainer gave me a code.") { mode = JoinMode.MEMBER; text = "" }
            RoleCard(Icons.Outlined.Badge, "I'm a trainer", "I coach members. The gym owner gave me the gym code.") { mode = JoinMode.TRAINER; text = "" }
            RoleCard(Icons.Outlined.Storefront, "I own the gym", "Set up the gym and see everyone's progress.") { mode = JoinMode.OWNER; text = "" }
            TextButton(onClick = { gvm.signOut() }) { Text("Use a different Google account") }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { mode = JoinMode.CHOOSE; gvm.message.value = null }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                Text(when (mode) { JoinMode.OWNER -> "Set up your gym"; JoinMode.TRAINER -> "Join as trainer"; else -> "Join your trainer" },
                    style = MaterialTheme.typography.headlineSmall)
            }
            val isCode = mode != JoinMode.OWNER
            Text(when (mode) {
                JoinMode.OWNER -> "What is your gym called? You'll get a gym code to give your trainers."
                JoinMode.TRAINER -> "Enter the 6-letter gym code from the gym owner. The owner approves you with one tap."
                else -> "Enter the 6-letter code your trainer gave you."
            }, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                text, { text = if (isCode) it.uppercase().filter(Char::isLetterOrDigit).take(6) else it.take(40) },
                Modifier.fillMaxWidth(), singleLine = true,
                label = { Text(if (isCode) "Code" else "Gym name") },
                textStyle = MaterialTheme.typography.headlineSmall,
                keyboardOptions = KeyboardOptions(capitalization = if (isCode) KeyboardCapitalization.Characters else KeyboardCapitalization.Words),
            )
            message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = {
                    gvm.message.value = null
                    when (mode) {
                        JoinMode.OWNER -> gvm.createGym(text)
                        JoinMode.TRAINER -> gvm.joinAsTrainer(text)
                        else -> gvm.joinAsMember(text)
                    }
                },
                enabled = !busy && (if (isCode) text.length == 6 else text.isNotBlank()),
                modifier = Modifier.fillMaxWidth().height(60.dp),
            ) {
                if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                else Text(if (mode == JoinMode.OWNER) "Create gym" else "Join", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun RoleCard(icon: ImageVector, title: String, sub: String, onClick: () -> Unit) {
    JkCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Trainer waiting for the owner, or someone removed from the gym. */
@Composable
fun WaitingScreen(gvm: GymViewModel, gym: Gym?, removed: Boolean) {
    JoinColumn {
        Spacer(Modifier.height(64.dp))
        Icon(Icons.Outlined.HourglassTop, null, Modifier.size(64.dp).align(Alignment.CenterHorizontally), tint = MaterialTheme.colorScheme.primary)
        Text(if (removed) "You're no longer part of ${gym?.name ?: "this gym"}" else "Waiting for approval",
            Modifier.fillMaxWidth(), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(
            if (removed) "Ask the gym owner if this is a mistake, or join another gym."
            else "We've asked the owner of ${gym?.name ?: "the gym"} to approve you. This screen updates by itself once they do.",
            Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = { gvm.leaveGym() }, modifier = Modifier.fillMaxWidth()) {
            Text(if (removed) "Join another gym" else "Cancel and use a different code")
        }
        TextButton(onClick = { gvm.signOut() }, modifier = Modifier.fillMaxWidth()) { Text("Sign out") }
    }
}
