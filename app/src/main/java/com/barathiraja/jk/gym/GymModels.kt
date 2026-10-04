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
    val firstName get() = name.substringBefore(' ').ifBlank { name }
}

data class AssignedExercise(val exerciseId: String, val bodyPart: String, val sets: List<SetSpec>) {
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
    val volumeKg get() = exercises.sumOf { e -> e.sets.filter { it.done && !it.timed }.sumOf { (it.reps * it.weightKg).toDouble() } }
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
