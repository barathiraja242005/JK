package com.barathiraja.jk

import com.barathiraja.jk.data.SetSpec
import com.barathiraja.jk.data.WorkoutSession
import com.barathiraja.jk.gym.AssignStatus
import com.barathiraja.jk.gym.AssignedExercise
import com.barathiraja.jk.gym.Assignment
import com.barathiraja.jk.gym.HistorySync
import com.barathiraja.jk.gym.Scoring
import com.barathiraja.jk.gym.SessionStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** A member's finished gym workouts show up in the phone's history exactly once, and leave it when undone or deleted. */
class HistorySyncTest {
    private class Store : SessionStore {
        val rows = mutableMapOf<String, WorkoutSession>()
        override suspend fun gymSessions() = rows.values.filter { it.workoutId.startsWith("gym:") }
        override suspend fun replaceSession(session: WorkoutSession) { rows[session.workoutId] = session }
        override suspend fun deleteSessionsFor(workoutId: String) { rows.remove(workoutId) }
    }

    private val window = 20_000L
    private fun noon(day: Long) = day * 86_400_000L + 43_200_000L
    private fun w(id: String, day: Long, done: Boolean, startedMinBefore: Long? = 40) = Assignment(
        id = id, trainerUid = "t", memberUid = "m", title = "Push", epochDay = day,
        exercises = listOf(AssignedExercise("Pushups", "CHEST", List(3) { SetSpec(10, done = done) })),
        status = if (done) AssignStatus.DONE else AssignStatus.ASSIGNED,
        startedAt = if (done && startedMinBefore != null) noon(day) - startedMinBefore * 60_000 else null,
        completedAt = if (done) noon(day) else null,
    )

    @Test fun finishedWorkoutIsLoggedOnceWithItsRealLength() = runBlocking {
        val store = Store(); val sync = HistorySync(store)
        val a = w("a", window + 10, done = true)
        sync.sync(listOf(a), window, restSec = 60, weightKg = 70f)
        sync.sync(listOf(a), window, restSec = 60, weightKg = 70f)
        val s = store.rows.getValue("gym:a")
        assertEquals(1, store.rows.size)
        assertEquals(40 * 60, s.durationSec)
        assertEquals(Scoring.dayOf(noon(window + 10)), s.epochDay)
        assertTrue(s.calories > 0)
    }

    @Test fun undoneWorkoutLeavesHistory() = runBlocking {
        val store = Store(); val sync = HistorySync(store)
        sync.sync(listOf(w("a", window + 10, done = true)), window, 60, 70f)
        sync.sync(listOf(w("a", window + 10, done = false)), window, 60, 70f)
        assertTrue(store.rows.isEmpty())
    }

    @Test fun deletedWorkoutLeavesHistoryOnlyWellInsideTheWindow() = runBlocking {
        val store = Store(); val sync = HistorySync(store)
        sync.sync(listOf(w("inside", window + 10, true), w("edge", window + 1, true), w("keep", window + 12, true)), window, 60, 70f)
        // The coach deleted "inside" and "edge"; "edge" might be a workout from before the window, so it stays.
        sync.sync(listOf(w("keep", window + 12, true)), window, 60, 70f)
        assertEquals(setOf("gym:edge", "gym:keep"), store.rows.keys)
    }

    @Test fun emptyListMayBeStillLoadingSoNothingIsDeleted() = runBlocking {
        val store = Store(); val sync = HistorySync(store)
        sync.sync(listOf(w("a", window + 10, true)), window, 60, 70f)
        sync.sync(emptyList(), window, 60, 70f)
        assertEquals(1, store.rows.size)
    }

    @Test fun oddTimingFallsBackToTheEstimate() = runBlocking {
        val store = Store(); val sync = HistorySync(store)
        // Started 6 hours before finishing: not one sitting, so the estimate (3 sets × (10 reps × 3 s + 60 s rest)) is used.
        sync.sync(listOf(w("a", window + 10, true, startedMinBefore = 360)), window, 60, 70f)
        assertEquals(3 * (30 + 60), store.rows.getValue("gym:a").durationSec)
    }
}
