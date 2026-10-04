package com.barathiraja.jk.ui.gym

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.barathiraja.jk.gym.Assignment
import com.barathiraja.jk.gym.OwnerStats
import com.barathiraja.jk.gym.Person
import com.barathiraja.jk.gym.Role
import com.barathiraja.jk.gym.Scoring
import com.barathiraja.jk.ui.GymViewModel
import com.barathiraja.jk.ui.Routes
import com.barathiraja.jk.ui.theme.HeroBlue
import com.barathiraja.jk.ui.theme.Mustard

/*
 * One person's page (a trainer for the owner, a member for the owner or their trainer): a black top card with
 * their month at a glance, three number tiles, the last four weeks as bars, then their people or workouts.
 * Colours follow the app theme so trainers in dark mode get a dark page.
 */

/** Page shell: round back button with a small label, then the content as one scrolling list. */
@Composable
internal fun PersonPage(backLabel: String, onBack: () -> Unit, content: LazyListScope.() -> Unit) {
    val cs = MaterialTheme.colorScheme
    LazyColumn(
        Modifier.fillMaxSize().background(cs.background).windowInsetsPadding(WindowInsets.statusBars).imePadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp,
            bottom = 32.dp + com.barathiraja.jk.ui.components.LocalNavBarInset.current),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)) {
                Surface(onClick = onBack, shape = CircleShape, color = cs.surfaceContainer, contentColor = cs.onSurface, modifier = Modifier.size(52.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", Modifier.size(24.dp)) }
                }
                Spacer(Modifier.width(12.dp))
                Text(backLabel, style = plex(16.sp), color = cs.onSurfaceVariant)
            }
        }
        content()
    }
}

/**
 * The black top card: who, a status chip, a ring for the month and one plain sentence, with a strip at the
 * bottom. The corner cut-out holds [corner] (usually their rank).
 */
