package com.barathiraja.jk

import com.barathiraja.jk.gym.DemoData
import com.barathiraja.jk.gym.OwnerStats
import com.barathiraja.jk.gym.Person
import com.barathiraja.jk.gym.PersonStatus
import com.barathiraja.jk.gym.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

class DemoDataTest {
    private val today = LocalDate.of(2026, 10, 20).toEpochDay()
    private val owner = Person("owner", "Owner", null, Role.OWNER, PersonStatus.ACTIVE)
    private val gym = DemoData.build(owner, today, ZoneOffset.UTC)
    private val digest = OwnerStats.digest(gym.people, gym.assignments, today, YearMonth.of(2026, 10), LocalDate.of(2026, 9, 1).toEpochDay())

    @Test fun everyOwnerSectionHasSomethingToShow() {
        assertTrue(gym.people.any { it.role == Role.TRAINER && it.status == PersonStatus.PENDING })
        assertEquals(17, digest.members)
        assertTrue(digest.trainedToday > 0)
        assertEquals(3, digest.trainers.size)
        assertTrue(gym.awards.isNotEmpty())
        assertTrue(gym.given.isNotEmpty())
    }

    @Test fun quietMembersCoverBothActions() {
        val names = digest.idle.associateBy { it.member.name }
        // Stopped training although workouts kept coming: message the member.
        assertTrue(names.getValue("Hari Haran").assignedRecently)
        // Trainer stopped sending workouts: remind the trainer.
        assertTrue(!names.getValue("Manoj Kumar").assignedRecently)
        // New joiners aren't flagged.
        assertTrue("Deepa Rani" !in names)
    }

    @Test fun sameDataEveryTime() {
        assertEquals(gym, DemoData.build(owner, today, ZoneOffset.UTC))
    }
}
