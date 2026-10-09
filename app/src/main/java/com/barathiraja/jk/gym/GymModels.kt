package com.barathiraja.jk.gym

import com.barathiraja.jk.data.SetSpec

enum class Role { OWNER, TRAINER, MEMBER }

/** PENDING = trainer waiting for the owner's approval; REMOVED = no longer part of the gym. */
enum class PersonStatus { PENDING, ACTIVE, REMOVED }

enum class AssignStatus { ASSIGNED, IN_PROGRESS, DONE }

data class Gym(val id: String, val name: String, val ownerUid: String, val gymCode: String)

data class Person(
    val uid: String,
    val name: String,
    val photoUrl: String?,
    val role: Role,
    val status: PersonStatus,
    /** Members: the trainer they belong to. */
    val trainerUid: String? = null,
    /** Trainers: the code members use to join them. */
    val trainerCode: String? = null,
    val joinedAt: Long = 0,
) {
    val active get() = status == PersonStatus.ACTIVE
    val firstName: String get() = firstNameOf(name)
}

/** What to call someone: the first real word of the name, skipping initials ("S. Janarthanan", "S.Janarthanan" -> "Janarthanan"). */
fun firstNameOf(name: String): String {
    val words = name.split(' ', '.').filter { it.isNotBlank() }
    return words.firstOrNull { it.length > 1 } ?: words.firstOrNull() ?: name
}

/** One exercise in a workout; [cue] is the trainer's short how-to for it (empty when there is none). */
data class AssignedExercise(val exerciseId: String, val bodyPart: String, val sets: List<SetSpec>, val cue: String = "") {
    val done get() = sets.isNotEmpty() && sets.all { it.done }
}

/** One workout a trainer assigned to one member for one day. */
data class Assignment(
    val id: String,
    val trainerUid: String,
    val memberUid: String,
    val title: String,
    val epochDay: Long,
    val exercises: List<AssignedExercise>,
    val status: AssignStatus = AssignStatus.ASSIGNED,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val verified: Boolean = false,
    val trainerNote: String = "",
    val memberNote: String = "",
) {
    val done get() = status == AssignStatus.DONE
    val setsTotal get() = exercises.sumOf { it.sets.size }
    val setsDone get() = exercises.sumOf { e -> e.sets.count { it.done } }
    val exercisesDone get() = exercises.count { it.done }
    /** Kilograms moved in completed sets. */
    /** Kilos lifted; each set is capped at believable numbers, since the server can't check every set. */
    val volumeKg get() = exercises.sumOf { e ->
        e.sets.filter { it.done && !it.timed }.sumOf { (it.reps.coerceIn(0, MAX_REPS) * it.weightKg.coerceIn(0f, MAX_KG)).toDouble() }
    }
}

data class Template(val id: String, val trainerUid: String, val title: String, val exercises: List<AssignedExercise>)

/** Winners of one month's awards, keyed by [Award]. */
data class MonthAwards(val month: String, val winners: Map<Award, String>)

enum class Award(val emoji: String, val label: String, val trainer: Boolean = false) {
    BEST_MEMBER("🥇", "Best Member"),
    BEST_TRAINER("🏅", "Best Trainer", trainer = true),
    MOST_CONSISTENT("🔥", "Most Consistent"),
    MOST_IMPROVED("📈", "Most Improved"),
    IRON_LIFTER("💪", "Iron Lifter"),
}

/** An award the owner hands out by hand, to a member or a trainer, with an optional prize. */
data class GivenAward(
    val id: String,
    val title: String,
    val emoji: String,
    val uid: String,
    val note: String,
    val month: String,
    val givenAt: Long,
)

/** The most reps and kilos one set can count for in scores and strength records. */
const val MAX_REPS = 100
const val MAX_KG = 500f
