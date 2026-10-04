package com.barathiraja.jk.ui.gym

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.barathiraja.jk.gym.PersonStatus
import com.barathiraja.jk.gym.Role
import com.barathiraja.jk.ui.GymViewModel
import com.barathiraja.jk.ui.Routes
import com.barathiraja.jk.ui.components.JkCard
import com.barathiraja.jk.ui.components.SectionTitle
import com.barathiraja.jk.ui.screens.Avatar
import com.barathiraja.jk.ui.screens.BackScreen
import com.barathiraja.jk.ui.screens.TabScreen
import com.barathiraja.jk.ui.theme.Leaf

/** Owner's home: one glance at today, trainers waiting for approval, and every trainer's performance. */
@Composable
fun OwnerHomeScreen(gvm: GymViewModel, nav: NavHostController) {
    val gym by gvm.gym.collectAsStateWithLifecycle()
    val people by gvm.people.collectAsStateWithLifecycle()
    val assignments by gvm.assignments.collectAsStateWithLifecycle()
    val trainers by gvm.trainerRanking.collectAsStateWithLifecycle()
    val me by gvm.me.collectAsStateWithLifecycle()
    val today = gvm.today

    val members = people.filter { it.role == Role.MEMBER && it.active }
    val todays = assignments.filter { it.epochDay == today }
    val trainedToday = todays.filter { it.done }.map { it.memberUid }.toSet()
    val pending = people.filter { it.role == Role.TRAINER && it.status == PersonStatus.PENDING }

    TabScreen(gym?.name ?: "My gym", subtitle = "Owner", action = { me?.let { Avatar(it.photoUrl, it.name, 44.dp) { nav.navigate(Routes.PROFILE) } } }) {
        item {
            JkCard(Modifier.fillMaxWidth()) {
                Text("TODAY", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${trainedToday.size}", fontSize = 56.sp, style = MaterialTheme.typography.displayMedium, color = Leaf)
                    Text(" of ${members.size}", style = MaterialTheme.typography.headlineSmall)
                }
                Text("members finished their workout", style = MaterialTheme.typography.titleMedium)
                Text("${todays.size} workouts assigned for today", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (pending.isNotEmpty()) {
            item { SectionTitle("Waiting for your approval") }
            items(pending, key = { "p" + it.uid }) { p ->
                JkCard(Modifier.fillMaxWidth()) {
                    PersonRow(p, size = 52.dp, sub = { Text("wants to join as a trainer", color = MaterialTheme.colorScheme.onSurfaceVariant) })
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { gvm.approve(p) }, Modifier.weight(1f).height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Leaf)) { Text("Approve", style = MaterialTheme.typography.titleMedium) }
                        OutlinedButton(onClick = { gvm.reject(p) }, Modifier.weight(1f).height(56.dp)) { Text("Reject", style = MaterialTheme.typography.titleMedium) }
                    }
                }
            }
        }

        item { SectionTitle("Trainers this month") }
        if (trainers.isEmpty()) {
            item { Text("No trainers yet. Share the gym code below with your trainers.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        item {
            if (trainers.isNotEmpty()) JkCard(Modifier.fillMaxWidth()) {
                Row {
                    Text("Trainer", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                    Text("Members", Modifier.width(76.dp), style = MaterialTheme.typography.labelLarge)
                    Text("Done", Modifier.width(56.dp), style = MaterialTheme.typography.labelLarge)
                }
                trainers.forEachIndexed { i, t ->
                    val p = gvm.person(t.uid) ?: return@forEachIndexed
                    if (i > 0) HorizontalDivider()
                    Row(Modifier.fillMaxWidth().height(64.dp).clickable { nav.navigate(Routes.gymTrainer(t.uid)) },
                        verticalAlignment = Alignment.CenterVertically) {
                        Avatar(p.photoUrl, p.name, 36.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(p.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, maxLines = 1)
                        Text("${t.members}", Modifier.width(76.dp), style = MaterialTheme.typography.titleMedium)
                        Text(if (t.members == 0) "–" else "${Math.round(t.rate * 100)}%", Modifier.width(56.dp),
                            style = MaterialTheme.typography.titleMedium, color = rateColor(t.rate, t.members))
                    }
                }
            }
        }

        gym?.let { g ->
            item { CodeCard("Gym code for trainers", g.gymCode,
                "Join ${g.name} as a trainer on the JK app. Open JK → Sign in → I'm a trainer → enter code ${g.gymCode}") }
        }
    }
}

/** One trainer's members and how each did today, for the owner. */
@Composable
fun TrainerDetailScreen(uid: String, gvm: GymViewModel, nav: NavHostController) {
    val people by gvm.people.collectAsStateWithLifecycle()
    val assignments by gvm.assignments.collectAsStateWithLifecycle()
    val scores by gvm.monthScores.collectAsStateWithLifecycle()
    val trainer = people.firstOrNull { it.uid == uid }
    val members = people.filter { it.role == Role.MEMBER && it.active && it.trainerUid == uid }
    val today = gvm.today
    BackScreen(trainer?.name ?: "Trainer", onBack = { nav.popBackStack() }) {
        item {
            val todays = assignments.filter { it.trainerUid == uid && it.epochDay == today }
            JkCard(Modifier.fillMaxWidth()) {
                Text("Today: ${todays.count { it.done }} of ${todays.size} workouts done", style = MaterialTheme.typography.titleLarge)
                Text("${members.size} members", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(members, key = { it.uid }) { m ->
            val a = assignments.filter { it.memberUid == m.uid && it.epochDay == today }.minByOrNull { if (it.done) 1 else 0 }
            val s = scores[m.uid]
            JkCard(Modifier.fillMaxWidth(), onClick = { nav.navigate(Routes.gymMember(m.uid)) }) {
                PersonRow(m, sub = {
                    Text("This month ${pct(s?.completed ?: 0, s?.due ?: 0)} · ${s?.points ?: 0} pts",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }, trailing = { StatusPill(a, today) })
            }
        }
        if (members.isEmpty()) item { Text("No members yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
