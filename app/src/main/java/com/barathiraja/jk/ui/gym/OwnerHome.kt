package com.barathiraja.jk.ui.gym

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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


@Composable
fun OwnerHomeScreen(gvm: GymViewModel, nav: NavHostController) {
    val gym by gvm.gym.collectAsStateWithLifecycle()
    val people by gvm.people.collectAsStateWithLifecycle()
    val digest by gvm.ownerDigest.collectAsStateWithLifecycle()
    val me by gvm.me.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var rejecting by remember { mutableStateOf<Person?>(null) }
    var choosingFor by remember { mutableStateOf<Person?>(null) }
    var showAll by rememberSaveable { mutableStateOf(false) }
    val gymName = gym?.name ?: "your gym"

    OwnerPage {
        item { GreetingHeader(me) }
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
        if (needs.isEmpty()) item { AllClearCard("No one is waiting to join and every member trained this week.") }
        else item {
            val rows = needs.map { n ->
                when (n) {
                    is Need.Joining -> joiningRow(n.p, onApprove = { gvm.approve(n.p) }, onReject = { rejecting = n.p })
                    is Need.NoTrainer -> noTrainerRow(n.p, onOpen = { nav.navigate(Routes.gymMember(n.p.uid)) }, onChoose = { choosingFor = n.p })
                    is Need.Away -> awayRow(n.i, gvm.trainerOf(n.i.member), gymName, context,
                        onOpen = { nav.navigate(Routes.gymMember(n.i.member.uid)) })
                }
            }
            NeedsList(rows, showAll) { showAll = !showAll }
        }
    }

    rejecting?.let { p ->
        AlertDialog(onDismissRequest = { rejecting = null },
            title = { Text("Reject ${p.firstName}?") },
            text = { Text("${p.firstName} won't join as a trainer. They can ask again with your gym code.", style = plex(15.sp, line = 21.sp)) },
            confirmButton = { TextButton(onClick = { rejecting = null; gvm.reject(p) }) {
                Text("Reject ${p.firstName}", style = plex(14.sp, FontWeight.SemiBold), color = Owner.RedText) } },
            dismissButton = { TextButton(onClick = { rejecting = null }) { Text("Keep waiting", style = plex(14.sp, FontWeight.SemiBold), color = Owner.Ink) } })
    }
    choosingFor?.let { m -> ChangeTrainerDialog(m, gvm) { choosingFor = null } }
}


/** Today on the charcoal card: members who trained, compared with yesterday, and the whole gym's month. */
@Composable
private fun TodayCard(d: OwnerStats.Digest, gymName: String) {
    TodayHero(
        label = "Today at $gymName", done = d.trainedToday, total = d.members,
        unit = if (d.members == 1) "member trained" else "members trained",
        compare = if (d.members == 0) AnnotatedString("Members join through their trainer.")
            else comparedWithYesterday(d.trainedToday, d.trainedYesterday, d.inProgressToday.takeIf { it > 0 }?.let { "$it training now" }),
        monthDone = d.trainers.sumOf { it.done }, monthDue = d.trainers.sumOf { it.due },
    )
}

// ---------- Needs you ----------

private sealed interface Need {
    val key: String
    data class Joining(val p: Person) : Need { override val key get() = "j" + p.uid }
    data class Away(val i: OwnerStats.Idle) : Need { override val key get() = "a" + i.member.uid }
    data class NoTrainer(val p: Person) : Need { override val key get() = "n" + p.uid }
}


@Composable
private fun joiningRow(p: Person, onApprove: () -> Unit, onReject: () -> Unit) = NeedRow(
    "j" + p.uid, p, "New trainer", Tone.WARN, "Wants to join", onOpen = null,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        SmallAction(Icons.Outlined.Close, "Reject ${p.firstName}", red = false, onReject)
        SmallAction(Icons.Outlined.Check, "Approve ${p.firstName}", red = true, onApprove)
    }
}

@Composable
private fun noTrainerRow(m: Person, onOpen: () -> Unit, onChoose: () -> Unit) = NeedRow(
    "n" + m.uid, m, "No trainer", Tone.BAD, "Their trainer left", onOpen,
) { SmallPill("Choose", onChoose) }

