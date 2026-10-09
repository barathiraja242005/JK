package com.barathiraja.jk.domain

import com.barathiraja.jk.data.BodyPart
import com.barathiraja.jk.data.BodyPart.ABDOMEN
import com.barathiraja.jk.data.BodyPart.BACK
import com.barathiraja.jk.data.BodyPart.BICEPS
import com.barathiraja.jk.data.BodyPart.CARDIO
import com.barathiraja.jk.data.BodyPart.CHEST
import com.barathiraja.jk.data.BodyPart.LEGS
import com.barathiraja.jk.data.BodyPart.SHOULDER
import com.barathiraja.jk.data.BodyPart.TRICEPS
import com.barathiraja.jk.data.Equipment
import com.barathiraja.jk.data.weightStep
import com.barathiraja.jk.data.Exercise
import com.barathiraja.jk.data.Goal
import com.barathiraja.jk.data.Level
import com.barathiraja.jk.data.SetLog
import com.barathiraja.jk.data.SetSpec
import com.barathiraja.jk.data.TrainingPool
import com.barathiraja.jk.data.TrainingPrefs
import java.time.DayOfWeek
import kotlin.random.Random

/** One exercise slot in a generated day, before it is stored. */
data class PlannedExercise(val part: BodyPart, val exerciseId: String, val sets: List<SetSpec>)

/**
 * Rule-based personal trainer: builds the weekly split and each day's workout from the user's
 * goal, level, equipment, injuries and training history. Pure logic so it is unit-testable.
 */
class TrainingEngine(private val lookup: (String) -> Exercise?) {

    // ---------------- Weekly split ----------------

    /** Templates by number of training days; abs are sprinkled in like a typical gym split. */
    private fun template(n: Int): List<List<BodyPart>> = when (n) {
        0 -> emptyList()
        1 -> listOf(listOf(CHEST, BACK, LEGS, SHOULDER, ABDOMEN))
        2 -> listOf(listOf(CHEST, BACK, SHOULDER, ABDOMEN), listOf(LEGS, BICEPS, TRICEPS, ABDOMEN))
        3 -> listOf(listOf(CHEST, ABDOMEN, TRICEPS), listOf(BACK, ABDOMEN, BICEPS), listOf(SHOULDER, ABDOMEN, LEGS))
        4 -> listOf(listOf(CHEST, TRICEPS), listOf(BACK, BICEPS), listOf(LEGS, ABDOMEN), listOf(SHOULDER, ABDOMEN, CARDIO))
        5 -> listOf(listOf(CHEST, ABDOMEN, TRICEPS), listOf(BACK, ABDOMEN, BICEPS), listOf(LEGS, ABDOMEN),
            listOf(SHOULDER, ABDOMEN), listOf(CHEST, BACK, CARDIO))
        6 -> listOf(listOf(CHEST, SHOULDER, TRICEPS), listOf(BACK, BICEPS, ABDOMEN), listOf(LEGS, ABDOMEN),
            listOf(CHEST, SHOULDER, TRICEPS), listOf(BACK, BICEPS, ABDOMEN), listOf(LEGS, CARDIO))
        else -> template(6) + listOf(listOf(CARDIO, ABDOMEN))
    }

    fun buildSplit(activeDays: Set<DayOfWeek>, goal: Goal): Map<DayOfWeek, List<BodyPart>> {
        val days = activeDays.sorted()
        val t = template(days.size)
        return DayOfWeek.entries.associateWith { dow ->
            val i = days.indexOf(dow)
            if (i < 0) emptyList()
            else {
                val parts = t[i]
                // Fat-loss goals finish every session with a cardio block.
                if (goal == Goal.LOSE && CARDIO !in parts) parts + CARDIO else parts
            }
        }
    }

    // ---------------- Daily workout ----------------

