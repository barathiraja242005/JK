package com.barathiraja.jk.data

import java.time.DayOfWeek

/** Body parts a training day can target, mirroring a classic gym split. */
enum class BodyPart(val label: String) {
    CHEST("Chest"), BACK("Back"), SHOULDER("Shoulder"), BICEPS("Biceps"), TRICEPS("Triceps"),
    LEGS("Legs"), ABDOMEN("Abdomen"), CARDIO("Cardio");

    companion object {
        fun parseList(s: String): List<BodyPart> =
            s.split(',').mapNotNull { n -> entries.firstOrNull { it.name == n.trim() } }
        fun encode(list: List<BodyPart>) = list.joinToString(",") { it.name }
    }
}

/** Equipment values match free-exercise-db's `equipment` field. */
enum class Equipment(val key: String, val label: String) {
    BODY_ONLY("body only", "Bodyweight"),
    DUMBBELL("dumbbell", "Dumbbells"),
    BARBELL("barbell", "Barbell"),
    EZ_BAR("e-z curl bar", "EZ curl bar"),
    CABLE("cable", "Cable machine"),
    MACHINE("machine", "Machines"),
    KETTLEBELL("kettlebells", "Kettlebells"),
    BANDS("bands", "Resistance bands"),
    EXERCISE_BALL("exercise ball", "Exercise ball"),
    MEDICINE_BALL("medicine ball", "Medicine ball"),
    OTHER("other", "Pull-up bar / other");

    companion object {
        fun of(key: String) = entries.firstOrNull { it.key == key } ?: OTHER
        val gymDefault = entries.toSet()
        val homeDefault = setOf(BODY_ONLY, DUMBBELL, BANDS, OTHER)
    }
}

/** Injuries / conditions; each one hides exercises that commonly aggravate it. */
enum class Injury(val label: String, private val avoid: Regex, private val avoidCategories: Set<String> = emptySet()) {
    KNEE("Knee pain", Regex("jump|lunge|leg extension|hack squat|full squat|step-up|step ups|tuck", RegexOption.IGNORE_CASE), setOf("plyometrics")),
    LOWER_BACK("Lower back pain", Regex("deadlift|good morning|bent over|hyperextension|superman|t-bar|sit-up|sit-ups|barbell squat|full squat|front barbell", RegexOption.IGNORE_CASE)),
    SHOULDER("Shoulder injury", Regex("press|upright|dips|handstand|behind the neck|pullover|military|lateral raise|front.*raise|flyes|crossover", RegexOption.IGNORE_CASE)),
    WRIST("Wrist pain", Regex("push-up|pushups|push up|handstand|plank|mountain|wrist|dips|bench dips", RegexOption.IGNORE_CASE)),
    NECK("Neck strain", Regex("shrug|neck|crunch|sit-up|handstand", RegexOption.IGNORE_CASE)),
    ANKLE("Ankle injury", Regex("jump|skipping|calf|running|jogging|butt kick|star|step", RegexOption.IGNORE_CASE), setOf("plyometrics"));

    fun excludes(e: Exercise) = avoid.containsMatchIn(e.name) || e.category in avoidCategories
}

/** One prescribed set. [seconds] > 0 means a timed set (plank, cardio). */
data class SetSpec(val reps: Int, val weightKg: Float = 0f, val seconds: Int = 0, val done: Boolean = false) {
    val timed get() = seconds > 0

    companion object {
        fun encode(list: List<SetSpec>) = list.joinToString("|") { "${it.reps}:${it.weightKg}:${it.seconds}:${if (it.done) 1 else 0}" }
        fun decode(s: String): List<SetSpec> = s.split('|').filter { it.isNotBlank() }.mapNotNull {
            val p = it.split(':')
            if (p.size != 4) null
            else SetSpec(p[0].toIntOrNull() ?: 0, p[1].toFloatOrNull() ?: 0f, p[2].toIntOrNull() ?: 0, p[3] == "1")
        }
    }
}

