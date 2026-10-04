package com.barathiraja.jk.gym

import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Points, rankings and monthly awards. Everything is computed from the raw assignments that every phone in
 * the gym can read, so all phones agree without a server.
 */
object Scoring {
    const val COMPLETED = 10
    const val ON_TIME = 5
    const val SET_POINT = 1
    const val SET_CAP_PER_DAY = 30
    const val VERIFIED = 5
    const val PERFECT_WEEK = 20
    const val VERIFIED_CAP = 50

    /** Minimum due workouts before someone can win a rate-based award. */
    const val MIN_DUE_CONSISTENT = 8
    const val MIN_DUE_IMPROVED = 4

    data class MemberScore(
        val uid: String,
        val points: Int,
        val due: Int,
        val completed: Int,
        val setsDone: Int,
        val volumeKg: Double,
    ) {
        val rate get() = if (due == 0) 0f else completed / due.toFloat()
    }

    data class TrainerScore(val uid: String, val score: Int, val members: Int, val activeMembers: Int, val rate: Float)

    fun dayOf(millis: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
        java.time.Instant.ofEpochMilli(millis).atZone(zone).toLocalDate().toEpochDay()

    fun monthRange(month: YearMonth): LongRange = month.atDay(1).toEpochDay()..month.atEndOfMonth().toEpochDay()

    /**
     * Scores for every member with at least one assignment in [range]. Only past workouts (and today's, once
     * done) count toward the completion rate, so upcoming ones don't drag it down.
     */
    fun memberScores(assignments: List<Assignment>, range: LongRange, today: Long, zone: ZoneId = ZoneId.systemDefault()): Map<String, MemberScore> =
        assignments.filter { it.epochDay in range }.groupBy { it.memberUid }.mapValues { (uid, list) ->
            var points = 0
            list.filter { it.done }.forEach { a ->
                points += COMPLETED
                if (a.completedAt != null && dayOf(a.completedAt, zone) <= a.epochDay) points += ON_TIME
                if (a.verified) points += VERIFIED
            }
            // Logged sets, capped per day so nobody farms points with 200-set workouts.
            points += list.groupBy { it.epochDay }.values.sumOf { day -> minOf(day.sumOf { it.setsDone } * SET_POINT, SET_CAP_PER_DAY) }
            // Perfect weeks: every workout that week was completed. Only finished weeks count, so the bonus
            // never appears and then disappears mid-week.
            points += list.groupBy { weekOf(it.epochDay) }.filterKeys { weekOf(today) > it }.values
                .count { week -> week.all { it.done } } * PERFECT_WEEK
            // Today's workout only counts once it's done; the member still has the rest of the day.
            val due = list.filter { it.epochDay < today || (it.epochDay == today && it.done) }
            MemberScore(uid, points, due.size, due.count { it.done }, list.sumOf { it.setsDone }, list.sumOf { it.volumeKg })
        }

    /**
     * Trainers are ranked on how well their members follow through, not on how many workouts they send:
     * average member completion rate × 100, plus 2 per member who completed something, plus verified sessions (capped).
     */
    fun trainerScores(people: List<Person>, assignments: List<Assignment>, range: LongRange, today: Long): List<TrainerScore> {
        val members = memberScores(assignments, range, today)
        return people.filter { it.role == Role.TRAINER && it.active }.map { t ->
            val mine = people.filter { it.role == Role.MEMBER && it.active && it.trainerUid == t.uid }
            val withDue = mine.mapNotNull { members[it.uid] }.filter { it.due > 0 }
            val rate = if (withDue.isEmpty()) 0f else withDue.map { it.rate }.average().toFloat()
            val active = mine.count { (members[it.uid]?.completed ?: 0) > 0 }
            val verified = assignments.count { it.trainerUid == t.uid && it.epochDay in range && it.verified && it.done }
            TrainerScore(t.uid, Math.round(rate * 100) + 2 * active + minOf(verified, VERIFIED_CAP), mine.size, active, rate)
        }.sortedWith(compareByDescending<TrainerScore> { it.score }.thenByDescending { it.rate }.thenBy { it.uid })
    }

    /** Members sorted for the leaderboard; ties go to more completed workouts, then a stable order. */
    fun memberRanking(scores: Map<String, MemberScore>): List<MemberScore> =
        scores.values.sortedWith(compareByDescending<MemberScore> { it.points }.thenByDescending { it.completed }.thenBy { it.uid })

    /**
     * Winners for [month]; categories nobody qualifies for are left out. [asOf] defaults to the month's last
     * day; pass today to get the current leaders of a month still in progress.
     */
    fun awards(people: List<Person>, assignments: List<Assignment>, month: YearMonth, asOf: Long = monthRange(month).last): Map<Award, String> {
        val range = monthRange(month)
        val end = asOf
        val activeMembers = people.filter { it.role == Role.MEMBER && it.active }.map { it.uid }.toSet()
        val scores = memberScores(assignments, range, end).filterKeys { it in activeMembers }
        val prev = memberScores(assignments, monthRange(month.minusMonths(1)), end)
        val out = mutableMapOf<Award, String>()

        memberRanking(scores).firstOrNull { it.points > 0 }?.let { out[Award.BEST_MEMBER] = it.uid }
        trainerScores(people, assignments, range, end).firstOrNull { it.activeMembers > 0 }?.let { out[Award.BEST_TRAINER] = it.uid }
        scores.values.filter { it.due >= MIN_DUE_CONSISTENT && it.completed > 0 }
            .sortedWith(compareByDescending<MemberScore> { it.rate }.thenByDescending { it.completed }.thenBy { it.uid })
            .firstOrNull()?.let { out[Award.MOST_CONSISTENT] = it.uid }
        scores.values.mapNotNull { s ->
            val p = prev[s.uid] ?: return@mapNotNull null
            if (s.due < MIN_DUE_IMPROVED || p.due < MIN_DUE_IMPROVED) null else s to (s.rate - p.rate)
        }.filter { it.second > 0f }
            .sortedWith(compareByDescending<Pair<MemberScore, Float>> { it.second }.thenBy { it.first.uid })
            .firstOrNull()?.let { out[Award.MOST_IMPROVED] = it.first.uid }
        scores.values.filter { it.volumeKg > 0 }
            .sortedWith(compareByDescending<MemberScore> { it.volumeKg }.thenBy { it.uid })
            .firstOrNull()?.let { out[Award.IRON_LIFTER] = it.uid }
        return out
    }

    private fun weekOf(day: Long): Long = ChronoUnit.WEEKS.between(LocalDate.of(2024, 1, 1), LocalDate.ofEpochDay(day)) // 2024-01-01 is a Monday
}
