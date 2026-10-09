package com.barathiraja.jk

import com.barathiraja.jk.data.SetSpec
import com.barathiraja.jk.gym.AssignStatus
import com.barathiraja.jk.gym.AssignedExercise
import com.barathiraja.jk.gym.Assignment
import com.barathiraja.jk.gym.Award
import com.barathiraja.jk.gym.Person
import com.barathiraja.jk.gym.PersonStatus
import com.barathiraja.jk.gym.Role
import com.barathiraja.jk.gym.Scoring
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import com.barathiraja.jk.gym.tidyName

class ScoringTest {
    private val utc = ZoneOffset.UTC
    private val month = YearMonth.of(2026, 9)
    private val range = Scoring.monthRange(month)
    private fun day(d: Int) = LocalDate.of(2026, 9, d).toEpochDay()
    private fun noonOf(epochDay: Long) = epochDay * 86_400_000L + 43_200_000L

    private var n = 0
    private fun a(member: String, d: Int, done: Boolean, sets: Int = 3, doneSets: Int = if (done) sets else 0,
                  lateBy: Int = 0, verified: Boolean = false, trainer: String = "t1", weight: Float = 20f) = Assignment(
        id = "a${n++}", trainerUid = trainer, memberUid = member, title = "W", epochDay = day(d),
        exercises = listOf(AssignedExercise("Pushups", "CHEST", List(sets) { SetSpec(10, weight, done = it < doneSets) })),
        status = if (done) AssignStatus.DONE else AssignStatus.ASSIGNED,
        completedAt = if (done) noonOf(day(d) + lateBy) else null, verified = verified,
    )

    private fun member(uid: String, trainer: String = "t1") = Person(uid, uid, null, Role.MEMBER, PersonStatus.ACTIVE, trainerUid = trainer)
    private fun trainer(uid: String) = Person(uid, uid, null, Role.TRAINER, PersonStatus.ACTIVE)

    @Test fun completedOnTimeWorkoutScores() {
        // 10 complete + 5 on time + 3 sets; the week isn't over yet, so no perfect-week bonus.
        val s = Scoring.memberScores(listOf(a("m", 1, true)), range, day(1), utc).getValue("m")
        assertEquals(18, s.points)
        assertEquals(1f, s.rate)
    }

    @Test fun perfectWeekBonusOnlyAfterTheWeekEnds() {
        // 1–2 Sep (Tue, Wed) both done; on Mon 7 Sep that week is over: 2 × 18 + 20.
        val list = listOf(a("m", 1, true), a("m", 2, true))
        assertEquals(36, Scoring.memberScores(list, range, day(6), utc).getValue("m").points)
        assertEquals(56, Scoring.memberScores(list, range, day(7), utc).getValue("m").points)
    }

    @Test fun lateCompletionLosesOnTimeBonus() {
        val s = Scoring.memberScores(listOf(a("m", 1, true, lateBy = 1)), range, day(1), utc).getValue("m")
        assertEquals(13, s.points) // 10 complete + 3 sets, no on-time bonus
    }

    @Test fun setPointsAreCappedPerDay() {
        val s = Scoring.memberScores(listOf(a("m", 1, false, sets = 50, doneSets = 50)), range, day(1), utc).getValue("m")
        assertEquals(Scoring.SET_CAP_PER_DAY, s.points)
    }

    @Test fun futureWorkoutsDoNotHurtRate() {
        val s = Scoring.memberScores(listOf(a("m", 1, true), a("m", 20, false)), range, day(10), utc).getValue("m")
        assertEquals(1, s.due)
        assertEquals(1f, s.rate)
    }

    @Test fun todaysUnfinishedWorkoutIsNotCountedYet() {
        val s = Scoring.memberScores(listOf(a("m", 1, true), a("m", 2, false)), range, day(2), utc).getValue("m")
        assertEquals(1, s.due)
        assertEquals(1f, s.rate)
    }

