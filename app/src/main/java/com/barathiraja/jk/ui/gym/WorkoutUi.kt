package com.barathiraja.jk.ui.gym

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barathiraja.jk.data.ExerciseRepo
import com.barathiraja.jk.data.SetSpec
import com.barathiraja.jk.data.weightLabel
import com.barathiraja.jk.domain.Health
import com.barathiraja.jk.domain.TrainingEngine
import com.barathiraja.jk.gym.AssignedExercise
import com.barathiraja.jk.gym.Assignment
import com.barathiraja.jk.ui.components.ExerciseDemo
import com.barathiraja.jk.ui.components.Ring
import com.barathiraja.jk.ui.components.formatDuration
import com.barathiraja.jk.data.Exercise
import com.barathiraja.jk.ui.components.CardBox
import com.barathiraja.jk.ui.components.RedButton
import com.barathiraja.jk.ui.theme.Jk
import com.barathiraja.jk.ui.theme.plex
import com.barathiraja.jk.gym.plural

/*
 * The member's side of a coach's workout, built to be read at a glance mid-set: numbers in big type, the how-to
 * as a few short chips instead of sentences, and one obvious thing to tap next.
 */

// ---------- the coach's cue, split into glanceable parts ----------

/**
 * A coach's cue taken apart: "3–4 sets of 6–8. Or Hack Squat. Brace the core, squat under control." becomes
 * reps "6–8", swap "Hack Squat" and tips ["Brace the core", "Squat under control"]. Free text from a trainer
 * simply becomes tips.
 */
internal data class Cue(val reps: String?, val swap: String?, val tips: List<String>)

internal fun parseCue(raw: String): Cue {
    var rest = raw.trim()
    var reps: String? = null
    Regex("^\\d+(?:[–-]\\d+)? sets? of (\\d+(?:[–-]\\d+)?)\\.?\\s*", RegexOption.IGNORE_CASE).find(rest)?.let { m ->
        reps = m.groupValues[1].replace('-', '–'); rest = rest.substring(m.range.last + 1)
    }
    val sentences = rest.split(Regex("(?<=\\.)\\s+")).map { it.trim().trimEnd('.') }.filter { it.isNotBlank() }
    val swap = sentences.firstOrNull { it.startsWith("Or ", ignoreCase = true) }?.substring(3)?.trim()
    val tips = sentences.filterNot { it.startsWith("Or ", ignoreCase = true) }
        .flatMap { s -> s.split(Regex(",\\s+|;\\s+")) }
        .map { t -> t.trim().replaceFirstChar { it.uppercase() } }
        .filter { it.isNotBlank() }
    return Cue(reps, swap?.replaceFirstChar { it.uppercase() }, tips)
}

/** Short how-to chips: a tick and 2–6 words each, wrapping onto as many rows as they need. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TipChips(tips: List<String>, modifier: Modifier = Modifier) {
    if (tips.isEmpty()) return
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        tips.forEach { t ->
            Row(Modifier.clip(RoundedCornerShape(10.dp)).background(Jk.Well).padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Check, null, Modifier.size(13.dp), tint = Jk.RedText)
                Spacer(Modifier.width(4.dp))
                Text(t, style = plex(12.sp, FontWeight.Medium, line = 16.sp), color = Jk.Ink)
            }
        }
    }
}

// ---------- Today: the coach's workout card ----------

/** Minutes a workout takes with [restSec] between sets. */
internal fun Assignment.minutes(restSec: Int) = Health.minutesUp(exercises.sumOf { TrainingEngine.estimateSec(it.sets, restSec) })

/** The muscles a workout trains, most used first ("Quadriceps · Hamstrings · Glutes"). */
internal fun Assignment.muscles(max: Int = 3): String =
    exercises.flatMap { ExerciseRepo.get(it.exerciseId)?.primary.orEmpty() }.groupingBy { it }.eachCount()
        .entries.sortedByDescending { it.value }.take(max).joinToString(" · ") { e -> e.key.replaceFirstChar { it.uppercase() } }

