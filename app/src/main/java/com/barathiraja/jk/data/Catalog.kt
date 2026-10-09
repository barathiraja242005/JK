package com.barathiraja.jk.data

import com.barathiraja.jk.domain.Health

enum class Level(val label: String) { BEGINNER("Beginner"), INTERMEDIATE("Intermediate"), ADVANCED("Advanced") }

/** One block in a workout: either timed ([seconds]) or counted ([reps]). */
data class Block(val exerciseId: String, val seconds: Int = 0, val reps: Int = 0) {
    /** Falls back to a bare placeholder if the id is missing from the database, instead of crashing the screen. */
    val exercise: Exercise get() = ExerciseRepo.get(exerciseId) ?: missingExercise(exerciseId)
    val isTimed get() = seconds > 0
    val label get() = if (isTimed) "${seconds}s" else "x$reps"

    fun encode() = if (isTimed) "$exerciseId:s:$seconds" else "$exerciseId:r:$reps"

    companion object {
        fun decodeAll(s: String): List<Block> = s.split(';').filter { it.isNotBlank() }.mapNotNull {
            val p = it.split(':')
            if (p.size != 3) return@mapNotNull null
            val n = p[2].toIntOrNull() ?: return@mapNotNull null
            if (p[1] == "s") Block(p[0], seconds = n) else Block(p[0], reps = n)
        }
        fun encodeAll(blocks: List<Block>) = blocks.joinToString(";") { it.encode() }
    }
}

data class Workout(
    val id: String,
    val title: String,
    val subtitle: String,
    val place: Place,
    val level: Level,
    val focus: String,
    val restSec: Int,
    val rounds: Int,
    val blocks: List<Block>,
) {
    /** Rough estimate: reps count ~3s each. */
    val estimatedSec: Int
        get() {
            val work = blocks.sumOf { if (it.isTimed) it.seconds else it.reps * 3 }
            return rounds * (work + restSec * blocks.size)
        }
    val estimatedMin get() = Health.minutesUp(estimatedSec)
    val cover: Exercise get() = blocks.firstOrNull()?.exercise ?: missingExercise(id)
}

/** Stand-in for an exercise id the bundled database doesn't know (e.g. a failed load or an old saved workout). */
private fun missingExercise(id: String) = Exercise(
    id = id, name = id.replace('_', ' '), category = "", equipment = "", level = "", force = null, mechanic = null,
    primary = emptyList(), secondary = emptyList(), instructions = emptyList(),
)

data class Challenge(
    val id: String,
    val title: String,
    val tagline: String,
    val about: String,
    val level: Level,
    val days: Int = 30,
    val plan: (day: Int) -> Workout?, // null = rest day
)

private fun s(id: String, sec: Int) = Block(id, seconds = sec)
private fun r(id: String, reps: Int) = Block(id, reps = reps)

object Catalog {
    // Exercise ids from free-exercise-db.
    const val STAR_JUMP = "Star_Jump"
    const val SQUAT = "Bodyweight_Squat"
    const val PUSHUP = "Pushups"
    const val INCLINE_PUSHUP = "Incline_Push-Up"
    const val DECLINE_PUSHUP = "Decline_Push-Up"
    const val LUNGE = "Bodyweight_Walking_Lunge"
    const val PLANK = "Plank"
    const val CLIMBER = "Mountain_Climbers"
    const val BRIDGE = "Butt_Lift_Bridge"
    const val SL_BRIDGE = "Single_Leg_Glute_Bridge"
    const val CRUNCH = "Crunches"
    const val SITUP = "Sit-Up"
    const val TUCK_JUMP = "Knee_Tuck_Jump"
    const val JUMP_SQUAT = "Freehand_Jump_Squat"
    const val DIP = "Bench_Dips"
    const val SUPERMAN = "Superman"
    const val FLUTTER = "Flutter_Kicks"
    const val TWIST = "Russian_Twist"
    const val LEG_RAISE = "Flat_Bench_Lying_Leg_Raise"
    const val SIDE_BRIDGE = "Side_Bridge"
    const val AIR_BIKE = "Air_Bike"
    const val ELBOW_KNEE = "Elbow_to_Knee"
    const val INCHWORM = "Inchworm"
    const val BUTT_KICK = "Double_Leg_Butt_Kick"
    const val SKIP = "Fast_Skipping"
    const val SPLIT_SQUAT = "Split_Squats"
    const val SIDE_LEG = "Side_Leg_Raises"
    const val STEP_UP = "Step-up_with_Knee_Raise"
    const val SPIDER = "Spider_Crawl"
    const val PLYO_PUSH = "Plyo_Push-up"
    const val CHILD = "Childs_Pose"
    const val CAT = "Cat_Stretch"
    const val HAM_STRETCH = "Hamstring_Stretch"
    const val HIP_FLEXOR = "Kneeling_Hip_Flexor"
    const val ARM_CIRCLES = "Arm_Circles"
    const val CHEST_STRETCH = "Dynamic_Chest_Stretch"
    const val SPINAL = "Spinal_Stretch"
    const val BODY_TRI = "Body_Tricep_Press"
    const val CHIN_UP = "Chin-Up"

