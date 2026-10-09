package com.barathiraja.jk.ui.member

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role as SemanticsRole
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.barathiraja.jk.gym.AssignStatus
import com.barathiraja.jk.gym.Assignment
import com.barathiraja.jk.ui.GymViewModel
import com.barathiraja.jk.ui.Routes
import com.barathiraja.jk.ui.components.LocalNavBarInset
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.ui.text.style.TextAlign
import com.barathiraja.jk.ui.components.CardBox
import com.barathiraja.jk.ui.components.Heading
import com.barathiraja.jk.ui.theme.Jk
import com.barathiraja.jk.ui.theme.plex
import com.barathiraja.jk.ui.gym.AssignmentCard
import com.barathiraja.jk.ui.gym.TodayWorkoutCard
import com.barathiraja.jk.ui.gym.dayLabel
import com.barathiraja.jk.ui.gym.look

/**
 * A gym member's training plan: the week of workouts their coach sent, a day picked from the strip, and every
 * past workout below so they can look back at what they lifted. The coach's plan is the only plan here; JK's own
 * generated plan is only offered (by [WorkoutsScreen]) while the coach hasn't sent anything.
 */
@Composable
fun MemberPlanTab(gvm: GymViewModel, nav: NavHostController) {
    val me by gvm.me.collectAsStateWithLifecycle()
    val assignments by gvm.assignments.collectAsStateWithLifecycle()
    val m = me ?: return
    val today = gvm.today
    val mine = assignments.filter { it.memberUid == m.uid }
    val byDay = mine.groupBy { it.epochDay }
    val coach = gvm.trainerOf(m)
    var selected by rememberSaveable { mutableLongStateOf(today) }
    val weekStart = LocalDate.ofEpochDay(selected).with(DayOfWeek.MONDAY).toEpochDay()
    // Only weeks the app has loaded: last month through the next two.
    val first = LocalDate.ofEpochDay(today).minusMonths(1).withDayOfMonth(1).with(DayOfWeek.MONDAY).toEpochDay()
    val last = today + GymViewModel.SCHEDULE_DAYS - 6
    val past = mine.filter { it.epochDay < today }.sortedByDescending { it.epochDay }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 24.dp + LocalNavBarInset.current),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            WeekPicker(weekStart, selected, today, byDay,
                canBack = weekStart - 7 >= first, canForward = weekStart + 7 <= last,
                onWeek = { selected = (weekStart + it).coerceIn(first, last).let { d -> if (today in d..d + 6) today else d } },
                onPick = { selected = it })
        }
        val day = byDay[selected].orEmpty().sortedBy { if (it.done) 1 else 0 }
        if (day.isEmpty()) item { NoWorkoutDay(selected, today, coach?.firstName) }
        items(day, key = { it.id }) { a ->
            TodayWorkoutCard(a, coach?.firstName.orEmpty(), gvm.restSec, today) { nav.navigate(Routes.assigned(a.id)) }
        }
        if (past.isNotEmpty()) {
            item { Heading("Past workouts", "Tap one to see the sets and weights you did.") }
            items(past, key = { "p" + it.id }) { a -> AssignmentCard(a, today, onClick = { nav.navigate(Routes.assigned(a.id)) }) }
        }
    }
}

private val monthDay = DateTimeFormatter.ofPattern("d MMM")