/**
 * Today's workout from the coach on the charcoal card: who sent it, the name big, the muscles, a strip of the
 * exercises' pictures, time / exercises / sets, and one button. Progress sits in a ring at the top right.
 */
@Composable
internal fun TodayWorkoutCard(w: Assignment, coachName: String, restSec: Int, today: Long = w.epochDay, onOpen: () -> Unit) {
    val upcoming = w.epochDay > today
    val missed = w.epochDay < today && !w.done
    val progress = if (w.setsTotal == 0) 0f else w.setsDone / w.setsTotal.toFloat()
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Jk.Hero).clickable(onClickLabel = "Open workout", onClick = onOpen)
        .padding(18.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                val from = (if (w.epochDay == today) "" else dayLabel(w.epochDay, today).uppercase() + " · ") +
                    (if (coachName.isBlank()) "FROM YOUR COACH" else "FROM COACH ${coachName.uppercase()}")
                Text(if (w.done) "DONE ✓" else from,
                    Modifier.clip(RoundedCornerShape(6.dp)).background(if (w.done) Jk.Yellow else Jk.DarkStrip)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    style = plex(11.sp, FontWeight.Bold, tracking = 0.8.sp), color = if (w.done) Jk.Black else Jk.OnDarkSoft, maxLines = 1)
                Text(w.title, Modifier.padding(top = 10.dp), style = plex(28.sp, FontWeight.Bold, line = 32.sp, tracking = (-0.8).sp),
                    color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val muscles = w.muscles()
                if (muscles.isNotBlank()) Text(muscles, Modifier.padding(top = 2.dp), style = plex(14.sp), color = Jk.OnDarkMuted,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(12.dp))
            Ring(progress, Jk.Yellow, size = 60.dp, stroke = 6.dp, track = Jk.DarkTrack) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${w.exercisesDone}/${w.exercises.size}", style = plex(15.sp, FontWeight.Bold), color = Color.White)
                }
            }
        }

        // The exercises as a strip of pictures, so the member sees what's coming without reading.
        Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val shown = w.exercises.take(5)
            shown.forEach { e ->
                Box(Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(Color.White)) {
                    ExerciseRepo.get(e.exerciseId)?.let { ExerciseDemo(it, Modifier.size(46.dp), animate = false) }
                    if (e.done) Box(Modifier.size(46.dp).background(Color.Black.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Check, null, Modifier.size(22.dp), tint = Jk.Yellow)
                    }
                }
            }
            if (w.exercises.size > shown.size) Box(Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(Jk.DarkStrip),
                contentAlignment = Alignment.Center) {
                Text("+${w.exercises.size - shown.size}", style = plex(14.sp, FontWeight.Bold), color = Color.White)
            }
        }

        Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            HeroStat(Icons.Outlined.Timer, "${w.minutes(restSec)} min")
            HeroStat(Icons.Outlined.FitnessCenter, plural(w.exercises.size, "exercise"))
            HeroStat(Icons.Outlined.Layers, "${w.setsDone}/${w.setsTotal} sets")
        }

        Spacer(Modifier.height(16.dp))
        if (w.done || upcoming) Surface(onClick = onOpen, shape = RoundedCornerShape(50), color = Jk.DarkStrip, contentColor = Color.White,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text(if (w.done) "See what you did" else "See the exercises", style = plex(15.sp, FontWeight.SemiBold))
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, Modifier.size(20.dp))
            }
        } else RedButton(when {
            w.setsDone > 0 -> "Continue · ${w.setsTotal - w.setsDone} sets left"
            missed -> "Do it now"
            else -> "Start workout"
        }, onOpen,
            Modifier.fillMaxWidth(), Icons.Filled.PlayArrow)
    }
}

@Composable
private fun HeroStat(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(16.dp), tint = Jk.Yellow)
        Spacer(Modifier.width(5.dp))
        Text(text, style = plex(13.sp, FontWeight.SemiBold), color = Color.White, maxLines = 1)
    }
}

