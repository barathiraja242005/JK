package com.barathiraja.jk.ui.gym

import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.barathiraja.jk.gym.AssignStatus
import com.barathiraja.jk.gym.Assignment
import com.barathiraja.jk.gym.OwnerStats
import com.barathiraja.jk.gym.Person
import com.barathiraja.jk.gym.Role
import com.barathiraja.jk.ui.GymViewModel
import com.barathiraja.jk.ui.Routes
import com.barathiraja.jk.ui.components.shareText
import androidx.compose.runtime.remember
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.barathiraja.jk.ui.components.Heading
import com.barathiraja.jk.ui.components.JkPage
import com.barathiraja.jk.ui.components.PageTitle
import com.barathiraja.jk.ui.components.RedButton
import com.barathiraja.jk.ui.theme.Jk
import com.barathiraja.jk.ui.theme.Tone
import com.barathiraja.jk.ui.theme.plex
import com.barathiraja.jk.gym.plural

/*
 * The trainer's app, laid out like the owner's: Home answers "how are my members doing today and what do I need to
 * do?", Members is the ranked list with the member code, and every action lives in one place.
 */

/**
 * Trainer Home: today's card, the one main action (assign a workout), the members who need the trainer (finished
 * workouts to check, members with no plan coming, members who stopped training), and today's workouts.
 */
@Composable
fun TrainerHomeScreen(gvm: GymViewModel, nav: NavHostController) {
    val me by gvm.me.collectAsStateWithLifecycle()
    val gym by gvm.gym.collectAsStateWithLifecycle()
    val people by gvm.people.collectAsStateWithLifecycle()
    val assignments by gvm.assignments.collectAsStateWithLifecycle()
    val scores by gvm.monthScores.collectAsStateWithLifecycle()
    val digest by gvm.ownerDigest.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showAllNeeds by rememberSaveable { mutableStateOf(false) }
    var showAllToday by rememberSaveable { mutableStateOf(false) }
    val trainer = me ?: return
    val today = gvm.today

    val members = people.filter { it.role == Role.MEMBER && it.active && it.trainerUid == trainer.uid }
    val byUid = members.associateBy { it.uid }
    val mine = assignments.filter { it.trainerUid == trainer.uid && it.memberUid in byUid }
    val todays = mine.filter { it.epochDay == today }
    val month = members.mapNotNull { scores[it.uid] }
    val needs = trainerNeeds(mine, members, digest?.idle.orEmpty(), today, gvm, nav, context)

    JkPage {
        item { GreetingHeader(trainer) }
        item {
            TodayHero(
                label = "Today at ${gym?.name ?: "the gym"}",
                done = todays.count { it.done }, total = todays.size,
                unit = if (todays.size == 1) "workout done" else "workouts done",
                compare = if (todays.isEmpty()) AnnotatedString("No workouts given for today yet.")
                    else comparedWithYesterday(todays.count { it.done }, mine.count { it.epochDay == today - 1 && it.done },
                        todays.count { it.status == AssignStatus.IN_PROGRESS }.takeIf { it > 0 }?.let { "$it training now" }),
                monthDone = month.sumOf { it.completed }, monthDue = month.sumOf { it.due },
            )
        }
        item {
            RedButton("Assign a workout", { nav.navigate(Routes.assign("")) }, Modifier.fillMaxWidth(), Icons.Outlined.Add,
                enabled = members.isNotEmpty())
        }
        item {
            MemberCodesCard(listOf(MemberCode(trainer, trainer.trainerCode.orEmpty())), gym?.name ?: "the gym", gvm, mine = true)
        }

        item {
            Heading("Needs you", if (needs.isEmpty()) null else
                "${if (needs.size == 1) "1 thing" else "${needs.size} things"} to check or sort out.")
        }
        item {
            when {
                members.isEmpty() -> AllClearCard("No members yet. Share your member code above.")
                needs.isEmpty() -> AllClearCard("Every finished workout is checked and every member has a plan.")
                else -> NeedsList(needs, showAllNeeds) { showAllNeeds = !showAllNeeds }
            }
        }

        if (todays.isNotEmpty()) {
            item { Heading("Today's workouts", "Tap a member to see their sets.") }
            item {
                val rows = todays.sortedBy { if (it.done) 2 else if (it.status == AssignStatus.IN_PROGRESS) 0 else 1 }.mapNotNull { a ->
                    val m = byUid[a.memberUid] ?: return@mapNotNull null
                    val (word, tone) = when {
                        a.done -> (if (a.verified) "Done, checked" else "Done") to Tone.GOOD
                        a.status == AssignStatus.IN_PROGRESS -> "In progress · ${a.exercisesDone} of ${a.exercises.size}" to Tone.WARN
                        else -> "Not started" to Tone.NONE
                    }
                    NeedRow("t" + a.id, m, word, tone, a.title, onOpen = { nav.navigate(Routes.gymMember(m.uid)) })
                }
                NeedsList(rows, showAllToday) { showAllToday = !showAllToday }
            }
        }
    }
}