@Composable
private fun awayRow(i: OwnerStats.Idle, trainer: Person?, gymName: String, context: Context, onOpen: () -> Unit): NeedRow {
    val m = i.member
    // The trainer to nudge when they gave no workout lately; null means the member is simply away.
    val nudge = trainer?.takeIf { !i.assignedRecently }
    val days = if (i.days > 30) "30+ days" else plural(i.days, "day")
    return NeedRow(
        "a" + m.uid, m, if (nudge != null) "No plan" else "Away", if (nudge != null) Tone.WARN else Tone.BAD,
        if (nudge != null) "No plan for $days" else "No workout in $days", onOpen,
    ) {
        SmallAction(Icons.AutoMirrored.Outlined.Send, if (nudge != null) "Message ${nudge.firstName}" else "Message ${m.firstName}", red = false) {
            if (nudge != null) context.whatsApp("Hi ${nudge.firstName}, ${m.name} hasn't had a workout for ${i.days} days. Please assign one in the JK app and check in with them. Thanks!")
            else context.whatsApp("Hi ${m.firstName}, we haven't seen you at $gymName for a while. Your trainer has a workout ready for you in the JK app. See you soon! 💪")
        }
    }
}

/** A small status chip, then a short muted line after it; the name above gets the full width. */
@Composable
private fun ChipLine(chip: String, tone: Tone, line: String, lines: Int) {
    Row(verticalAlignment = Alignment.Top) {
        OwnerChip(chip, tone, Modifier.padding(top = 1.dp), small = true)
        Spacer(Modifier.width(6.dp))
        Text(line, style = plex(13.sp, line = 18.sp), color = Owner.Muted, maxLines = lines, overflow = TextOverflow.Ellipsis)
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
            SegmentedTabs(listOf(SegmentTab("Members", members.size), SegmentTab("Trainers", trainers.size)),
                if (trainersTab) 1 else 0, { trainersTab = it == 1 })
        }
        item {
            Text(
                if (trainersTab) "Workouts their members finished this month, out of those given. Higher is better."
                else "Workouts each member finished this month, out of those given.",
                style = plex(14.sp, line = 19.sp), color = Owner.Muted, modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        // Both tabs become the same kind of entries, so members and trainers look exactly alike.
        val entries = if (trainersTab) trainers.mapIndexed { i, t ->
            RankEntry(
                key = "t" + t.trainer.uid, person = t.trainer, ranked = t.due > 0,
                status = trainerStatus(t, top = i == 0 && t.due > 0),
                pct = if (t.due > 0) Math.round(t.rate * 100) else null,
                detail = if (t.due > 0) "${t.done} of ${t.due} done · ${plural(t.members, "member")}" else "${plural(t.members, "member")} · no workouts yet",
                onClick = { nav.navigate(Routes.gymTrainer(t.trainer.uid)) },
            )
        } else members.mapIndexed { i, (p, sc) ->
            val trainer = gvm.trainerOf(p)
            memberEntry(p, sc, top = i == 0 && sc.points > 0, idle[p.uid], trainer != null, trainer?.firstName) {
                nav.navigate(Routes.gymMember(p.uid))
            }
        }
        if (entries.isEmpty()) item {
            Empty(if (trainersTab) "No trainers yet. Share your gym code from the Me tab so they can join."
                else "No members yet. Trainers add members with their own code.")
        }
        leaderboard(entries, if (trainersTab) "trainers" else "members")
    }
}

@Composable
private fun Empty(text: String) {
    OwnerCardBox { Text(text, style = plex(15.sp, line = 21.sp), color = Owner.Muted) }
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

/** A compact list row for one person: face, name with status, one line of numbers and a thin bar. */
@Composable
private fun PersonLine(
    photo: String?, name: String, status: Pair<String, Tone>, line: String, fraction: Float?, onClick: () -> Unit,
) {
    OwnerCardBox(onClick = onClick, onClickLabel = "Open $name", padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OwnerAvatar(photo, name, 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(name, style = plex(15.sp, FontWeight.SemiBold), color = Owner.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                ChipLine(status.first, status.second, line, lines = 1)
                if (fraction != null) OwnerBar(fraction, Owner.Well, if (status.second == Tone.BAD) Owner.Red else Owner.Ink, "row-$name-$fraction", 5.dp)
            }
            Spacer(Modifier.width(4.dp))
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, Modifier.size(22.dp), tint = Owner.Muted)
        }
    }
}

/** A member row for other pages (a trainer's members): same look as the People tab, without the rank. */
@Composable
internal fun MemberCard(p: Person, s: Scoring.MemberScore, idle: OwnerStats.Idle?, trainer: Person?, onClick: () -> Unit) {
    PersonLine(
        p.photoUrl, p.name, memberStatus(s, false, idle, trainer != null),
        when {
            idle != null -> "No workout for ${if (idle.days > 30) "over a month" else plural(idle.days, "day")}"
            s.due == 0 -> "No workouts given yet this month"
            else -> "Finished ${s.completed} of ${plural(s.due, "workout")} this month"
        },
        s.rate.takeIf { s.due > 0 }, onClick,
    )
}
