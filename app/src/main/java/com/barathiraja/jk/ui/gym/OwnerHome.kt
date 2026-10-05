package com.barathiraja.jk.ui.gym

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.NorthEast
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role as A11yRole
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.barathiraja.jk.ui.switchTab
import com.barathiraja.jk.ui.components.shareText
import com.barathiraja.jk.ui.theme.CodeFont

/**
 * Owner's home, written for an older owner: today's turnout, then the people who need them as a swipeable row,
 * the month's best trainer and member (each opens the full list), and the gym code. Big text, 48dp+ targets.
 */
@Composable
fun OwnerHomeScreen(gvm: GymViewModel, nav: NavHostController) {
    val gym by gvm.gym.collectAsStateWithLifecycle()
    val people by gvm.people.collectAsStateWithLifecycle()
    val digest by gvm.ownerDigest.collectAsStateWithLifecycle()
    val ranking by gvm.memberRanking.collectAsStateWithLifecycle()
    val demoOn = gvm.demo.collectAsStateWithLifecycle().value != null
    val me by gvm.me.collectAsStateWithLifecycle()
    val givenCount = gvm.givenAwards.collectAsStateWithLifecycle().value.size
    val context = LocalContext.current
    val d = digest
    val pending = people.filter { it.role == Role.TRAINER && it.status == PersonStatus.PENDING }
    var rejecting by remember { mutableStateOf<Person?>(null) }
    val gymName = gym?.name ?: "the gym"
    val invite = gym?.let { g -> "Join ${g.name} as a trainer on the JK app. Open JK → Sign in → I'm a trainer → enter code ${g.gymCode}" }

    LazyColumn(
        Modifier.fillMaxSize().background(Owner.Paper).windowInsetsPadding(WindowInsets.statusBars),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp,
            bottom = 32.dp + com.barathiraja.jk.ui.components.LocalNavBarInset.current),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)) {
                Box(Modifier.clip(CircleShape).clickable { nav.navigate(Routes.PROFILE) }) {
                    OwnerAvatar(me?.photoUrl, me?.name ?: "Owner", me?.uid ?: "owner", 52.dp, Owner.Lavender)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("${greeting()},", style = plex(16.sp), color = Owner.Muted)
                    Text(me?.firstName ?: "Owner", style = plex(22.sp, FontWeight.SemiBold), color = Owner.Ink)
                }
            }
        }
        if (demoOn) item {
            Row(Modifier.fillMaxWidth().padding(bottom = 16.dp).clip(RoundedCornerShape(18.dp)).background(Owner.Butter)
                .padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Sample gym, not your real data", Modifier.weight(1f), style = plex(16.sp), color = Owner.Black)
                TextButton(onClick = { gvm.setDemo(false) }, Modifier.heightIn(min = 48.dp)) {
                    Text("Turn off", style = plex(16.sp, FontWeight.SemiBold), color = Owner.Black)
                }
            }
        }
        if (d == null) return@LazyColumn

        item { TurnoutCard(d) }

        // Members whose trainer left: the owner picks a new one. They are left out of "Away" so nobody shows twice.
        val orphans = people.filter { it.role == Role.MEMBER && it.active && gvm.trainerOf(it) == null }
        val needs = pending.map { Need.Joining(it) } + orphans.map { Need.NoTrainer(it) } +
            d.idle.filter { i -> orphans.none { it.uid == i.member.uid } }.map { Need.Away(it) }
        // Always shown, so the home keeps its shape; an empty tab shows an "all clear" card instead.
        item { Heading("Needs you", "People to say yes to, or to check on. Swipe to see them all.") }
        item {
            NeedsPanel(needs, gymName, gvm, nav, context, onReject = { rejecting = it })
        }

        val topTrainer = d.trainers.filter { it.due > 0 }.maxByOrNull { it.rate }
        val topMember = ranking.firstOrNull { it.points > 0 }?.let { s -> gvm.person(s.uid)?.let { it to s } }
        if (topTrainer != null || topMember != null) {
            item { Heading("Best this month", if (topTrainer != null && topMember != null) "Swipe to see the top member and the top trainer. Tap a card to see them."
                else "The best this month so far. Tap the card to see them.") }
            item {
                val members = people.count { it.role == Role.MEMBER && it.active }
                val single = topTrainer == null || topMember == null
                CardRow(carouselCardWidth(inPanel = false, single = single), 16.dp, Modifier.offsetEdges(16.dp)) {
                    topMember?.let { (p, s) ->
                        TopMemberCard(p, s, gvm.trainerOf(p), members, onOpen = { nav.navigate(Routes.gymMember(p.uid)) },
                            onSeeAll = { nav.navigate(Routes.people(PEOPLE_MEMBERS)) })
                    }
                    topTrainer?.let { t ->
                        TopTrainerCard(t, d.trainers.size, onOpen = { nav.navigate(Routes.gymTrainer(t.trainer.uid)) },
                            onSeeAll = { nav.navigate(Routes.people(PEOPLE_TRAINERS)) })
                    }
                }
            }
        }

        item { Heading("Awards", "Reward your best members and trainers. Everyone in the gym sees them on the Ranks tab.") }
        item { InkButton("Give an award", { nav.navigate(Routes.GIVE_AWARD) }, Icons.Outlined.EmojiEvents, Owner.Mustard, Modifier.fillMaxWidth()) }
        if (givenCount > 0) item {
            InkButton("See or change awards ($givenCount)", { gvm.ranksTab.value = RANKS_AWARDS; nav.switchTab(Routes.RANKS) },
                Icons.AutoMirrored.Outlined.ArrowForward, Owner.Mustard, Modifier.fillMaxWidth().padding(top = 10.dp), light = true)
        }

        gym?.let { g ->
            item { Heading("Gym code", "New trainers join your gym with this code.") }
            item { GymCodeCard(g.gymCode, context, gvm) { invite?.let { context.shareText(it) } } }
        }
    }

    rejecting?.let { p ->
        AlertDialog(onDismissRequest = { rejecting = null },
            title = { Text("Reject ${p.firstName}?") },
            text = { Text("${p.firstName} won't join as a trainer. They can ask again with your gym code.", style = plex(17.sp, line = 25.sp)) },
            confirmButton = { TextButton(onClick = { rejecting = null; gvm.reject(p) }) { Text("Reject ${p.firstName}", style = plex(16.sp, FontWeight.SemiBold)) } },
            dismissButton = { TextButton(onClick = { rejecting = null }) { Text("Keep waiting", style = plex(16.sp, FontWeight.SemiBold)) } })
    }
}