/** Rank on the gym leaderboard: a yellow disc with the place, points, and how far to the next one up. */
@Composable
internal fun RankCard(rank: Int, points: Int, gap: Int?, aheadName: String?, onClick: () -> Unit) {
    CardBox(onClick = onClick, onClickLabel = "Open leaderboard", padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).clip(CircleShape).background(if (points == 0) Jk.Well else Jk.Yellow), contentAlignment = Alignment.Center) {
                Text(if (points == 0) "–" else "#$rank", style = plex(18.sp, FontWeight.Bold, tracking = (-0.5).sp), color = Jk.Black)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(when { points == 0 -> "Gym leaderboard"; gap == null -> "You're leading the gym"; else -> "${ordinal(rank)} in the gym" },
                    style = plex(16.sp, FontWeight.Bold), color = Jk.Ink)
                Text(when {
                    points == 0 -> "Finish a workout to get on the board"
                    gap == null -> "$points pts this month"
                    else -> "$points pts · $gap behind ${aheadName ?: "#${rank - 1}"}"
                }, style = plex(14.sp), color = Jk.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, Modifier.size(22.dp), tint = Jk.Muted)
        }
    }
}

private fun ordinal(n: Int) = "$n" + if (n % 100 in 11..13) "th" else when (n % 10) { 1 -> "st"; 2 -> "nd"; 3 -> "rd"; else -> "th" }

// ---------- the workout screen ----------

/** Top of the workout screen: progress ring, sets and exercises done, time, and the coach's note folded to one line. */
@Composable
internal fun SessionSummary(w: Assignment, dayText: String, restSec: Int, locked: Boolean) {
    var noteOpen by rememberSaveable { mutableStateOf(false) }
    val progress = if (w.setsTotal == 0) 0f else w.setsDone / w.setsTotal.toFloat()
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Jk.Hero).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Ring(progress, Jk.Yellow, size = 76.dp, stroke = 7.dp, track = Jk.DarkTrack) {
                Text("${(progress * 100).toInt()}%", style = plex(17.sp, FontWeight.Bold), color = Color.White)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(dayText.uppercase(), style = plex(11.sp, FontWeight.Bold, tracking = 0.8.sp), color = Jk.OnDarkMuted)
                Text("${w.setsDone} of ${w.setsTotal} sets", style = plex(22.sp, FontWeight.Bold, tracking = (-0.5).sp), color = Color.White)
                Text("${w.exercisesDone}/${w.exercises.size} exercises · ~${w.minutes(restSec)} min", style = plex(14.sp), color = Jk.OnDarkSoft)
            }
        }
        if (locked) Text("Opens on $dayText", Modifier.padding(top = 12.dp).clip(RoundedCornerShape(8.dp)).background(Jk.DarkStrip)
            .padding(horizontal = 10.dp, vertical = 6.dp), style = plex(13.sp, FontWeight.SemiBold), color = Jk.Yellow)
        if (w.trainerNote.isNotBlank()) {
            Row(Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Jk.DarkStrip)
                .clickable(role = Role.Button, onClickLabel = if (noteOpen) "Fold coach note" else "Read coach note") { noteOpen = !noteOpen }
                .padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.ChatBubbleOutline, null, Modifier.size(18.dp).offset(y = 1.dp), tint = Jk.Yellow)
                Spacer(Modifier.width(8.dp))
                Text(w.trainerNote, Modifier.weight(1f), style = plex(14.sp, line = 19.sp), color = Color.White,
                    maxLines = if (noteOpen) Int.MAX_VALUE else 1, overflow = TextOverflow.Ellipsis)
                Icon(if (noteOpen) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, null, Modifier.size(20.dp), tint = Jk.OnDarkMuted)
            }
        }
    }
}

/**
 * One exercise of the workout: its number, picture and name (the ⓘ opens the how-to), the target in three big
 * numbers, the coach's tips as chips, then the sets as a table. In the [current] exercise the next set to do is marked. A finished
 * exercise folds down to one line; tap it to open again.
 */
