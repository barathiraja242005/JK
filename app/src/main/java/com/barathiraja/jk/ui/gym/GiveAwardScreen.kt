package com.barathiraja.jk.ui.gym

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import kotlinx.coroutines.delay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.RadioButton
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.barathiraja.jk.gym.Award
import com.barathiraja.jk.gym.Person
import com.barathiraja.jk.gym.Role
import com.barathiraja.jk.gym.Scoring
import com.barathiraja.jk.ui.GymViewModel
import com.barathiraja.jk.ui.components.JkCard
import com.barathiraja.jk.ui.components.Avatar
import com.barathiraja.jk.ui.components.BackScreen
import com.barathiraja.jk.ui.theme.Leaf
import com.barathiraja.jk.ui.theme.Sun
import java.time.YearMonth

private const val PEOPLE_PREVIEW = 5

/** A choice on the Give-an-award screen; [award] links it to the automatic category used for suggestions. */
private data class AwardOption(val emoji: String, val title: String, val award: Award? = null, val trainersOnly: Boolean = false, val custom: Boolean = false)

private val awardOptions = Award.entries.map { AwardOption(it.emoji, it.label, it, it.trainer) } + listOf(
    AwardOption("⭐", "Star of the Week"),
    AwardOption("🔄", "Best Transformation"),
    AwardOption("📅", "Best Attendance"),
    AwardOption("✏️", "Your own award", custom = true),
)

/** Reward ideas for the owner; [custom] lets them type their own, an empty [text] means no gift. */
private data class RewardOption(val emoji: String, val text: String, val custom: Boolean = false)

/** Gifts that make sense for a member: their membership, training and the gym. */
private val memberRewards = listOf(
    RewardOption("🗓️", "1 month free"),
    RewardOption("🥤", "Free protein shake"),
    RewardOption("🏋️", "Free PT session"),
    RewardOption("👕", "Gym T-shirt"),
    RewardOption("💸", "20% off next fees"),
    RewardOption("📜", "Certificate"),
    RewardOption("📣", "Social shout-out"),
    RewardOption("✏️", "Your own reward", custom = true),
    RewardOption("🙌", "No gift, just the title"),
)

/** Gifts that make sense for a trainer: they work here, so pay, time off and growth rather than membership. */
private val trainerRewards = listOf(
    RewardOption("💰", "Cash bonus"),
    RewardOption("🌴", "Extra day off"),
    RewardOption("🎓", "Course or certification paid"),
    RewardOption("🎁", "Gift voucher"),
    RewardOption("🍽️", "Team dinner"),
    RewardOption("👕", "Gym T-shirt"),
    RewardOption("📜", "Certificate"),
    RewardOption("📣", "Social shout-out"),
    RewardOption("✏️", "Your own reward", custom = true),
    RewardOption("🙌", "No gift, just the title"),
)

/**
 * Three plain steps for the owner: pick an award, pick a person, decide the reward. For the automatic
 * categories the current month's leader is suggested at the top of the list.
 */