internal const val PEOPLE_TRAINERS = "trainers"
internal const val PEOPLE_MEMBERS = "members"

private fun greeting(): String = when (java.time.LocalTime.now().hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}

/**
 * A swipeable row of fixed-width cards that are all as tall as the tallest one, so nothing changes size while
 * scrolling. Flings snap so a card's left edge lines up with the row's start. Cards fill the row's height and
 * pin their bottom action with a weighted spacer.
 */
@Composable
private fun CardRow(cardWidth: Dp, edge: Dp, modifier: Modifier = Modifier, key: Any? = null, content: @Composable () -> Unit) {
    val scroll = remember(key) { androidx.compose.foundation.ScrollState(0) }
    val gap = 10.dp
    val step = with(androidx.compose.ui.platform.LocalDensity.current) { (cardWidth + gap).toPx() }
    val snap = remember(scroll, step) {
        object : androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider {
            override fun calculateSnapOffset(velocity: Float): Float {
                val now = scroll.value.toFloat()
                val i = now / step
                val target = when {
                    velocity > 0f -> kotlin.math.ceil(i)
                    velocity < 0f -> kotlin.math.floor(i)
                    else -> kotlin.math.round(i)
                } * step
                return target.coerceIn(0f, scroll.maxValue.toFloat()) - now
            }
        }
    }
    Row(
        modifier.fillMaxWidth()
            .horizontalScroll(scroll, flingBehavior = rememberSnapFlingBehavior(snap))
            .height(androidx.compose.foundation.layout.IntrinsicSize.Max)
            .padding(horizontal = edge),
        horizontalArrangement = Arrangement.spacedBy(gap),
    ) { androidx.compose.runtime.CompositionLocalProvider(LocalCardWidth provides cardWidth, content = content) }
}

/** Lets a horizontal row run edge to edge inside the list's 16dp side padding. */
private fun Modifier.offsetEdges(edge: Dp) = this.layout { measurable, constraints ->
    val extra = edge.roundToPx() * 2
    val p = measurable.measure(constraints.copy(maxWidth = constraints.maxWidth + extra, minWidth = constraints.minWidth + extra))
    layout(p.width - extra, p.height) { p.place(-extra / 2, 0) }
}

/** Section title with one plain sentence under it. */
@Composable
private fun Heading(title: String, explain: String) {
    Column(Modifier.padding(start = 4.dp, end = 4.dp, top = 24.dp, bottom = 14.dp)) {
        Text(title, style = plex(21.sp, FontWeight.SemiBold), color = Owner.Ink)
        Text(explain, style = plex(16.sp, line = 23.sp), color = Owner.Muted, modifier = Modifier.padding(top = 4.dp))
    }
}

/** Black pill button with a coloured icon tile on its end. */
@Composable
private fun InkButton(text: String, onClick: () -> Unit, icon: ImageVector, tile: Color, modifier: Modifier = Modifier, light: Boolean = false) {
    Surface(onClick = onClick, modifier = modifier.heightIn(min = 56.dp), shape = RoundedCornerShape(18.dp),
        color = if (light) Owner.Card else Owner.Ink, contentColor = if (light) Owner.Ink else Owner.OnInk) {
        Row(Modifier.padding(start = 18.dp, end = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, Modifier.weight(1f), style = plex(16.sp, FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(tile), contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(19.dp), tint = Owner.Black)
            }
        }
    }
}

