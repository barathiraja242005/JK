package com.barathiraja.jk

import com.barathiraja.jk.data.Catalog
import com.barathiraja.jk.data.Goal
import com.barathiraja.jk.data.Level
import com.barathiraja.jk.data.Place
import com.barathiraja.jk.data.Profile
import com.barathiraja.jk.data.Sex
import com.barathiraja.jk.domain.Health
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthTest {
    @Test fun bmi() = assertEquals(22.86f, Health.bmi(70f, 175f), 0.01f)

    @Test fun bmiCategories() {
        assertEquals("Underweight", Health.bmiCategory(17f))
        assertEquals("Healthy", Health.bmiCategory(22f))
        assertEquals("Overweight", Health.bmiCategory(27f))
        assertEquals("Obese", Health.bmiCategory(31f))
    }

    @Test fun bmrMifflinStJeor() {
        // 10*70 + 6.25*175 - 5*30 + 5 = 1648.75
        assertEquals(1649, Health.bmr(Sex.MALE, 70f, 175f, 30))
        assertEquals(1483, Health.bmr(Sex.FEMALE, 70f, 175f, 30))
    }

    @Test fun macrosAddUpToTarget() {
        val p = Profile(weightKg = 80f, heightCm = 180f, age = 28, goal = Goal.LOSE)
        val m = Health.macros(p)
        val kcal = m.proteinG * 4 + m.carbsG * 4 + m.fatG * 9
        assertTrue(kotlin.math.abs(kcal - Health.targetCalories(p)) < 15)
    }

    @Test fun streakCountsConsecutiveDays() {
        assertEquals(3, Health.streak(listOf(100, 99, 98, 96), today = 100))
        assertEquals(2, Health.streak(listOf(99, 98), today = 100)) // not yet trained today
        assertEquals(0, Health.streak(listOf(97), today = 100))
        assertEquals(0, Health.streak(emptyList(), today = 100))
    }

    @Test fun todaysWorkoutMatchesPlace() {
        for (day in 0L..20L) {
            assertEquals(Place.GYM, Catalog.todayFor(Place.GYM, Level.BEGINNER, day).place)
            assertEquals(Place.HOME, Catalog.todayFor(Place.HOME, Level.ADVANCED, day).place)
        }
    }

    @Test fun workoutIdsUnique() {
        assertEquals(Catalog.workouts.size, Catalog.workouts.map { it.id }.toSet().size)
    }
}
