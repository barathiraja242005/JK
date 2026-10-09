package com.barathiraja.jk.gym

import com.barathiraja.jk.data.BodyPart
import com.barathiraja.jk.data.SetSpec

/**
 * Ready-made plans a trainer can start from (shown as "Suggested plans" when assigning). Each day loads like a
 * template: the exercises with their sets and reps, a short how-to [AssignedExercise.cue] per exercise, and a
 * [PlanDay.note] the member sees as the coach's note. Exercises are ids from the bundled exercise library.
 */
object PlanLibrary {

    /** A whole plan: its name, one sentence on how to run it, and its training days in order. */
    data class Plan(val name: String, val about: String, val days: List<PlanDay>)

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
    private const val PPL6 = "Push · Pull · Legs, 6 days"

    /** The 6-day plan has no tempo rule, only progressive overload and the warm-up. */
    private const val OVERLOAD_AND_WARM_UP =
        "Before each main exercise do 1–2 easy warm-up sets. When you hit the top of the rep range on every set, " +
            "add a little weight next time."

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

    /** A six-day split that runs Push, Pull, Legs twice with new exercises the second time, then one rest day. */
    val pushPullLegs6: List<PlanDay> = listOf(
        PlanDay(PPL6, "Day 1", "Push", "Chest, shoulders and triceps", OVERLOAD_AND_WARM_UP, listOf(
            ex("Barbell_Bench_Press_-_Medium_Grip", BodyPart.CHEST, "4", 6, 8, "Feet planted, shoulder blades pinned, lower to mid-chest and drive up."),
            ex("Incline_Dumbbell_Press", BodyPart.CHEST, "3", 8, 10, "Lower slowly to the upper chest, elbows slightly tucked, press smoothly."),
            ex("Seated_Dumbbell_Press", BodyPart.SHOULDER, "3", 8, 10, "Back on the pad, lower to ear height, press without arching."),
            ex("Side_Lateral_Raise", BodyPart.SHOULDER, "3", 12, 15, "Lead with the elbows, lift to shoulder height, no swinging."),
            ex("Cable_Crossover", BodyPart.CHEST, "3", 12, 15, "Soft elbows, hug the hands together at chest height, squeeze."),
            ex("Triceps_Pushdown", BodyPart.TRICEPS, "3", 10, 12, "Elbows pinned at your sides, extend fully, pause at the bottom."),
            ex("Cable_Rope_Overhead_Triceps_Extension", BodyPart.TRICEPS, "3", 10, 12, "Or with a dumbbell. Stretch behind the head, extend straight up."),
        )),
        PlanDay(PPL6, "Day 2", "Pull", "Back and biceps", OVERLOAD_AND_WARM_UP, listOf(
            ex("Pullups", BodyPart.BACK, "4", 6, 10, "Full hang at the bottom, chest to the bar. Use the assisted machine if needed."),
            ex("Bent_Over_Barbell_Row", BodyPart.BACK, "4", 8, 10, "Hinge to about 45°, flat back, pull the bar to the belly."),
            ex("Wide-Grip_Lat_Pulldown", BodyPart.BACK, "3", 10, 12, "Pull the elbows down to the ribs, chest tall, no momentum."),
            ex("Seated_Cable_Rows", BodyPart.BACK, "3", 10, 12, "Sit tall, pull to the belly button, control the way back."),
            ex("Face_Pull", BodyPart.SHOULDER, "3", 12, 15, "Rope to the forehead, elbows high, pull the hands apart."),
            ex("Dumbbell_Bicep_Curl", BodyPart.BICEPS, "3", 10, 12, "Elbows by your sides, palms up, curl slowly and squeeze."),
            ex("Hammer_Curls", BodyPart.BICEPS, "3", 10, 12, "Thumbs up, elbows still, lower slowly."),
        )),
        PlanDay(PPL6, "Day 3", "Legs", "Quads, hamstrings, glutes and calves", OVERLOAD_AND_WARM_UP, listOf(
            ex("Barbell_Squat", BodyPart.LEGS, "4", 6, 8, "Brace the core, squat to at least parallel, push through the whole foot."),
            ex("Romanian_Deadlift", BodyPart.LEGS, "3", 8, 10, "Soft knees, push the hips back, bar close to the legs, flat back."),
            ex("Leg_Press", BodyPart.LEGS, "3", 10, 12, "Feet mid-platform, knees to about 90°, don't lock out."),
            ex("Dumbbell_Lunges", BodyPart.LEGS, "3", 10, 10, "Walking, 10 steps each leg. Long stride, back knee close to the floor."),
            ex("Leg_Extensions", BodyPart.LEGS, "3", 12, 15, "Lift with the quads, pause at the top, lower slowly."),
            ex("Lying_Leg_Curls", BodyPart.LEGS, "3", 12, 15, "Hips down, curl smoothly, squeeze at the bottom."),
            ex("Standing_Calf_Raises", BodyPart.LEGS, "4", 12, 15, "Full stretch at the bottom, pause at the top, lower slowly."),
        )),
        PlanDay(PPL6, "Day 4", "Push", "Chest, shoulders and triceps, new exercises", OVERLOAD_AND_WARM_UP, listOf(
            ex("Barbell_Incline_Bench_Press_-_Medium_Grip", BodyPart.CHEST, "4", 6, 8, "Bench at 30°, lower to the upper chest, press up and slightly back."),
            ex("Machine_Bench_Press", BodyPart.CHEST, "3", 8, 10, "Shoulders back, squeeze the chest at the top, slow on the way back."),
            ex("Arnold_Dumbbell_Press", BodyPart.SHOULDER, "3", 8, 10, "Start palms facing you, rotate out as you press, reverse on the way down."),
            ex("Cable_Seated_Lateral_Raise", BodyPart.SHOULDER, "3", 12, 15, "Cable from the low pulley, lift to shoulder height, constant tension."),
            ex("Butterfly", BodyPart.CHEST, "3", 12, 15, "Pec deck. Elbows soft, bring the pads together, pause and squeeze."),
            ex("Close-Grip_Barbell_Bench_Press", BodyPart.TRICEPS, "3", 8, 10, "Hands shoulder-width, elbows tucked, lower to the lower chest."),
            ex("Triceps_Pushdown_-_Rope_Attachment", BodyPart.TRICEPS, "3", 10, 12, "Spread the rope at the bottom, elbows still."),
        )),
        PlanDay(PPL6, "Day 5", "Pull", "Back and biceps, new exercises", OVERLOAD_AND_WARM_UP, listOf(
            ex("Barbell_Deadlift", BodyPart.BACK, "4", 5, 6, "Bar over mid-foot, flat back, push the floor away, lock out with the hips."),
            ex("Dumbbell_Incline_Row", BodyPart.BACK, "3", 8, 10, "Chest-supported. Chest on the pad, pull to the lower chest, squeeze."),
            ex("Close-Grip_Front_Lat_Pulldown", BodyPart.BACK, "3", 10, 12, "Pull to the upper chest, lean back slightly, elbows down."),
            ex("Straight-Arm_Pulldown", BodyPart.BACK, "3", 12, 15, "Arms nearly straight, sweep the bar to the thighs with the lats."),
            ex("Reverse_Machine_Flyes", BodyPart.SHOULDER, "3", 12, 15, "Rear delt fly. Arms out wide, feel the rear delts, don't shrug."),
            ex("EZ-Bar_Curl", BodyPart.BICEPS, "3", 8, 10, "Elbows by your sides, curl up, lower for a slow count of three."),
            ex("Preacher_Curl", BodyPart.BICEPS, "3", 10, 12, "Arms flat on the pad, full stretch at the bottom, no bouncing."),
        )),
        PlanDay(PPL6, "Day 6", "Legs and abs", "Legs and core",
            "$OVERLOAD_AND_WARM_UP Rest tomorrow: focus on steps, water, protein and recovery.", listOf(
            ex("Front_Squat_Clean_Grip", BodyPart.LEGS, "4", 6, 8, "Elbows high, torso upright, squat between the heels."),
            ex("Split_Squats", BodyPart.LEGS, "3", 8, 10, "Bulgarian split squat, each leg. Back foot on a bench, drop straight down."),
            ex("Barbell_Hip_Thrust", BodyPart.LEGS, "3", 8, 12, "Upper back on the bench, drive through the heels, squeeze the glutes at the top."),
            ex("Leg_Extensions", BodyPart.LEGS, "3", 12, 15, "Squeeze the quads at the top, pause, lower slowly."),
            ex("Seated_Leg_Curl", BodyPart.LEGS, "3", 12, 15, "Thigh pad snug, curl all the way, control the way up."),
            ex("Hanging_Leg_Raise", BodyPart.ABDOMEN, "3", 12, 15, "No swinging, curl the hips up, lower slowly."),
            ex("Cable_Crunch", BodyPart.ABDOMEN, "3", 12, 15, "Kneel, rope by the head, crunch the ribs toward the hips."),
        )),
    )

    /** Every suggested plan, in the order they're shown. */
    val plans: List<Plan> = listOf(
        Plan(PPL6, "Push, Pull, Legs, then again with new exercises. Rest on Day 7.", pushPullLegs6),
        Plan(PPL, "Each muscle twice a week. Rest after Day 3 and Day 6.", pushPullLegs),
    )
}