/** Today's turnout: a white card with a black badge sitting on its top edge. */
@Composable
private fun TurnoutCard(d: OwnerStats.Digest) {
    Box(Modifier.fillMaxWidth().padding(top = 26.dp)) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Owner.Card).padding(top = 38.dp, bottom = 20.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f).padding(horizontal = 20.dp)) {
                    Text("Trained today", style = plex(16.sp), color = Owner.Muted)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("${d.trainedToday}", style = plex(36.sp, FontWeight.SemiBold, tracking = (-1).sp), color = Owner.Ink)
                        Text(" / ${d.members}", style = plex(24.sp, FontWeight.SemiBold), color = Owner.Faint, modifier = Modifier.padding(bottom = 4.dp))
                    }
                }
                Box(Modifier.width(1.dp).height(72.dp).background(Owner.Line))
                Column(Modifier.weight(1f).padding(horizontal = 20.dp)) {
                    Text("Training now", style = plex(16.sp), color = Owner.Muted)
                    Text("${d.inProgressToday}", style = plex(36.sp, FontWeight.SemiBold, tracking = (-1).sp), color = Owner.Ink)
                }
            }
            Box(Modifier.padding(horizontal = 20.dp, vertical = 14.dp).fillMaxWidth().height(1.dp).background(Owner.Line))
            val diff = d.trainedToday - d.trainedYesterday
            val (lead, rest) = when {
                d.members == 0 -> "" to "Members join through their trainer."
                diff > 0 -> "$diff more" to " than yesterday"
                diff < 0 -> "${-diff} fewer" to " than yesterday so far"
                else -> "Same" to " as yesterday"
            }
            Text(androidx.compose.ui.text.buildAnnotatedString {
                pushStyle(androidx.compose.ui.text.SpanStyle(color = Owner.Ink, fontWeight = FontWeight.SemiBold)); append(lead); pop()
                append(rest)
            }, style = plex(16.sp), color = Owner.Muted, modifier = Modifier.padding(horizontal = 20.dp))
        }
        Box(
            Modifier.align(Alignment.TopCenter).offset(y = (-26).dp).size(60.dp).clip(CircleShape).background(Owner.Paper).padding(6.dp)
                .clip(CircleShape).background(Owner.Ink),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Outlined.FitnessCenter, null, Modifier.size(22.dp), tint = Owner.OnInk) }
    }
}

// ---------- Needs you ----------

private sealed interface Need {
    val key: String
    data class Joining(val p: Person) : Need { override val key get() = "j" + p.uid }
    data class Away(val i: OwnerStats.Idle) : Need { override val key get() = "a" + i.member.uid }
    data class NoTrainer(val p: Person) : Need { override val key get() = "n" + p.uid }
}

/** Members with no trainer count under "Away": they won't get workouts until the owner acts. */
private enum class NeedTab(val label: String) { ALL("All"), JOINING("Joining"), AWAY("Away") }

/** Folder tabs over a mustard panel; the panel holds the people as a swipeable row of cards. */
@Composable
private fun NeedsPanel(
    needs: List<Need>, gymName: String, gvm: GymViewModel, nav: NavHostController, context: Context, onReject: (Person) -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(NeedTab.ALL) }
    val counts = mapOf(
        NeedTab.ALL to needs.size,
        NeedTab.JOINING to needs.count { it is Need.Joining },
        NeedTab.AWAY to needs.count { it !is Need.Joining },
    )
    val shown = when (tab) {
        NeedTab.ALL -> needs
        NeedTab.JOINING -> needs.filterIsInstance<Need.Joining>()
        NeedTab.AWAY -> needs.filter { it !is Need.Joining }
    }
    Column {
        // Neighbouring tabs overlap by one flare width: a flare only shows on the selected tab.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(-TAB_FLARE)) {
            val tabs = NeedTab.entries
            tabs.forEachIndexed { i, t ->
                val first = i == 0; val last = i == tabs.lastIndex
                // Each tab gets the width its flares, count and label need; text shrinks a little on narrow phones.
                val need = (if (first) 0f else TAB_FLARE.value) + 50f + t.label.length * 10.5f + (if (last) 0f else TAB_FLARE.value)
                FolderTab(t.label, counts[t] ?: 0, selected = tab == t, first = first, last = last, Modifier.weight(need)) { tab = t }
            }
        }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = if (tab == NeedTab.entries.first()) 0.dp else 32.dp,
                topEnd = if (tab == NeedTab.entries.last()) 0.dp else 32.dp,
                bottomStart = 32.dp, bottomEnd = 32.dp)).background(Owner.Mustard).padding(vertical = 10.dp),
        ) {
            CardRow(carouselCardWidth(inPanel = true, single = shown.size <= 1), 10.dp, key = tab) {
                if (shown.isEmpty()) EmptyNeedCard(tab) { nav.navigate(Routes.people(if (tab == NeedTab.JOINING) PEOPLE_TRAINERS else PEOPLE_MEMBERS)) }
                shown.forEach { n ->
                        androidx.compose.runtime.key(n.key) {
                            when (n) {
                                is Need.Joining -> JoiningCard(n.p, gymName, onApprove = { gvm.approve(n.p) }, onReject = { onReject(n.p) })
                                is Need.Away -> AwayCard(n.i, gvm, gymName, nav, context)
                                is Need.NoTrainer -> NoTrainerCard(n.p, gvm, nav)
                            }
                        }
                    }
            }
            // The hint keeps its space when hidden, so the panel is the same height on every tab.
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 2.dp).alpha(if (shown.size > 1) 1f else 0f),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Swipe for more", style = plex(14.sp, FontWeight.SemiBold), color = Owner.Warm)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(16.dp), tint = Owner.Warm)
            }
        }
    }
}

private val TAB_FLARE = 16.dp

