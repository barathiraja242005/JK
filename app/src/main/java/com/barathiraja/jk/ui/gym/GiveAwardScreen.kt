package com.barathiraja.jk.ui.gym

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role as A11yRole
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.barathiraja.jk.gym.Award
import com.barathiraja.jk.gym.Person
import com.barathiraja.jk.gym.Role
import com.barathiraja.jk.gym.Scoring
import com.barathiraja.jk.ui.GymViewModel
import com.barathiraja.jk.ui.theme.HeroFill
import java.time.YearMonth
import kotlinx.coroutines.delay
import com.barathiraja.jk.ui.components.CardBox
import com.barathiraja.jk.ui.components.PageTitle
import com.barathiraja.jk.ui.components.PersonAvatar
import com.barathiraja.jk.ui.components.RedButton
import com.barathiraja.jk.ui.components.SegmentTab
import com.barathiraja.jk.ui.components.SegmentedTabs
import com.barathiraja.jk.ui.components.StatusChip
import com.barathiraja.jk.ui.theme.Jk
import com.barathiraja.jk.ui.theme.Tone
import com.barathiraja.jk.ui.theme.plex

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
/** Everything needed to give the award, once all three steps are done. */
private class Giving(val option: AwardOption, val title: String, val person: Person, val gift: String)

/**
 * Give an award in three steps: which award, who gets it, and a gift. Each finished step folds into one line with
 * Change, so the next step is always in view. Automatic categories belong to one side (trainer awards to trainers,
 * the rest to members); the owner's own awards suit anyone.
 */
