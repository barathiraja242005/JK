package com.barathiraja.jk

import com.barathiraja.jk.gym.AssignStatus
import com.barathiraja.jk.gym.PersonStatus
import com.barathiraja.jk.gym.Role
import com.barathiraja.jk.gym.TestGym
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** The made-up gym has every kind of person and workout the screens need to show, and its actions work. */
class TestGymTest {
    private val today = LocalDate.of(2026, 10, 8)
    private val gym = TestGym(today)
    private val id = TestGym.GYM_ID

    @Test fun seedHasEveryKindOfPerson() = runBlocking {
        val people = gym.people(id).first()
        assertEquals(1, people.count { it.role == Role.OWNER })
        assertEquals(5, people.count { it.role == Role.TRAINER && it.active })
        assertEquals(1, people.count { it.role == Role.TRAINER && it.status == PersonStatus.PENDING })
        assertTrue(people.count { it.role == Role.MEMBER && it.active } >= 35)
        assertNull(gym.userGymId(TestGym.NEWCOMER_UID).first())
    }

    @Test fun seedHasPastTodayAndFutureWorkouts() = runBlocking {
        val all = gym.assignments(id, 0, Long.MAX_VALUE).first()
        val day = today.toEpochDay()
        assertTrue(all.any { it.epochDay < day && it.done && it.verified })
        assertTrue(all.any { it.epochDay < day && !it.done })
        assertTrue(all.any { it.epochDay == day && it.memberUid == "m-0" && it.status == AssignStatus.ASSIGNED })
        assertTrue(all.any { it.epochDay > day })
        assertTrue(gym.awards(id).first().size == 2)
    }

    @Test fun newcomerJoinsWithATrainerCode() = runBlocking {
        gym.joinAsMember("arjun7", TestGym.NEWCOMER_UID, "Nila", null)
        assertEquals(id, gym.userGymId(TestGym.NEWCOMER_UID).first())
        assertEquals("t-arjun", gym.person(id, TestGym.NEWCOMER_UID).first()?.trainerUid)
    }
}