/** Mon–Sun of one week with a mark under each day that has a workout; arrows step a week back or forward. */
@Composable
private fun WeekPicker(
    weekStart: Long, selected: Long, today: Long, byDay: Map<Long, List<Assignment>>,
    canBack: Boolean, canForward: Boolean, onWeek: (Int) -> Unit, onPick: (Long) -> Unit,
) {
    CardBox(padding = 10.dp) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onWeek(-7) }, enabled = canBack) {
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Previous week", tint = if (canBack) Jk.Ink else Jk.Line)
                }
                val label = when (weekStart) {
                    LocalDate.ofEpochDay(today).with(DayOfWeek.MONDAY).toEpochDay() -> "This week"
                    LocalDate.ofEpochDay(today).with(DayOfWeek.MONDAY).toEpochDay() + 7 -> "Next week"
                    LocalDate.ofEpochDay(today).with(DayOfWeek.MONDAY).toEpochDay() - 7 -> "Last week"
                    else -> LocalDate.ofEpochDay(weekStart).format(monthDay) + " – " + LocalDate.ofEpochDay(weekStart + 6).format(monthDay)
                }
                Text(label, Modifier.weight(1f), style = plex(15.sp, FontWeight.SemiBold), color = Jk.Ink,
                    textAlign = TextAlign.Center)
                IconButton(onClick = { onWeek(7) }, enabled = canForward) {
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Next week", tint = if (canForward) Jk.Ink else Jk.Line)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                (weekStart..weekStart + 6).forEach { d -> DayCell(d, d == selected, d == today, byDay[d].orEmpty(), today) { onPick(d) } }
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp), horizontalArrangement = Arrangement.Center) {
                Legend(Jk.Yellow, "Done"); Legend(Jk.Ink, "To do"); Legend(Jk.RedText, "Missed")
            }
        }
    }
}

@Composable
private fun DayCell(day: Long, isSelected: Boolean, isToday: Boolean, list: List<Assignment>, today: Long, onClick: () -> Unit) {
    val date = LocalDate.ofEpochDay(day)
    val mark: Color? = when {
        list.isEmpty() -> null
        list.all { it.done } -> Jk.Yellow
        day < today && list.none { it.status == AssignStatus.IN_PROGRESS } -> Jk.RedText
        else -> Jk.Ink
    }
    val words = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()) +
        when { mark == null -> ", no workout"; list.all { it.done } -> ", done"; day < today -> ", missed"; else -> ", workout" }
    Column(
        Modifier.size(width = 42.dp, height = 70.dp).clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) Jk.Ink else Color.Transparent)
            .clickable(role = SemanticsRole.Tab, onClick = onClick)
            .semantics { this.selected = isSelected; contentDescription = words },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        val fg = if (isSelected) Jk.OnInk else Jk.Ink
        Text(date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2), style = plex(12.sp, FontWeight.Medium),
            color = if (isSelected) fg else if (isToday) Jk.RedText else Jk.Muted)
        Text("${date.dayOfMonth}", style = plex(17.sp, FontWeight.Bold), color = fg)
        Spacer(Modifier.height(4.dp))
        Box(Modifier.size(14.dp), contentAlignment = Alignment.Center) {
            when (mark) {
                null -> {}
                Jk.Yellow -> Box(Modifier.size(14.dp).clip(CircleShape).background(Jk.Yellow), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Check, null, Modifier.size(10.dp), tint = Jk.Black)
                }
                else -> Box(Modifier.size(7.dp).clip(CircleShape).background(if (isSelected && mark == Jk.Ink) Jk.OnInk else mark))
            }
        }
    }
}

@Composable
private fun Legend(color: Color, text: String) {
    Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        Text(text, Modifier.padding(start = 5.dp), style = plex(12.sp), color = Jk.Muted)
    }
}

/** A day the coach sent nothing for: say so plainly, and for today, that rest is part of training. */
@Composable
private fun NoWorkoutDay(day: Long, today: Long, coach: String?) {
    val who = coach?.let { "Coach $it" } ?: "Your coach"
    CardBox(Modifier.heightIn(min = 96.dp)) {
        Column {
            val label = dayLabel(day, today)
            Text(when {
                day == today -> "Rest day"
                day == today + 1 || day == today - 1 -> "No workout ${label.lowercase()}"
                else -> "No workout on $label"
            },
                style = plex(17.sp, FontWeight.Bold), color = Jk.Ink)
            Text(when {
                day == today -> "$who hasn't set a workout for today. Recovery is part of training: stretch, walk, drink water."
                day > today -> "$who hasn't set one yet. New workouts show up here as soon as they're sent."
                else -> "Nothing was set for this day."
            }, Modifier.padding(top = 4.dp), style = plex(15.sp, line = 21.sp), color = Jk.Muted)
        }
    }
}
