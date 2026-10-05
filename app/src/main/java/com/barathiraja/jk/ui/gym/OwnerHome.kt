package com.barathiraja.jk.ui.gym

import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.barathiraja.jk.gym.OwnerStats
import com.barathiraja.jk.gym.Person
import com.barathiraja.jk.gym.PersonStatus
import com.barathiraja.jk.gym.Role
import com.barathiraja.jk.gym.Scoring
import com.barathiraja.jk.ui.GymViewModel
import com.barathiraja.jk.ui.Routes

/*
 * The owner's Home answers two questions and nothing else: "how is my gym doing today?" (the black card) and
 * "what do I need to do?" (Needs you). People, awards and the gym code each live on their own tab, once.
 */

/** How many people "Needs you" shows before "Show all". */
private const val NEEDS_PREVIEW = 3

@Composable
fun OwnerHomeScreen(gvm: GymViewModel, nav: NavHostController) {
    val gym by gvm.gym.collectAsStateWithLifecycle()
    val people by gvm.people.collectAsStateWithLifecycle()
    val digest by gvm.ownerDigest.collectAsStateWithLifecycle()
    val demoOn = gvm.demo.collectAsStateWithLifecycle().value != null
    val me by gvm.me.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var rejecting by remember { mutableStateOf<Person?>(null) }
    var choosingFor by remember { mutableStateOf<Person?>(null) }
    var showAll by rememberSaveable { mutableStateOf(false) }
    val gymName = gym?.name ?: "your gym"

    OwnerPage {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(greeting(), style = plex(17.sp), color = Owner.Muted)
                    Text(me?.firstName ?: "Owner", style = plex(30.sp, FontWeight.Bold, tracking = (-0.6).sp), color = Owner.Ink,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                OwnerAvatar(me?.photoUrl, me?.name ?: "Owner", 52.dp)
            }
        }
        if (demoOn) item {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Tone.WARN.fill)
                .padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Sample gym, not your real data", Modifier.weight(1f), style = plex(16.sp), color = Tone.WARN.ink)
                TextButton(onClick = { gvm.setDemo(false) }, Modifier.heightIn(min = 48.dp)) {
                    Text("Turn off", style = plex(16.sp, FontWeight.SemiBold), color = Tone.WARN.ink)
                }
            }
        }
        val d = digest ?: return@OwnerPage

        item { TodayCard(d, gymName) }

        // Members whose trainer left come first after join requests: they get no workouts until the owner acts.
        val pending = people.filter { it.role == Role.TRAINER && it.status == PersonStatus.PENDING }
        val orphans = people.filter { it.role == Role.MEMBER && it.active && gvm.trainerOf(it) == null }
        val needs = pending.map { Need.Joining(it) } + orphans.map { Need.NoTrainer(it) } +
            d.idle.filter { i -> orphans.none { it.uid == i.member.uid } }.map { Need.Away(it) }

        item {
            OwnerHeading("Needs you", if (needs.isEmpty()) null else
                "${if (needs.size == 1) "1 person" else "${needs.size} people"} to say yes to or check on.")
        }
        if (needs.isEmpty()) item { AllClearCard() }
        val shown = if (showAll) needs else needs.take(NEEDS_PREVIEW)
        items(shown, key = { it.key }) { n ->
            when (n) {
                is Need.Joining -> JoiningCard(n.p, onApprove = { gvm.approve(n.p) }, onReject = { rejecting = n.p })
                is Need.NoTrainer -> NoTrainerCard(n.p, onOpen = { nav.navigate(Routes.gymMember(n.p.uid)) }, onChoose = { choosingFor = n.p })
                is Need.Away -> AwayCard(n.i, gvm.trainerOf(n.i.member), gymName, context, onOpen = { nav.navigate(Routes.gymMember(n.i.member.uid)) })
            }
        }
        if (needs.size > NEEDS_PREVIEW) item {
            PlainButton(if (showAll) "Show fewer" else "Show all ${needs.size}", { showAll = !showAll }, Modifier.fillMaxWidth())
        }
    }

    rejecting?.let { p ->
        AlertDialog(onDismissRequest = { rejecting = null },
            title = { Text("Reject ${p.firstName}?") },
            text = { Text("${p.firstName} won't join as a trainer. They can ask again with your gym code.", style = plex(17.sp, line = 25.sp)) },
            confirmButton = { TextButton(onClick = { rejecting = null; gvm.reject(p) }) {
                Text("Reject ${p.firstName}", style = plex(16.sp, FontWeight.SemiBold), color = Owner.RedText) } },
            dismissButton = { TextButton(onClick = { rejecting = null }) { Text("Keep waiting", style = plex(16.sp, FontWeight.SemiBold)) } })
    }
    choosingFor?.let { m -> ChangeTrainerDialog(m, gvm) { choosingFor = null } }
}

