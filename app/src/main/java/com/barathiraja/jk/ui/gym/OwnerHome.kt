package com.barathiraja.jk.ui.gym

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
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
import com.barathiraja.jk.ui.theme.Charcoal
import com.barathiraja.jk.ui.theme.Watch
import java.time.LocalTime

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
                    Text(greeting(), style = plex(15.sp), color = Owner.Muted)
                    Text(me?.firstName ?: "Owner", style = plex(23.sp, FontWeight.Bold, tracking = (-0.6).sp), color = Owner.Ink,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                OwnerAvatar(me?.photoUrl, me?.name ?: "Owner", 40.dp)
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

private fun greeting(): String = when (LocalTime.now().hour) {
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
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Owner.Hero).padding(16.dp)) {
        Text("Today at $gymName", style = plex(14.sp), color = Owner.OnDarkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 6.dp)) {
            Text("${d.trainedToday}", style = plex(40.sp, FontWeight.Bold, line = 50.sp, tracking = (-2).sp), color = Color.White)
            Text("  of ${d.members}", style = plex(20.sp, FontWeight.SemiBold), color = Owner.OnDarkMuted, modifier = Modifier.padding(bottom = 8.dp))
        }
        Text(if (d.members == 1) "member trained" else "members trained", style = plex(15.sp, FontWeight.SemiBold), color = Color.White)
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
        }, style = plex(14.sp, line = 18.sp), color = Owner.OnDarkSoft)
        // The month in one sentence: of every 100 workouts trainers gave, how many members finished.
        val due = d.trainers.sumOf { it.due }
        val done = d.trainers.sumOf { it.done }
        if (due > 0) Row(Modifier.padding(top = 16.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Owner.DarkStrip)
            .padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${Math.round(done * 100f / due)}%", style = plex(18.sp, FontWeight.Bold), color = Owner.Yellow)
            Spacer(Modifier.width(12.dp))
            Text("of this month's workouts finished ($done of $due)", style = plex(13.sp, line = 17.sp), color = Owner.OnDarkSoft)
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
                Icon(Icons.Outlined.Check, null, Modifier.size(22.dp), tint = Owner.Black)
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text("All clear", style = plex(15.sp, FontWeight.Bold), color = Owner.Ink)
                Text("No one is waiting to join and every member trained this week.", style = plex(14.sp, line = 18.sp), color = Owner.Muted)
            }
        }
    }
}

/** One "Needs you" row: who, their status word and reason, and the action(s) for the right-hand slot. */
private class NeedRow(
    val key: String, val person: Person, val status: String, val tone: Tone, val reason: String,
    val onOpen: (() -> Unit)?, val action: @Composable () -> Unit,
)

/** Width of the action slot, so names and reasons line up down the list whatever the action is. */
private val NEED_ACTION_SLOT = 96.dp

/**
 * Everyone who needs the owner in one card, rows split by fine lines like the People list. Every row has the same
 * three columns: photo, three short lines (name, status word, reason), and a fixed-width action slot on the right. Only the first
 * [NEEDS_PREVIEW] show until "Show all" (the card's last row).
 */
@Composable
private fun NeedsList(rows: List<NeedRow>, showAll: Boolean, onToggle: () -> Unit) {
    val shown = if (showAll) rows else rows.take(NEEDS_PREVIEW)
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Owner.Card)) {
        shown.forEachIndexed { i, r ->
            if (i > 0) RowDivider()
            key(r.key) { NeedLine(r) }
        }
        if (rows.size > NEEDS_PREVIEW) {
            RowDivider(inset = 0.dp)
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onToggle).heightIn(min = 52.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (showAll) "Show fewer" else "Show all ${rows.size}", style = plex(14.sp, FontWeight.SemiBold), color = Owner.Ink)
                Spacer(Modifier.width(4.dp))
                Icon(if (showAll) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, null, Modifier.size(20.dp), tint = Owner.Ink)
            }
        }
    }
}

/** A fine line between rows, starting under the text column (after the photo) unless [inset] says otherwise. */
@Composable
private fun RowDivider(inset: Dp = 68.dp) {
    Box(Modifier.padding(start = inset).fillMaxWidth().height(1.dp).background(Owner.Line))
}

@Composable
private fun NeedLine(r: NeedRow) {
    Row(
        Modifier.fillMaxWidth()
            .then(if (r.onOpen != null) Modifier.clickable(onClickLabel = "Open ${r.person.firstName}", onClick = r.onOpen) else Modifier)
            .heightIn(min = 72.dp).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OwnerAvatar(r.person.photoUrl, r.person.name, 44.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(r.person.name, style = plex(15.sp, FontWeight.SemiBold), color = Owner.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(r.status, style = plex(13.sp, FontWeight.SemiBold), color = if (r.tone == Tone.WARN) Watch else r.tone.ink, maxLines = 1)
            Text(r.reason, style = plex(13.sp), color = Owner.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Box(Modifier.width(NEED_ACTION_SLOT), contentAlignment = Alignment.CenterEnd) { r.action() }
    }
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

/** A round 48dp icon button: red for the yes, outlined otherwise. [label] is read out by TalkBack. */
@Composable
private fun SmallAction(icon: ImageVector, label: String, red: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = if (red) Owner.Red else Owner.Card, contentColor = if (red) Color.White else Owner.Ink,
        border = if (red) null else BorderStroke(1.5.dp, Owner.Line), modifier = Modifier.size(48.dp)) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, label, Modifier.size(20.dp)) }
    }
}

