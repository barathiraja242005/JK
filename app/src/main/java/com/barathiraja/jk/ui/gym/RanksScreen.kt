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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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

/** Live leaderboards for this month plus the wall of past monthly awards. */
@Composable
fun RanksScreen(gvm: GymViewModel, nav: NavHostController) {
    val members by gvm.memberRanking.collectAsStateWithLifecycle()
    val trainers by gvm.trainerRanking.collectAsStateWithLifecycle()
    val awards by gvm.awards.collectAsStateWithLifecycle()
    val given by gvm.givenAwards.collectAsStateWithLifecycle()
    val me by gvm.me.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val month = YearMonth.now()

    TabScreen("Leaderboard", subtitle = month.format(monthFmt)) {
        item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf("Members", "Trainers", "Awards").forEachIndexed { i, l ->
                    SegmentedButton(tab == i, { tab = i }, SegmentedButtonDefaults.itemShape(i, 3)) { Text(l) }
                }
            }
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
                        highlight = t.uid == me?.uid)
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

@Composable
private fun RankRow(rank: Int, photo: String?, name: String, sub: String, value: String, unit: String, highlight: Boolean, onClick: (() -> Unit)? = null) {
    val medal = when (rank) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> "$rank" }
    JkCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(medal, Modifier.width(36.dp), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.width(8.dp))
            Avatar(photo, name, 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(if (highlight) "$name (you)" else name, style = MaterialTheme.typography.titleMedium,
                    color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, maxLines = 1)
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(value, style = MaterialTheme.typography.titleLarge)
                Text(unit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun AwardsCard(m: MonthAwards, gvm: GymViewModel) {
    JkCard(Modifier.fillMaxWidth()) {
        SectionTitle("🏆 ${monthLabel(m.month)}")
        Award.entries.forEach { a ->
            val p = gvm.person(m.winners[a]) ?: return@forEach
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(a.emoji, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(a.label, style = MaterialTheme.typography.labelLarge, color = Sun)
                    Text(p.name, style = MaterialTheme.typography.titleMedium)
                }
                Avatar(p.photoUrl, p.name, 36.dp)
            }
        }
    }
}