/** A tab with flared feet when selected, so it reads as part of the panel below it. */
@Composable
private fun FolderTab(label: String, count: Int, selected: Boolean, first: Boolean, last: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val flare = TAB_FLARE
    val ink = if (selected) Owner.Black else Owner.Muted
    Box(
        modifier
            .then(if (selected) Modifier.background(Owner.Mustard, FolderTabShape(flare = flare, left = !first, right = !last)) else Modifier)
            .padding(start = if (first) 0.dp else flare, end = if (last) 0.dp else flare)
            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .semantics { this.selected = selected }
            .clickable(role = A11yRole.Tab, onClick = onClick)
            .height(56.dp)
            .padding(start = 6.dp, end = 8.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(30.dp).border(1.5.dp, ink, CircleShape), contentAlignment = Alignment.Center) {
                Text("$count", style = plex(14.sp, FontWeight.SemiBold), color = ink, maxLines = 1, softWrap = false)
            }
            Spacer(Modifier.width(6.dp))
            androidx.compose.foundation.text.BasicText(
                label, style = plex(16.sp, FontWeight.SemiBold).copy(color = ink), maxLines = 1, softWrap = false,
                autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 16.sp),
            )
        }
    }
}

/** Card shell for the "Needs you" row: notched cream card, chip and name on top, round button in the notch. */
@Composable
private fun NeedCard(
    chip: String, chipFill: Color, name: String, notchIcon: ImageVector, notchLabel: String, onNotch: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(Modifier.width(LocalCardWidth.current).fillMaxHeight()) {
        Column(Modifier.fillMaxSize().clip(NotchedShape()).background(Owner.CardCream).padding(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.padding(end = 60.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OwnerChip(chip, chipFill)
                Text(name, style = plex(22.sp, FontWeight.SemiBold, line = 26.sp), color = Owner.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            content()
        }
        NotchButton(notchIcon, notchLabel, Owner.Ink, Owner.OnInk, onNotch, Modifier.align(Alignment.TopEnd))
    }
}

/** The round button that sits in a card's corner cut-out. */
@Composable
private fun NotchButton(icon: ImageVector, label: String, fill: Color, ink: Color, onClick: () -> Unit, modifier: Modifier = Modifier, notch: Dp = 68.dp) {
    Box(modifier.size(notch), contentAlignment = Alignment.Center) {
        Surface(onClick = onClick, shape = CircleShape, color = fill, contentColor = ink, modifier = Modifier.size(notch - 12.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = label, Modifier.size(24.dp)) }
        }
    }
}

/** Three columns on white: who → what → who. */
@Composable
private fun Triad(left: @Composable () -> Unit, middle: @Composable () -> Unit, right: @Composable () -> Unit) {
    // One height for every card's panel, so the cards in a row match without empty gaps.
    Row(Modifier.fillMaxWidth().heightIn(min = 128.dp).clip(RoundedCornerShape(22.dp)).background(Owner.Well).padding(vertical = 14.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { left() }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { middle() }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { right() }
    }
}

@Composable
private fun TriadPerson(photoUrl: String?, name: String, key: String, role: String, fill: Color = Owner.pastel(key)) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OwnerAvatar(photoUrl, name, key, 48.dp, fill)
        Text(name.substringBefore(' '), style = plex(15.sp, FontWeight.SemiBold), color = Owner.Ink, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
        Text(role, style = plex(13.sp), color = Owner.Muted, maxLines = 1)
    }
}

@Composable
private fun TriadNumber(top: String, number: String, bottom: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(top, style = plex(13.sp), color = Owner.Muted, textAlign = TextAlign.Center)
        Text(number, style = plex(38.sp, FontWeight.SemiBold, line = 42.sp, tracking = (-1).sp), color = Owner.Ink)
        Text(bottom, style = plex(13.sp), color = Owner.Muted, textAlign = TextAlign.Center)
    }
}

/** Takes the card's spare height and keeps [content] at its bottom, so actions line up across equal-height cards. */
@Composable
private fun ColumnScope.BottomPinned(content: @Composable () -> Unit) {
    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomStart) { content() }
}

/** Full-width black pill with a mustard arrow disc. */
@Composable
private fun PillAction(text: String, onClick: () -> Unit, icon: ImageVector = Icons.AutoMirrored.Outlined.ArrowForward) {
    Surface(onClick = onClick, shape = RoundedCornerShape(50), color = Owner.Ink, contentColor = Owner.OnInk,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
        Row(Modifier.padding(start = 22.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, Modifier.weight(1f), style = plex(16.sp, FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Box(Modifier.size(40.dp).clip(CircleShape).background(Owner.Mustard), contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(19.dp), tint = Owner.Black)
            }
        }
    }
}

/**
 * Stand-in for an empty tab: the same shell and size as a person card, saying what would show up here and
 * offering the next useful step. (Sharing the gym code lives in its own section below, not here.)
 */
@Composable
private fun EmptyNeedCard(tab: NeedTab, onSeeAll: () -> Unit) {
    val (chip, title, line) = when (tab) {
        NeedTab.ALL -> Triple("All clear", "All good", "No trainer is waiting and every member trained this week.")
        NeedTab.JOINING -> Triple("No requests", "None waiting", "Trainers who ask to join with your gym code show up here.")
        NeedTab.AWAY -> Triple("All active", "All training", "Members who skip workouts for ${OwnerStats.IDLE_DAYS} days show up here.")
    }
    val label = if (tab == NeedTab.JOINING) "See all trainers" else "See all members"
    NeedCard(chip, Owner.Mint, title, Icons.Outlined.NorthEast, label, onSeeAll) {
        Row(Modifier.fillMaxWidth().heightIn(min = 128.dp).clip(RoundedCornerShape(22.dp)).background(Owner.Well).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).clip(CircleShape).background(Owner.Mint).border(1.5.dp, Owner.Black, CircleShape),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Check, null, Modifier.size(26.dp), tint = Owner.Black)
            }
            Spacer(Modifier.width(14.dp))
            Text(line, style = plex(15.sp, line = 21.sp), color = Owner.Muted)
        }
        BottomPinned { PillAction(label, onSeeAll) }
    }
}