@Composable
internal fun ExerciseCard(
    index: Int, e: AssignedExercise, canTick: Boolean, current: Boolean, onHowTo: (String) -> Unit, onToggle: (Int) -> Unit,
    editor: (@Composable () -> Unit)?,
) {
    val ex = ExerciseRepo.get(e.exerciseId)
    val name = ex?.name ?: e.exerciseId
    val cue = remember(e.cue) { parseCue(e.cue) }
    var open by rememberSaveable(e.exerciseId, index) { mutableStateOf(!e.done) }
    val showSets = open || editor != null
    // Only the exercise being done right now marks its next set; the rest wait their turn.
    val next = if (current) e.sets.indexOfFirst { !it.done } else -1
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Jk.Card)
        .then(if (current && canTick) Modifier.border(2.dp, Jk.Ink, RoundedCornerShape(20.dp)) else Modifier)) {
        Row(Modifier.fillMaxWidth().clickable(onClickLabel = if (open) "Fold $name" else "Open $name") { open = !open }.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box {
                Box(Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)).background(Color.White)) {
                    ex?.let { ExerciseDemo(it, Modifier.size(64.dp), animate = open && !e.done) }
                }
                Box(Modifier.offset((-4).dp, (-4).dp).size(24.dp).clip(CircleShape).background(if (e.done) Jk.Yellow else Jk.Black),
                    contentAlignment = Alignment.Center) {
                    if (e.done) Icon(Icons.Filled.Check, null, Modifier.size(15.dp), tint = Jk.Black)
                    else Text("${index + 1}", style = plex(12.sp, FontWeight.Bold), color = Color.White)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(name, style = plex(16.sp, FontWeight.Bold, line = 20.sp), color = Jk.Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(if (e.done) "Done · ${setsLine(e)}" else ex?.primary?.joinToString(" · ") { it.replaceFirstChar(Char::uppercase) }.orEmpty(),
                    style = plex(13.sp), color = Jk.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (ex != null) Surface(onClick = { onHowTo(ex.id) }, shape = CircleShape, color = Jk.Well, contentColor = Jk.Ink,
                modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Info, "How to do $name", Modifier.size(20.dp)) }
            }
        }

        AnimatedVisibility(showSets) {
            Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                TargetStrip(e, cue.reps)
                if (cue.swap != null) Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.SwapHoriz, null, Modifier.size(16.dp), tint = Jk.Muted)
                    Spacer(Modifier.width(4.dp))
                    Text("Machine busy? ${cue.swap}", style = plex(12.sp, FontWeight.Medium), color = Jk.Muted)
                }
                TipChips(cue.tips, Modifier.padding(top = 10.dp))
                Spacer(Modifier.height(12.dp))
                if (editor != null) editor()
                else SetTable(e, next, canTick, onToggle)
            }
        }
    }
}

/** "3 × 8 @ 87.5 kg" for a folded, finished exercise. */
private fun setsLine(e: AssignedExercise): String {
    val s = e.sets.firstOrNull() ?: return ""
    if (s.timed) return "${e.sets.size} × ${formatDuration(s.seconds.toLong())}"
    val w = weightLabel(e.sets.maxOf { it.weightKg }, e.exerciseId)
    return "${e.sets.size} × ${e.sets.maxOf { it.reps }}" + when (w) { "–" -> ""; "BW" -> " · bodyweight"; else -> " @ $w kg" }
}

/** The target in three big numbers: sets, reps (the coach's range when there is one) and weight. */
@Composable
private fun TargetStrip(e: AssignedExercise, repRange: String?) {
    val timed = e.sets.any { it.timed }
    val reps = repRange ?: e.sets.map { it.reps }.distinct().sorted().let { if (it.size > 1) "${it.first()}–${it.last()}" else "${it.firstOrNull() ?: 0}" }
    val kg = weightLabel(e.sets.maxOfOrNull { it.weightKg } ?: 0f, e.exerciseId)
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Jk.Well).padding(vertical = 10.dp)) {
        TargetCell("${e.sets.size}", "sets", Modifier.weight(1f))
        if (timed) TargetCell(formatDuration((e.sets.maxOfOrNull { it.seconds } ?: 0).toLong()), "each", Modifier.weight(1f))
        else {
            TargetCell(reps, "reps", Modifier.weight(1f))
            TargetCell(kg, if (kg == "BW") "bodyweight" else "kg", Modifier.weight(1f))
        }
    }
}

