package com.barathiraja.jk

import com.barathiraja.jk.data.SetSpec
import com.barathiraja.jk.gym.AssignStatus
import com.barathiraja.jk.gym.AssignedExercise
import com.barathiraja.jk.gym.Assignment
import com.barathiraja.jk.gym.OwnerStats
import com.barathiraja.jk.gym.Person
import com.barathiraja.jk.gym.PersonStatus
import com.barathiraja.jk.gym.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class OwnerStatsTest {
    private val month = YearMonth.of(2026, 10)
    private fun day(d: Int) = LocalDate.of(2026, 10, d).toEpochDay()
    private val windowStart = LocalDate.of(2026, 9, 1).toEpochDay()
    private val today = day(20)

    private var n = 0
    private fun a(member: String, epochDay: Long, done: Boolean, trainer: String = "t1") = Assignment(
        id = "a${n++}", trainerUid = trainer, memberUid = member, title = "W", epochDay = epochDay,
        exercises = listOf(AssignedExercise("Pushups", "CHEST", listOf(SetSpec(10, 0f, done = done)))),
        status = if (done) AssignStatus.DONE else AssignStatus.ASSIGNED,
    )

    private fun member(uid: String, trainer: String = "t1", status: PersonStatus = PersonStatus.ACTIVE) =
        Person(uid, uid, null, Role.MEMBER, status, trainerUid = trainer)
    private fun trainer(uid: String) = Person(uid, uid, null, Role.TRAINER, PersonStatus.ACTIVE)

    private fun digest(people: List<Person>, list: List<Assignment>) = OwnerStats.digest(people, list, today, month, windowStart)

    @Test fun turnoutCountsMembersNotWorkouts() {
        val d = digest(listOf(member("m1"), member("m2"), trainer("t1")),
            listOf(a("m1", today, true), a("m1", today, true), a("m2", today, false)))
        assertEquals(2, d.members)
        assertEquals(1, d.trainedToday)
        assertEquals(2, d.assignedToday)
    }

    @Test fun memberIdleSevenDaysIsListedLongestFirst() {
        val d = digest(listOf(member("recent"), member("week"), member("long"), trainer("t1")), listOf(
            a("recent", today - 2, true),
            a("week", today - 7, true), a("week", today - 3, false),
            a("long", today - 15, true),
        ))
        assertEquals(listOf("long", "week"), d.idle.map { it.member.uid })
        val week = d.idle.last()
        assertEquals(7, week.days)
        assertTrue(week.assignedRecently)
        // Nothing assigned to "long" in the last 7 days: the trainer needs the nudge.
        assertFalse(d.idle.first().assignedRecently)
    }

    @Test fun newMemberIsNotIdleBeforeAWeek() {
        val joined = LocalDate.ofEpochDay(today - 3).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val d = digest(listOf(member("m1").copy(joinedAt = joined), trainer("t1")), emptyList())
        assertTrue(d.idle.isEmpty())
    }

    @Test fun removedMembersAreIgnored() {
        val d = digest(listOf(member("gone", status = PersonStatus.REMOVED), trainer("t1")), listOf(a("gone", today, true)))
        assertEquals(0, d.members)
        assertEquals(0, d.trainedToday)
        assertTrue(d.idle.isEmpty())
    }

    @Test fun trainersWithQuietMembersComeFirst() {
        val d = digest(listOf(trainer("good"), trainer("slack"), member("a", "good"), member("b", "slack")), listOf(
            a("a", today, true, "good"), a("b", today - 10, true, "slack"),
        ))
        assertEquals(listOf("slack", "good"), d.trainers.map { it.trainer.uid })
        assertEquals(1, d.trainers.first().idle)
        assertEquals(1f, d.trainers.last().rate)
    }
}
