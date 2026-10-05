package com.barathiraja.jk.ui.gym

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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

    val people by gvm.people.collectAsStateWithLifecycle()
    val digest by gvm.ownerDigest.collectAsStateWithLifecycle()
    val idle = digest?.idle.orEmpty().associateBy { it.member.uid }

    OwnerPage {
        item { PageTitle("Ranks", "Who's on top in ${month.format(monthFmt)}. Points come from finishing workouts.") }
        item {
            SegmentedTabs(listOf(SegmentTab("Members"), SegmentTab("Trainers"), SegmentTab("Awards")), tab, onSelect = { gvm.showRanksTab(it) })
        }
        when (tab) {
            0 -> {
                item {
                    Text("Finish a workout +${Scoring.COMPLETED}, on the day +${Scoring.ON_TIME}, each set +${Scoring.SET_POINT}, " +
                        "checked by your coach +${Scoring.VERIFIED}, full week +${Scoring.PERFECT_WEEK}.",
                        style = plex(13.sp, line = 18.sp), color = Owner.Muted, modifier = Modifier.padding(horizontal = 4.dp))
                }
                val entries = members.mapIndexedNotNull { i, s ->
                    val p = people.firstOrNull { it.uid == s.uid } ?: return@mapIndexedNotNull null
                    memberEntry(p, s, top = i == 0 && s.points > 0, idle[p.uid], gvm.trainerOf(p) != null, extra = null,
                        value = "${s.points} pts", isMe = p.uid == me?.uid,
                        onClick = { nav.navigate(Routes.gymMember(p.uid)) }.takeIf { canOpen })
                }
                if (entries.isEmpty()) item { OwnerCardBox { Text("No members yet.", style = plex(15.sp), color = Owner.Muted) } }
                leaderboard(entries, "rank-members")
            }
            1 -> {
                item {
                    Text("Trainers are ranked by how many of their members' workouts get done, not by how many they give.",
                        style = plex(13.sp, line = 18.sp), color = Owner.Muted, modifier = Modifier.padding(horizontal = 4.dp))
                }
                val entries = trainers.mapIndexedNotNull { i, t ->
                    val p = people.firstOrNull { it.uid == t.uid } ?: return@mapIndexedNotNull null
                    RankEntry(
                        key = "t" + t.uid, person = p, ranked = t.score > 0,
                        status = (if (i == 0 && t.score > 0) "Top trainer" to Tone.TOP else "${Math.round(t.rate * 100)}% done" to Tone.NONE),
                        pct = Math.round(t.rate * 100), detail = plural(t.members, "member"),
                        onClick = { nav.navigate(Routes.gymTrainer(t.uid)) }.takeIf { canOpen },
                        isMe = t.uid == me?.uid,
                    )
                }
                if (entries.isEmpty()) item { OwnerCardBox { Text("No trainers yet.", style = plex(15.sp), color = Owner.Muted) } }
                leaderboard(entries, "rank-trainers")
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