@Composable
private fun TargetCell(value: String, label: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = plex(20.sp, FontWeight.Bold, tracking = (-0.3).sp), color = Jk.Ink, maxLines = 1)
        Text(label.uppercase(), style = plex(10.sp, FontWeight.SemiBold, tracking = 0.8.sp), color = Jk.Muted)
    }
}

/** Sets as rows of SET · KG · REPS · tick. The next one to do is black; done ones are ticked and quiet. */
@Composable
private fun SetTable(e: AssignedExercise, next: Int, canTick: Boolean, onToggle: (Int) -> Unit) {
    val timed = e.sets.any { it.timed }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.padding(horizontal = 12.dp)) {
            Header("SET", Modifier.width(40.dp))
            if (timed) Header("TIME", Modifier.weight(1f)) else { Header("KG", Modifier.weight(1f)); Header("REPS", Modifier.weight(1f)) }
            Spacer(Modifier.width(44.dp))
        }
        e.sets.forEachIndexed { i, s -> SetLine(i, s, e.exerciseId, isNext = i == next, canTick = canTick) { onToggle(i) } }
    }
}

@Composable
private fun Header(text: String, modifier: Modifier) {
    Text(text, modifier, style = plex(10.sp, FontWeight.Bold, tracking = 1.sp), color = Jk.Muted)
}

@Composable
private fun SetLine(i: Int, s: SetSpec, exerciseId: String, isNext: Boolean, canTick: Boolean, onTick: () -> Unit) {
    val bg = when { isNext && canTick -> Jk.Ink; s.done -> Jk.Well; else -> Color.Transparent }
    val ink = if (isNext && canTick) Jk.OnInk else Jk.Ink
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(14.dp)).background(bg)
            .then(if (!isNext && !s.done) Modifier.border(BorderStroke(1.dp, Jk.Line), RoundedCornerShape(14.dp)) else Modifier)
            .clickable(enabled = canTick, role = Role.Checkbox, onClickLabel = if (s.done) "Untick set ${i + 1}" else "Tick set ${i + 1}", onClick = onTick)
            .padding(start = 12.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("${i + 1}", Modifier.width(40.dp), style = plex(15.sp, FontWeight.Bold), color = if (s.done) Jk.Muted else ink)
        val faded = if (s.done) Jk.Muted else ink
        if (s.timed) Text(formatDuration(s.seconds.toLong()), Modifier.weight(1f), style = plex(18.sp, FontWeight.Bold), color = faded)
        else {
            Text(weightLabel(s.weightKg, exerciseId), Modifier.weight(1f), style = plex(18.sp, FontWeight.Bold), color = faded)
            Text("${s.reps}", Modifier.weight(1f), style = plex(18.sp, FontWeight.Bold), color = faded)
        }
        Box(Modifier.size(40.dp).clip(CircleShape).background(
            when { s.done -> Jk.Yellow; isNext && canTick -> Jk.Red; else -> Color.Transparent })
            .then(if (!s.done && !(isNext && canTick)) Modifier.border(1.5.dp, Jk.Line, CircleShape) else Modifier),
            contentAlignment = Alignment.Center) {
            if (s.done || (isNext && canTick)) Icon(Icons.Filled.Check, null, Modifier.size(22.dp), tint = if (s.done) Jk.Black else Color.White)
        }
    }
}

// ---------- one exercise's how-to page ----------