@Composable
fun GiveAwardScreen(gvm: GymViewModel, nav: NavHostController) {
    val people by gvm.people.collectAsStateWithLifecycle()
    val assignments by gvm.assignments.collectAsStateWithLifecycle()
    val busy by gvm.busy.collectAsStateWithLifecycle()
    val memberScores by gvm.monthScores.collectAsStateWithLifecycle()
    val trainerScores by gvm.trainerRanking.collectAsStateWithLifecycle()
    var optionIndex by rememberSaveable { mutableStateOf(-1) }
    var customTitle by rememberSaveable { mutableStateOf("") }
    var chosenUid by rememberSaveable { mutableStateOf<String?>(null) }
    var rewardIndex by rememberSaveable { mutableStateOf(-1) }
    // Which side the gift was picked for: gifts differ for members and trainers, so a pick for the other side is void.
    var rewardForTrainer by rememberSaveable { mutableStateOf(false) }
    var customReward by rememberSaveable { mutableStateOf("") }
    var showEveryone by rememberSaveable { mutableStateOf(false) }
    var trainerTabPicked by rememberSaveable { mutableStateOf(false) }
    // Which finished step is open again for a change; finished steps otherwise fold into one line.
    var editStep by rememberSaveable { mutableStateOf(0) }

    val option = awardOptions.getOrNull(optionIndex)
    val leaders = remember(people, assignments) { Scoring.awards(people, assignments, YearMonth.now(), gvm.today) }
    val suggested = option?.award?.let { leaders[it] }
    val membersAllowed = option == null || !option.trainersOnly
    val trainersAllowed = option == null || option.trainersOnly || option.award == null
    val trainerTab = when { !trainersAllowed -> false; !membersAllowed -> true; else -> trainerTabPicked }
    val candidates = people.filter { it.active && (it.role == Role.MEMBER && membersAllowed || it.role == Role.TRAINER && trainersAllowed) }
        .sortedWith(compareByDescending<Person> { it.uid == suggested }
            .thenByDescending { p -> memberScores[p.uid]?.points ?: trainerScores.firstOrNull { it.uid == p.uid }?.score ?: 0 }
            .thenBy { it.name.lowercase() })
    val title = if (option?.custom == true) customTitle.trim() else option?.title.orEmpty()
    val person = candidates.firstOrNull { it.uid == chosenUid }
    // Gifts follow who gets the award: the chosen person, else the side the owner is looking at.
    val forTrainer = person?.role == Role.TRAINER || (person == null && trainerTab)
    val rewardOptions = if (forTrainer) trainerRewards else memberRewards
    val reward = rewardOptions.getOrNull(rewardIndex)?.takeIf { rewardForTrainer == forTrainer }
    val gift = when { reward == null -> null; reward.custom -> customReward.trim().ifBlank { null }; reward.text.startsWith("No gift") -> ""; else -> reward.text }
    val giving = if (option != null && person != null && gift != null && title.isNotBlank()) Giving(option, title, person, gift) else null

    // Leaving a half-filled form asks first, so a stray Back doesn't lose the owner's choices.
    val started = optionIndex >= 0 || chosenUid != null || rewardIndex >= 0
    var leaving by remember { mutableStateOf(false) }
    BackHandler(enabled = started && !busy) { leaving = true }
    if (leaving) AlertDialog(onDismissRequest = { leaving = false },
        title = { Text("Leave without giving the award?") },
        text = { Text("What you picked here will be lost.", style = plex(15.sp, line = 21.sp)) },
        confirmButton = { TextButton(onClick = { leaving = false; nav.popBackStack() }) {
            Text("Leave", style = plex(14.sp, FontWeight.SemiBold), color = Jk.RedText) } },
        dismissButton = { TextButton(onClick = { leaving = false }) { Text("Stay", style = plex(14.sp, FontWeight.SemiBold), color = Jk.Ink) } })

    PersonPage("Back", onBack = { if (started) leaving = true else nav.popBackStack() }) {
        item { PageTitle("Give an award", "Pick the award, who gets it, and a gift. Everyone in the gym will see it.") }

        // Step 1: the award.
        if (option != null && editStep != 1) item {
            StepSummary("Award", if (option.custom) "Your own award" else option.title) { editStep = 1 }
        } else {
            item { StepTitle(1, "Choose the award", done = option != null) }
            item {
                AwardChoices(optionIndex) { i ->
                    val o = awardOptions[i]
                    optionIndex = i
                    editStep = 0
                    // Keep the chosen person only if the new award can go to them.
                    val chosenRole = people.firstOrNull { it.uid == chosenUid }?.role
                    val keep = when (chosenRole) { Role.TRAINER -> o.trainersOnly || o.award == null; Role.MEMBER -> !o.trainersOnly; else -> false }
                    if (!keep) chosenUid = null
                }
            }
        }
        if (option?.custom == true) item {
            FocusedField(customTitle, { customTitle = it.take(32) }, "Award name", "e.g. Early Bird", KeyboardCapitalization.Words)
        }

        // Step 2: who gets it.
        if (option != null && person != null && editStep != 2) item {
            StepSummary(if (person.role == Role.TRAINER) "Trainer" else "Member", person.name) { editStep = 2 }
        }
        if (option != null && (person == null || editStep == 2)) {
            item { StepTitle(2, "Who gets it?", done = person != null) }
            item {
                val active = people.filter { it.active }
                SegmentedTabs(
                    listOf(
                        SegmentTab("Members", active.count { it.role == Role.MEMBER }, enabled = membersAllowed),
                        SegmentTab("Trainers", active.count { it.role == Role.TRAINER }, enabled = trainersAllowed),
                    ),
                    if (trainerTab) 1 else 0, { trainerTabPicked = it == 1; showEveryone = false },
                )
                if (!membersAllowed || !trainersAllowed) Text(
                    "${option.title} is only for ${if (trainersAllowed) "trainers" else "members"}.",
                    style = plex(13.sp), color = Jk.Muted, modifier = Modifier.padding(start = 6.dp, top = 10.dp),
                )
            }
            item {
                PersonChoices(
                    people = candidates.filter { (it.role == Role.TRAINER) == trainerTab }, trainerTab = trainerTab,
                    chosenUid = chosenUid, suggested = suggested, showEveryone = showEveryone,
                    detail = { p ->
                        if (p.role == Role.TRAINER) trainerScores.firstOrNull { it.uid == p.uid }?.takeIf { it.members > 0 }
                            ?.let { "Members finished ${Math.round(it.rate * 100)}% of workouts" } ?: "No members' workouts yet"
                        else "${memberScores[p.uid]?.points ?: 0} points · trainer ${gvm.trainerOf(p)?.firstName ?: "–"}"
                    },
                    onShowEveryone = { showEveryone = !showEveryone },
                    onPick = { chosenUid = it.uid; editStep = 0 },
                )
            }
        }

        // Step 3: the gift.
        if (option != null && person != null) {
            if (reward != null && editStep != 3) item {
                StepSummary("Gift", if (reward.custom) "Your own gift" else reward.text) { editStep = 3 }
            } else {
                item { StepTitle(3, if (forTrainer) "Choose a trainer gift" else "Choose a member gift", done = gift != null) }
                item {
                    TileGrid(rewardOptions.size) { i, m ->
                        val r = rewardOptions[i]
                        RewardTile(rewardIcon(r.emoji), r.text, reward != null && i == rewardIndex, m) {
                            rewardIndex = i; rewardForTrainer = forTrainer; editStep = 0
                        }
                    }
                }
            }
            if (reward?.custom == true) item {
                FocusedField(customReward, { customReward = it.take(60) }, "Your gift", "e.g. Free diet plan", KeyboardCapitalization.Sentences)
            }
        }

        if (giving != null) item { AwardPreview(awardLook(giving.option.emoji, giving.option.title), giving.title, giving.person, giving.gift) }
        item {
            RedButton(giving?.let { "Give award to ${it.person.firstName}" } ?: "Finish the three steps",
                { giving?.let { g -> gvm.giveAward(g.title, g.option.emoji, g.person, g.gift) { nav.popBackStack() } } },
                Modifier.fillMaxWidth().padding(top = 4.dp), Icons.Outlined.EmojiEvents, enabled = giving != null && !busy)
        }
    }
}

