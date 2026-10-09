package com.barathiraja.jk.gym

/** A member's strength per exercise, from the weights they logged on their coach's workouts. */
object Lifts {
    /**
     * [bestKg] × [bestReps] is the heaviest set done (most reps breaks a tie); [firstKg] is the heaviest set on the
     * first day the exercise was done with weight, so [gainKg] says how far they've come; [lastDay] is when it was last done.
     */
    data class Best(val exerciseId: String, val bestKg: Float, val bestReps: Int, val firstKg: Float, val lastDay: Long, val days: Int) {
        val gainKg get() = bestKg - firstKg
    }

    /** One entry per exercise done with weight, most recently trained first. */
    fun bests(mine: List<Assignment>): List<Best> {
        // (exercise, day, kg, reps) for every finished set that carried weight.
        val sets = mine.flatMap { a ->
            a.exercises.flatMap { e ->
                e.sets.filter { it.done && !it.timed && it.weightKg > 0f }
                    .map { Triple(e.exerciseId, a.epochDay, it.copy(reps = it.reps.coerceIn(0, MAX_REPS), weightKg = it.weightKg.coerceAtMost(MAX_KG))) }
            }
        }
        return sets.groupBy { it.first }.map { (id, list) ->
            val best = list.maxWith(compareBy({ it.third.weightKg }, { it.third.reps })).third
            val firstDay = list.minOf { it.second }
            Best(
                exerciseId = id, bestKg = best.weightKg, bestReps = best.reps,
                firstKg = list.filter { it.second == firstDay }.maxOf { it.third.weightKg },
                lastDay = list.maxOf { it.second }, days = list.map { it.second }.distinct().size,
            )
        }.sortedWith(compareByDescending<Best> { it.lastDay }.thenByDescending { it.bestKg })
    }
}
