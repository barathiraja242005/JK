package com.barathiraja.jk.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: String,
    val title: String,
    val finishedAt: Long,
    val epochDay: Long,
    val durationSec: Int,
    val calories: Int,
)

@Entity(tableName = "water_days")
data class WaterDay(
    @PrimaryKey val epochDay: Long,
    val glasses: Int,
)

@Entity(tableName = "weight_entries")
data class WeightEntry(
    @PrimaryKey val epochDay: Long,
    val kg: Float,
)

@Entity(tableName = "fasts")
data class Fast(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val targetHours: Int,
    val endedAt: Long? = null,
)

@Entity(tableName = "step_days")
data class StepDay(
    @PrimaryKey val epochDay: Long,
    val steps: Int,
)

/** Per-exercise flags used by Shorts and the library. */
@Entity(tableName = "exercise_flags")
data class ExerciseFlag(
    @PrimaryKey val exerciseId: String,
    val liked: Boolean = false,
    val saved: Boolean = false,
)

@Entity(tableName = "custom_workouts")
data class CustomWorkout(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val restSec: Int,
    val rounds: Int,
    val blocks: String, // Block.encodeAll
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "challenge_days", primaryKeys = ["challengeId", "day"])
data class ChallengeDay(
    val challengeId: String,
    val day: Int,
    val doneAt: Long,
)

enum class Meal(val label: String) { BREAKFAST("Breakfast"), LUNCH("Lunch"), SNACK("Snacks"), DINNER("Dinner") }

@Entity(tableName = "food_entries")
data class FoodEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val meal: Meal,
    val name: String,
    val servings: Float,
    val kcal: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
)

@Entity(tableName = "body_photos")
data class BodyPhoto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val uri: String,
    val weightKg: Float?,
)

@Entity(tableName = "walk_sessions")
data class WalkSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val startedAt: Long,
    val durationSec: Int,
    val steps: Int,
    val calories: Int,
)

@Entity(tableName = "mind_sessions")
data class MindSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val title: String,
    val durationSec: Int,
)

/** Weekly split: which body parts are trained on each weekday (empty = rest day). */
@Entity(tableName = "split_days")
data class SplitDay(
    @PrimaryKey val dayOfWeek: Int, // 1 = Monday … 7 = Sunday
    val parts: String,              // BodyPart.encode
)

enum class DayMode { PLAN, FAT_LOSS }

/** A generated (and possibly user-edited) training day. */
@Entity(tableName = "plan_days")
data class PlanDay(
    @PrimaryKey val epochDay: Long,
    val parts: String,
    val mode: DayMode = DayMode.PLAN,
    val completedAt: Long? = null,
)

@Entity(tableName = "plan_items")
data class PlanItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val bodyPart: BodyPart,
    val exerciseId: String,
    val position: Int,
    val sets: String, // SetSpec.encode
) {
    val setList: List<SetSpec> get() = SetSpec.decode(sets)
    val done: Boolean get() = setList.isNotEmpty() && setList.all { it.done }
}

/** Every completed set, used for exercise history and progressive weight suggestions. */
@Entity(tableName = "set_logs")
data class SetLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val exerciseId: String,
    val setIndex: Int,
    val reps: Int,
    val weightKg: Float,
    val seconds: Int,
    val loggedAt: Long = System.currentTimeMillis(),
)
