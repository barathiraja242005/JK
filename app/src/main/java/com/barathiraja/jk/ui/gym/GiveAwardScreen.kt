package com.barathiraja.jk.ui.gym

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

private val rewardOptions = listOf(
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
    val reward = rewardOptions.getOrNull(rewardIndex)
    val rewardText = when { reward == null -> null; reward.custom -> customReward.trim().ifBlank { null }; reward.text.startsWith("No gift") -> ""; else -> reward.text }
    val ready = title.isNotBlank() && person != null && rewardText != null

    BackScreen("Give an award", onBack = { nav.popBackStack() }) {
        item { StepTitle(1, "Choose the award") }
        item {
            TileGrid(awardOptions.size) { i, m ->
                val o = awardOptions[i]
                ChoiceTile(o.emoji, o.title, i == optionIndex, m) {
                    optionIndex = i
                    if (candidates.none { it.uid == chosenUid }) chosenUid = null
                }
            }
        }
        if (option?.custom == true) item {
            OutlinedTextField(customTitle, { customTitle = it.take(32) }, Modifier.fillMaxWidth(), singleLine = true,
                label = { Text("Award name") }, placeholder = { Text("e.g. Early Bird") })
        }

        if (option != null) {
            item { StepTitle(2, "Who gets it?") }
            if (candidates.isEmpty()) item {
                Text(if (option.trainersOnly) "No trainers yet." else "No members yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (candidates.isNotEmpty()) item {
                JkCard(Modifier.fillMaxWidth()) {
                    // Best this month first; the rest stay folded so the reward step is one short scroll away.
                    val shown = if (showEveryone) candidates else candidates.take(PEOPLE_PREVIEW).let { top ->
                        if (chosenUid != null && top.none { it.uid == chosenUid }) top + candidates.filter { it.uid == chosenUid } else top
                    }
                    shown.forEachIndexed { i, p ->
                        if (i > 0) HorizontalDivider()
                        val selected = p.uid == chosenUid
                        Row(Modifier.fillMaxWidth().clickable { chosenUid = p.uid }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Avatar(p.photoUrl, p.name, 44.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(p.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                                Text(
                                    when {
                                        p.uid == suggested -> "⭐ Leading this month"
                                        p.role == Role.TRAINER -> "Trainer · score ${trainerScores.firstOrNull { it.uid == p.uid }?.score ?: 0}"
                                        else -> "${memberScores[p.uid]?.points ?: 0} pts · Coach ${gvm.person(p.trainerUid)?.firstName ?: "–"}"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (p.uid == suggested) Leaf else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            RadioButton(selected = selected, onClick = { chosenUid = p.uid })
                        }
                    }
                    if (candidates.size > PEOPLE_PREVIEW) {
                        TextButton(onClick = { showEveryone = !showEveryone }, Modifier.fillMaxWidth()) {
                            Text(if (showEveryone) "Show fewer" else "Show everyone (${candidates.size})", style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            }

            item { StepTitle(3, "Choose the reward") }
            item {
                TileGrid(rewardOptions.size) { i, m ->
                    val r = rewardOptions[i]
                    ChoiceTile(r.emoji, r.text, i == rewardIndex, m) { rewardIndex = i }
                }
            }
            if (reward?.custom == true) item {
                OutlinedTextField(customReward, { customReward = it.take(60) }, Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text("Reward") }, placeholder = { Text("e.g. Free diet plan") })
            }
        }

        item {
            if (ready) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(option!!.emoji, style = MaterialTheme.typography.displaySmall)
                        Text(title, style = MaterialTheme.typography.titleLarge, color = Sun)
                        Text(person!!.name, style = MaterialTheme.typography.titleMedium)
                        if (rewardText!!.isNotBlank()) Text("🎁 $rewardText", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item {
            Button(
                onClick = { if (ready) gvm.giveAward(title, option!!.emoji, person!!, rewardText!!) { nav.popBackStack() } },
                enabled = ready && !busy,
                modifier = Modifier.fillMaxWidth().height(60.dp),
            ) {
                Text(if (ready) "Give award to ${person!!.firstName}" else "Give award", style = MaterialTheme.typography.titleMedium, maxLines = 1)
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun StepTitle(n: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
        Surface(Modifier.size(32.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
            Box(contentAlignment = Alignment.Center) {
                Text("$n", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.titleLarge)
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

@Composable
private fun ChoiceTile(emoji: String, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick, modifier = modifier, shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Text(emoji, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
        }
    }
}