@Composable
internal fun PersonHero(
    p: Person, chip: Pair<String, Color>, fraction: Float?, line: String, strip: String, corner: String?,
) {
    val k = "hero-" + p.uid
    Box(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().clip(NotchedShape(radius = 30.dp)).background(HeroBlue).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(Modifier.padding(end = 60.dp), verticalAlignment = Alignment.CenterVertically) {
                OwnerAvatar(p.photoUrl, p.name, p.uid, 60.dp)
                Spacer(Modifier.width(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(p.name, style = plex(22.sp, FontWeight.SemiBold, line = 26.sp), color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    OwnerChip(chip.first, chip.second)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OwnerRing(fraction ?: 0f, 96.dp, 10.dp, Owner.DarkTrack, Mustard, k) {
                    Text(if (fraction == null) "–" else "${Math.round(fraction * 100)}%",
                        style = plex(if ((fraction ?: 0f) >= 1f) 21.sp else 25.sp, FontWeight.SemiBold, tracking = (-1).sp), color = Color.White, maxLines = 1)
                }
                Spacer(Modifier.width(16.dp))
                Text(line, style = plex(16.sp, line = 22.sp), color = Owner.OnDarkSoft)
            }
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Owner.DarkStrip).heightIn(min = 48.dp)
                .padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(strip, style = plex(15.sp, FontWeight.SemiBold), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (corner != null) Box(Modifier.align(Alignment.TopEnd).size(68.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(56.dp).clip(CircleShape).background(Mustard), contentAlignment = Alignment.Center) {
                Text(corner, style = plex(17.sp, FontWeight.SemiBold).copy(fontFamily = com.barathiraja.jk.ui.theme.CodeFont), color = Color.Black)
            }
        }
    }
}

/** One number tile: big value, a label and a short note. [dark] makes it the ink tile of the row. */
internal data class Tile(val value: String, val label: String, val note: String, val dark: Boolean = false)

@Composable
internal fun StatTiles(tiles: List<Tile>) {
    val cs = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        tiles.forEach { t ->
            Column(
                Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(22.dp)).background(if (t.dark) HeroBlue else cs.surfaceContainer)
                    .padding(14.dp),
            ) {
                Text(t.value, style = plex(26.sp, FontWeight.SemiBold, tracking = (-0.5).sp), color = if (t.dark) Color.White else cs.onSurface, maxLines = 1)
                Text(t.label, style = plex(14.sp, FontWeight.SemiBold), color = if (t.dark) Color.White else cs.onSurface,
                    modifier = Modifier.padding(top = 2.dp))
                Text(t.note, style = plex(12.sp, line = 16.sp), color = if (t.dark) Owner.OnDarkMuted else cs.onSurfaceVariant)
            }
        }
    }
}

/** Workouts finished (of those due) in each of the last four weeks, oldest first. */
internal fun lastWeeks(list: List<Assignment>, today: Long): List<Pair<Int, Int>> = (3 downTo 0).map { w ->
    val end = today - 7L * w
    val due = list.filter { it.epochDay in (end - 6)..end && (it.epochDay < today || it.done) }
    due.count { it.done } to due.size
}

/** Four bars, one per week, each filled to the share finished; this week in mustard. */
@Composable
internal fun WeeksCard(weeks: List<Pair<Int, Int>>) {
    val cs = MaterialTheme.colorScheme
    val labels = listOf("3 weeks ago", "2 weeks ago", "Last week", "This week")
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(cs.surfaceContainer).padding(18.dp)) {
        Row(Modifier.fillMaxWidth().height(150.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            weeks.forEachIndexed { i, (done, due) ->
                val f = if (due == 0) 0f else done / due.toFloat()
                val k = rememberFillIn("w$i-$done-$due", 200 + i * 80)
                Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (due == 0) "–" else "$done/$due", style = plex(14.sp, FontWeight.SemiBold), color = cs.onSurface)
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.weight(1f).width(40.dp).clip(RoundedCornerShape(14.dp)).background(cs.surfaceContainerHigh),
                        contentAlignment = Alignment.BottomCenter) {
                        Box(Modifier.fillMaxWidth().fillMaxHeight((f * k).coerceIn(0f, 1f)).clip(RoundedCornerShape(14.dp))
                            .background(if (i == weeks.lastIndex) Mustard else cs.onSurface))
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            labels.forEach { Text(it, Modifier.weight(1f), style = plex(12.sp, line = 15.sp), color = cs.onSurfaceVariant, textAlign = TextAlign.Center) }
        }
    }
}

/** Full-width ink pill with a mustard icon disc: the page's main action. */
@Composable
internal fun WideAction(text: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(50), color = HeroBlue, contentColor = Color.White,
        modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp)) {
        Row(Modifier.padding(start = 22.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, Modifier.weight(1f), style = plex(17.sp, FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Box(Modifier.size(42.dp).clip(CircleShape).background(Mustard), contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(20.dp), tint = Color.Black)
            }
        }
    }
}

