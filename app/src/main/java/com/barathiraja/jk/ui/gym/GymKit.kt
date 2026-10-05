package com.barathiraja.jk.ui.gym

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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barathiraja.jk.gym.OwnerStats
import com.barathiraja.jk.gym.Person
import com.barathiraja.jk.gym.Scoring
import com.barathiraja.jk.ui.theme.Charcoal
import com.barathiraja.jk.ui.theme.Watch
import java.time.LocalTime

/*
 * Pieces the owner's and the trainer's Home share: the greeting, the "Needs you" list (one card, aligned rows with a
 * fixed-width action slot) and its small round and pill actions.
 */

/** How many people "Needs you" shows before "Show all". */
internal const val NEEDS_PREVIEW = 3

internal fun greeting(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}

/** One "Needs you" row: who, their status word and reason, and the action(s) for the right-hand slot. */
internal class NeedRow(
    val key: String, val person: Person, val status: String, val tone: Tone, val reason: String,
    val onOpen: (() -> Unit)?, val action: (@Composable () -> Unit)? = null,
)

/** Width of the action slot, so names and reasons line up down the list whatever the action is. */
internal val NEED_ACTION_SLOT = 96.dp

/**
 * Everyone who needs the owner in one card, rows split by fine lines like the People list. Every row has the same
 * three columns: photo, three short lines (name, status word, reason), and a fixed-width action slot on the right. Only the first
 * [NEEDS_PREVIEW] show until "Show all" (the card's last row).
 */
@Composable
internal fun NeedsList(rows: List<NeedRow>, showAll: Boolean, onToggle: () -> Unit) {
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
internal fun RowDivider(inset: Dp = 68.dp) {
    Box(Modifier.padding(start = inset).fillMaxWidth().height(1.dp).background(Owner.Line))
}

@Composable
internal fun NeedLine(r: NeedRow) {
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
        // Rows without an action show a chevron instead, so every row still ends at the same edge.
        if (r.action != null) Box(Modifier.width(NEED_ACTION_SLOT), contentAlignment = Alignment.CenterEnd) { r.action.invoke() }
        else Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, Modifier.size(22.dp), tint = Owner.Muted)
    }
}

/** A round 48dp icon button: red for the yes, outlined otherwise. [label] is read out by TalkBack. */
@Composable
internal fun SmallAction(icon: ImageVector, label: String, red: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = if (red) Owner.Red else Owner.Card, contentColor = if (red) Color.White else Owner.Ink,
        border = if (red) null else BorderStroke(1.5.dp, Owner.Line), modifier = Modifier.size(48.dp)) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, label, Modifier.size(20.dp)) }
    }
}

/** A short outlined pill with a word, for an action an icon can't say. */
@Composable
internal fun SmallPill(text: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(50), color = Owner.Ink, contentColor = Owner.OnInk, modifier = Modifier.heightIn(min = 48.dp)) {
        Box(Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) { Text(text, style = plex(13.sp, FontWeight.SemiBold)) }
    }
}

/**
 * Today on the charcoal card: [done] of [total] in big type with a yellow bar, a [compare] line under it, and the
 * month so far in the strip ([monthDone] of [monthDue]; hidden while nothing is due).
 */
@Composable
internal fun TodayHero(label: String, done: Int, total: Int, unit: String, compare: AnnotatedString, monthDone: Int, monthDue: Int) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Owner.Hero).padding(16.dp)) {
        Text(label, style = plex(14.sp), color = Owner.OnDarkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 6.dp)) {
            Text("$done", style = plex(40.sp, FontWeight.Bold, line = 50.sp, tracking = (-2).sp), color = Color.White)
            Text("  of $total", style = plex(20.sp, FontWeight.SemiBold), color = Owner.OnDarkMuted, modifier = Modifier.padding(bottom = 8.dp))
        }
        Text(unit, style = plex(15.sp, FontWeight.SemiBold), color = Color.White)
        Box(Modifier.padding(top = 14.dp, bottom = 12.dp)) {
            OwnerBar(if (total == 0) 0f else done / total.toFloat(), Owner.DarkTrack, Owner.Yellow, "today-$label-$done-$total", 10.dp)
        }
        Text(compare, style = plex(14.sp, line = 18.sp), color = Owner.OnDarkSoft)
        if (monthDue > 0) Row(Modifier.padding(top = 16.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Owner.DarkStrip)
            .padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${Math.round(monthDone * 100f / monthDue)}%", style = plex(18.sp, FontWeight.Bold), color = Owner.Yellow)
            Spacer(Modifier.width(12.dp))
            Text("of this month's workouts finished ($monthDone of $monthDue)", style = plex(13.sp, line = 17.sp), color = Owner.OnDarkSoft)
        }
    }
}

