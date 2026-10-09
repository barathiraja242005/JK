package com.barathiraja.jk.gym

import com.barathiraja.jk.data.WorkoutSession
import com.barathiraja.jk.domain.Health
import com.barathiraja.jk.domain.TrainingEngine

/** The part of the history database [HistorySync] needs (the Room DAO in the app, a map in tests). */
interface SessionStore {
    suspend fun gymSessions(): List<WorkoutSession>
    suspend fun replaceSession(session: WorkoutSession)
    suspend fun deleteSessionsFor(workoutId: String)
}

/**
 * Keeps this phone's workout history in step with a member's finished gym workouts, so the streak, calories and
 * Progress count them: one session per finished workout ("gym:{id}"), dated the day it was finished, removed if
 * undone or deleted by the coach. Works from the gym's data, so workouts finished on another phone or before a
 * reinstall count too.
 */
class HistorySync(private val dao: SessionStore) {
    /**
     * [mine] is every workout of this member in the loaded window, which starts on [windowStart]; [restSec] and
     * [weightKg] feed the time and calorie estimates.
     */
    suspend fun sync(mine: List<Assignment>, windowStart: Long, restSec: Int, weightKg: Float) {
        val local = dao.gymSessions().associateBy { it.workoutId }
        mine.forEach { a ->
            val id = sessionId(a)
            val have = local[id]
            if (a.done) {
                val finished = a.completedAt ?: return@forEach
                if (have != null && have.finishedAt == finished && have.title == a.title) return@forEach
                val estimate = a.exercises.sumOf { TrainingEngine.estimateSec(it.sets, restSec) }
                // The real time from first set to last, when it looks like one sitting; the estimate otherwise.
                val sec = a.startedAt?.let { ((finished - it) / 1000).toInt() }?.takeIf { it in MIN_SITTING_SEC..MAX_SITTING_SEC } ?: estimate
                val kcal = a.exercises.sumOf { e ->
                    val share = if (estimate == 0) 0 else sec * TrainingEngine.estimateSec(e.sets, restSec) / estimate
                    Health.exerciseKcal(e.exerciseId, weightKg, share)
                }
                dao.replaceSession(WorkoutSession(workoutId = id, title = a.title, finishedAt = finished,
                    epochDay = Scoring.dayOf(finished), durationSec = sec, calories = kcal))
            } else if (have != null) dao.deleteSessionsFor(id)
        }
        // Workouts the coach deleted: drop their sessions too, but only well inside the loaded window (a workout from
        // before it may have been finished inside it), and never on an empty list, which may just be still loading.
        if (mine.isEmpty()) return
        val ids = mine.mapTo(HashSet(), ::sessionId)
        local.values.filter { it.workoutId !in ids && it.epochDay >= windowStart + EDGE_DAYS }.forEach { dao.deleteSessionsFor(it.workoutId) }
    }

    private fun sessionId(a: Assignment) = "gym:${a.id}"

    private companion object {
        const val MIN_SITTING_SEC = 300
        const val MAX_SITTING_SEC = 3 * 3600
        const val EDGE_DAYS = 7
    }
}
