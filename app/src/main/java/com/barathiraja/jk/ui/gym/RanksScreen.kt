package com.barathiraja.jk.ui.gym

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.barathiraja.jk.gym.Award
import com.barathiraja.jk.gym.MonthAwards
import com.barathiraja.jk.gym.Scoring
import com.barathiraja.jk.ui.GymViewModel
import com.barathiraja.jk.ui.components.JkCard
import com.barathiraja.jk.ui.components.SectionTitle
import com.barathiraja.jk.ui.components.Avatar
import com.barathiraja.jk.ui.components.TabScreen
import com.barathiraja.jk.ui.theme.Sun
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

    TabScreen("Leaderboard", subtitle = month.format(monthFmt)) {
        item {
            PillSwitch(listOf("Members", "Trainers", "Awards"), tab) { gvm.ranksTab.value = it }
        }
        when (tab) {
            0 -> {
                item { Text("Points: finish a workout +${Scoring.COMPLETED}, on the day +${Scoring.ON_TIME}, each set +${Scoring.SET_POINT}, coach verified +${Scoring.VERIFIED}, full week +${Scoring.PERFECT_WEEK}.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                if (members.isEmpty()) item { Text("No members yet.") }
                itemsIndexed(members, key = { _, s -> s.uid }) { i, s ->
                    val p = gvm.person(s.uid) ?: return@itemsIndexed
                    RankRow(i + 1, p.photoUrl, p.name, "${s.completed}/${s.due} workouts done" + (if (s.due > 0) " · ${pct(s.completed, s.due)}" else ""), "${s.points}", "pts",
                        highlight = s.uid == me?.uid, onClick = { nav.navigate(com.barathiraja.jk.ui.Routes.gymMember(s.uid)) }.takeIf { me?.role != com.barathiraja.jk.gym.Role.MEMBER })
                }
            }
            1 -> {
                item { Text("Trainers are ranked by how many of their members' workouts get done, not by how many they assign.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                if (trainers.isEmpty()) item { Text("No trainers yet.") }
                itemsIndexed(trainers, key = { _, t -> t.uid }) { i, t ->
                    val p = gvm.person(t.uid) ?: return@itemsIndexed
                    RankRow(i + 1, p.photoUrl, p.name, "${plural(t.members, "member")} · ${Math.round(t.rate * 100)}% completion", "${t.score}", "score",
                        highlight = t.uid == me?.uid,
                        onClick = { nav.navigate(com.barathiraja.jk.ui.Routes.gymTrainer(t.uid)) }.takeIf { me?.role != com.barathiraja.jk.gym.Role.MEMBER })
                }
            }
            else -> {
                if (given.isNotEmpty()) {
                    item { SectionTitle("From the owner") }
                    items(given, key = { "g" + it.id }) { a -> GivenAwardRow(a, gvm.person(a.uid)) }
                    if (awards.isNotEmpty()) item { SectionTitle("Monthly awards") }
                }
                if (awards.isEmpty() && given.isEmpty()) item {
                    JkCard(Modifier.fillMaxWidth()) {
                        Text("🏆 The first awards are announced on the 1st of next month.", style = MaterialTheme.typography.titleMedium)
                        Text("Best Member, Best Trainer, Most Consistent, Most Improved and Iron Lifter.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                awards.sortedByDescending { it.month }.forEach { m -> item(key = m.month) { AwardsCard(m, gvm) } }
            }
        }
    }
}

/**
 * One leaderboard line. The top three get a filled rank disc (yellow for first, ink for second and third);
 * your own line is tinted so you can find yourself at a glance.
 */
@Composable
private fun RankRow(rank: Int, photo: String?, name: String, sub: String, value: String, unit: String, highlight: Boolean, onClick: (() -> Unit)? = null) {
    val cs = MaterialTheme.colorScheme
    androidx.compose.material3.Surface(
        onClick = onClick ?: {}, enabled = onClick != null, modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        color = if (highlight) cs.secondaryContainer else cs.surfaceContainer, contentColor = cs.onSurface,
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            val (disc, ink) = when (rank) {
                1 -> com.barathiraja.jk.ui.theme.Yellow to androidx.compose.ui.graphics.Color.Black
                2, 3 -> cs.onSurface to cs.surface
                else -> androidx.compose.ui.graphics.Color.Transparent to cs.onSurfaceVariant
            }
            androidx.compose.foundation.layout.Box(
                Modifier.size(32.dp).clip(androidx.compose.foundation.shape.CircleShape).background(disc),
                contentAlignment = Alignment.Center,
            ) {
                Text("$rank", style = MaterialTheme.typography.titleMedium.copy(fontFamily = com.barathiraja.jk.ui.theme.CodeFont), color = ink)
            }
            Spacer(Modifier.width(10.dp))
            OwnerAvatar(photo, name, 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(if (highlight) "$name (you)" else name, style = MaterialTheme.typography.titleMedium, maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(sub, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 2)
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(value, style = MaterialTheme.typography.headlineSmall)
                Text(unit, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
            }
        }
    }
}

/** A white pill track with the chosen option filled in the primary colour; every option is a 48dp target. */
@Composable
internal fun PillSwitch(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(50)).background(cs.surfaceContainer).padding(4.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            androidx.compose.material3.Surface(
                onClick = { onSelect(i) },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).semantics { this.selected = on },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                color = if (on) cs.primary else androidx.compose.ui.graphics.Color.Transparent,
                contentColor = if (on) cs.onPrimary else cs.onSurface,
            ) {
                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                    Text(label, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                }
            }
        }
    }
}

/** One month's automatic awards: a black header with the month, then each winner with their medal. */
@Composable
fun AwardsCard(m: MonthAwards, gvm: GymViewModel) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(26.dp)).background(cs.surfaceContainer)) {
        Row(Modifier.fillMaxWidth().background(com.barathiraja.jk.ui.theme.HeroBlue).padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(monthLabel(m.month), style = plex(15.sp, androidx.compose.ui.text.font.FontWeight.SemiBold), color = androidx.compose.ui.graphics.Color.White)
            }
            OwnerChip("${m.winners.size} winners", Tone.TOP)
        }
        Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            Award.entries.forEach { a ->
                val p = gvm.person(m.winners[a]) ?: return@forEach
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    AwardBadge(a.look(), 46.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(a.label, style = plex(13.sp), color = cs.onSurfaceVariant)
                        Text(p.name, style = plex(15.sp, androidx.compose.ui.text.font.FontWeight.SemiBold), color = cs.onSurface, maxLines = 1)
                    }
                    OwnerAvatar(p.photoUrl, p.name, 38.dp)
                }
            }
        }
    }
}
