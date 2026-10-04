package com.barathiraja.jk.gym

import com.barathiraja.jk.data.SetSpec
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlin.random.Random

/**
 * A made-up gym for trying the app: trainers (one waiting for approval), members with different habits,
 * six weeks of workouts, last month's awards and a couple of hand-given ones. Lives only in memory and is
 * never written to Firestore. Fixed seed, so the same people show the same story every time.
 */
object DemoData {
    data class Gym(val people: List<Person>, val assignments: List<Assignment>, val awards: List<MonthAwards>, val given: List<GivenAward>)

    /** How a demo member behaves; each one exercises a different part of the owner screens. */
    private enum class Habit(val doneChance: Double) {
        STAR(0.97), REGULAR(0.85), PATCHY(0.55), QUIT(0.8), FORGOTTEN(0.85), NEW(0.9), IMPROVING(0.9)
    }

    private data class Plan(val name: String, val trainer: String, val habit: Habit, val stopDaysAgo: Int = 0, val joinedDaysAgo: Int = 60)

    private val trainers = listOf("Ravi Kumar", "Priya Lakshmi", "Arjun Selvam")

    private val plans = listOf(
        Plan("Arun Prakash", "Ravi Kumar", Habit.STAR),
        Plan("Divya Bharathi", "Ravi Kumar", Habit.REGULAR),
        Plan("Karthik Raja", "Ravi Kumar", Habit.IMPROVING),
        Plan("Meena Sundaram", "Ravi Kumar", Habit.QUIT, stopDaysAgo = 9),
        Plan("Suresh Babu", "Ravi Kumar", Habit.PATCHY),
        Plan("Lakshmi Narayanan", "Ravi Kumar", Habit.NEW, joinedDaysAgo = 5),
        Plan("Gokul Krishnan", "Priya Lakshmi", Habit.STAR),
        Plan("Nithya Devi", "Priya Lakshmi", Habit.REGULAR),
        Plan("Hari Haran", "Priya Lakshmi", Habit.QUIT, stopDaysAgo = 21),
        Plan("Sangeetha Mohan", "Priya Lakshmi", Habit.REGULAR),
        Plan("Vijay Anand", "Priya Lakshmi", Habit.IMPROVING),
        Plan("Deepa Rani", "Priya Lakshmi", Habit.NEW, joinedDaysAgo = 2),
        Plan("Manoj Kumar", "Arjun Selvam", Habit.FORGOTTEN, stopDaysAgo = 11),
        Plan("Kavya Shree", "Arjun Selvam", Habit.FORGOTTEN, stopDaysAgo = 8),
        Plan("Rahul Dev", "Arjun Selvam", Habit.PATCHY),
        Plan("Anitha Ramesh", "Arjun Selvam", Habit.QUIT, stopDaysAgo = 14),
        Plan("Prakash Raj", "Arjun Selvam", Habit.REGULAR),
    )

    private class Move(val id: String, val part: String, val reps: Int, val kg: Float, val seconds: Int = 0)

    private val days = listOf(
        "Chest & Triceps" to listOf(Move("Barbell_Bench_Press_-_Medium_Grip", "CHEST", 10, 40f), Move("Pushups", "CHEST", 15, 0f), Move("Triceps_Pushdown", "TRICEPS", 12, 20f)),
        "Back & Biceps" to listOf(Move("Wide-Grip_Lat_Pulldown", "BACK", 10, 35f), Move("Bent_Over_Barbell_Row", "BACK", 10, 30f), Move("Dumbbell_Bicep_Curl", "BICEPS", 12, 8f)),
        "Legs" to listOf(Move("Barbell_Full_Squat", "LEGS", 8, 50f), Move("Leg_Press", "LEGS", 12, 80f), Move("Dumbbell_Lunges", "LEGS", 10, 10f)),
        "Shoulders & Core" to listOf(Move("Standing_Military_Press", "SHOULDER", 8, 25f), Move("Side_Lateral_Raise", "SHOULDER", 12, 5f), Move("Plank", "ABDOMEN", 1, 0f, 45), Move("Crunches", "ABDOMEN", 20, 0f)),
        "Full body" to listOf(Move("Barbell_Deadlift", "BACK", 6, 60f), Move("Pullups", "BACK", 6, 0f), Move("Pushups", "CHEST", 12, 0f)),
    )