    const val GOBLET = "Goblet_Squat"
    const val DB_BENCH = "Dumbbell_Bench_Press"
    const val DB_ROW = "Bent_Over_Two-Dumbbell_Row"
    const val RDL = "Romanian_Deadlift"
    const val DB_PRESS = "Dumbbell_Shoulder_Press"
    const val LAT_PULL = "Wide-Grip_Lat_Pulldown"
    const val LEG_PRESS = "Leg_Press"
    const val DB_CURL = "Dumbbell_Bicep_Curl"
    const val HAMMER = "Hammer_Curls"
    const val ROWER = "Rowing_Stationary"
    const val LATERAL = "Side_Lateral_Raise"
    const val PUSHDOWN = "Triceps_Pushdown"
    const val BB_SQUAT = "Barbell_Squat"
    const val DEADLIFT = "Barbell_Deadlift"
    const val BENCH = "Barbell_Bench_Press_-_Medium_Grip"
    const val CALF = "Seated_Calf_Raise"
    const val FLYES = "Dumbbell_Flyes"
    const val FACE_PULL = "Face_Pull"
    const val HYPER = "Hyperextensions_Back_Extensions"
    const val TREADMILL = "Running_Treadmill"
    const val ELLIPTICAL = "Elliptical_Trainer"
    const val DB_STEP_UP = "Dumbbell_Step_Ups"
    const val KB_SWING = "One-Arm_Kettlebell_Swings"
    const val CABLE_ROW = "Seated_Cable_Rows"
    const val CROSSOVER = "Cable_Crossover"
    const val LEG_EXT = "Leg_Extensions"
    const val LEG_CURL = "Lying_Leg_Curls"
    const val HANG_RAISE = "Hanging_Leg_Raise"
    const val DB_LUNGE = "Dumbbell_Lunges"

