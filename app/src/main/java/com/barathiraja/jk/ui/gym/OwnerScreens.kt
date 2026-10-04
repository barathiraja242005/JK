package com.barathiraja.jk.ui.gym

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.barathiraja.jk.gym.OwnerStats
import com.barathiraja.jk.ui.components.shareText
import com.barathiraja.jk.ui.theme.Ember
import com.barathiraja.jk.ui.theme.Alert
import com.barathiraja.jk.ui.theme.Sun
import java.time.LocalDate
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.clickable
import java.util.Locale
import java.time.format.TextStyle
import com.barathiraja.jk.ui.components.ProgressLine
import com.barathiraja.jk.ui.components.ProgressRing
import com.barathiraja.jk.ui.components.SmallButton
import com.barathiraja.jk.ui.components.Eyebrow
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.barathiraja.jk.gym.PersonStatus
import com.barathiraja.jk.gym.Role
import com.barathiraja.jk.ui.GymViewModel
import com.barathiraja.jk.ui.Routes
import com.barathiraja.jk.ui.components.JkCard
import com.barathiraja.jk.ui.components.Avatar
import com.barathiraja.jk.ui.components.BackScreen

/**
 * Owner's home, kept minimal: a greeting, today's turnout, then "Needs you" and "Trainers".
 * Each section says in one sentence what it shows.
 */
