package com.barathiraja.jk

import com.barathiraja.jk.data.BodyPart
import com.barathiraja.jk.data.Equipment
import com.barathiraja.jk.data.Exercise
import com.barathiraja.jk.data.Goal
import com.barathiraja.jk.data.Injury
import com.barathiraja.jk.data.Level
import com.barathiraja.jk.data.SetLog
import com.barathiraja.jk.data.SetSpec
import com.barathiraja.jk.data.TrainingPool
import com.barathiraja.jk.data.TrainingPrefs
import com.barathiraja.jk.domain.TrainingEngine
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.DayOfWeek

class TrainingEngineTest {
    private val db: Map<String, Exercise> by lazy {
        val arr = JSONArray(File("src/main/assets/exercises.json").readText())
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            fun strs(k: String) = o.optJSONArray(k)?.let { a -> List(a.length()) { a.getString(it) } }.orEmpty()
            Exercise(o.getString("id"), o.getString("name"), o.optString("category"), if (o.isNull("equipment")) "other" else o.getString("equipment"),
                o.optString("level"), o.optString("force"), if (o.isNull("mechanic")) null else o.getString("mechanic"),
                strs("primaryMuscles"), strs("secondaryMuscles"), strs("instructions"))
        }.associateBy { it.id }
    }
    private val engine by lazy { TrainingEngine { db[it] } }
    private val gym = TrainingPrefs(true, Level.INTERMEDIATE, setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), Equipment.gymDefault)
    private val home = gym.copy(level = Level.BEGINNER, equipment = setOf(Equipment.BODY_ONLY))

    @Test fun poolIdsAllExistAndBundled() {
        val ids = TrainingPool.byPart.values.flatten() + TrainingPool.warmUpper + TrainingPool.warmLower + TrainingPool.warmCardio
        ids.forEach {
            assertTrue("$it not in db", it in db)
            assertTrue("$it photo not bundled", File("src/main/assets/exercise_images/$it/0.jpg").exists())
        }
    }

    @Test fun splitUsesExactlyActiveDays() {
        for (n in 1..7) {
            val days = DayOfWeek.entries.take(n).toSet()
            val split = engine.buildSplit(days, Goal.MAINTAIN)
            assertEquals(n, split.count { it.value.isNotEmpty() })
            DayOfWeek.entries.filter { it !in days }.forEach { assertTrue(split[it]!!.isEmpty()) }
        }
    }

    @Test fun threeDaySplitMatchesClassicPattern() {
        val split = engine.buildSplit(gym.activeDays, Goal.MAINTAIN)
        assertEquals(listOf(BodyPart.CHEST, BodyPart.ABDOMEN, BodyPart.TRICEPS), split[DayOfWeek.MONDAY])
        assertEquals(listOf(BodyPart.BACK, BodyPart.ABDOMEN, BodyPart.BICEPS), split[DayOfWeek.WEDNESDAY])
    }

    @Test fun fatLossGoalAddsCardio() {
        engine.buildSplit(gym.activeDays, Goal.LOSE).values.filter { it.isNotEmpty() }.forEach { assertTrue(BodyPart.CARDIO in it) }
    }

    @Test fun gymDayHasExercisesForEveryPart() {
        val parts = listOf(BodyPart.CHEST, BodyPart.ABDOMEN, BodyPart.TRICEPS)
        val day = engine.generateDay(parts, gym, Goal.GAIN, 20000)
        assertEquals(6, day.size)
        parts.forEach { p -> assertTrue("$p missing", day.any { it.part == p }) }
        assertEquals(day.size, day.map { it.exerciseId }.toSet().size) // no duplicates
        day.filter { it.exerciseId !in TrainingPool.timedIds }.forEach { assertEquals(4, it.sets.size); assertEquals(10, it.sets[0].reps.coerceAtMost(10)) }
    }

    @Test fun homeBodyweightOnlyRespectsEquipment() {
        for (p in listOf(BodyPart.CHEST, BodyPart.LEGS, BodyPart.ABDOMEN)) {
            val day = engine.generateDay(listOf(p), home, Goal.MAINTAIN, 1)
            assertTrue(day.isNotEmpty())
            day.forEach { assertEquals("body only", db[it.exerciseId]!!.equipment) }
        }
    }

    @Test fun injuriesExcludeRiskyExercises() {
        val prefs = gym.copy(level = Level.ADVANCED, injuries = setOf(Injury.KNEE, Injury.LOWER_BACK))
        val legs = engine.available(BodyPart.LEGS, prefs)
        assertTrue(legs.isNotEmpty())
        assertFalse(legs.any { it.name.contains("Lunge", true) || it.name.contains("Jump", true) })
        assertFalse(engine.available(BodyPart.BACK, prefs).any { it.name.contains("Deadlift", true) })
    }

    @Test fun progressiveOverloadAddsWeightWhenAllRepsHit() {
        val bench = db["Barbell_Bench_Press_-_Medium_Grip"]!!
        val hit = List(3) { SetLog(epochDay = 10, exerciseId = bench.id, setIndex = it, reps = 10, weightKg = 40f, seconds = 0) }
        assertEquals(42.5f, engine.suggestWeight(bench, 10, hit), 0.01f)
        val missed = hit.mapIndexed { i, l -> if (i == 2) l.copy(reps = 7) else l }
        assertEquals(40f, engine.suggestWeight(bench, 10, missed), 0.01f)
        assertEquals(0f, engine.suggestWeight(bench, 10, emptyList()), 0.01f)
    }

    @Test fun weightedExercisesStartWithAWeight() {
        val goblet = engine.prescribe(db["Goblet_Squat"]!!, BodyPart.LEGS, Level.BEGINNER, Goal.MAINTAIN, emptyList())
        assertTrue(goblet.all { it.weightKg > 0f })
        val squat = engine.prescribe(db["Bodyweight_Squat"]!!, BodyPart.LEGS, Level.BEGINNER, Goal.MAINTAIN, emptyList())
        assertTrue(squat.all { it.weightKg == 0f })
    }

    @Test fun timedExercisesGetSeconds() {
        val plank = engine.prescribe(db["Plank"]!!, BodyPart.ABDOMEN, Level.BEGINNER, Goal.MAINTAIN, emptyList())
        assertTrue(plank.all { it.timed && it.seconds == 30 })
    }

    @Test fun setSpecRoundTrip() {
        val s = listOf(SetSpec(10, 42.5f), SetSpec(0, 0f, 45, true))
        assertEquals(s, SetSpec.decode(SetSpec.encode(s)))
    }

    @Test fun fatLossDayIsCircuit() {
        val d = engine.fatLossDay(gym, Goal.LOSE, 5)
        assertTrue(d.size >= 4)
        assertTrue(d.any { it.part == BodyPart.CARDIO })
    }

    @Test fun titleFormatting() {
        assertEquals("Shoulder, Abdomen & Legs", TrainingEngine.title(listOf(BodyPart.SHOULDER, BodyPart.ABDOMEN, BodyPart.LEGS)))
        assertEquals("Rest day", TrainingEngine.title(emptyList()))
    }
}
