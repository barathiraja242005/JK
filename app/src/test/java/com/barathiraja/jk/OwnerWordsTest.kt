package com.barathiraja.jk

import com.barathiraja.jk.gym.OwnerStats
import com.barathiraja.jk.gym.Person
import com.barathiraja.jk.gym.PersonStatus
import com.barathiraja.jk.gym.Role
import com.barathiraja.jk.gym.Scoring
import com.barathiraja.jk.ui.theme.Tone
import com.barathiraja.jk.ui.gym.memberStatus
import com.barathiraja.jk.ui.gym.trainerStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import com.barathiraja.jk.gym.plural
import com.barathiraja.jk.gym.tidyName

/** The words the owner reads: names, plurals and the status chips on people. */
class OwnerWordsTest {
    private fun person(name: String, role: Role = Role.MEMBER) = Person("u", name, null, role, PersonStatus.ACTIVE)
    private fun score(due: Int, completed: Int, points: Int = completed * 10) = Scoring.MemberScore("u", points, due, completed, 0, 0.0)
    private fun trainer(rate: Float, due: Int) = OwnerStats.TrainerRow(person("T", Role.TRAINER), members = 3, rate = rate, due = due, idle = 0)

    @Test fun tidyNameCleansGoogleNames() {
        assertEquals("S. Janarthanan", tidyName("_S. Janarthanan_"))
        assertEquals("Koundar Barathiraja", tidyName("KOUNDAR BARATHIRAJA"))
        assertEquals("Barathiraja K", tidyName("Barathiraja K 2023-2027"))
        assertEquals("Ravi Kumar", tidyName("  Ravi   Kumar "))
    }

    @Test fun firstNameSkipsInitials() {
        assertEquals("Janarthanan", person("S. Janarthanan").firstName)
        assertEquals("Ravi", person("Ravi Kumar").firstName)
        assertEquals("K", person("K").firstName)
    }

    @Test fun pluralUsesTheRightWord() {
        assertEquals("1 member", plural(1, "member"))
        assertEquals("0 members", plural(0, "member"))
        assertEquals("3 members", plural(3, "member"))
    }

    @Test fun memberStatusPicksOneClearWord() {
        assertEquals("No trainer" to Tone.BAD, memberStatus(score(3, 3), top = true, idle = null, hasTrainer = false))
        assertEquals("Top member" to Tone.TOP, memberStatus(score(3, 3), top = true, idle = null, hasTrainer = true))
        val away = OwnerStats.Idle(person("M"), days = 9, assignedRecently = true)
        assertEquals("Away" to Tone.BAD, memberStatus(score(3, 0), top = false, idle = away, hasTrainer = true))
        val noPlan = away.copy(assignedRecently = false)
        assertEquals("No plan" to Tone.WARN, memberStatus(score(0, 0), top = false, idle = noPlan, hasTrainer = true))
        assertEquals("No workouts yet" to Tone.NONE, memberStatus(score(0, 0), top = false, idle = null, hasTrainer = true))
        assertEquals("Doing well" to Tone.GOOD, memberStatus(score(10, 7), top = false, idle = null, hasTrainer = true))
        assertEquals("Could do better" to Tone.WARN, memberStatus(score(10, 4), top = false, idle = null, hasTrainer = true))
        assertEquals("Falling behind" to Tone.BAD, memberStatus(score(10, 3), top = false, idle = null, hasTrainer = true))
    }

    @Test fun trainerStatusFollowsTheirMembersWorkouts() {
        assertEquals("Top trainer" to Tone.TOP, trainerStatus(trainer(0.2f, 10), top = true))
        assertEquals("No workouts yet" to Tone.NONE, trainerStatus(trainer(0f, 0), top = false))
        assertEquals("Doing well" to Tone.GOOD, trainerStatus(trainer(0.7f, 10), top = false))
        assertEquals("Could do better" to Tone.WARN, trainerStatus(trainer(0.4f, 10), top = false))
        assertEquals("Falling behind" to Tone.BAD, trainerStatus(trainer(0.39f, 10), top = false))
    }
}