/** The award tiles in two columns; [selected] is the picked index (-1 for none). */
@Composable
private fun AwardChoices(selected: Int, onPick: (Int) -> Unit) {
    TileGrid(awardOptions.size) { i, m ->
        val o = awardOptions[i]
        AwardTile(awardLook(o.emoji, o.title), o.title, i == selected, m) { onPick(i) }
    }
}

/**
 * The people who can get the award, best this month first, each a radio choice. Only [PEOPLE_PREVIEW] show until
 * "Show all", so the gift step stays a short scroll away; the chosen person always stays visible.
 */
@Composable
private fun PersonChoices(
    people: List<Person>, trainerTab: Boolean, chosenUid: String?, suggested: String?, showEveryone: Boolean,
    detail: (Person) -> String, onShowEveryone: () -> Unit, onPick: (Person) -> Unit,
) {
    if (people.isEmpty()) {
        Text(if (trainerTab) "No trainers yet. Share your gym code from the Me tab to add one." else "No members yet. Trainers add members with their own code.",
            style = plex(14.sp, line = 19.sp), color = Jk.Muted, modifier = Modifier.padding(horizontal = 6.dp))
        return
    }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Jk.Card).padding(6.dp).selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(2.dp)) {
        val shown = if (showEveryone) people else people.take(PEOPLE_PREVIEW).let { top ->
            if (chosenUid != null && top.none { it.uid == chosenUid }) top + people.filter { it.uid == chosenUid } else top
        }
        shown.forEach { p ->
            val selected = p.uid == chosenUid
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(if (selected) Tone.WARN.fill else Color.Transparent)
                .selectable(selected = selected, role = A11yRole.RadioButton) { onPick(p) }
                .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                PersonAvatar(p.photoUrl, p.name, 44.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.name, style = plex(15.sp, FontWeight.SemiBold), color = Jk.Ink, maxLines = 1)
                    if (p.uid == suggested) StatusChip("Leading this month", Tone.TOP, Modifier.padding(top = 3.dp))
                    else Text(detail(p), style = plex(13.sp), color = Jk.Muted)
                }
                CheckDisc(selected)
            }
        }
        if (people.size > PEOPLE_PREVIEW) {
            TextButton(onClick = onShowEveryone, Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(if (showEveryone) "Show fewer" else "Show all ${if (trainerTab) "trainers" else "members"} (${people.size})",
                    style = plex(13.sp, FontWeight.SemiBold), color = Jk.Ink)
            }
        }
    }
}

