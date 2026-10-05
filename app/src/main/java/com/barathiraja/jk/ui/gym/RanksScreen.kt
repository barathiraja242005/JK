package com.barathiraja.jk.ui.gym

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.barathiraja.jk.gym.Award
import com.barathiraja.jk.gym.MonthAwards
import com.barathiraja.jk.gym.Role
import com.barathiraja.jk.gym.Scoring
import com.barathiraja.jk.ui.GymViewModel
import com.barathiraja.jk.ui.Routes
import com.barathiraja.jk.ui.components.TabScreen
import com.barathiraja.jk.ui.theme.CodeFont
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy")
fun monthLabel(key: String): String = runCatching { YearMonth.parse(key).format(monthFmt) }.getOrDefault(key)

/** Trainers' and members' Ranks tab: live leaderboards for this month plus the wall of awards. (The owner has People and Awards tabs instead.) */
@Composable
fun RanksScreen(gvm: GymViewModel, nav: NavHostController) {
    val members by gvm.memberRanking.collectAsStateWithLifecycle()
    val trainers by gvm.trainerRanking.collectAsStateWithLifecycle()
    val awards by gvm.awards.collectAsStateWithLifecycle()
    val given by gvm.givenAwards.collectAsStateWithLifecycle()
    val me by gvm.me.collectAsStateWithLifecycle()
    val tab by gvm.ranksTab.collectAsStateWithLifecycle()
    val month = YearMonth.now()
    val canOpen = me?.role != Role.MEMBER

    TabScreen("Leaderboard", subtitle = month.format(monthFmt)) {
        item {
            SegmentedTabs(listOf(SegmentTab("Members"), SegmentTab("Trainers"), SegmentTab("Awards")), tab, onSelect = { gvm.showRanksTab(it) })
        }
        when (tab) {
            0 -> {
                item { Text("Points: finish a workout +${Scoring.COMPLETED}, on the day +${Scoring.ON_TIME}, each set +${Scoring.SET_POINT}, coach verified +${Scoring.VERIFIED}, full week +${Scoring.PERFECT_WEEK}.",
                    style = plex(13.sp, line = 18.sp), color = Owner.Muted) }
                if (members.isEmpty()) item { Text("No members yet.", style = plex(15.sp), color = Owner.Ink) }
                itemsIndexed(members, key = { _, s -> s.uid }) { i, s ->
                    val p = gvm.person(s.uid) ?: return@itemsIndexed
                    RankRow(i + 1, p.photoUrl, p.name, "${s.completed}/${s.due} workouts done" + (if (s.due > 0) " · ${pct(s.completed, s.due)}" else ""), "${s.points}", "pts",
                        highlight = s.uid == me?.uid, onClick = { nav.navigate(Routes.gymMember(s.uid)) }.takeIf { canOpen })
                }
            }
            1 -> {
                item { Text("Trainers are ranked by how many of their members' workouts get done, not by how many they assign.",
                    style = plex(13.sp, line = 18.sp), color = Owner.Muted) }
                if (trainers.isEmpty()) item { Text("No trainers yet.", style = plex(15.sp), color = Owner.Ink) }
                itemsIndexed(trainers, key = { _, t -> t.uid }) { i, t ->
                    val p = gvm.person(t.uid) ?: return@itemsIndexed
                    RankRow(i + 1, p.photoUrl, p.name, "${plural(t.members, "member")} · ${Math.round(t.rate * 100)}% completion", "${t.score}", "score",
                        highlight = t.uid == me?.uid,
                        onClick = { nav.navigate(Routes.gymTrainer(t.uid)) }.takeIf { canOpen })
                }
            }
            else -> {
                if (given.isNotEmpty()) {
                    item { OwnerHeading("From the owner") }
                    items(given, key = { "g" + it.id }) { a -> GivenAwardRow(a, gvm.person(a.uid)) }
                    if (awards.isNotEmpty()) item { OwnerHeading("Monthly awards") }
                }
                if (awards.isEmpty() && given.isEmpty()) item {
                    OwnerCardBox {
                        Column {
                            Text("The first awards are announced on the 1st of next month.", style = plex(16.sp, FontWeight.SemiBold, line = 22.sp), color = Owner.Ink)
                            Text("Best Member, Best Trainer, Most Consistent, Most Improved and Iron Lifter.", Modifier.padding(top = 4.dp),
                                style = plex(15.sp, line = 21.sp), color = Owner.Muted)
                        }
                    }
                }
                awards.sortedByDescending { it.month }.forEach { m -> item(key = m.month) { AwardsCard(m, gvm) } }
            }
        }
    }
}

/**
 * One leaderboard line. The top three get a filled rank disc (yellow for first, ink for second and third);
 * your own line is tinted so you can find yourself at a glance. Only a line with [onClick] is a button.
 */
@Composable
private fun RankRow(rank: Int, photo: String?, name: String, sub: String, value: String, unit: String, highlight: Boolean, onClick: (() -> Unit)? = null) {
    val shape = RoundedCornerShape(24.dp)
    val fill = if (highlight) Tone.WARN.fill else Owner.Card
    if (onClick != null) {
        Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = shape, color = fill, contentColor = Owner.Ink) {
            RankRowContent(rank, photo, name, sub, value, unit, highlight)
        }
    } else {
        Surface(modifier = Modifier.fillMaxWidth(), shape = shape, color = fill, contentColor = Owner.Ink) {
            RankRowContent(rank, photo, name, sub, value, unit, highlight)
        }
    }
}

@Composable
private fun RankRowContent(rank: Int, photo: String?, name: String, sub: String, value: String, unit: String, highlight: Boolean) {
    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        val (disc, ink) = when (rank) {
            1 -> Owner.Yellow to Owner.Black
            2, 3 -> Owner.Ink to Owner.OnInk
            else -> Color.Transparent to Owner.Muted
        }
        Box(Modifier.size(32.dp).clip(CircleShape).background(disc), contentAlignment = Alignment.Center) {
            Text("$rank", style = plex(16.sp, FontWeight.SemiBold).copy(fontFamily = CodeFont), color = ink)
        }
        Spacer(Modifier.width(10.dp))
        OwnerAvatar(photo, name, 44.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(if (highlight) "$name (you)" else name, style = plex(16.sp, FontWeight.SemiBold), color = Owner.Ink, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Text(sub, style = plex(13.sp, line = 18.sp), color = Owner.Muted, maxLines = 2)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(value, style = plex(18.sp, FontWeight.Bold), color = Owner.Ink)
            Text(unit, style = plex(13.sp), color = Owner.Muted)
        }
    }
}

/** One month's automatic awards: a black header with the month, then each winner with their medal. */
@Composable
fun AwardsCard(m: MonthAwards, gvm: GymViewModel) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Owner.Card)) {
        Row(Modifier.fillMaxWidth().background(Owner.Hero).padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(monthLabel(m.month), style = plex(15.sp, FontWeight.SemiBold), color = Color.White)
            }
            OwnerChip(plural(m.winners.size, "winner"), Tone.TOP)
        }
        Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            Award.entries.forEach { a ->
                val p = gvm.person(m.winners[a]) ?: return@forEach
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    AwardBadge(a.look(), 46.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(a.label, style = plex(13.sp), color = Owner.Muted)
                        Text(p.name, style = plex(15.sp, FontWeight.SemiBold), color = Owner.Ink, maxLines = 1)
                    }
                    OwnerAvatar(p.photoUrl, p.name, 38.dp)
                }
            }
        }
    }
}