    fun available(part: BodyPart, prefs: TrainingPrefs): List<Exercise> {
        val equipKeys = prefs.equipment.map { it.key }.toSet() + Equipment.BODY_ONLY.key
        return TrainingPool.byPart[part].orEmpty().mapNotNull(lookup).filter { e ->
            e.equipment in equipKeys &&
                prefs.injuries.none { it.excludes(e) } &&
                when (prefs.level) {
                    Level.BEGINNER -> e.level == "beginner"
                    Level.INTERMEDIATE -> e.level != "expert"
                    Level.ADVANCED -> true
                }
        }
    }

    /** How many exercises each part gets: the first (main) part gets the most. */
    private fun counts(parts: List<BodyPart>, level: Level): List<Int> {
        val total = when (level) { Level.BEGINNER -> 5; Level.INTERMEDIATE -> 6; Level.ADVANCED -> 8 }
        if (parts.isEmpty()) return emptyList()
        val minor = setOf(ABDOMEN, CARDIO)
        val majors = parts.filter { it !in minor }
        val result = parts.map { if (it in minor) 1 else 0 }.toMutableList()
        var left = total - result.sum()
        if (majors.isEmpty()) return parts.map { (total / parts.size).coerceAtLeast(2) }
        var i = 0
        while (left > 0) {
            val idx = parts.indexOf(majors[i % majors.size])
            result[idx]++
            left--
            i++
        }
        return result.map { it.coerceAtLeast(1) }
    }

    fun generateDay(
        parts: List<BodyPart>,
        prefs: TrainingPrefs,
        goal: Goal,
        epochDay: Long,
        history: (String) -> List<SetLog> = { emptyList() },
    ): List<PlannedExercise> {
        val counts = counts(parts, prefs.level)
        val used = mutableSetOf<String>()
        val out = mutableListOf<PlannedExercise>()
        parts.forEachIndexed { i, part ->
            val rnd = Random(epochDay * 31 + part.ordinal * 7)
            val pool = available(part, prefs).filter { it.id !in used }
            // Compound movements first, then shuffle the rest so each week feels different.
            val (compound, other) = pool.shuffled(rnd).partition { it.mechanic == "compound" }
            val ordered = if (part == CARDIO && prefs.equipment.contains(Equipment.MACHINE)) {
                pool.shuffled(rnd).sortedByDescending { it.id in TrainingPool.machineCardio }
            } else compound + other
            ordered.take(counts[i]).forEach { e ->
                used += e.id
                out += PlannedExercise(part, e.id, prescribe(e, part, prefs.level, goal, history(e.id)))
            }
        }
        return out
    }

    /** Sets × reps (or seconds) and a weight suggestion based on goal, level and last performance. */
    fun prescribe(e: Exercise, part: BodyPart, level: Level, goal: Goal, history: List<SetLog>): List<SetSpec> {
        val timed = e.id in TrainingPool.timedIds
        if (timed) {
            val machine = e.id in TrainingPool.machineCardio
            return when {
                machine -> listOf(SetSpec(0, seconds = when (goal) { Goal.LOSE -> 900; else -> 600 }))
                e.id == "Plank" || e.id == "Side_Bridge" ->
                    List(3) { SetSpec(0, seconds = when (level) { Level.BEGINNER -> 30; Level.INTERMEDIATE -> 45; Level.ADVANCED -> 60 }) }
                else -> List(3) { SetSpec(0, seconds = when (level) { Level.BEGINNER -> 30; Level.INTERMEDIATE -> 40; Level.ADVANCED -> 45 }) }
            }
        }
        val sets = when (goal) { Goal.GAIN -> 4; else -> 3 } + if (level == Level.ADVANCED) 1 else 0
        val reps = when {
            part == ABDOMEN -> 15
            goal == Goal.GAIN -> 10
            goal == Goal.LOSE -> 15
            else -> 12
        }
        val weight = if (e.equipment == Equipment.BODY_ONLY.key) 0f else suggestWeight(e, reps, history).takeIf { it > 0f } ?: startWeight(e, level)
        return List(sets) { SetSpec(reps, weight) }
    }