    val workouts: List<Workout> = listOf(
        // ---------- Home ----------
        Workout("home_warmup", "5-Minute Warm-Up", "Prime your joints before any session", Place.HOME, Level.BEGINNER,
            "Warm-up", 5, 1, listOf(s(ARM_CIRCLES, 30), s(SKIP, 30), s(BUTT_KICK, 30), s(INCHWORM, 40), s(SQUAT, 40), s(STAR_JUMP, 30))),
        Workout("home_starter", "Starter Burn", "Gentle full-body circuit", Place.HOME, Level.BEGINNER,
            "Full body", 20, 2, listOf(s(STAR_JUMP, 30), r(SQUAT, 12), r(INCLINE_PUSHUP, 10), r(BRIDGE, 12), s(PLANK, 25))),
        Workout("home_core", "Core Crusher", "Abs and stability", Place.HOME, Level.BEGINNER,
            "Core", 15, 2, listOf(r(CRUNCH, 15), s(CLIMBER, 30), r(SUPERMAN, 12), s(FLUTTER, 30), s(PLANK, 30))),
        Workout("home_fatburn", "Fat Burner", "Low-impact cardio that still sweats", Place.HOME, Level.BEGINNER,
            "Cardio", 15, 3, listOf(s(SKIP, 40), s(BUTT_KICK, 40), r(SQUAT, 15), s(STEP_UP, 40), s(AIR_BIKE, 30))),
        Workout("home_legs", "Leg Day at Home", "Quads, glutes & hamstrings", Place.HOME, Level.INTERMEDIATE,
            "Legs", 20, 3, listOf(r(SQUAT, 20), r(LUNGE, 16), r(SPLIT_SQUAT, 10), r(SL_BRIDGE, 12), r(SIDE_LEG, 15))),
        Workout("home_upper", "Upper Body Builder", "Push strength without equipment", Place.HOME, Level.INTERMEDIATE,
            "Upper body", 20, 3, listOf(r(PUSHUP, 12), r(DIP, 12), r(DECLINE_PUSHUP, 8), r(BODY_TRI, 10), r(SUPERMAN, 12))),
        Workout("home_abs", "Six-Pack Sculpt", "Every angle of your core", Place.HOME, Level.INTERMEDIATE,
            "Core", 15, 3, listOf(r(SITUP, 15), r(TWIST, 20), r(LEG_RAISE, 12), r(ELBOW_KNEE, 16), s(SIDE_BRIDGE, 30), s(PLANK, 45))),
        Workout("home_glutes", "Glute Lab", "Shape and strengthen your glutes", Place.HOME, Level.INTERMEDIATE,
            "Glutes", 20, 3, listOf(r(BRIDGE, 20), r(SL_BRIDGE, 12), r(SPLIT_SQUAT, 12), r(SIDE_LEG, 20), r(SQUAT, 20))),
        Workout("home_hiit", "HIIT Inferno", "Short and brutal fat burner", Place.HOME, Level.ADVANCED,
            "Cardio", 15, 4, listOf(s(JUMP_SQUAT, 40), s(TUCK_JUMP, 30), s(CLIMBER, 40), s(PLYO_PUSH, 30), s(STAR_JUMP, 40))),
        Workout("home_strength", "Bodyweight Beast", "Advanced calisthenics strength", Place.HOME, Level.ADVANCED,
            "Full body", 30, 4, listOf(r(PLYO_PUSH, 10), r(JUMP_SQUAT, 15), r(DECLINE_PUSHUP, 15), r(SPLIT_SQUAT, 14), s(SPIDER, 40), s(PLANK, 60))),
        Workout("home_mobility", "Recovery Flow", "Easy stretch for rest days", Place.HOME, Level.BEGINNER,
            "Mobility", 5, 1, listOf(s(CAT, 45), s(CHILD, 60), s(HIP_FLEXOR, 45), s(HAM_STRETCH, 45), s(SPINAL, 45), s(CHEST_STRETCH, 40))),
        Workout("home_desk", "Desk Break Reset", "Undo hours of sitting in 6 minutes", Place.HOME, Level.BEGINNER,
            "Mobility", 5, 1, listOf(s(ARM_CIRCLES, 30), s(CHEST_STRETCH, 40), s(HIP_FLEXOR, 45), r(SQUAT, 10), s(SPINAL, 40), s(CAT, 40))),

        // ---------- Gym ----------
        Workout("gym_full", "Gym Foundations", "Full-body strength basics", Place.GYM, Level.BEGINNER,
            "Full body", 60, 3, listOf(r(GOBLET, 10), r(DB_BENCH, 10), r(LAT_PULL, 10), r(DB_PRESS, 10), s(PLANK, 30))),
        Workout("gym_cardio_easy", "Cardio Starter", "Machines for fat loss", Place.GYM, Level.BEGINNER,
            "Cardio", 30, 1, listOf(s(TREADMILL, 300), s(ELLIPTICAL, 300), s(ROWER, 240))),
        Workout("gym_upper_b", "Upper Body Basics", "Machine and dumbbell upper body", Place.GYM, Level.BEGINNER,
            "Upper body", 60, 3, listOf(r(DB_BENCH, 12), r(CABLE_ROW, 12), r(LATERAL, 12), r(DB_CURL, 12), r(PUSHDOWN, 12))),
        Workout("gym_push", "Push Power", "Chest, shoulders, triceps", Place.GYM, Level.INTERMEDIATE,
            "Push", 75, 4, listOf(r(BENCH, 8), r(DB_PRESS, 10), r(FLYES, 12), r(LATERAL, 15), r(PUSHDOWN, 12))),
        Workout("gym_pull", "Pull Strength", "Back and biceps", Place.GYM, Level.INTERMEDIATE,
            "Pull", 75, 4, listOf(r(LAT_PULL, 10), r(DB_ROW, 10), r(FACE_PULL, 15), r(DB_CURL, 12), r(HAMMER, 12))),
        Workout("gym_legs_i", "Leg Builder", "Machines + free weights", Place.GYM, Level.INTERMEDIATE,
            "Legs", 75, 4, listOf(r(LEG_PRESS, 12), r(DB_LUNGE, 12), r(LEG_EXT, 15), r(LEG_CURL, 12), r(CALF, 15))),
        Workout("gym_chest", "Chest Day", "Build a bigger chest", Place.GYM, Level.INTERMEDIATE,
            "Chest", 75, 4, listOf(r(BENCH, 10), r(DB_BENCH, 10), r(FLYES, 12), r(CROSSOVER, 15), r(DIP, 12))),
        Workout("gym_cardio", "Engine Builder", "Conditioning on the rower", Place.GYM, Level.INTERMEDIATE,
            "Cardio", 30, 5, listOf(s(ROWER, 60), r(KB_SWING, 15), s(AIR_BIKE, 30))),
        Workout("gym_legs", "Heavy Legs", "Lower-body strength", Place.GYM, Level.ADVANCED,
            "Legs", 90, 5, listOf(r(BB_SQUAT, 6), r(RDL, 8), r(LEG_PRESS, 12), r(DB_STEP_UP, 10), r(CALF, 15))),
        Workout("gym_power", "Power Lifts", "Squat, bench, deadlift", Place.GYM, Level.ADVANCED,
            "Strength", 120, 5, listOf(r(BB_SQUAT, 5), r(BENCH, 5), r(DEADLIFT, 5))),
        Workout("gym_back_core", "Back & Core Armor", "Posture and a strong midsection", Place.GYM, Level.ADVANCED,
            "Back", 60, 4, listOf(r(CHIN_UP, 8), r(CABLE_ROW, 12), r(HYPER, 15), r(HANG_RAISE, 12), r(FACE_PULL, 15))),
    )

