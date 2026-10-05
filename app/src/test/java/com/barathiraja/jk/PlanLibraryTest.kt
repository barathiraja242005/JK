package com.barathiraja.jk

import com.barathiraja.jk.data.BodyPart
import com.barathiraja.jk.gym.PlanLibrary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** The suggested plans only use exercises that ship with the app, and match the plan they come from. */
class PlanLibraryTest {
    private val libraryIds: Set<String> =
        Regex("\"id\"\\s*:\\s*\"([^\"]+)\"").findAll(File("src/main/assets/exercises.json").readText()).map { it.groupValues[1] }.toSet()

    @Test fun everyExerciseIsInTheLibrary() {
        val missing = PlanLibrary.all.flatMap { it.exercises }.map { it.exerciseId }.filterNot { it in libraryIds }
        assertTrue("Not in exercises.json: $missing", missing.isEmpty())
    }

    @Test fun pushPullLegsHasFiveTrainingDays() {
        assertEquals(listOf("Day 1", "Day 2", "Day 3", "Day 5", "Day 6"), PlanLibrary.pushPullLegs.map { it.day })
        assertEquals(listOf(6, 6, 5, 6, 5), PlanLibrary.pushPullLegs.map { it.exercises.size })
    }

    @Test fun everyExerciseHasSetsACueAndABodyPart() {
        PlanLibrary.all.flatMap { it.exercises }.forEach { e ->
            assertTrue(e.exerciseId, e.sets.size in 2..4 && e.sets.all { it.reps in 6..15 })
            assertTrue(e.exerciseId, e.cue.isNotBlank())
            assertTrue(e.exerciseId, BodyPart.entries.any { it.name == e.bodyPart })
        }
    }

    @Test fun everyDayCarriesTheTempoAndWarmUp() {
        PlanLibrary.all.forEach { assertTrue(it.day, "3:1:2:1" in it.note && "warm-up" in it.note) }
    }
}