    @Test fun tidiesGoogleNames() {
        assertEquals("S. Janarthanan", tidyName("_S. Janarthanan_"))
        assertEquals("Koundar Barathiraja", tidyName("KOUNDAR  BARATHIRAJA"))
        assertEquals("Ravi Kumar", tidyName("Ravi Kumar"))
        assertEquals("Barathiraja K", tidyName("BARATHIRAJA K 2023-2027"))
        assertEquals("Sanjay M", tidyName("Sanjay M (2021 - 25)"))
    }

    @Test fun firstNameSkipsInitials() {
        fun p(name: String) = Person("u", name, null, Role.MEMBER, PersonStatus.ACTIVE)
        assertEquals("Janarthanan", p("S. Janarthanan").firstName)
        assertEquals("Ravi", p("Ravi Kumar").firstName)
        assertEquals("K", p("K").firstName)
        assertEquals("Barathiraja", p("S.Barathiraja").firstName)
    }

    @Test fun missedWorkoutBreaksPerfectWeek() {
        val s = Scoring.memberScores(listOf(a("m", 1, true), a("m", 2, false)), range, day(3), utc).getValue("m")
        assertEquals(10 + 5 + 3, s.points)
        assertEquals(0.5f, s.rate)
    }

    @Test fun trainerScoredOnMemberCompletionNotVolume() {
        val people = listOf(trainer("busy"), trainer("good"), member("m1", "busy"), member("m2", "good"))
        // "busy" sends 6 workouts, 1 done; "good" sends 2, both done.
        val list = (1..6).map { a("m1", it, it == 1, trainer = "busy") } + listOf(a("m2", 1, true, trainer = "good"), a("m2", 2, true, trainer = "good"))
        val ranked = Scoring.trainerScores(people, list, range, day(10))
        assertEquals("good", ranked.first().uid)
        assertEquals(100 + 2, ranked.first().score)
    }

    @Test fun awardsPickWinnersAndSkipUnqualified() {
        val people = listOf(trainer("t1"), member("ace"), member("lazy"))
        val list = (1..10).map { a("ace", it, true) } + (1..10).map { a("lazy", it, it <= 2, weight = 0f) }
        val w = Scoring.awards(people, list, month)
        assertEquals("ace", w[Award.BEST_MEMBER])
        assertEquals("ace", w[Award.MOST_CONSISTENT])
        assertEquals("ace", w[Award.IRON_LIFTER])
        assertEquals("t1", w[Award.BEST_TRAINER])
        assertFalse(Award.MOST_IMPROVED in w) // no previous month data
    }

    @Test fun mostImprovedComparesWithLastMonth() {
        val people = listOf(trainer("t1"), member("m"))
        val aug = (1..4).map { d ->
            a("m", 1, d == 1).copy(id = "aug$d", epochDay = LocalDate.of(2026, 8, d).toEpochDay())
        }
        val sep = (1..4).map { a("m", it, true) }
        assertEquals("m", Scoring.awards(people, aug + sep, month)[Award.MOST_IMPROVED])
    }

    @Test fun removedMembersCannotWin() {
        val people = listOf(trainer("t1"), member("m").copy(status = PersonStatus.REMOVED))
        assertFalse(Award.BEST_MEMBER in Scoring.awards(people, listOf(a("m", 1, true)), month))
    }

    @Test fun workoutStreakSkipsRestDaysAndWaitsForToday() {
        // Done on the 1st, 3rd and 5th (rest days between), today's (7th) not done yet: still a streak of 3.
        val list = listOf(a("m", 1, true), a("m", 3, true), a("m", 5, true), a("m", 7, false))
        assertEquals(3, Scoring.workoutStreak(list, day(7)))
        // Once today's is done it counts.
        assertEquals(4, Scoring.workoutStreak(list.dropLast(1) + a("m", 7, true), day(7)))
        // A missed workout ends it.
        assertEquals(1, Scoring.workoutStreak(listOf(a("m", 1, true), a("m", 3, false), a("m", 5, true)), day(7)))
        assertEquals(0, Scoring.workoutStreak(emptyList(), day(7)))
    }
}