@Composable
fun GiveAwardScreen(gvm: GymViewModel, nav: NavHostController) {
    val people by gvm.people.collectAsStateWithLifecycle()
    val assignments by gvm.assignments.collectAsStateWithLifecycle()
    val busy by gvm.busy.collectAsStateWithLifecycle()
    var optionIndex by rememberSaveable { mutableStateOf(-1) }
    var customTitle by rememberSaveable { mutableStateOf("") }
    var chosenUid by rememberSaveable { mutableStateOf<String?>(null) }
    var rewardIndex by rememberSaveable { mutableStateOf(-1) }
    var customReward by rememberSaveable { mutableStateOf("") }
    var showEveryone by rememberSaveable { mutableStateOf(false) }
    val memberScores by gvm.monthScores.collectAsStateWithLifecycle()
    val trainerScores by gvm.trainerRanking.collectAsStateWithLifecycle()

    val option = awardOptions.getOrNull(optionIndex)
    val leaders = remember(people, assignments) { Scoring.awards(people, assignments, YearMonth.now(), gvm.today) }
    val suggested = option?.award?.let { leaders[it] }
    val candidates = people.filter { it.active && (it.role == Role.MEMBER || it.role == Role.TRAINER) }
        .filter { option == null || !option.trainersOnly || it.role == Role.TRAINER }
        .filter { option?.award == null || option.trainersOnly || it.role == Role.MEMBER }
        .sortedWith(compareByDescending<Person> { it.uid == suggested }.thenBy { it.role != Role.MEMBER }
            .thenByDescending { p -> memberScores[p.uid]?.points ?: trainerScores.firstOrNull { it.uid == p.uid }?.score ?: 0 }
            .thenBy { it.name.lowercase() })
    val title = if (option?.custom == true) customTitle.trim() else option?.title.orEmpty()
    val person = candidates.firstOrNull { it.uid == chosenUid }
    // Rewards follow who gets the award: the chosen person, else what the award is for.
    val forTrainer = person?.role == Role.TRAINER || (person == null && option?.trainersOnly == true)
    val rewardOptions = if (forTrainer) trainerRewards else memberRewards
    var rewardsFor by rememberSaveable { mutableStateOf(forTrainer) }
    if (rewardsFor != forTrainer) { rewardsFor = forTrainer; rewardIndex = -1; customReward = "" }
    val reward = rewardOptions.getOrNull(rewardIndex)
    val rewardText = when { reward == null -> null; reward.custom -> customReward.trim().ifBlank { null }; reward.text.startsWith("No gift") -> ""; else -> reward.text }
    val ready = title.isNotBlank() && person != null && rewardText != null

    PersonPage("Back", onBack = { nav.popBackStack() }) {
        item {
            Column(Modifier.padding(horizontal = 4.dp)) {
                Text("Give an award", style = plex(30.sp, FontWeight.SemiBold, tracking = (-0.6).sp), color = MaterialTheme.colorScheme.onSurface)
                Text("Three steps: pick the award, the person, and a reward. Everyone in the gym sees it on the Ranks tab.",
                    style = plex(16.sp, line = 23.sp), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
            }
        }
        item { StepTitle(1, "Choose the award", done = option != null) }
        item {
            TileGrid(awardOptions.size) { i, m ->
                val o = awardOptions[i]
                AwardTile(awardLook(o.emoji, o.title), o.title, i == optionIndex, m) {
                    optionIndex = i
                    if (candidates.none { it.uid == chosenUid }) chosenUid = null
                }
            }
        }
        if (option?.custom == true) item {
            FocusedField(customTitle, { customTitle = it.take(32) }, "Award name", "e.g. Early Bird", KeyboardCapitalization.Words)
        }

        if (option != null) {
            item { StepTitle(2, "Who gets it?", done = person != null) }
            if (candidates.isEmpty()) item {
                Text(if (option.trainersOnly) "No trainers yet." else "No members yet.", style = plex(17.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (candidates.isNotEmpty()) item {
                val cs = MaterialTheme.colorScheme
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(cs.surfaceContainer).padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    // Best this month first; the rest stay folded so the reward step is one short scroll away.
                    val shown = if (showEveryone) candidates else candidates.take(PEOPLE_PREVIEW).let { top ->
                        if (chosenUid != null && top.none { it.uid == chosenUid }) top + candidates.filter { it.uid == chosenUid } else top
                    }
                    shown.forEach { p ->
                        val selected = p.uid == chosenUid
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                            .background(if (selected) cs.secondaryContainer else Color.Transparent)
                            .clickable { chosenUid = p.uid }.padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            OwnerAvatar(p.photoUrl, p.name, p.uid, 44.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(p.name, style = plex(17.sp, FontWeight.SemiBold), color = cs.onSurface, maxLines = 1)
                                Text(
                                    when {
                                        p.uid == suggested -> "Leading this month"
                                        p.role == Role.TRAINER -> "Trainer · score ${trainerScores.firstOrNull { it.uid == p.uid }?.score ?: 0}"
                                        else -> "${memberScores[p.uid]?.points ?: 0} points · trainer ${gvm.person(p.trainerUid)?.firstName ?: "–"}"
                                    },
                                    style = plex(14.sp, if (p.uid == suggested) FontWeight.SemiBold else FontWeight.Normal),
                                    color = if (p.uid == suggested) Leaf else cs.onSurfaceVariant,
                                )
                            }
                            CheckDisc(selected)
                        }
                    }
                    if (candidates.size > PEOPLE_PREVIEW) {
                        TextButton(onClick = { showEveryone = !showEveryone }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text(if (showEveryone) "Show fewer" else "Show everyone (${candidates.size})", style = plex(15.sp, FontWeight.SemiBold), color = cs.onSurface)
                        }
                    }
                }
            }

            item { StepTitle(3, if (forTrainer) "Choose a trainer reward" else "Choose a member reward", done = rewardText != null) }
            item {
                TileGrid(rewardOptions.size) { i, m ->
                    val r = rewardOptions[i]
                    RewardTile(rewardIcon(r.emoji), r.text, i == rewardIndex, m) { rewardIndex = i }
                }
            }
            if (reward?.custom == true) item {
                FocusedField(customReward, { customReward = it.take(60) }, "Your reward", "e.g. Free diet plan", KeyboardCapitalization.Sentences)
            }
        }

        if (ready) item { AwardPreview(awardLook(option!!.emoji, option.title), title, person!!, rewardText!!) }
        item {
            val cs = MaterialTheme.colorScheme
            Surface(
                onClick = { if (ready) gvm.giveAward(title, option!!.emoji, person!!, rewardText!!) { nav.popBackStack() } },
                enabled = ready && !busy, shape = RoundedCornerShape(50),
                color = if (ready) com.barathiraja.jk.ui.theme.HeroBlue else cs.surfaceContainerHighest,
                contentColor = if (ready) Color.White else cs.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
            ) {
                Row(Modifier.padding(start = 22.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (ready) "Give award to ${person!!.firstName}" else "Finish the three steps", Modifier.weight(1f),
                        style = plex(17.sp, FontWeight.SemiBold), maxLines = 1)
                    Box(Modifier.size(44.dp).clip(CircleShape).background(if (ready) com.barathiraja.jk.ui.theme.Mustard else cs.surfaceContainer),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.EmojiEvents, null, Modifier.size(20.dp), tint = if (ready) Color.Black else cs.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/**
 * A text box that appears when "Your own …" is picked: it takes the cursor at once, scrolls itself above the
 * keyboard once the keyboard has opened, and the keyboard's Done key closes the keyboard.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun FocusedField(value: String, onChange: (String) -> Unit, label: String, hint: String, caps: KeyboardCapitalization) {
    val focus = remember { FocusRequester() }
    val bring = remember { BringIntoViewRequester() }
    val focusManager = LocalFocusManager.current
    val imeVisible = WindowInsets.isImeVisible
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { focus.requestFocus() }
    // Wait for the keyboard to finish opening (the list shrinks with it), then scroll the box into view.
    LaunchedEffect(focused, imeVisible) {
        if (focused) { delay(250); bring.bringIntoView() }
    }
    OutlinedTextField(
        value, onChange,
        Modifier.fillMaxWidth().bringIntoViewRequester(bring).focusRequester(focus).onFocusChanged { focused = it.isFocused },
        singleLine = true, shape = RoundedCornerShape(18.dp),
        label = { Text(label) }, placeholder = { Text(hint) },
        keyboardOptions = KeyboardOptions(capitalization = caps, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
    )
}

/** Step number in a disc (a tick once that step is done) and the step's question. */
@Composable
private fun StepTitle(n: Int, text: String, done: Boolean) {
    val cs = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 14.dp, start = 4.dp)) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(if (done) com.barathiraja.jk.ui.theme.Mustard else cs.onSurface),
            contentAlignment = Alignment.Center) {
            if (done) Icon(Icons.Outlined.Check, "Done", Modifier.size(18.dp), tint = Color.Black)
            else Text("$n", style = plex(16.sp, FontWeight.SemiBold), color = cs.surface)
        }
        Spacer(Modifier.width(12.dp))
        Text(text, style = plex(21.sp, FontWeight.SemiBold), color = cs.onSurface)
    }
}

/** [count] tiles in two equal columns, so every tile lines up. */
@Composable
private fun TileGrid(count: Int, tile: @Composable (index: Int, modifier: Modifier) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        (0 until count).chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { tile(it, Modifier.weight(1f).fillMaxHeight()) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** Round tick in the corner of a chosen tile or row; an empty ring otherwise. */
@Composable
private fun CheckDisc(on: Boolean) {
    val cs = MaterialTheme.colorScheme
    Box(Modifier.size(28.dp).clip(CircleShape).background(if (on) com.barathiraja.jk.ui.theme.Mustard else Color.Transparent)
        .border(1.5.dp, if (on) Color.Black else cs.outline, CircleShape), contentAlignment = Alignment.Center) {
        if (on) Icon(Icons.Outlined.Check, null, Modifier.size(16.dp), tint = Color.Black)
    }
}

/** An award choice: its medal and name; the chosen one turns ink with a mustard tick. */
@Composable
private fun AwardTile(look: AwardLook, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(onClick = onClick, modifier = modifier.semantics { this.selected = selected }, shape = RoundedCornerShape(24.dp),
        color = if (selected) com.barathiraja.jk.ui.theme.HeroBlue else cs.surfaceContainer,
        contentColor = if (selected) Color.White else cs.onSurface) {
        Box {
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                AwardBadge(look, 58.dp, ribbon = if (selected) com.barathiraja.jk.ui.theme.Mustard else Owner.Ink)
                Spacer(Modifier.height(10.dp))
                Text(label, style = plex(15.sp, FontWeight.SemiBold, line = 19.sp), textAlign = TextAlign.Center)
            }
            if (selected) Box(Modifier.align(Alignment.TopEnd).padding(10.dp)) { CheckDisc(true) }
        }
    }
}

/** A reward choice: icon disc and words in one row. */
@Composable
private fun RewardTile(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(onClick = onClick, modifier = modifier.heightIn(min = 64.dp).semantics { this.selected = selected }, shape = RoundedCornerShape(22.dp),
        color = if (selected) com.barathiraja.jk.ui.theme.HeroBlue else cs.surfaceContainer,
        contentColor = if (selected) Color.White else cs.onSurface) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(if (selected) com.barathiraja.jk.ui.theme.Mustard else cs.surfaceContainerHigh),
                contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(19.dp), tint = if (selected) Color.Black else cs.onSurface)
            }
            Spacer(Modifier.width(10.dp))
            Text(label, style = plex(14.sp, FontWeight.SemiBold, line = 18.sp))
        }
    }
}

/** What everyone will see: the medal, who it is for, the award and the gift, on a mustard card. */
@Composable
private fun AwardPreview(look: AwardLook, title: String, person: Person, reward: String) {
    Column(Modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(com.barathiraja.jk.ui.theme.Mustard).padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Preview", style = plex(13.sp, FontWeight.SemiBold), color = Owner.Warm)
        Spacer(Modifier.height(12.dp))
        AwardBadge(look, 92.dp)
        Spacer(Modifier.height(14.dp))
        Text(title, style = plex(22.sp, FontWeight.SemiBold), color = Color.Black, textAlign = TextAlign.Center)
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            OwnerAvatar(person.photoUrl, person.name, person.uid, 30.dp)
            Spacer(Modifier.width(8.dp))
            Text(person.name, style = plex(17.sp, FontWeight.SemiBold), color = Color.Black)
        }
        if (reward.isNotBlank()) OwnerChip("Gift · $reward", Owner.Cream, modifier = Modifier.padding(top = 12.dp))
    }
}
