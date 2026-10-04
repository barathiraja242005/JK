package com.barathiraja.jk.gym

import java.time.YearMonth

/**
 * Everything the owner's home screen shows, computed from the gym's people and assignments.
 * Kept free of Android/Firebase so it can be unit-tested.
 */
object OwnerStats {
    /** Days without a finished workout before a member shows up in "Needs you". */
    const val IDLE_DAYS = 7

    /**
     * A member who has gone quiet. [assignedRecently] tells the owner who to talk to: false means the
     * trainer hasn't sent anything lately, so the trainer needs the nudge, not the member.
     */
    data class Idle(val member: Person, val days: Int, val assignedRecently: Boolean)

    data class TrainerRow(val trainer: Person, val members: Int, val rate: Float, val due: Int, val idle: Int)

    data class Digest(
        val members: Int,
        val trainedToday: Int,
        /** Members who finished a workout yesterday, for the "vs yesterday" line. */
        val trainedYesterday: Int,
        val assignedToday: Int,
        /** Members part-way through today's workout. */
        val inProgressToday: Int,
        val idle: List<Idle>,
        val trainers: List<TrainerRow>,
    )

    /**
     * [windowStart] is the first day assignments are loaded for; a member idle since before it is reported
     * as idle since then (the screen shows that as "30+ days").
     */
    fun digest(people: List<Person>, assignments: List<Assignment>, today: Long, month: YearMonth, windowStart: Long): Digest {
        val members = people.filter { it.role == Role.MEMBER && it.active }
        val memberIds = members.map { it.uid }.toSet()
        val mine = assignments.filter { it.memberUid in memberIds }
        val byMember = mine.groupBy { it.memberUid }

        val idle = members.mapNotNull { m ->
            val list = byMember[m.uid].orEmpty()
            val lastDone = list.filter { it.done && it.epochDay <= today }.maxOfOrNull { it.epochDay }
            val joined = if (m.joinedAt > 0) Scoring.dayOf(m.joinedAt) else windowStart
            val since = maxOf(lastDone ?: windowStart, joined, windowStart)
            val days = (today - since).toInt()
            if (days < IDLE_DAYS) return@mapNotNull null
            val recent = list.filter { it.epochDay in (today - IDLE_DAYS + 1)..today }
            Idle(m, days, recent.isNotEmpty())
        }.sortedByDescending { it.days }

        val idleByTrainer = idle.groupBy { it.member.trainerUid }
        val scores = Scoring.memberScores(mine, Scoring.monthRange(month), today)
        val trainers = people.filter { it.role == Role.TRAINER && it.active }.map { t ->
            val theirs = members.filter { it.trainerUid == t.uid }
            val s = theirs.mapNotNull { scores[it.uid] }
            val due = s.sumOf { it.due }
            TrainerRow(t, theirs.size, if (due == 0) 0f else s.sumOf { it.completed } / due.toFloat(), due, idleByTrainer[t.uid].orEmpty().size)
        }.sortedWith(compareByDescending<TrainerRow> { it.idle }.thenBy { it.rate })

        return Digest(
            members = members.size,
            trainedToday = mine.filter { it.epochDay == today && it.done }.map { it.memberUid }.distinct().size,
            trainedYesterday = mine.filter { it.epochDay == today - 1 && it.done }.map { it.memberUid }.distinct().size,
            assignedToday = mine.filter { it.epochDay == today }.map { it.memberUid }.distinct().size,
            inProgressToday = mine.filter { it.epochDay == today && it.status == AssignStatus.IN_PROGRESS }.map { it.memberUid }
                .distinct().count { uid -> mine.none { it.memberUid == uid && it.epochDay == today && it.done } },
            idle = idle,
            trainers = trainers,
        )
    }
}