/** How the user wants to train; drives plan generation. */
data class TrainingPrefs(
    val setupDone: Boolean = false,
    val level: Level = Level.BEGINNER,
    val activeDays: Set<DayOfWeek> = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
    val equipment: Set<Equipment> = Equipment.homeDefault,
    val injuries: Set<Injury> = emptySet(),
    val restSec: Int = 60,
)

/**
 * Curated exercise pool per body part (ids from free-exercise-db). Photos for all of these are bundled
 * so daily training works offline.
 */
object TrainingPool {
    val byPart: Map<BodyPart, List<String>> = mapOf(
        BodyPart.CHEST to listOf("Barbell_Bench_Press_-_Medium_Grip", "Barbell_Incline_Bench_Press_-_Medium_Grip", "Decline_Barbell_Bench_Press",
            "Dumbbell_Bench_Press", "Incline_Dumbbell_Press", "Decline_Dumbbell_Bench_Press", "Dumbbell_Flyes", "Incline_Dumbbell_Flyes",
            "Cable_Crossover", "Low_Cable_Crossover", "Butterfly", "Machine_Bench_Press", "Cable_Chest_Press", "Straight-Arm_Dumbbell_Pullover",
            "Dips_-_Chest_Version", "Pushups", "Push-Up_Wide", "Incline_Push-Up", "Decline_Push-Up", "Pushups_Close_and_Wide_Hand_Positions",
            "Plyo_Push-up", "Single-Arm_Push-Up", "Alternating_Floor_Press"),
        BodyPart.BACK to listOf("Pullups", "Chin-Up", "Wide-Grip_Lat_Pulldown", "Close-Grip_Front_Lat_Pulldown", "V-Bar_Pulldown", "Seated_Cable_Rows",
            "Bent_Over_Barbell_Row", "Bent_Over_Two-Dumbbell_Row", "One-Arm_Dumbbell_Row", "T-Bar_Row_with_Handle", "Straight-Arm_Pulldown",
            "Barbell_Deadlift", "Dumbbell_Shrug", "Barbell_Shrug", "Hyperextensions_Back_Extensions", "Superman", "Inverted_Row",
            "Hyperextensions_With_No_Hyperextension_Bench", "Dumbbell_Incline_Row", "One-Arm_Kettlebell_Row"),
        BodyPart.SHOULDER to listOf("Dumbbell_Shoulder_Press", "Seated_Dumbbell_Press", "Arnold_Dumbbell_Press", "Standing_Military_Press",
            "Barbell_Shoulder_Press", "Machine_Shoulder_Military_Press", "Side_Lateral_Raise", "Cable_Seated_Lateral_Raise", "Front_Dumbbell_Raise",
            "Reverse_Flyes", "Reverse_Machine_Flyes", "Face_Pull", "Upright_Barbell_Row", "Cable_Rear_Delt_Fly", "Handstand_Push-Ups",
            "Standing_Dumbbell_Press", "Two-Arm_Kettlebell_Military_Press", "Front_Cable_Raise"),
        BodyPart.BICEPS to listOf("Barbell_Curl", "EZ-Bar_Curl", "Dumbbell_Bicep_Curl", "Hammer_Curls", "Incline_Dumbbell_Curl", "Concentration_Curls",
            "Preacher_Curl", "Machine_Preacher_Curls", "Standing_Biceps_Cable_Curl", "Cable_Hammer_Curls_-_Rope_Attachment", "Zottman_Curl",
            "Spider_Curl", "Dumbbell_Alternate_Bicep_Curl", "Cross_Body_Hammer_Curl", "Chin-Up"),
        BodyPart.TRICEPS to listOf("Triceps_Pushdown", "Triceps_Pushdown_-_Rope_Attachment", "Close-Grip_Barbell_Bench_Press", "EZ-Bar_Skullcrusher",
            "Dumbbell_One-Arm_Triceps_Extension", "Cable_Rope_Overhead_Triceps_Extension", "Tricep_Dumbbell_Kickback", "Bench_Dips",
            "Dips_-_Triceps_Version", "Body_Tricep_Press", "Close-Grip_Push-Up_off_of_a_Dumbbell", "Seated_Triceps_Press",
            "Reverse_Grip_Triceps_Pushdown"),
        BodyPart.LEGS to listOf("Barbell_Squat", "Barbell_Full_Squat", "Front_Barbell_Squat", "Leg_Press", "Leg_Extensions", "Lying_Leg_Curls",
            "Seated_Leg_Curl", "Romanian_Deadlift", "Stiff-Legged_Dumbbell_Deadlift", "Dumbbell_Lunges", "Barbell_Walking_Lunge", "Goblet_Squat",
            "Dumbbell_Squat", "Barbell_Hip_Thrust", "Standing_Calf_Raises", "Seated_Calf_Raise", "Dumbbell_Step_Ups", "Barbell_Hack_Squat",
            "Bodyweight_Squat", "Bodyweight_Walking_Lunge", "Split_Squats", "Freehand_Jump_Squat", "Butt_Lift_Bridge", "Single_Leg_Glute_Bridge",
            "Side_Leg_Raises", "Step-up_with_Knee_Raise", "Calf_Raises_-_With_Bands"),
        BodyPart.ABDOMEN to listOf("Crunches", "Plank", "Sit-Up", "Reverse_Crunch", "Russian_Twist", "Flat_Bench_Lying_Leg_Raise", "Hanging_Leg_Raise",
            "Cable_Crunch", "Ab_Crunch_Machine", "Dead_Bug", "Air_Bike", "Elbow_to_Knee", "Side_Bridge", "Mountain_Climbers", "Flutter_Kicks",
            "Oblique_Crunches_-_On_The_Floor", "Ab_Roller", "Jackknife_Sit-Up", "Cable_Russian_Twists", "Exercise_Ball_Crunch"),
        BodyPart.CARDIO to listOf("Running_Treadmill", "Jogging_Treadmill", "Walking_Treadmill", "Elliptical_Trainer", "Rowing_Stationary",
            "Bicycling_Stationary", "Stairmaster", "Recumbent_Bike", "Star_Jump", "Fast_Skipping", "Double_Leg_Butt_Kick", "Knee_Tuck_Jump",
            "Rope_Jumping", "Spider_Crawl", "Inchworm", "Mountain_Climbers", "Freehand_Jump_Squat", "Air_Bike", "One-Arm_Kettlebell_Swings"),
    )