@Composable
private fun JoiningCard(p: Person, gymName: String, onApprove: () -> Unit, onReject: () -> Unit) {
    NeedCard("New trainer", Owner.Lavender, p.name, Icons.Outlined.Close, "Reject ${p.firstName}", onReject) {
        Triad(
            { TriadPerson(p.photoUrl, p.name, p.uid, "Trainer") },
            {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Wants to", style = plex(13.sp), color = Owner.Muted)
                    Box(Modifier.padding(vertical = 6.dp).size(44.dp).clip(CircleShape).background(Owner.Ink), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Add, null, Modifier.size(22.dp), tint = Owner.OnInk)
                    }
                    Text("join", style = plex(13.sp), color = Owner.Muted)
                }
            },
            { TriadPerson(null, gymName, "gym", "Your gym", Owner.Mustard) },
        )
        BottomPinned { PillAction("Approve ${p.firstName}", onApprove, Icons.Outlined.Check) }
    }
}

@Composable
private fun AwayCard(i: OwnerStats.Idle, gvm: GymViewModel, gymName: String, nav: NavHostController, context: Context) {
    val m = i.member
    val trainer = gvm.trainerOf(m)
    val noPlan = !i.assignedRecently && trainer != null
    val days = if (i.days > 30) "30+" else "${i.days}"
    val open = { nav.navigate(Routes.gymMember(m.uid)) }
    NeedCard(if (noPlan) "No plan" else "Away", if (noPlan) Owner.Butter else Owner.Coral, m.name,
        Icons.Outlined.NorthEast, "Open ${m.firstName}", open) {
        Triad(
            { TriadPerson(m.photoUrl, m.name, m.uid, "Member") },
            { if (noPlan) TriadNumber("No workout", days, "days") else TriadNumber("Last workout", days, "days ago") },
            { if (trainer != null) TriadPerson(trainer.photoUrl, trainer.name, trainer.uid, "Trainer") else TriadPerson(null, "No trainer", "none", "—") },
        )
        BottomPinned {
            if (noPlan) PillAction("Tell ${trainer!!.firstName}", {
                context.whatsApp("Hi ${trainer.firstName}, ${m.name} hasn't had a workout for ${i.days} days. Please assign one in the JK app and check in with them. Thanks!")
            }, Icons.AutoMirrored.Outlined.Send)
            else PillAction("Message ${m.firstName}", {
                context.whatsApp("Hi ${m.firstName}, we haven't seen you at $gymName for a while. Your trainer has a workout ready for you in the JK app. See you soon! 💪")
            }, Icons.AutoMirrored.Outlined.Send)
        }
    }
}

/** A member whose trainer left the gym: pick who trains them now. */
@Composable
private fun NoTrainerCard(m: Person, gvm: GymViewModel, nav: NavHostController) {
    var choosing by remember { mutableStateOf(false) }
    NeedCard("No trainer", Owner.Coral, m.name, Icons.Outlined.NorthEast, "Open ${m.firstName}", { nav.navigate(Routes.gymMember(m.uid)) }) {
        Triad(
            { TriadPerson(m.photoUrl, m.name, m.uid, "Member") },
            {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Needs a", style = plex(13.sp), color = Owner.Muted)
                    Box(Modifier.padding(vertical = 6.dp).size(44.dp).clip(CircleShape).background(Owner.Ink), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Add, null, Modifier.size(22.dp), tint = Owner.OnInk)
                    }
                    Text("trainer", style = plex(13.sp), color = Owner.Muted)
                }
            },
            { TriadPerson(null, "?", "none", "Trainer", Owner.Stone) },
        )
        BottomPinned { PillAction("Choose a trainer", { choosing = true }, Icons.Outlined.Add) }
    }
    if (choosing) ChangeTrainerDialog(m, gvm) { choosing = false }
}

// ---------- Best this month ----------

/**
 * Carousel cards fill the screen minus a peek of the next card, capped for big phones. [inPanel] cards sit inside
 * the mustard panel's 10dp padding; the others run edge to edge.
 */
@Composable
private fun carouselCardWidth(inPanel: Boolean, single: Boolean): Dp {
    val screen = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp
    val used = if (inPanel) 32.dp + 20.dp else 32.dp
    // A lone card has nothing to peek at, so it takes the full row.
    return if (single) screen - used else (screen - used - 36.dp).coerceIn(260.dp, 340.dp)
}

/** The width [CardRow] gives each of its cards. */
private val LocalCardWidth = androidx.compose.runtime.compositionLocalOf { 300.dp }

