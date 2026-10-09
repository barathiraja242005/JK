package com.barathiraja.jk

import com.barathiraja.jk.data.SetSpec
import com.barathiraja.jk.gym.AssignStatus
import com.barathiraja.jk.gym.AssignedExercise
import com.barathiraja.jk.gym.Assignment
import com.barathiraja.jk.gym.Lifts
import org.junit.Assert.assertEquals
import org.junit.Test

class LiftsTest {
    private fun w(day: Long, vararg ex: AssignedExercise) = Assignment(
        id = "a$day", trainerUid = "t", memberUid = "m", title = "W", epochDay = day, exercises = ex.toList(), status = AssignStatus.DONE,
    )
    private fun ex(id: String, vararg sets: SetSpec) = AssignedExercise(id, "CHEST", sets.toList())

    @Test fun bestSetAndGainSinceFirstDay() {
        val list = listOf(
            w(1, ex("bench", SetSpec(10, 30f, done = true), SetSpec(8, 32.5f, done = true))),
            w(5, ex("bench", SetSpec(8, 40f, done = true), SetSpec(10, 40f, done = true), SetSpec(5, 50f, done = false))),
        )
        val b = Lifts.bests(list).single()
        assertEquals(40f, b.bestKg)
        assertEquals(10, b.bestReps) // more reps wins the tie at 40 kg; the 50 kg set wasn't done
        assertEquals(32.5f, b.firstKg)
        assertEquals(7.5f, b.gainKg)
        assertEquals(5L, b.lastDay)
        assertEquals(2, b.days)
    }

    @Test fun bodyweightAndUndoneSetsAreLeftOut() {
        val list = listOf(w(1, ex("pushup", SetSpec(15, 0f, done = true)), ex("row", SetSpec(10, 20f, done = false))))
        assertEquals(emptyList<Lifts.Best>(), Lifts.bests(list))
    }

    @Test fun mostRecentlyTrainedFirst() {
        val list = listOf(w(1, ex("squat", SetSpec(5, 60f, done = true))), w(3, ex("bench", SetSpec(5, 40f, done = true))))
        assertEquals(listOf("bench", "squat"), Lifts.bests(list).map { it.exerciseId })
    }
}