private fun greeting(): String = when (java.time.LocalTime.now().hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}

/**
 * Today at a glance on the black card: the one number that matters (members who trained), a yellow bar for it,
 * how that compares with yesterday in words, and the month so far in the strip.
 */
@Composable
private fun TodayCard(d: OwnerStats.Digest, gymName: String) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Owner.Hero).padding(20.dp)) {
        Text("Today at $gymName", style = plex(16.sp), color = Owner.OnDarkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 6.dp)) {
            Text("${d.trainedToday}", style = plex(56.sp, FontWeight.Bold, line = 60.sp, tracking = (-2).sp), color = Color.White)
            Text("  of ${d.members}", style = plex(24.sp, FontWeight.SemiBold), color = Owner.OnDarkMuted, modifier = Modifier.padding(bottom = 8.dp))
        }
        Text(if (d.members == 1) "member trained" else "members trained", style = plex(17.sp, FontWeight.SemiBold), color = Color.White)
        Box(Modifier.padding(top = 14.dp, bottom = 12.dp)) {
            OwnerBar(if (d.members == 0) 0f else d.trainedToday / d.members.toFloat(), Owner.DarkTrack, Owner.Yellow, "today-${d.trainedToday}-${d.members}", 10.dp)
        }
        val diff = d.trainedToday - d.trainedYesterday
        Text(buildAnnotatedString {
            when {
                d.members == 0 -> append("Members join through their trainer.")
                diff > 0 -> { withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.SemiBold)) { append("$diff more") }; append(" than yesterday") }
                diff < 0 -> { withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.SemiBold)) { append("${-diff} fewer") }; append(" than yesterday so far") }
                else -> { withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.SemiBold)) { append("Same") }; append(" as yesterday") }
            }
            if (d.inProgressToday > 0) append(" · ${d.inProgressToday} training now")
        }, style = plex(16.sp, line = 22.sp), color = Owner.OnDarkSoft)
        // The month in one sentence: of every 100 workouts trainers gave, how many members finished.
        val due = d.trainers.sumOf { it.due }
        val done = d.trainers.sumOf { it.done }
        if (due > 0) Row(Modifier.padding(top = 16.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Owner.DarkStrip)
            .padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${Math.round(done * 100f / due)}%", style = plex(22.sp, FontWeight.Bold), color = Owner.Yellow)
            Spacer(Modifier.width(12.dp))
            Text("of this month's workouts finished ($done of $due)", style = plex(15.sp, line = 20.sp), color = Owner.OnDarkSoft)
        }
    }
}

// ---------- Needs you ----------

private sealed interface Need {
    val key: String
    data class Joining(val p: Person) : Need { override val key get() = "j" + p.uid }
    data class Away(val i: OwnerStats.Idle) : Need { override val key get() = "a" + i.member.uid }
    data class NoTrainer(val p: Person) : Need { override val key get() = "n" + p.uid }
}