@Composable
private fun TopTrainerCard(t: OwnerStats.TrainerRow, trainers: Int, onOpen: () -> Unit, onSeeAll: () -> Unit) {
    val pct = Math.round(t.rate * 100)
    Box(Modifier.width(LocalCardWidth.current).fillMaxHeight()) {
        Column(
            Modifier.fillMaxSize().clip(NotchedShape(radius = 30.dp)).background(Owner.Hero).clickable(onClickLabel = "Open ${t.trainer.firstName}", onClick = onOpen)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Badge("Top trainer", Icons.Outlined.EmojiEvents, Owner.Mustard, Owner.Black)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OwnerRing(t.rate, 100.dp, 10.dp, Owner.DarkTrack, Owner.Mustard, "top-" + t.trainer.uid) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$pct%", style = plex(if (pct >= 100) 22.sp else 26.sp, FontWeight.SemiBold, tracking = (-1).sp), color = Color.White, maxLines = 1)
                        Text("finished", style = plex(12.sp), color = Owner.OnDarkMuted)
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(t.trainer.name, style = plex(22.sp, FontWeight.SemiBold, line = 25.sp), color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("Members finished ${t.done} of ${plural(t.due, "workout")} this month", style = plex(14.sp, line = 19.sp), color = Owner.OnDarkSoft,
                        modifier = Modifier.padding(top = 4.dp))
                }
            }
            BottomPinned {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Owner.DarkStrip)
                    .clickable(onClick = onSeeAll).heightIn(min = 52.dp).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("See all trainers ($trainers)", Modifier.weight(1f), style = plex(16.sp, FontWeight.SemiBold), color = Color.White)
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(18.dp), tint = Color.White)
                }
            }
        }
        NotchCorner(Owner.Mustard, Owner.Black, Modifier.align(Alignment.TopEnd))
    }
}

@Composable
private fun TopMemberCard(p: Person, s: Scoring.MemberScore, trainer: Person?, members: Int, onOpen: () -> Unit, onSeeAll: () -> Unit) {
    Box(Modifier.width(LocalCardWidth.current).fillMaxHeight()) {
        Column(
            Modifier.fillMaxSize().clip(NotchedShape(radius = 30.dp)).background(Owner.Mustard).clickable(onClickLabel = "Open ${p.firstName}", onClick = onOpen)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Badge("Top member", Icons.Outlined.Star, Owner.Black, Color.White)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OwnerAvatar(p.photoUrl, p.name, p.uid, 64.dp, Owner.Lavender, ring = Owner.Black)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(p.name, style = plex(22.sp, FontWeight.SemiBold, line = 25.sp), color = Owner.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("Finished ${s.completed} of ${s.due} workouts", style = plex(15.sp), color = Owner.Warm)
                    if (trainer != null) Text("Trainer: ${trainer.name}", style = plex(14.sp), color = Owner.Warm, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                }
            }
            OwnerBar(s.rate, Owner.Cream, Owner.Black, "top-" + p.uid, 10.dp)
            BottomPinned {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Owner.Cream)
                    .clickable(onClick = onSeeAll).heightIn(min = 52.dp).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("See all members ($members)", Modifier.weight(1f), style = plex(16.sp, FontWeight.SemiBold), color = Owner.Black)
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(18.dp), tint = Owner.Black)
                }
            }
        }
        NotchCorner(Owner.Ink, Owner.OnInk, Modifier.align(Alignment.TopEnd))
    }
}

/** Decorative arrow disc in a card's cut-out when the whole card is the button. */
@Composable
private fun NotchCorner(fill: Color, ink: Color, modifier: Modifier, notch: Dp = 68.dp) {
    Box(modifier.size(notch), contentAlignment = Alignment.Center) {
        Box(Modifier.size(notch - 12.dp).clip(CircleShape).background(fill), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.NorthEast, null, Modifier.size(24.dp), tint = ink)
        }
    }
}

@Composable
private fun Badge(text: String, icon: ImageVector, fill: Color, ink: Color) {
    Row(Modifier.clip(RoundedCornerShape(50)).background(fill).padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(16.dp), tint = ink)
        Spacer(Modifier.width(6.dp))
        Text(text, style = plex(14.sp, FontWeight.SemiBold), color = ink)
    }
}

// ---------- Gym code ----------

@Composable
private fun GymCodeCard(code: String, context: Context, gvm: GymViewModel, onShare: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Owner.Card).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Gym code for new trainers", style = plex(16.sp), color = Owner.Muted)
            Text(code, style = plex(40.sp, FontWeight.SemiBold, tracking = 8.sp).copy(fontFamily = CodeFont), color = Owner.Ink,
                modifier = Modifier.padding(top = 6.dp, bottom = 16.dp))
            InkButton("Share code", onShare, Icons.Outlined.Share, Owner.Mustard, Modifier.fillMaxWidth())
        }
        // A tab hanging from the card: the folder-tab shape, flipped so its flares meet the card above.
        Box(Modifier.width(272.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.matchParentSize().graphicsLayer { scaleY = -1f }.background(Owner.Mustard, FolderTabShape(radius = 24.dp)))
            Row(
                Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                    .clickable(onClickLabel = "Copy gym code") {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("Gym code", code))
                        gvm.message.value = "Gym code copied"
                    }.heightIn(min = 56.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.ContentCopy, null, Modifier.size(20.dp), tint = Owner.Black)
                Spacer(Modifier.width(8.dp))
                Text("Copy code", style = plex(16.sp, FontWeight.SemiBold), color = Owner.Black)
            }
        }
    }
}

// ---------- Trainers / Members page ----------

/**
 * Everyone in the gym, one page with a Trainers/Members switch. Opened from the home's "Best this month" cards;
 * [start] picks the side it opens on.
 */
