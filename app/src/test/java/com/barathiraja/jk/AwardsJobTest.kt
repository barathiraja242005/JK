package com.barathiraja.jk

import com.barathiraja.jk.gym.AwardsJob
import com.barathiraja.jk.gym.TestGym
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** The owner's phone saves last month's awards once, after the grace day, and then stops checking. */
class AwardsJobTest {
    private val today = LocalDate.of(2026, 10, 8)
    private var checked: String? = null
    private var clock = 0L
    private fun job() = AwardsJob({ checked }, { checked = it }, { clock })

    @Test fun waitsForTheGraceDay() = runBlocking {
        val gym = TestGym(today)
        job().saveLastMonthIfDue(gym, TestGym.GYM_ID, emptyList(), LocalDate.of(2026, 10, 1))
        assertNull(checked)
    }

    @Test fun savesLastMonthOnceThenRemembers() = runBlocking {
        val gym = TestGym(today)
        val before = gym.awards(TestGym.GYM_ID).first().map { it.month }.toSet()
        val j = job()
        clock = 3_600_000_000L
        j.saveLastMonthIfDue(gym, TestGym.GYM_ID, gym.awards(TestGym.GYM_ID).first(), today)
        assertEquals("2026-09", checked)
        val after = gym.awards(TestGym.GYM_ID).first()
        assertTrue("2026-09" in before || after.any { it.month == "2026-09" })
    }

    @Test fun alreadySavedMonthIsJustRemembered() = runBlocking {
        val gym = TestGym(today)
        val saved = gym.awards(TestGym.GYM_ID).first()
        checked = null
        job().saveLastMonthIfDue(gym, TestGym.GYM_ID, saved + com.barathiraja.jk.gym.MonthAwards("2026-09", emptyMap()), today)
        assertEquals("2026-09", checked)
    }
}