    fun build(owner: Person, today: Long, zone: ZoneId = ZoneId.systemDefault()): Gym {
        val rnd = Random(42)
        fun millis(day: Long, hour: Int) = LocalDate.ofEpochDay(day).atStartOfDay(zone).plusHours(hour.toLong()).toInstant().toEpochMilli()
        val from = YearMonth.from(LocalDate.ofEpochDay(today)).minusMonths(1).atDay(1).toEpochDay()

        val trainerPeople = trainers.mapIndexed { i, n ->
            Person("demo-t$i", n, null, Role.TRAINER, PersonStatus.ACTIVE, trainerCode = "DEMO${i}T", joinedAt = millis(from - 30, 9))
        }
        val tid = trainerPeople.associate { it.name to it.uid }
        val members = plans.mapIndexed { i, p ->
            Person("demo-m$i", p.name, null, Role.MEMBER, PersonStatus.ACTIVE, trainerUid = tid.getValue(p.trainer),
                joinedAt = millis(today - p.joinedDaysAgo, 10))
        }
        val extras = listOf(
            Person("demo-pending", "Vignesh Raja", null, Role.TRAINER, PersonStatus.PENDING, joinedAt = millis(today, 8)),
            Person("demo-removed", "Old Member", null, Role.MEMBER, PersonStatus.REMOVED, trainerUid = tid.getValue("Ravi Kumar"), joinedAt = millis(from - 60, 9)),
        )

        val assignments = mutableListOf<Assignment>()
        var n = 0
        plans.forEachIndexed { i, p ->
            val m = members[i]
            val start = maxOf(from, today - p.joinedDaysAgo)
            for (day in start..today + 3) {
                val dow = LocalDate.ofEpochDay(day).dayOfWeek.value // 1 = Monday
                // Rest days, except today: the gym is always open on the day you look, so "Today" has data.
                if (day != today && dow == 7) continue
                if (day != today && p.habit != Habit.STAR && dow == 6) continue
                // "Forgotten" members: the trainer stopped sending workouts, so the owner should nudge the trainer.
                if (p.habit == Habit.FORGOTTEN && day > today - p.stopDaysAgo) continue
                val (title, moves) = days[(day % days.size).toInt()]
                val past = day < today
                val quit = p.habit == Habit.QUIT && day > today - p.stopDaysAgo
                // Improving members do half their workouts last month and nearly all this month.
                val chance = if (p.habit == Habit.IMPROVING && day < today - 20) 0.45 else p.habit.doneChance
                val done = when {
                    day > today -> false
                    quit -> false
                    day == today -> rnd.nextDouble() < 0.5
                    else -> rnd.nextDouble() < chance
                }
                val inProgress = day == today && !done && !quit && rnd.nextDouble() < 0.4
                // Weights creep up over the six weeks so "Iron Lifter" and volume have something to show.
                val progress = 1f + (day - from).toFloat() / 120f + (if (p.habit == Habit.STAR) 0.3f else 0f)
                val exercises = moves.mapIndexed { k, mv ->
                    val setsDone = when { done -> 3; inProgress && k == 0 -> 3; inProgress && k == 1 -> 1; else -> 0 }
                    AssignedExercise(mv.id, mv.part, List(3) { s ->
                        SetSpec(mv.reps, if (mv.kg > 0) (mv.kg * progress).roundTo(2.5f) else 0f, mv.seconds, done = s < setsDone)
                    })
                }
                val late = done && rnd.nextDouble() < 0.1
                assignments += Assignment(
                    id = "demo-a${n++}", trainerUid = m.trainerUid.orEmpty(), memberUid = m.uid, title = title, epochDay = day,
                    exercises = exercises,
                    status = when { done -> AssignStatus.DONE; inProgress -> AssignStatus.IN_PROGRESS; else -> AssignStatus.ASSIGNED },
                    startedAt = if (done || inProgress) millis(day, 7) else null,
                    completedAt = if (done) millis(if (late) day + 1 else day, if (past) 8 else 7) else null,
                    verified = done && past && rnd.nextDouble() < 0.6,
                    memberNote = if (done && rnd.nextDouble() < 0.08) "Felt strong today 💪" else "",
                )
            }
        }

        val people = listOf(owner) + trainerPeople + members + extras
        val lastMonth = YearMonth.from(LocalDate.ofEpochDay(today)).minusMonths(1)
        val awards = Scoring.awards(people, assignments, lastMonth).takeIf { it.isNotEmpty() }
            ?.let { listOf(MonthAwards(lastMonth.toString(), it)) }.orEmpty()
        val given = listOf(
            GivenAward("demo-g0", "Star of the Week", "⭐", members[6].uid, "Free protein shake", YearMonth.from(LocalDate.ofEpochDay(today)).toString(), millis(today - 2, 18)),
            GivenAward("demo-g1", "Best Transformation", "🔄", members[2].uid, "One month free", lastMonth.toString(), millis(from + 25, 18)),
        )
        return Gym(people, assignments, awards, given)
    }

    private fun Float.roundTo(step: Float) = Math.round(this / step) * step
}