    /**
     * Progressive overload: repeat last session's weight, adding a small increment when every
     * logged set hit the target reps.
     */
    fun suggestWeight(e: Exercise, targetReps: Int, history: List<SetLog>): Float {
        if (history.isEmpty()) return 0f
        val lastDay = history.maxOf { it.epochDay }
        val last = history.filter { it.epochDay == lastDay }
        val w = last.maxOf { it.weightKg }
        if (w <= 0f) return 0f
        val allHit = last.all { it.reps >= targetReps }
        val step = e.weightStep
        return if (allHit) w + step else w
    }

    /** Conservative first-session weights (per dumbbell / total for bars and stacks); users adjust in the set editor. */
    fun startWeight(e: Exercise, level: Level): Float {
        val i = level.ordinal // 0 beginner, 1 intermediate, 2 advanced
        val isolation = e.mechanic == "isolation"
        return when (e.equipment) {
            "dumbbell" -> if (isolation) floatArrayOf(4f, 6f, 10f)[i] else floatArrayOf(6f, 10f, 16f)[i]
            "kettlebells" -> floatArrayOf(8f, 12f, 16f)[i]
            "barbell" -> if (isolation) floatArrayOf(10f, 20f, 30f)[i] else floatArrayOf(20f, 40f, 60f)[i]
            "e-z curl bar" -> floatArrayOf(10f, 20f, 30f)[i]
            "cable" -> if (isolation) floatArrayOf(10f, 15f, 25f)[i] else floatArrayOf(20f, 30f, 45f)[i]
            "machine" -> if (isolation) floatArrayOf(15f, 25f, 40f)[i] else floatArrayOf(30f, 50f, 80f)[i]
            "medicine ball" -> 4f
            else -> 0f
        }
    }

    /** "Switch to fat loss" day: a fast circuit of cardio and full-body moves. */
    fun fatLossDay(prefs: TrainingPrefs, goal: Goal, epochDay: Long): List<PlannedExercise> {
        val rnd = Random(epochDay * 13)
        val out = mutableListOf<PlannedExercise>()
        listOf(CARDIO, LEGS, CHEST, CARDIO, ABDOMEN, BACK).forEach { part ->
            val e = available(part, prefs.copy(level = Level.INTERMEDIATE.coerceAtMost(prefs.level)))
                .filter { c -> out.none { it.exerciseId == c.id } }
                .shuffled(rnd).firstOrNull() ?: return@forEach
            val sets = if (e.id in TrainingPool.timedIds) {
                if (e.id in TrainingPool.machineCardio) listOf(SetSpec(0, seconds = 600)) else List(3) { SetSpec(0, seconds = 40) }
            } else List(3) { SetSpec(15, 0f) }
            out += PlannedExercise(part, e.id, sets)
        }
        return out
    }

    /** Alternatives offered when replacing an exercise. */
    fun alternatives(part: BodyPart, currentId: String, prefs: TrainingPrefs): List<Exercise> =
        available(part, prefs.copy(level = Level.ADVANCED)).filter { it.id != currentId }

    fun warmUpIds(parts: List<BodyPart>): List<String> {
        val ids = mutableListOf<String>()
        if (parts.any { it in setOf(CHEST, BACK, SHOULDER, BICEPS, TRICEPS) }) ids += TrainingPool.warmUpper.take(3)
        if (parts.any { it == LEGS || it == CARDIO }) ids += TrainingPool.warmLower.take(3)
        ids += TrainingPool.warmCardio.take(2)
        return ids.distinct().take(5)
    }

    companion object {
        /** Estimated minutes: each rep ~3 s, plus rest between sets. */
        fun estimateSec(sets: List<SetSpec>, restSec: Int): Int =
            sets.sumOf { (if (it.timed) it.seconds else it.reps * 3) + restSec }

        fun title(parts: List<BodyPart>): String = when (parts.size) {
            0 -> "Rest day"
            1 -> parts[0].label
            else -> parts.dropLast(1).joinToString(", ") { it.label } + " & " + parts.last().label
        }
    }
}

private fun Level.coerceAtMost(other: Level) = if (ordinal <= other.ordinal) this else other
