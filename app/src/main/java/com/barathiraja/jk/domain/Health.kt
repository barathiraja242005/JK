package com.barathiraja.jk.domain

import com.barathiraja.jk.data.ExerciseRepo
import com.barathiraja.jk.data.Goal
import com.barathiraja.jk.data.Profile
import com.barathiraja.jk.data.Sex
import kotlin.math.max
import kotlin.math.roundToInt

object Health {
    fun bmi(weightKg: Float, heightCm: Float): Float {
        val m = heightCm / 100f
        return if (m <= 0f) 0f else weightKg / (m * m)
    }

    fun bmiCategory(bmi: Float): String = when {
        bmi <= 0f -> "—"
        bmi < 18.5f -> "Underweight"
        bmi < 25f -> "Healthy"
        bmi < 30f -> "Overweight"
        else -> "Obese"
    }

    /** Mifflin–St Jeor basal metabolic rate, kcal/day. */
    fun bmr(sex: Sex, weightKg: Float, heightCm: Float, age: Int): Int {
        val base = 10 * weightKg + 6.25 * heightCm - 5 * age
        return (base + if (sex == Sex.MALE) 5 else -161).roundToInt()
    }

    fun tdee(p: Profile): Int = (bmr(p.sex, p.weightKg, p.heightCm, p.age) * p.activity.factor).roundToInt()

    fun targetCalories(p: Profile): Int = when (p.goal) {
        Goal.LOSE -> tdee(p) - 450
        Goal.MAINTAIN -> tdee(p)
        Goal.GAIN -> tdee(p) + 300
    }

    /** Devine formula, kg. */
    fun idealWeight(sex: Sex, heightCm: Float): Float {
        val inchesOver5ft = max(0f, heightCm / 2.54f - 60f)
        return (if (sex == Sex.MALE) 50f else 45.5f) + 2.3f * inchesOver5ft
    }

    data class Macros(val proteinG: Int, val fatG: Int, val carbsG: Int)

    fun macros(p: Profile): Macros {
        val kcal = targetCalories(p)
        val proteinPerKg = when (p.goal) { Goal.LOSE -> 2.0f; Goal.MAINTAIN -> 1.6f; Goal.GAIN -> 1.8f }
        val protein = (p.weightKg * proteinPerKg).roundToInt()
        val fat = (kcal * 0.25f / 9f).roundToInt()
        val carbs = max(0, ((kcal - protein * 4 - fat * 9) / 4f).roundToInt())
        return Macros(protein, fat, carbs)
    }

    /** Calories burned for an activity at a given MET over [seconds]. */
    fun caloriesBurned(met: Float, weightKg: Float, seconds: Int): Int =
        (met * weightKg * seconds / 3600f).roundToInt()

    /** MET for an exercise with none on record (moderate resistance training). */
    const val DEFAULT_MET = 5f

    /** Calories for [seconds] of one exercise, at its MET from the library. */
    fun exerciseKcal(exerciseId: String, weightKg: Float, seconds: Int): Int =
        caloriesBurned(ExerciseRepo.get(exerciseId)?.met ?: DEFAULT_MET, weightKg, seconds)

    /** Seconds as whole minutes, rounded up (a 61-second workout is "2 min"). */
    fun minutesUp(seconds: Int): Int = (seconds + 59) / 60

    fun stepCalories(steps: Int, weightKg: Float): Int = (steps * weightKg * 0.00057f).roundToInt()

    fun stepKm(steps: Int, heightCm: Float): Float = steps * heightCm * 0.415f / 100_000f

    fun waterGoalGlasses(weightKg: Float): Int = ((weightKg * 35f) / 250f).roundToInt().coerceIn(6, 16)

    fun kgToLb(kg: Float) = kg * 2.20462f
}
