package com.barathiraja.jk.gym

import com.barathiraja.jk.data.BodyPart
import com.barathiraja.jk.data.SetSpec

/**
 * Ready-made plans a trainer can start from (shown as "Suggested plans" when assigning). Each day loads like a
 * template: the exercises with their sets and reps, a short how-to [AssignedExercise.cue] per exercise, and a
 * [PlanDay.note] the member sees as the coach's note. Exercises are ids from the bundled exercise library.
 */
object PlanLibrary {

    /** One training day of a plan. */
    data class PlanDay(
        val plan: String,
        val day: String,
        val title: String,
        val focus: String,
        val note: String,
        val exercises: List<AssignedExercise>,
    )

    /** Every rep of this plan uses the same tempo and warm-up, so they go in each day's note. */
    private const val TEMPO_AND_WARM_UP =
        "Tempo 3:1:2:1 on every rep: 3 s down, 1 s pause, 2 s up, 1 s pause. " +
            "Before each main exercise do 1–2 easy warm-up sets at 60–75% of your working weight."

    private const val PPL = "Push · Pull · Legs, 5 days"

    /**
     * An exercise prescribed as [sets] sets of [repsLow]–[repsHigh]; the app stores one rep target per set, so it
     * uses the top of the range and the cue keeps the full range in words.
     */
    private fun ex(id: String, part: BodyPart, sets: String, repsLow: Int, repsHigh: Int, cue: String): AssignedExercise {
        val count = sets.substringBefore('–').trim().toInt()
        return AssignedExercise(id, part.name, List(count) { SetSpec(repsHigh) }, "$sets sets of $repsLow–$repsHigh. $cue")
    }

    /**
     * A five-day split that trains each muscle twice a week (Days 1–3, rest, Days 5–6, rest). Where the plan offers
     * a choice of exercise, the first is loaded and the cue names the other.
     */
    val pushPullLegs: List<PlanDay> = listOf(
        PlanDay(PPL, "Day 1", "Push", "Chest, shoulders and triceps", TEMPO_AND_WARM_UP, listOf(
            ex("Incline_Dumbbell_Press", BodyPart.CHEST, "3–4", 8, 10, "Lower slowly to the upper chest, elbows slightly tucked, press smoothly."),
            ex("Machine_Bench_Press", BodyPart.CHEST, "3", 10, 12, "Shoulders back, grip a little wide, squeeze the chest at the top."),
            ex("Machine_Shoulder_Military_Press", BodyPart.SHOULDER, "3", 8, 10, "Lower to chin height, keep the core braced, press with control."),
            ex("Side_Lateral_Raise", BodyPart.SHOULDER, "3", 12, 15, "Lead with the elbows, lift to shoulder height, no swinging."),
            ex("Triceps_Pushdown", BodyPart.TRICEPS, "3", 12, 15, "Elbows pinned at your sides, extend fully, pause at the bottom."),
            ex("Cable_Rope_Overhead_Triceps_Extension", BodyPart.TRICEPS, "3", 12, 15, "Stretch behind the head, extend straight up, keep the back still."),
        )),
        PlanDay(PPL, "Day 2", "Pull", "Back, rear delts and biceps", TEMPO_AND_WARM_UP, listOf(
            ex("Wide-Grip_Lat_Pulldown", BodyPart.BACK, "3–4", 8, 10, "Pull the elbows down to the ribs, chest tall, no momentum."),
            ex("Dumbbell_Incline_Row", BodyPart.BACK, "3", 8, 10, "Chest on the bench, pull to the lower chest, squeeze the shoulder blades."),
            ex("Seated_Cable_Rows", BodyPart.BACK, "3", 10, 12, "Sit tall, pull to the belly button, control the way back."),
            ex("Reverse_Flyes", BodyPart.SHOULDER, "3", 12, 15, "Arms out wide, feel the rear delts, don't shrug."),
            ex("Dumbbell_Bicep_Curl", BodyPart.BICEPS, "3", 10, 12, "Elbows by your sides, palms up, curl slowly and squeeze."),
            ex("Incline_Dumbbell_Curl", BodyPart.BICEPS, "2–3", 10, 12, "Let the arms stretch fully at the bottom, curl slowly, no swinging."),
        )),
        PlanDay(PPL, "Day 3", "Legs", "Quads, hamstrings and calves", TEMPO_AND_WARM_UP, listOf(
            ex("Barbell_Squat", BodyPart.LEGS, "3–4", 6, 8, "Or Hack Squat. Brace the core, squat under control, push through the whole foot."),
            ex("Leg_Press", BodyPart.LEGS, "3", 10, 12, "Feet mid-platform, knees to about 90°, don't lock out."),
            ex("Leg_Extensions", BodyPart.LEGS, "3", 12, 15, "Lift with the quads, pause at the top, lower slowly."),
            ex("Lying_Leg_Curls", BodyPart.LEGS, "3", 10, 12, "Hips down, curl smoothly, squeeze at the bottom."),
            ex("Standing_Calf_Raises", BodyPart.LEGS, "3–4", 12, 15, "Full stretch at the bottom, push through the big toe, lower slowly."),
        )),
        PlanDay(PPL, "Day 5", "Chest, back and arms", "Second chest and back day, plus arms", TEMPO_AND_WARM_UP, listOf(
            ex("Dumbbell_Bench_Press", BodyPart.CHEST, "3–4", 6, 8, "Lower smoothly, elbows tucked, press evenly."),
            ex("Cable_Crossover", BodyPart.CHEST, "3", 12, 15, "Slight bend in the elbows, bring the hands together at chest height."),
            ex("V-Bar_Pulldown", BodyPart.BACK, "3", 10, 12, "Neutral grip. Pull the elbows down and forward, keep the chest up."),
            ex("Seated_Cable_Rows", BodyPart.BACK, "3", 10, 12, "Pull to the stomach, squeeze the mid-back, return slowly."),
            ex("Standing_Biceps_Cable_Curl", BodyPart.BICEPS, "3", 12, 15, "Elbows still, curl smoothly, full squeeze."),
            ex("Dips_-_Triceps_Version", BodyPart.TRICEPS, "3", 8, 10, "Or the dip machine. Lower to about 90°, push up under control."),
        )),
        PlanDay(PPL, "Day 6", "Shoulders and legs", "Second shoulder and leg day",
            "$TEMPO_AND_WARM_UP Rest tomorrow: focus on steps, water, protein and recovery.", listOf(
            ex("Dumbbell_Shoulder_Press", BodyPart.SHOULDER, "3–4", 6, 8, "Lower to chin height, don't lock the elbows, stay braced."),
            ex("Side_Lateral_Raise", BodyPart.SHOULDER, "3", 12, 15, "Lift to shoulder height, soft elbows, lower slowly."),
            ex("Front_Squat_Clean_Grip", BodyPart.LEGS, "3", 6, 8, "Or Hack Squat. Torso upright, let the knees travel forward comfortably."),
            ex("Lying_Leg_Curls", BodyPart.LEGS, "3", 10, 12, "Curl smoothly, squeeze hard, control the stretch."),
            ex("Leg_Extensions", BodyPart.LEGS, "3", 12, 15, "Squeeze the quads at the top, pause, lower slowly."),
        )),
    )

    /** All suggested days, in the order they're shown. */
    val all: List<PlanDay> = pushPullLegs
}