@Composable
private fun AllClearCard() {
    OwnerCardBox {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(Owner.Yellow), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Check, null, Modifier.size(26.dp), tint = Owner.Black)
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text("All clear", style = plex(19.sp, FontWeight.Bold), color = Owner.Ink)
                Text("No one is waiting to join and every member trained this week.", style = plex(16.sp, line = 22.sp), color = Owner.Muted)
            }
        }
    }
}

/** One person who needs the owner: who, why in words, and the action(s) under it. Tapping the top opens them. */
@Composable
private fun NeedCard(
    p: Person, chip: String, tone: Tone, line: String, onOpen: (() -> Unit)?, actions: @Composable () -> Unit,
) {
    OwnerCardBox(padding = 16.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .then(if (onOpen != null) Modifier.clickable(onClickLabel = "Open ${p.firstName}", onClick = onOpen) else Modifier),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OwnerAvatar(p.photoUrl, p.name, 52.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(p.name, style = plex(19.sp, FontWeight.SemiBold, line = 23.sp), color = Owner.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    OwnerChip(chip, tone)
                }
                if (onOpen != null) Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, Modifier.size(26.dp), tint = Owner.Muted)
            }
            Text(line, style = plex(17.sp, line = 24.sp), color = Owner.Ink)
            actions()
        }
    }
}

@Composable
private fun JoiningCard(p: Person, onApprove: () -> Unit, onReject: () -> Unit) {
    NeedCard(p, "Wants to join", Tone.WARN, "${p.firstName} asked to join as a trainer with your gym code.", onOpen = null) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PlainButton("Reject", onReject, Modifier.weight(1f))
            RedButton("Approve", onApprove, Modifier.weight(1.4f), Icons.Outlined.Check)
        }
    }
}

@Composable
private fun NoTrainerCard(m: Person, onOpen: () -> Unit, onChoose: () -> Unit) {
    NeedCard(m, "No trainer", Tone.BAD, "${m.firstName}'s trainer left the gym. Pick who trains ${m.firstName} now.", onOpen) {
        RedButton("Choose a trainer", onChoose, Modifier.fillMaxWidth())
    }
}

@Composable
private fun AwayCard(i: OwnerStats.Idle, trainer: Person?, gymName: String, context: Context, onOpen: () -> Unit) {
    val m = i.member
    val noPlan = !i.assignedRecently && trainer != null
    val days = if (i.days > 30) "over a month" else plural(i.days, "day")
    NeedCard(
        m, if (noPlan) "No plan" else "Away", if (noPlan) Tone.WARN else Tone.BAD,
        if (noPlan) "No workout given for $days. Ask ${trainer!!.firstName} to give one."
        else "No workout done for $days." + (trainer?.let { " Trainer: ${it.firstName}." } ?: ""),
        onOpen,
    ) {
        if (noPlan) RedButton("Message ${trainer!!.firstName}", {
            context.whatsApp("Hi ${trainer.firstName}, ${m.name} hasn't had a workout for ${i.days} days. Please assign one in the JK app and check in with them. Thanks!")
        }, Modifier.fillMaxWidth(), Icons.AutoMirrored.Outlined.Send)
        else RedButton("Message ${m.firstName}", {
            context.whatsApp("Hi ${m.firstName}, we haven't seen you at $gymName for a while. Your trainer has a workout ready for you in the JK app. See you soon! 💪")
        }, Modifier.fillMaxWidth(), Icons.AutoMirrored.Outlined.Send)
    }
}

// ---------- People tab ----------