@Composable
fun OwnerPeopleScreen(start: String, gvm: GymViewModel, nav: NavHostController) {
    val digest by gvm.ownerDigest.collectAsStateWithLifecycle()
    val people by gvm.people.collectAsStateWithLifecycle()
    val ranking by gvm.memberRanking.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var tab by rememberSaveable { mutableStateOf(if (start == PEOPLE_MEMBERS) PEOPLE_MEMBERS else PEOPLE_TRAINERS) }
    val d = digest ?: return
    val trainers = d.trainers.sortedWith(compareByDescending<OwnerStats.TrainerRow> { it.due > 0 }.thenByDescending { it.rate })
    val idle = d.idle.associateBy { it.member.uid }
    val members = ranking.mapNotNull { s -> people.firstOrNull { it.uid == s.uid }?.let { it to s } }

    LazyColumn(
        Modifier.fillMaxSize().background(Owner.Paper).windowInsetsPadding(WindowInsets.statusBars),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp,
            bottom = 32.dp + com.barathiraja.jk.ui.components.LocalNavBarInset.current),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Surface(onClick = { nav.popBackStack() }, shape = CircleShape, color = Owner.Card, contentColor = Owner.Ink, modifier = Modifier.size(52.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back to home", Modifier.size(24.dp)) }
                }
                Spacer(Modifier.width(12.dp))
                Text("Home", style = plex(16.sp), color = Owner.Muted)
            }
        }
        item {
            Column(Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp)) {
                Text(if (tab == PEOPLE_TRAINERS) "Trainers" else "Members", style = plex(30.sp, FontWeight.SemiBold, tracking = (-0.6).sp), color = Owner.Ink)
                Text(
                    if (tab == PEOPLE_TRAINERS) "How many of the workouts each trainer gave this month their members finished. Higher is better."
                    else "How many workouts each member finished this month, and who trains them.",
                    style = plex(16.sp, line = 23.sp), color = Owner.Muted, modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
        item {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(Owner.Card).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Segment("Members", members.size, tab == PEOPLE_MEMBERS, Modifier.weight(1f)) { tab = PEOPLE_MEMBERS }
                Segment("Trainers", trainers.size, tab == PEOPLE_TRAINERS, Modifier.weight(1f)) { tab = PEOPLE_TRAINERS }
            }
        }
        if (tab == PEOPLE_TRAINERS) {
            if (trainers.isEmpty()) item { Empty("No trainers yet. Share your gym code so they can join.") }
            items(trainers.size, key = { "t" + trainers[it].trainer.uid }) { i ->
                TrainerCard(trainers[i], i, top = i == 0 && trainers[i].due > 0, context) { nav.navigate(Routes.gymTrainer(trainers[i].trainer.uid)) }
            }
        } else {
            item {
                val well = members.count { (p, s) -> p.uid !in idle && s.due > 0 && s.rate >= 0.7f }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OwnerChip("$well doing well", Owner.Mint)
                    OwnerChip("${idle.size} away", Owner.Coral)
                }
            }
            if (members.isEmpty()) item { Empty("No members yet. Members join through their trainer.") }
            items(members.size, key = { "m" + members[it].first.uid }) { i ->
                val (p, s) = members[i]
                MemberCard(p, s, top = i == 0 && s.points > 0, idle[p.uid], gvm.trainerOf(p)) { nav.navigate(Routes.gymMember(p.uid)) }
            }
        }
    }
}

@Composable
private fun Empty(text: String) {
    Text(text, style = plex(17.sp, line = 25.sp), color = Owner.Muted, modifier = Modifier.padding(8.dp))
}

@Composable
private fun Segment(label: String, count: Int, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier.heightIn(min = 52.dp).semantics { this.selected = selected }, shape = RoundedCornerShape(50),
        color = if (selected) Owner.Ink else Color.Transparent, contentColor = if (selected) Owner.OnInk else Owner.Ink) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.heightIn(min = 30.dp).clip(RoundedCornerShape(50)).background(if (selected) Owner.Mustard else Owner.Paper)
                .padding(horizontal = 9.dp), contentAlignment = Alignment.Center) {
                Text("$count", style = plex(14.sp, FontWeight.SemiBold), color = if (selected) Owner.Black else Owner.Ink)
            }
            Spacer(Modifier.width(8.dp))
            Text(label, style = plex(16.sp, FontWeight.SemiBold))
        }
    }
}

internal fun trainerStatus(t: OwnerStats.TrainerRow, top: Boolean): Pair<String, Color> = when {
    top -> "Top trainer" to Owner.Mustard
    t.due == 0 -> "No workouts yet" to Owner.Stone
    t.rate >= 0.7f -> "Doing well" to Owner.Mint
    t.rate >= 0.4f -> "Could do better" to Owner.Butter
    else -> "Falling behind" to Owner.Coral
}