    val challenges: List<Challenge> = listOf(
        Challenge("pushup30", "30-Day Push-Up", "From 5 to 50 push-ups",
            "Build chest, shoulder and arm strength with a push-up ladder that grows every day. Every 4th day is lighter.",
            Level.BEGINNER) { d ->
            val reps = 5 + (d - 1) * 3 / 2
            if (d % 7 == 0) null else Workout("challenge:pushup30:$d", "Push-Up · Day $d", "${reps} reps per set", Place.HOME,
                Level.BEGINNER, "Chest", 45, 3,
                listOf(r(INCLINE_PUSHUP, (reps / 2).coerceAtLeast(5)), r(PUSHUP, reps), s(PLANK, 20 + d)))
        },
        Challenge("squat30", "30-Day Squat", "Stronger legs, rounder glutes",
            "A progressive squat challenge, adding reps every day with bridges for balance.", Level.BEGINNER) { d ->
            val reps = 15 + d * 2
            if (d % 7 == 0) null else Workout("challenge:squat30:$d", "Squat · Day $d", "$reps squats", Place.HOME,
                Level.BEGINNER, "Legs", 30, 3, listOf(r(SQUAT, reps / 3 + 5), r(BRIDGE, 15), r(LUNGE, 10 + d / 3)))
        },
        Challenge("plank30", "30-Day Plank", "Hold 20s → 3 minutes",
            "Steel core in a month. Each day adds a few seconds to your plank holds.", Level.BEGINNER) { d ->
            val hold = 20 + d * 5
            if (d % 7 == 0) null else Workout("challenge:plank30:$d", "Plank · Day $d", "${hold}s holds", Place.HOME,
                Level.BEGINNER, "Core", 30, 2, listOf(s(PLANK, hold), s(SIDE_BRIDGE, hold / 2), s(SIDE_BRIDGE, hold / 2)))
        },
        Challenge("abs30", "30-Day Abs", "Visible core definition",
            "Rotating core circuits with growing volume. Pair with a calorie deficit to reveal your abs.", Level.INTERMEDIATE) { d ->
            if (d % 7 == 0) null else {
                val v = 10 + d
                Workout("challenge:abs30:$d", "Abs · Day $d", "Core circuit", Place.HOME, Level.INTERMEDIATE, "Core", 20, 3,
                    listOf(r(CRUNCH, v), r(TWIST, v), r(LEG_RAISE, v / 2 + 4), s(CLIMBER, 20 + d), s(PLANK, 30 + d)))
            }
        },
        Challenge("transform30", "30-Day Transformation", "Full-body fat loss plan",
            "A complete month mixing HIIT, strength and mobility, with planned recovery days. Take a before photo on day 1!",
            Level.INTERMEDIATE) { d ->
            val pool = listOf("home_starter", "home_fatburn", "home_legs", "home_upper", "home_abs", "home_glutes", "home_hiit")
            when {
                d % 7 == 0 -> null
                d % 7 == 4 -> byId("home_mobility")?.copy(id = "challenge:transform30:$d", title = "Transform · Day $d")
                else -> byId(pool[(d - 1) % pool.size])?.copy(id = "challenge:transform30:$d", title = "Transform · Day $d")
            }
        },
        Challenge("hiit21", "21-Day HIIT Shred", "Short, intense, every day",
            "Three weeks of high-intensity intervals. For people who already train regularly.", Level.ADVANCED, days = 21) { d ->
            if (d % 7 == 0) null else Workout("challenge:hiit21:$d", "Shred · Day $d", "HIIT", Place.HOME, Level.ADVANCED,
                "Cardio", 15, 3 + d / 7, listOf(s(JUMP_SQUAT, 30 + d), s(CLIMBER, 30 + d), s(TUCK_JUMP, 20 + d / 2), s(PLYO_PUSH, 20 + d / 2)))
        },
    )