/** A short outlined pill with a word, for an action an icon can't say. */
@Composable
private fun SmallPill(text: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(50), color = Owner.Ink, contentColor = Owner.OnInk, modifier = Modifier.heightIn(min = 48.dp)) {
        Box(Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) { Text(text, style = plex(13.sp, FontWeight.SemiBold)) }
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
            val away = idle[p.uid]
            RankEntry(
                key = "m" + p.uid, person = p, ranked = sc.points > 0,
                status = memberStatus(sc, top = i == 0 && sc.points > 0, away, trainer != null),
                pct = if (sc.due > 0) Math.round(sc.rate * 100) else null,
                detail = when {
                    away != null -> "No workout for ${if (away.days > 30) "30+ days" else plural(away.days, "day")}"
                    sc.due == 0 -> "No workouts yet"
                    else -> "${sc.completed} of ${sc.due} done"
                } + (trainer?.let { " · ${it.firstName}" } ?: ""),
                onClick = { nav.navigate(Routes.gymMember(p.uid)) },
            )
        }
        if (entries.isEmpty()) item {
            Empty(if (trainersTab) "No trainers yet. Share your gym code from the Me tab so they can join."
                else "No members yet. Trainers add members with their own code.")
        }
        // The top three (who have done something) stand on a podium; everyone else is one tidy list.
        val podium = entries.take(3).filter { it.ranked }
        if (podium.isNotEmpty()) item(key = "podium-$trainersTab") { Podium(podium) }
        val rest = entries.drop(podium.size)
        if (rest.isNotEmpty()) item(key = "list-$trainersTab") { RankList(rest, firstRank = podium.size + 1) }
    }
}

/** One person on the People tab, the same shape for members and trainers. [pct] is null when nothing was due. */
private class RankEntry(
    val key: String, val person: Person, val ranked: Boolean, val status: Pair<String, Tone>,
    val pct: Int?, val detail: String, val onClick: () -> Unit,
)

/**
 * The top three as a podium on a white card: first in the middle, larger, with a yellow ring and a yellow rank
 * badge on the photo; second and third either side with charcoal badges. Each shows first name and percentage.
 */
@Composable
private fun Podium(top: List<RankEntry>) {
    // Visual order: 2nd, 1st, 3rd.
    val order = listOfNotNull(top.getOrNull(1)?.let { 2 to it }, top.getOrNull(0)?.let { 1 to it }, top.getOrNull(2)?.let { 3 to it })
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Owner.Card).padding(horizontal = 8.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom,
    ) {
        order.forEach { (rank, e) ->
            val first = rank == 1
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).clickable(onClickLabel = "Open ${e.person.firstName}", onClick = e.onClick)
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val size = if (first) 72.dp else 56.dp
                Box(Modifier.size(size + 10.dp), contentAlignment = Alignment.TopCenter) {
                    Box(Modifier.size(size).clip(CircleShape).background(if (first) Owner.Yellow else Owner.Line).padding(3.dp)
                        .clip(CircleShape).background(Owner.Card).padding(2.dp)) {
                        OwnerAvatar(e.person.photoUrl, e.person.name, size - 10.dp)
                    }
                    RankBadge(rank, Modifier.align(Alignment.BottomCenter))
                }
                Spacer(Modifier.height(6.dp))
                Text(e.person.firstName, style = plex(14.sp, FontWeight.SemiBold), color = Owner.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(e.pct?.let { "$it%" } ?: "–", style = plex(if (first) 20.sp else 17.sp, FontWeight.Bold), color = Owner.Ink)
                Text(e.status.first, style = plex(11.sp, FontWeight.SemiBold), color = Owner.Muted, maxLines = 1)
            }
        }
    }
}

/** A small round rank number with a white ring, sitting on the bottom edge of a photo. Yellow for first. */
@Composable
private fun RankBadge(rank: Int, modifier: Modifier = Modifier) {
    Box(
        modifier.size(24.dp).clip(CircleShape).background(Owner.Card).padding(2.dp).clip(CircleShape)
            .background(if (rank == 1) Owner.Yellow else Charcoal),
        contentAlignment = Alignment.Center,
    ) { Text("$rank", style = plex(11.sp, FontWeight.Bold), color = if (rank == 1) Owner.Black else Color.White) }
}

/** Everyone after the podium in one card: rank number, photo, name with status and numbers, percentage. */
@Composable
private fun RankList(entries: List<RankEntry>, firstRank: Int) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Owner.Card)) {
        entries.forEachIndexed { i, e ->
            if (i > 0) Box(Modifier.padding(start = 92.dp).fillMaxWidth().height(1.dp).background(Owner.Line))
            Row(
                Modifier.fillMaxWidth().clickable(onClickLabel = "Open ${e.person.firstName}", onClick = e.onClick)
                    .padding(start = 8.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (e.ranked) "${firstRank + i}" else "–", Modifier.width(32.dp), style = plex(15.sp, FontWeight.Bold),
                    color = Owner.Muted, textAlign = TextAlign.Center)
                OwnerAvatar(e.person.photoUrl, e.person.name, 44.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(e.person.name, style = plex(15.sp, FontWeight.SemiBold), color = Owner.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OwnerChip(e.status.first, e.status.second, small = true)
                        Spacer(Modifier.width(6.dp))
                        Text(e.detail, style = plex(12.sp), color = Owner.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(e.pct?.let { "$it%" } ?: "–", style = plex(16.sp, FontWeight.Bold), color = Owner.Ink)
            }
        }
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