/** Level, equipment and the muscles it works, as chips: the main muscles filled, the helpers outlined. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseFacts(ex: Exercise) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FactChip(ex.level.replaceFirstChar(Char::uppercase), Jk.Yellow, Jk.Black)
            FactChip(ex.equipment.replaceFirstChar(Char::uppercase), Jk.Well, Jk.Ink)
            ex.mechanic?.let { FactChip(it.replaceFirstChar(Char::uppercase), Jk.Well, Jk.Ink) }
        }
        Text("WORKS", style = plex(11.sp, FontWeight.Bold, tracking = 1.sp), color = Jk.Muted)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ex.primary.forEach { FactChip(it.replaceFirstChar(Char::uppercase), Jk.Red, Color.White) }
            ex.secondary.forEach { m ->
                Text(m.replaceFirstChar(Char::uppercase), Modifier.clip(RoundedCornerShape(50)).border(1.5.dp, Jk.Line, RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 6.dp), style = plex(13.sp, FontWeight.SemiBold), color = Jk.Ink)
            }
        }
    }
}

@Composable
private fun FactChip(text: String, fill: Color, ink: Color) {
    Text(text, Modifier.clip(RoundedCornerShape(50)).background(fill).padding(horizontal = 12.dp, vertical = 6.dp),
        style = plex(13.sp, FontWeight.SemiBold), color = ink)
}

/**
 * One how-to step cut down to the action: its first sentence, without "Begin to…"-style openers or the "as you…"
 * and, when it's still long, the rest after the last natural break ("Begin to slowly lower the bar by bending the
 * knees and hips as you maintain…" becomes "Slowly lower the bar by bending the knees and hips"). Null for steps that say nothing ("Repeat for the
 * recommended amount of repetitions").
 */
internal fun shortStep(step: String): String? {
    if (Regex("^(repeat|perform|do)\\b.*(recommended|desired|prescribed)", RegexOption.IGNORE_CASE).containsMatchIn(step.trim())) return null
    var t = step.trim().split(Regex("(?<=[.!?])\\s+")).first().trimEnd('.', '!', ' ')
    t = t.replace(Regex("^(now|then|next|finally|slowly begin to|begin to|start to|start by|begin by|proceed to)\\s+", RegexOption.IGNORE_CASE), "")
    // Short enough to take in at a glance: cut at the last natural break before MAX_STEP characters.
    if (t.length > MAX_STEP) {
        val breaks = Regex("\\s(as you|while|making sure|so that|in order to|by|using|with|and|at the same time|for safety)\\b|[,;:]",
            RegexOption.IGNORE_CASE).findAll(t).map { it.range.first }.filter { it in MIN_STEP..MAX_STEP }.toList()
        breaks.lastOrNull()?.let { t = t.substring(0, it) }
    }
    return t.trim().trimEnd(',').replaceFirstChar { it.uppercase() }
}

/**
 * The how-to as numbered steps on a line, each cut to its action (see [shortStep]); tap a step for the whole
 * sentence from the exercise library.
 */
private const val MIN_STEP = 18
private const val MAX_STEP = 60

@Composable
fun HowToSteps(steps: List<String>) {
    val shown = steps.mapNotNull { full -> shortStep(full)?.let { it to full } }
    if (shown.isEmpty()) return
    Column {
        Text("HOW TO", style = plex(11.sp, FontWeight.Bold, tracking = 1.sp), color = Jk.Muted)
        Spacer(Modifier.height(8.dp))
        shown.forEachIndexed { i, (short, full) ->
            var open by rememberSaveable(i) { mutableStateOf(false) }
            val more = full.trim().trimEnd('.') != short
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .clickable(enabled = more, onClickLabel = if (open) "Show less" else "Show the whole step") { open = !open }
                .padding(vertical = 7.dp), verticalAlignment = Alignment.Top) {
                Box(Modifier.size(26.dp).clip(CircleShape).background(Jk.Ink), contentAlignment = Alignment.Center) {
                    Text("${i + 1}", style = plex(12.sp, FontWeight.Bold), color = Jk.OnInk)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f).padding(top = 2.dp)) {
                    Text(short, style = plex(15.sp, FontWeight.SemiBold, line = 20.sp), color = Jk.Ink)
                    if (open) Text(full, Modifier.padding(top = 4.dp), style = plex(13.sp, line = 18.sp), color = Jk.Muted)
                }
                if (more) Icon(if (open) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, if (open) "Less" else "More",
                    Modifier.size(20.dp), tint = Jk.Muted)
            }
        }
    }
}