/**
 * What needs the trainer, most useful first: finished workouts from the last week waiting to be checked (one tap),
 * members with no workout coming up (assign one), and members who stopped training (message them).
 */
@Composable
private fun trainerNeeds(
    mine: List<Assignment>, members: List<Person>, idle: List<OwnerStats.Idle>, today: Long,
    gvm: GymViewModel, nav: NavHostController, context: Context,
): List<NeedRow> {
    val byUid = members.associateBy { it.uid }
    val toCheck = mine.filter { it.done && !it.verified && it.epochDay in (today - CHECK_DAYS + 1)..today }
        .sortedByDescending { it.epochDay }
        .mapNotNull { a ->
            val m = byUid[a.memberUid] ?: return@mapNotNull null
            NeedRow("v" + a.id, m, "Finished", Tone.GOOD, "${a.title} · ${dayLabel(a.epochDay, today)}",
                onOpen = { nav.navigate(Routes.gymMember(m.uid)) }) {
                SmallAction(Icons.Outlined.Check, "Mark ${m.firstName}'s workout as checked", red = false) { gvm.verify(a, true, a.trainerNote) }
            }
        }
    val mineIdle = idle.filter { it.member.uid in byUid }
    val noPlan = mineIdle.filter { !it.assignedRecently }.map { i ->
        val m = i.member
        NeedRow("p" + m.uid, m, "No plan", Tone.WARN, "Nothing given for ${daysText(i.days)}", onOpen = { nav.navigate(Routes.gymMember(m.uid)) }) {
            SmallPill("Assign") { nav.navigate(Routes.assign(m.uid)) }
        }
    }
    val away = mineIdle.filter { it.assignedRecently }.map { i ->
        val m = i.member
        NeedRow("a" + m.uid, m, "Away", Tone.BAD, "No workout in ${daysText(i.days)}", onOpen = { nav.navigate(Routes.gymMember(m.uid)) }) {
            SmallAction(Icons.AutoMirrored.Outlined.Send, "Message ${m.firstName}", red = false) {
                context.whatsApp("Hi ${m.firstName}, I haven't seen you train for a while. Your workout is ready in the JK app. See you soon! 💪")
            }
        }
    }
    return toCheck + noPlan + away
}

private fun daysText(days: Int) = if (days > 30) "30+ days" else plural(days, "day")

/** Finished workouts this many days back still show up to be checked. */
private const val CHECK_DAYS = 7

/** The trainer's members, best this month first, and the code new members join with (also on Home). */
@Composable
fun TrainerMembersScreen(gvm: GymViewModel, nav: NavHostController) {
    val me by gvm.me.collectAsStateWithLifecycle()
    val gym by gvm.gym.collectAsStateWithLifecycle()
    val people by gvm.people.collectAsStateWithLifecycle()
    val ranking by gvm.memberRanking.collectAsStateWithLifecycle()
    val digest by gvm.ownerDigest.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val trainer = me ?: return
    val idle = digest?.idle.orEmpty().associateBy { it.member.uid }
    val mine = ranking.mapNotNull { s -> people.firstOrNull { it.uid == s.uid && it.active && it.trainerUid == trainer.uid }?.let { it to s } }
    val code = trainer.trainerCode.orEmpty()
    var newCode by remember { mutableStateOf(false) }
    if (newCode) AlertDialog(onDismissRequest = { newCode = false },
        title = { Text("Make a new member code?") },
        text = { Text("New members will need the new code. $code stops working, so someone who has it can't join " +
            "anymore. Members who already joined stay with you.", style = plex(15.sp, line = 21.sp)) },
        confirmButton = { TextButton(onClick = { newCode = false; gvm.newMemberCode() }) {
            Text("Make new code", style = plex(14.sp, FontWeight.SemiBold), color = Jk.RedText) } },
        dismissButton = { TextButton(onClick = { newCode = false }) { Text("Cancel", style = plex(14.sp, FontWeight.SemiBold), color = Jk.Ink) } })
    val codeCard: LazyListScope.() -> Unit = {
        item { Heading("Add a member", "New members sign in to JK, choose \"I'm a member\" and type this code.") }
        item {
            CodeHeroCard("Your member code", "Members join you with", code, onRename = null,
                onShare = { context.shareText("Join me on the JK app at ${gym?.name ?: "the gym"}! Open JK → Sign in with Google → I'm a member → enter code $code") },
                onCopy = { copyText(context, "Member code", code); gvm.showMessage("Member code copied") },
                onNewCode = { newCode = true })
        }
    }

    JkPage {
        item { PageTitle("Members", "Your members, best this month first. Tap someone to assign, check or message.") }
        if (mine.isEmpty()) codeCard()
        else {
            leaderboard(mine.mapIndexed { i, (p, s) ->
                memberEntry(p, s, top = i == 0 && s.points > 0, idle[p.uid], hasTrainer = true, extra = "${s.points} pts") {
                    nav.navigate(Routes.gymMember(p.uid))
                }
            }, "mine")
            codeCard()
        }
    }
}