/** Everyone in the gym on one tab, Members | Trainers, best first. Tapping someone opens their page. */
@Composable
fun OwnerPeopleScreen(gvm: GymViewModel, nav: NavHostController) {
    val digest by gvm.ownerDigest.collectAsStateWithLifecycle()
    val people by gvm.people.collectAsStateWithLifecycle()
    val ranking by gvm.memberRanking.collectAsStateWithLifecycle()
    var trainersTab by rememberSaveable { mutableStateOf(false) }
    val d = digest
    val trainers = d?.trainers.orEmpty().sortedWith(compareByDescending<OwnerStats.TrainerRow> { it.due > 0 }.thenByDescending { it.rate })
    val idle = d?.idle.orEmpty().associateBy { it.member.uid }
    val members = ranking.mapNotNull { s -> people.firstOrNull { it.uid == s.uid && it.active }?.let { it to s } }

    OwnerPage {
        item { PageTitle("People", "Everyone in your gym, best this month first. Tap someone to see them or make a change.") }
        item {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(Owner.Card).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Segment("Members", members.size, !trainersTab, Modifier.weight(1f)) { trainersTab = false }
                Segment("Trainers", trainers.size, trainersTab, Modifier.weight(1f)) { trainersTab = true }
            }
        }
        item {
            Text(
                if (trainersTab) "Out of every 100 workouts a trainer gave this month, how many their members finished. Higher is better."
                else "How many of their workouts each member finished this month.",
                style = plex(16.sp, line = 23.sp), color = Owner.Muted, modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        if (trainersTab) {
            if (trainers.isEmpty()) item { Empty("No trainers yet. Share your gym code from the Me tab so they can join.") }
            items(trainers.size, key = { "t" + trainers[it].trainer.uid }) { i ->
                val t = trainers[i]
                TrainerRow(t, i + 1, top = i == 0 && t.due > 0) { nav.navigate(Routes.gymTrainer(t.trainer.uid)) }
            }
        } else {
            if (members.isEmpty()) item { Empty("No members yet. Trainers add members with their own code.") }
            items(members.size, key = { "m" + members[it].first.uid }) { i ->
                val (p, s) = members[i]
                MemberRow(p, s, i + 1, top = i == 0 && s.points > 0, idle[p.uid], gvm.trainerOf(p)) { nav.navigate(Routes.gymMember(p.uid)) }
            }
        }
    }
}

@Composable
private fun Empty(text: String) {
    OwnerCardBox { Text(text, style = plex(17.sp, line = 25.sp), color = Owner.Muted) }
}

/** One side of a two-way switch: black when open, with the count in a small pill. */
@Composable
internal fun Segment(label: String, count: Int, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier.heightIn(min = 52.dp).semantics { this.selected = selected }, shape = RoundedCornerShape(50),
        color = if (selected) Owner.Ink else Color.Transparent, contentColor = if (selected) Owner.OnInk else Owner.Ink) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = plex(17.sp, FontWeight.SemiBold))
            Spacer(Modifier.width(8.dp))
            Box(Modifier.heightIn(min = 26.dp).clip(RoundedCornerShape(50)).background(if (selected) Owner.Yellow else Owner.Well)
                .padding(horizontal = 9.dp), contentAlignment = Alignment.Center) {
                Text("$count", style = plex(14.sp, FontWeight.SemiBold), color = if (selected) Owner.Black else Owner.Ink)
            }
        }
    }
}

internal fun trainerStatus(t: OwnerStats.TrainerRow, top: Boolean): Pair<String, Tone> = when {
    top -> "Top trainer" to Tone.TOP
    t.due == 0 -> "No workouts yet" to Tone.NONE
    t.rate >= 0.7f -> "Doing well" to Tone.GOOD
    t.rate >= 0.4f -> "Could do better" to Tone.WARN
    else -> "Falling behind" to Tone.BAD
}

/** A member's status word and its tone; the same rules everywhere the owner sees members. */
internal fun memberStatus(s: Scoring.MemberScore?, top: Boolean, idle: OwnerStats.Idle?, hasTrainer: Boolean): Pair<String, Tone> {
    val noPlan = idle != null && !idle.assignedRecently && hasTrainer
    return when {
        !hasTrainer -> "No trainer" to Tone.BAD
        top -> "Top member" to Tone.TOP
        noPlan -> "No plan" to Tone.WARN
        idle != null -> "Away" to Tone.BAD
        s == null || s.due == 0 -> "No workouts yet" to Tone.NONE
        s.rate >= 0.7f -> "Doing well" to Tone.GOOD
        s.rate >= 0.4f -> "Could do better" to Tone.WARN
        else -> "Falling behind" to Tone.BAD
    }
}