    val warmUpper = listOf("Arm_Circles", "Shoulder_Circles", "Dynamic_Chest_Stretch", "Chest_And_Front_Of_Shoulder_Stretch", "Cat_Stretch",
        "Dynamic_Back_Stretch", "Round_The_World_Shoulder_Stretch", "Upward_Stretch", "Spinal_Stretch")
    val warmLower = listOf("Standing_Hip_Circles", "Knee_Circles", "Ankle_Circles", "Hamstring_Stretch", "Kneeling_Hip_Flexor", "Quad_Stretch",
        "Groiners", "Bodyweight_Squat", "Inchworm")
    val warmCardio = listOf("Fast_Skipping", "Double_Leg_Butt_Kick", "Star_Jump", "Inchworm", "Mountain_Climbers")

    /** Exercises held for time rather than counted (planks and continuous cardio). */
    val timedIds = setOf("Plank", "Side_Bridge", "Running_Treadmill", "Jogging_Treadmill", "Walking_Treadmill", "Elliptical_Trainer",
        "Rowing_Stationary", "Bicycling_Stationary", "Stairmaster", "Recumbent_Bike", "Rope_Jumping", "Fast_Skipping", "Double_Leg_Butt_Kick",
        "Star_Jump", "Mountain_Climbers", "Air_Bike", "Spider_Crawl", "Flutter_Kicks")
    val machineCardio = setOf("Running_Treadmill", "Jogging_Treadmill", "Walking_Treadmill", "Elliptical_Trainer", "Rowing_Stationary",
        "Bicycling_Stationary", "Stairmaster", "Recumbent_Bike")

    fun partOf(exerciseId: String): BodyPart? = byPart.entries.firstOrNull { exerciseId in it.value }?.key
}