    fun byId(id: String): Workout? = workouts.firstOrNull { it.id == id }
    fun challenge(id: String): Challenge? = challenges.firstOrNull { it.id == id }

    /** Resolves built-in and challenge workout ids ("challenge:<id>:<day>"). Custom ids are resolved by the ViewModel. */
    fun resolve(id: String): Workout? {
        if (id.startsWith("challenge:")) {
            val p = id.split(':')
            return challenge(p.getOrNull(1) ?: return null)?.plan?.invoke(p.getOrNull(2)?.toIntOrNull() ?: return null)
        }
        return byId(id)
    }

    /** Weekly rotation so "today's workout" changes day to day. */
    fun todayFor(place: Place, level: Level, epochDay: Long): Workout {
        val pool = workouts.filter { it.place == place && it.focus != "Warm-up" }
        val preferred = pool.filter { it.level == level }.ifEmpty { pool }
        if (place == Place.HOME && epochDay % 7 == 6L) byId("home_mobility")?.let { return it }
        return preferred[(epochDay % preferred.size).toInt()]
    }

    /** Every exercise id referenced by built-in content (used to decide which photos to bundle). */
    fun referencedIds(): Set<String> = buildSet {
        workouts.forEach { w -> w.blocks.forEach { add(it.exerciseId) } }
        challenges.forEach { c -> (1..c.days).forEach { d -> c.plan(d)?.blocks?.forEach { add(it.exerciseId) } } }
    }
}