/** "[count] more / fewer / Same … than yesterday", with the change in bold white for the charcoal card. */
internal fun comparedWithYesterday(today: Int, yesterday: Int, extra: String? = null): AnnotatedString = buildAnnotatedString {
    val diff = today - yesterday
    withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.SemiBold)) {
        append(when { diff > 0 -> "$diff more"; diff < 0 -> "${-diff} fewer"; else -> "Same" })
    }
    append(when { diff > 0 -> " than yesterday"; diff < 0 -> " than yesterday so far"; else -> " as yesterday" })
    if (extra != null) append(" · $extra")
}

/** The "nothing to do" state of a Needs-you list: a yellow tick, "All clear" and what would show up here. */
@Composable
internal fun AllClearCard(explain: String) {
    OwnerCardBox {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(Owner.Yellow), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Check, null, Modifier.size(22.dp), tint = Owner.Black)
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text("All clear", style = plex(15.sp, FontWeight.Bold), color = Owner.Ink)
                Text(explain, style = plex(14.sp, line = 18.sp), color = Owner.Muted)
            }
        }
    }
}

/** A tab's greeting: "Good evening," over the first name, with the photo on the right. */
@Composable
internal fun GreetingHeader(me: Person?) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 8.dp)) {
        Column(Modifier.weight(1f)) {
            Text(greeting(), style = plex(15.sp), color = Owner.Muted)
            Text(me?.firstName ?: "", style = plex(23.sp, FontWeight.Bold, tracking = (-0.6).sp), color = Owner.Ink,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        OwnerAvatar(me?.photoUrl, me?.name ?: "", 40.dp)
    }
}

/** One person in a ranked list (owner People tab, trainer Members tab), the same shape for members and trainers. [pct] is null when nothing was due. */
internal class RankEntry(
    val key: String, val person: Person, val ranked: Boolean, val status: Pair<String, Tone>,
    val pct: Int?, val detail: String, val onClick: () -> Unit,
)

/**
 * The top three as a podium on a white card: first in the middle, larger, with a yellow ring and a yellow rank
 * badge on the photo; second and third either side with charcoal badges. Each shows first name and percentage.
 */
@Composable
internal fun Podium(top: List<RankEntry>) {
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
internal fun RankBadge(rank: Int, modifier: Modifier = Modifier) {
    Box(
        modifier.size(24.dp).clip(CircleShape).background(Owner.Card).padding(2.dp).clip(CircleShape)
            .background(if (rank == 1) Owner.Yellow else Charcoal),
        contentAlignment = Alignment.Center,
    ) { Text("$rank", style = plex(11.sp, FontWeight.Bold), color = if (rank == 1) Owner.Black else Color.White) }
}

/** Everyone after the podium in one card: rank number, photo, name with status and numbers, percentage. */
@Composable
internal fun RankList(entries: List<RankEntry>, firstRank: Int) {
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

/** The top three (who have done something) on a podium, everyone else in one ranked list below. */
internal fun LazyListScope.leaderboard(entries: List<RankEntry>, key: String) {
    val podium = entries.take(3).filter { it.ranked }
    if (podium.isNotEmpty()) item(key = "podium-$key") { Podium(podium) }
    val rest = entries.drop(podium.size)
    if (rest.isNotEmpty()) item(key = "list-$key") { RankList(rest, firstRank = podium.size + 1) }
}

/** A member as a ranked entry: status word, this month's share done, and "3 of 4 done" plus [extra]. */
internal fun memberEntry(
    p: Person, s: Scoring.MemberScore, top: Boolean, idle: OwnerStats.Idle?, hasTrainer: Boolean, extra: String?, onClick: () -> Unit,
) = RankEntry(
    key = "m" + p.uid, person = p, ranked = s.points > 0,
    status = memberStatus(s, top, idle, hasTrainer),
    pct = if (s.due > 0) Math.round(s.rate * 100) else null,
    detail = when {
        idle != null -> "No workout for ${if (idle.days > 30) "30+ days" else plural(idle.days, "day")}"
        s.due == 0 -> "No workouts yet"
        else -> "${s.completed} of ${s.due} done"
    } + (extra?.let { " · $it" } ?: ""),
    onClick = onClick,
)