@Composable
private fun TrainerCard(t: OwnerStats.TrainerRow, index: Int, top: Boolean, context: Context, onClick: () -> Unit) {
    val dark = top
    val fg = if (dark) Color.White else Owner.Ink
    val sub = if (dark) Owner.OnDarkSoft else Owner.Muted
    val pct = Math.round(t.rate * 100)
    val (status, fill) = trainerStatus(t, top)
    val behind = !top && t.due > 0 && t.rate < 0.4f
    Box(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().clip(NotchedShape(notch = 64.dp, smooth = 20.dp)).background(if (dark) Owner.Hero else Owner.Card)
                .clickable(onClickLabel = "Open ${t.trainer.firstName}", onClick = onClick).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(Modifier.padding(end = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                OwnerAvatar(t.trainer.photoUrl, t.trainer.name, t.trainer.uid, 52.dp)
                Spacer(Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(t.trainer.name, style = plex(19.sp, FontWeight.SemiBold, line = 23.sp), color = fg, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    OwnerChip(status, fill)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OwnerRing(t.rate, 76.dp, 8.dp, if (dark) Owner.DarkTrack else Owner.Paper,
                    when { dark -> Owner.Mustard; behind -> Owner.Behind; else -> Owner.Ink }, "t-" + t.trainer.uid) {
                    Text(if (t.due == 0) "–" else "$pct%", style = plex(if (pct >= 100) 17.sp else 20.sp, FontWeight.SemiBold, tracking = (-0.5).sp), color = fg)
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    when {
                        t.due == 0 -> "No workouts given to members yet this month"
                        behind -> "Members finished only ${t.done} of ${plural(t.due, "workout")}"
                        else -> "Members finished ${t.done} of ${plural(t.due, "workout")}"
                    },
                    style = plex(16.sp, line = 22.sp), color = sub,
                )
            }
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(if (dark) Owner.DarkStrip else Owner.Paper)
                .heightIn(min = 48.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Trains ${plural(t.members, "member")}" + if (t.idle > 0) ", ${t.idle} away" else "", Modifier.weight(1f),
                    style = plex(15.sp, FontWeight.SemiBold), color = fg)
                if (behind) {
                    Text("Talk to ${t.trainer.firstName}", style = plex(15.sp, FontWeight.SemiBold), color = Owner.Ink,
                        modifier = Modifier.clip(RoundedCornerShape(50)).clickable {
                            context.whatsApp("Hi ${t.trainer.firstName}, your members finished ${t.done} of ${plural(t.due, "workout")} this month. Can we talk about how to help them train more?")
                        }.padding(horizontal = 6.dp, vertical = 12.dp))
                } else if (t.due > 0) Text("#${index + 1} this month", style = plex(15.sp), color = if (dark) Owner.OnDarkMuted else Owner.Muted)
            }
        }
        NotchCorner(if (dark) Owner.Mustard else Owner.Ink, if (dark) Owner.Black else Owner.OnInk, Modifier.align(Alignment.TopEnd), 64.dp)
    }
}

/** A member's status word and its chip colour; the same rules everywhere the owner sees members. */
internal fun memberStatus(s: Scoring.MemberScore?, top: Boolean, idle: OwnerStats.Idle?, hasTrainer: Boolean): Pair<String, Color> {
    val noPlan = idle != null && !idle.assignedRecently && hasTrainer
    return when {
        top -> "Top member" to Owner.Mustard
        noPlan -> "No plan" to Owner.Butter
        idle != null -> "Away" to Owner.Coral
        s == null || s.due == 0 -> "No workouts yet" to Owner.Stone
        s.rate >= 0.7f -> "Doing well" to Owner.Mint
        s.rate >= 0.4f -> "Could do better" to Owner.Butter
        else -> "Falling behind" to Owner.Coral
    }
}

@Composable
internal fun MemberCard(p: Person, s: Scoring.MemberScore, top: Boolean, idle: OwnerStats.Idle?, trainer: Person?, onClick: () -> Unit) {
    val noPlan = idle != null && !idle.assignedRecently && trainer != null
    val (status, fill) = memberStatus(s, top, idle, trainer != null)
    Box(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().clip(NotchedShape(notch = 64.dp, smooth = 20.dp)).background(Owner.Card)
                .clickable(onClickLabel = "Open ${p.firstName}", onClick = onClick).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(Modifier.padding(end = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                OwnerAvatar(p.photoUrl, p.name, p.uid, 52.dp)
                Spacer(Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(p.name, style = plex(19.sp, FontWeight.SemiBold, line = 23.sp), color = Owner.Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    OwnerChip(status, fill)
                }
            }
            if (idle != null) {
                val days = if (idle.days > 30) "over a month" else "${idle.days} days"
                val who = trainer?.firstName
                Text(
                    when {
                        noPlan -> "No workout plan for $days. Ask $who to give one."
                        who != null -> "No workout for $days. Ask $who to call ${p.firstName}."
                        else -> "No workout for $days."
                    },
                    style = plex(16.sp, line = 22.sp), color = Owner.Warm,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(fill).padding(horizontal = 14.dp, vertical = 12.dp),
                )
            } else if (s.due == 0) {
                Text("No workouts given yet this month.", style = plex(16.sp), color = Owner.Muted)
            } else {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${s.completed}", style = plex(30.sp, FontWeight.SemiBold, tracking = (-1).sp), color = Owner.Ink)
                    Text("  of ${s.due} workouts finished", style = plex(16.sp), color = Owner.Muted, modifier = Modifier.padding(bottom = 5.dp))
                }
                OwnerBar(s.rate, Owner.Paper, Owner.Ink, "m-" + p.uid)
            }
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Owner.Paper).padding(start = 8.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                if (trainer != null) {
                    OwnerAvatar(trainer.photoUrl, trainer.name, trainer.uid, 32.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Trainer  ", style = plex(15.sp), color = Owner.Muted)
                    Text(trainer.name, style = plex(15.sp, FontWeight.SemiBold), color = Owner.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                } else Text("No trainer", Modifier.padding(start = 6.dp, top = 6.dp, bottom = 6.dp), style = plex(15.sp), color = Owner.Muted)
            }
        }
        NotchCorner(Owner.Ink, Owner.OnInk, Modifier.align(Alignment.TopEnd), 64.dp)
    }
}