@Composable
internal fun PageHeading(title: String, explain: String) {
    Column(Modifier.padding(start = 4.dp, end = 4.dp, top = 16.dp)) {
        Text(title, style = plex(21.sp, FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
        Text(explain, style = plex(16.sp, line = 23.sp), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
    }
}

/** One trainer for the owner: how their members are doing, week by week, and each member. */
@Composable
fun TrainerDetailScreen(uid: String, gvm: GymViewModel, nav: NavHostController) {
    val digest by gvm.ownerDigest.collectAsStateWithLifecycle()
    val people by gvm.people.collectAsStateWithLifecycle()
    val assignments by gvm.assignments.collectAsStateWithLifecycle()
    val scores by gvm.monthScores.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val d = digest
    val trainer = people.firstOrNull { it.uid == uid }
    if (d == null || trainer == null) {
        PersonPage("Back", { nav.popBackStack() }) { item { Text("This trainer isn't in the gym anymore.", style = plex(17.sp)) } }
        return
    }
    val row = d.trainers.firstOrNull { it.trainer.uid == uid }
    val ranked = d.trainers.filter { it.due > 0 }.sortedByDescending { it.rate }
    val rank = ranked.indexOfFirst { it.trainer.uid == uid } + 1
    val members = people.filter { it.role == Role.MEMBER && it.active && it.trainerUid == uid }
        .sortedByDescending { scores[it.uid]?.points ?: 0 }
    val idle = d.idle.associateBy { it.member.uid }
    val today = gvm.today
    val todays = assignments.filter { it.trainerUid == uid && it.epochDay == today }
    val mine = assignments.filter { it.trainerUid == uid }
    val pct = row?.let { Math.round(it.rate * 100) } ?: 0

    PersonPage("Back", { nav.popBackStack() }) {
        item {
            PersonHero(
                trainer,
                chip = row?.let { trainerStatus(it, rank == 1) } ?: ("No workouts yet" to Owner.Paper),
                fraction = row?.takeIf { it.due > 0 }?.rate,
                line = if (row == null || row.due == 0) "No workouts given to members yet this month"
                    else "Members finished $pct of every 100 workouts this month",
                strip = "Trains ${plural(members.size, "member")}" + if (row != null && row.idle > 0) " · ${row.idle} away" else "",
                corner = if (rank > 0) "#$rank" else null,
            )
        }
        item {
            StatTiles(listOf(
                Tile("${todays.count { it.done }}/${todays.size}", "Done today", "workouts finished", dark = true),
                Tile("${row?.idle ?: 0}", "Away", "no workout in ${OwnerStats.IDLE_DAYS} days"),
                Tile(if (rank > 0) "#$rank" else "–", "Rank", "of ${ranked.size} this month"),
            ))
        }
        item { PageHeading("Last 4 weeks", "How many of the workouts ${trainer.firstName} gave were finished each week.") }
        item { WeeksCard(lastWeeks(mine, today)) }
        item {
            Spacer(Modifier.height(4.dp))
            WideAction("Message ${trainer.firstName}", Icons.AutoMirrored.Outlined.Send) {
                context.whatsApp("Hi ${trainer.firstName}, ")
            }
        }
        item { PageHeading("${trainer.firstName}'s members", "Tap a member to see their workouts.") }
        if (members.isEmpty()) item { Text("No members yet.", style = plex(17.sp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(members, key = { it.uid }) { m ->
            MemberCard(m, scores[m.uid] ?: Scoring.MemberScore(m.uid, 0, 0, 0, 0, 0.0), top = false, idle[m.uid], trainer) {
                nav.navigate(Routes.gymMember(m.uid))
            }
        }
    }
}

/** Top of a member's page: the black card, number tiles and their last four weeks. */
internal fun LazyListScope.memberOverview(
    member: Person, s: Scoring.MemberScore?, rank: Int, idle: OwnerStats.Idle?, trainer: Person?, list: List<Assignment>, today: Long,
) {
    val lastDone = list.filter { it.done && it.epochDay <= today }.maxOfOrNull { it.epochDay }
    val since = lastDone?.let { (today - it).toInt() }
    item {
        PersonHero(
            member,
            chip = memberStatus(s, rank == 1 && (s?.points ?: 0) > 0, idle, trainer != null),
            fraction = s?.takeIf { it.due > 0 }?.rate,
            line = if (s == null || s.due == 0) "No workouts given yet this month"
                else "Finished ${s.completed} of ${s.due} workouts this month",
            strip = if (trainer != null) "Trainer · ${trainer.name}" else "No trainer yet",
            corner = if (rank > 0 && (s?.points ?: 0) > 0) "#$rank" else null,
        )
    }
    item {
        StatTiles(listOf(
            Tile("${s?.points ?: 0}", "Points", if (rank > 0 && (s?.points ?: 0) > 0) "#$rank in the gym" else "this month", dark = true),
            when (since) {
                null -> Tile("–", "Last workout", "none yet")
                0 -> Tile("Today", "Last workout", "finished")
                1 -> Tile("1", "Day ago", "last workout")
                else -> Tile("$since", "Days ago", "last workout")
            },
            Tile("%,d".format((s?.volumeKg ?: 0.0).toLong()), "Kg lifted", "this month"),
        ))
    }
    item { PageHeading("Last 4 weeks", "How many of ${member.firstName}'s workouts were finished each week.") }
    item { WeeksCard(lastWeeks(list, today)) }
}