@Composable
fun OwnerHomeScreen(gvm: GymViewModel, nav: NavHostController) {
    val gym by gvm.gym.collectAsStateWithLifecycle()
    val people by gvm.people.collectAsStateWithLifecycle()
    val digest by gvm.ownerDigest.collectAsStateWithLifecycle()
    val demoOn = gvm.demo.collectAsStateWithLifecycle().value != null
    val me by gvm.me.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val d = digest
    val pending = people.filter { it.role == Role.TRAINER && it.status == PersonStatus.PENDING }
    var showAllIdle by rememberSaveable { mutableStateOf(false) }
    val gymName = gym?.name ?: "the gym"
    val today = LocalDate.now()

    LazyColumn(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Column(Modifier.weight(1f)) {
                    Eyebrow("${gym?.name ?: "My gym"} · ${today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())}, ${today.dayOfMonth} ${today.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())}")
                    Text("${greeting()}, ${me?.firstName ?: "Owner"}", style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(top = 2.dp))
                }
                me?.let { Avatar(it.photoUrl, it.name, 44.dp) { nav.navigate(Routes.PROFILE) } }
            }
        }
        if (demoOn) item {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Sample gym, not your real data", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer)
                TextButton(onClick = { gvm.setDemo(false) }) { Text("Turn off") }
            }
        }
        if (d == null) return@LazyColumn

        item { TodayPlate(d) }

        item { Section("Needs you", "Absent = no finished workout for ${OwnerStats.IDLE_DAYS}+ days. No plan = their trainer sent nothing. Buttons open WhatsApp.") }
        item {
            JkCard(Modifier.fillMaxWidth(), padding = 14.dp) {
                if (pending.isEmpty() && d.idle.isEmpty()) {
                    Text("All good. Everyone trained this week.", style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(6.dp))
                }
                var first = true
                fun divider(): Boolean = (!first).also { first = false }
                pending.forEach { p ->
                    if (divider()) HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
                    PersonLine(p, "Wants to join as trainer", Ember, onOpen = null) {}
                    Row(Modifier.padding(start = 60.dp, bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SmallButton("Approve", { gvm.approve(p) }, icon = Icons.Outlined.Check)
                        SmallButton("Reject", { gvm.reject(p) }, ghost = true)
                    }
                }
                val idle = if (showAllIdle) d.idle else d.idle.take(IDLE_PREVIEW)
                idle.forEach { i ->
                    if (divider()) HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
                    val trainer = gvm.person(i.member.trainerUid)
                    val days = if (i.days > 30) "30+ days" else "${i.days} days"
                    val nudgeTrainer = !i.assignedRecently && trainer != null
                    PersonLine(i.member, if (nudgeTrainer) "No plan · $days" else "Absent · $days",
                        if (i.days >= 14) Alert else Sun, onOpen = { nav.navigate(Routes.gymMember(i.member.uid)) }) {
                        SmallButton(if (nudgeTrainer) "Tell ${trainer!!.firstName}" else "Message", {
                            if (nudgeTrainer) context.whatsApp("Hi ${trainer!!.firstName}, ${i.member.name} hasn't had a workout for $days. Please assign one in the JK app and check in with them. Thanks!")
                            else context.whatsApp("Hi ${i.member.firstName}, we haven't seen you at $gymName for a while. Your trainer has a workout ready for you in the JK app. See you soon! 💪")
                        }, icon = Icons.AutoMirrored.Outlined.ArrowForward)
                    }
                }
                if (d.idle.size > IDLE_PREVIEW) {
                    TextButton(onClick = { showAllIdle = !showAllIdle }, Modifier.fillMaxWidth()) {
                        Text(if (showAllIdle) "Show less" else "Show all ${d.idle.size}")
                    }
                }
            }
        }

        item { Section("Trainers", "Share of their members' workouts finished this month. Tap a trainer to see their members.") }
        item {
            JkCard(Modifier.fillMaxWidth(), padding = 14.dp) {
                if (d.trainers.isEmpty()) {
                    Text("No trainers yet. Share your gym code from the Me tab.", color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(6.dp))
                }
                d.trainers.forEachIndexed { i, t ->
                    if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
                    val color = when { t.due == 0 -> MaterialTheme.colorScheme.outline; t.rate >= 0.5f -> Ember; else -> Alert }
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { nav.navigate(Routes.gymTrainer(t.trainer.uid)) }
                        .padding(vertical = 12.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Avatar(t.trainer.photoUrl, t.trainer.name, 46.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(t.trainer.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(plural(t.members, "member") + if (t.idle > 0) " · ${t.idle} absent" else "",
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            Spacer(Modifier.height(8.dp))
                            ProgressLine(t.rate, color)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(if (t.due == 0) "–" else "${Math.round(t.rate * 100)}%", style = MaterialTheme.typography.headlineSmall)
                            Text("finished", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

private const val IDLE_PREVIEW = 5

private fun greeting(): String = when (java.time.LocalTime.now().hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}

/** Section heading (wide, uppercase) with one plain sentence under it. */
@Composable
internal fun Section(title: String, explain: String? = null) {
    Column(Modifier.padding(start = 6.dp, end = 6.dp, top = 14.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (explain != null) Text(explain, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp))
    }
}

/** A section heading followed by its steel card; used by the owner's Me screen. */
@Composable
internal fun OwnerCard(title: String, count: Int = 0, explain: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Section(if (count > 0) "$title · $count" else title, explain)
        JkCard(Modifier.fillMaxWidth(), content = content)
    }
}

/** Today's turnout: a thin ring with the percentage, and the plain numbers beside it. */
@Composable
private fun TodayPlate(d: OwnerStats.Digest) {
    val fraction = if (d.members == 0) 0f else d.trainedToday / d.members.toFloat()
    val left = (d.assignedToday - d.trainedToday - d.inProgressToday).coerceAtLeast(0)
    JkCard(Modifier.fillMaxWidth(), padding = 20.dp) {
        Text("Today", style = MaterialTheme.typography.titleLarge)
        Text("Members who finished the workout their trainer gave them.", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                ProgressRing(fraction, Modifier.fillMaxSize())
                Text("${Math.round(fraction * 100)}%", style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.width(20.dp))
            Column {
                Text("${d.trainedToday} of ${d.members}", style = MaterialTheme.typography.headlineMedium)
                Text("members finished", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (d.inProgressToday > 0 || left > 0) Text(
                    listOfNotNull(
                        "${d.inProgressToday} training now".takeIf { d.inProgressToday > 0 },
                        "$left not started".takeIf { left > 0 },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

/** A person, a status dot with its word, and the action on the right. */
@Composable
private fun PersonLine(p: com.barathiraja.jk.gym.Person, why: String, dot: Color, onOpen: (() -> Unit)?, actions: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).let { if (onOpen != null) it.clickable(onClick = onOpen) else it }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(p.photoUrl, p.name, 46.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(p.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
                Spacer(Modifier.width(8.dp))
                Text(why, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
        Spacer(Modifier.width(8.dp))
        actions()
    }
}

/** Opens WhatsApp with [text] so the owner just picks the contact; falls back to the share sheet. */
private fun Context.whatsApp(text: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    runCatching { startActivity(Intent(send).setPackage("com.whatsapp")) }
        .onFailure { shareText(text) }
}

/** One trainer's members and how each did today, for the owner. */
@Composable
fun TrainerDetailScreen(uid: String, gvm: GymViewModel, nav: NavHostController) {
    val people by gvm.people.collectAsStateWithLifecycle()
    val assignments by gvm.assignments.collectAsStateWithLifecycle()
    val scores by gvm.monthScores.collectAsStateWithLifecycle()
    val trainer = people.firstOrNull { it.uid == uid }
    val members = people.filter { it.role == Role.MEMBER && it.active && it.trainerUid == uid }
    val today = gvm.today
    BackScreen(trainer?.name ?: "Trainer", onBack = { nav.popBackStack() }) {
        item {
            val todays = assignments.filter { it.trainerUid == uid && it.epochDay == today }
            JkCard(Modifier.fillMaxWidth()) {
                Text("Today: ${todays.count { it.done }} of ${todays.size} workouts done", style = MaterialTheme.typography.titleLarge)
                Text("${members.size} members", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(members, key = { it.uid }) { m ->
            val a = assignments.filter { it.memberUid == m.uid && it.epochDay == today }.minByOrNull { if (it.done) 1 else 0 }
            val s = scores[m.uid]
            JkCard(Modifier.fillMaxWidth(), onClick = { nav.navigate(Routes.gymMember(m.uid)) }) {
                PersonRow(m, sub = {
                    Text("This month ${pct(s?.completed ?: 0, s?.due ?: 0)} · ${s?.points ?: 0} pts",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }, trailing = { StatusPill(a, today) })
            }
        }
        if (members.isEmpty()) item { Text("No members yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