/**
 * A text box that appears when "Your own …" is picked: it takes the cursor at once, scrolls itself above the
 * keyboard once the keyboard has opened, and the keyboard's Done key closes the keyboard.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
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
        singleLine = true, shape = RoundedCornerShape(16.dp),
        label = { Text(label) }, placeholder = { Text(hint) },
        keyboardOptions = KeyboardOptions(capitalization = caps, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
    )
}

/** A finished step folded to one line: a yellow tick, what was picked, and Change to open the step again. */
@Composable
private fun StepSummary(label: String, value: String, onChange: () -> Unit) {
    CardBox(padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(30.dp).clip(CircleShape).background(Jk.Yellow), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Check, null, Modifier.size(16.dp), tint = Color.Black)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(label, style = plex(12.sp), color = Jk.Muted)
                Text(value, style = plex(15.sp, FontWeight.SemiBold), color = Jk.Ink, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
            TextButton(onClick = onChange, Modifier.heightIn(min = 48.dp)) {
                Text("Change", style = plex(14.sp, FontWeight.SemiBold), color = Jk.Ink)
            }
        }
    }
}

/** Step number in a disc (a tick once that step is done) and the step's question. */
@Composable
private fun StepTitle(n: Int, text: String, done: Boolean) {
    val cs = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 14.dp, start = 4.dp)) {
        Box(Modifier.size(30.dp).clip(CircleShape).background(if (done) Jk.Yellow else cs.onSurface),
            contentAlignment = Alignment.Center) {
            if (done) Icon(Icons.Outlined.Check, "Done", Modifier.size(18.dp), tint = Color.Black)
            else Text("$n", style = plex(14.sp, FontWeight.SemiBold), color = cs.surface)
        }
        Spacer(Modifier.width(12.dp))
        Text(text, style = plex(17.sp, FontWeight.SemiBold), color = cs.onSurface)
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
    Box(Modifier.size(24.dp).clip(CircleShape).background(if (on) Jk.Yellow else Color.Transparent)
        .border(1.5.dp, if (on) Color.Black else cs.outline, CircleShape), contentAlignment = Alignment.Center) {
        if (on) Icon(Icons.Outlined.Check, null, Modifier.size(16.dp), tint = Color.Black)
    }
}

/** An award choice: its medal and name; the chosen one turns ink with a mustard tick. */
@Composable
private fun AwardTile(look: AwardLook, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(onClick = onClick, modifier = modifier.semantics { this.selected = selected }, shape = RoundedCornerShape(20.dp),
        color = if (selected) HeroFill else cs.surfaceContainer,
        contentColor = if (selected) Color.White else cs.onSurface) {
        Box {
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                AwardBadge(look, 48.dp)
                Spacer(Modifier.height(10.dp))
                Text(label, style = plex(13.sp, FontWeight.SemiBold, line = 16.sp), textAlign = TextAlign.Center)
            }
            if (selected) Box(Modifier.align(Alignment.TopEnd).padding(10.dp)) { CheckDisc(true) }
        }
    }
}

/** A reward choice: icon disc and words in one row. */
@Composable
private fun RewardTile(icon: ImageVector, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(onClick = onClick, modifier = modifier.heightIn(min = 52.dp).semantics { this.selected = selected }, shape = RoundedCornerShape(18.dp),
        color = if (selected) HeroFill else cs.surfaceContainer,
        contentColor = if (selected) Color.White else cs.onSurface) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(if (selected) Jk.Yellow else cs.surfaceContainerHigh),
                contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(19.dp), tint = if (selected) Color.Black else cs.onSurface)
            }
            Spacer(Modifier.width(10.dp))
            Text(label, style = plex(13.sp, FontWeight.SemiBold, line = 15.sp))
        }
    }
}

/** What everyone will see: the medal, who it is for, the award and the gift, on a mustard card. */
@Composable
private fun AwardPreview(look: AwardLook, title: String, person: Person, reward: String) {
    Column(Modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Jk.Yellow).padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Preview", style = plex(13.sp, FontWeight.SemiBold), color = Jk.Black)
        Spacer(Modifier.height(12.dp))
        AwardBadge(look, 72.dp)
        Spacer(Modifier.height(14.dp))
        Text(title, style = plex(18.sp, FontWeight.SemiBold), color = Color.Black, textAlign = TextAlign.Center)
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            PersonAvatar(person.photoUrl, person.name, 26.dp)
            Spacer(Modifier.width(8.dp))
            Text(person.name, style = plex(15.sp, FontWeight.SemiBold), color = Color.Black)
        }
        if (reward.isNotBlank()) StatusChip("Gift · $reward", Tone.GOOD, Modifier.padding(top = 12.dp), lines = 3)
    }
}