/** Rank disc: yellow for first, black for second and third, plain after that. */
@Composable
private fun RankDisc(rank: Int, show: Boolean) {
    val (fill, ink) = when {
        !show -> Color.Transparent to Owner.Muted
        rank == 1 -> Owner.Yellow to Owner.Black
        rank <= 3 -> Owner.Ink to Owner.OnInk
        else -> Owner.Well to Owner.Ink
    }
    Box(Modifier.size(34.dp).clip(CircleShape).background(fill), contentAlignment = Alignment.Center) {
        Text(if (show) "$rank" else "–", style = plex(15.sp, FontWeight.Bold), color = ink)
    }
}

/** A list row for one person: rank, face, name and status, then one line of numbers with a bar. */
@Composable
private fun PersonRow(
    rank: Int?, ranked: Boolean, photo: String?, name: String, status: Pair<String, Tone>, line: String, fraction: Float?, onClick: () -> Unit,
) {
    OwnerCardBox(onClick = onClick, onClickLabel = "Open $name", padding = 14.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (rank != null) {
                    RankDisc(rank, ranked)
                    Spacer(Modifier.width(10.dp))
                }
                OwnerAvatar(photo, name, 48.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(name, style = plex(18.sp, FontWeight.SemiBold, line = 22.sp), color = Owner.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    OwnerChip(status.first, status.second)
                }
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, Modifier.size(26.dp), tint = Owner.Muted)
            }
            Text(line, style = plex(16.sp, line = 22.sp), color = Owner.Muted)
            if (fraction != null) OwnerBar(fraction, Owner.Well, if (status.second == Tone.BAD) Owner.Red else Owner.Ink, "row-$name-$fraction")
        }
    }
}

@Composable
private fun TrainerRow(t: OwnerStats.TrainerRow, rank: Int, top: Boolean, onClick: () -> Unit) {
    PersonRow(
        rank, t.due > 0, t.trainer.photoUrl, t.trainer.name, trainerStatus(t, top),
        if (t.due == 0) "Trains ${plural(t.members, "member")}. No workouts given yet this month."
        else "Members finished ${t.done} of ${plural(t.due, "workout")} · trains ${plural(t.members, "member")}",
        t.rate.takeIf { t.due > 0 }, onClick,
    )
}

@Composable
private fun MemberRow(p: Person, s: Scoring.MemberScore, rank: Int, top: Boolean, idle: OwnerStats.Idle?, trainer: Person?, onClick: () -> Unit) {
    val who = trainer?.let { " · trainer ${it.firstName}" } ?: ""
    PersonRow(
        rank, s.points > 0, p.photoUrl, p.name, memberStatus(s, top, idle, trainer != null),
        when {
            idle != null -> "No workout for ${if (idle.days > 30) "over a month" else plural(idle.days, "day")}$who"
            s.due == 0 -> "No workouts given yet this month$who"
            else -> "Finished ${s.completed} of ${plural(s.due, "workout")}$who"
        },
        s.rate.takeIf { s.due > 0 }, onClick,
    )
}

/** A member row for other pages (a trainer's members): same look as the People tab, without the rank. */
@Composable
internal fun MemberCard(p: Person, s: Scoring.MemberScore, idle: OwnerStats.Idle?, trainer: Person?, onClick: () -> Unit) {
    PersonRow(
        null, false, p.photoUrl, p.name, memberStatus(s, false, idle, trainer != null),
        when {
            idle != null -> "No workout for ${if (idle.days > 30) "over a month" else plural(idle.days, "day")}"
            s.due == 0 -> "No workouts given yet this month"
            else -> "Finished ${s.completed} of ${plural(s.due, "workout")} this month"
        },
        s.rate.takeIf { s.due > 0 }, onClick,
    )
}
