package com.barathiraja.jk.data

import androidx.room.withTransaction
import com.barathiraja.jk.domain.Health
import com.barathiraja.jk.domain.TrainingEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.DayOfWeek
import java.time.LocalDate
import com.barathiraja.jk.domain.PlannedExercise

/** Owns the weekly split and daily plans: generation, editing, set logging and completion. */
class TrainingRepo(private val db: JkDatabase, private val prefs: UserPrefs) {
    private val dao = db.dao()
    val engine = TrainingEngine { ExerciseRepo.get(it) }
    private val lock = Mutex()

    /** Runs a multi-step plan edit atomically and one at a time, so a crash or a double tap never leaves it half done. */
    private suspend fun <T> write(block: suspend () -> T): T = lock.withLock { db.withTransaction { block() } }

    val split: Flow<Map<DayOfWeek, List<BodyPart>>> = dao.split().map { rows ->
        DayOfWeek.entries.associateWith { dow -> rows.firstOrNull { it.dayOfWeek == dow.value }?.let { BodyPart.parseList(it.parts) }.orEmpty() }
    }

    private val today get() = LocalDate.now().toEpochDay()

    /** Saves preferences, rebuilds the weekly split and regenerates upcoming days. */
    suspend fun setup(t: TrainingPrefs) {
        prefs.saveTraining(t.copy(setupDone = true))
        val split = engine.buildSplit(t.activeDays, prefs.profile.value.goal)
        dao.upsertSplit(split.map { (dow, parts) -> SplitDay(dow.value, BodyPart.encode(parts)) })
        resetUpcoming()
    }

    suspend fun setSplitDay(dow: DayOfWeek, parts: List<BodyPart>) {
        dao.upsertSplit(listOf(SplitDay(dow.value, BodyPart.encode(parts))))
        val active = DayOfWeek.entries.filter { d -> d == dow && parts.isNotEmpty() || d != dow && d in prefs.training.value.activeDays }.toSet()
        prefs.saveTraining(prefs.training.value.copy(activeDays = active))
        resetUpcoming()
    }

    /** Drops generated-but-untouched plans from today onwards so they regenerate from the new settings. */
    private suspend fun resetUpcoming() = write {
        val todayItems = dao.planItemsNow(today)
        val todayStarted = todayItems.any { item -> item.setList.any { it.done } }
        val from = if (todayStarted) today + 1 else today
        dao.clearFutureItems(from)
        dao.clearFuturePlans(from)
    }

    /** Generates the plan for [day] if needed (today or later). */
    suspend fun ensureDay(day: Long) = write {
        if (!prefs.training.value.setupDone || day < today) return@write
        if (dao.planDay(day) != null) {
            patchMissingWeights(day)
            return@write
        }
        val dow = LocalDate.ofEpochDay(day).dayOfWeek
        val parts = dao.splitNow().firstOrNull { it.dayOfWeek == dow.value }?.let { BodyPart.parseList(it.parts) }.orEmpty()
        dao.upsertPlanDay(PlanDay(day, BodyPart.encode(parts)))
        if (parts.isNotEmpty()) writeItems(day, engine.generateDay(parts, prefs.training.value, prefs.profile.value.goal, day) { history(it) })
    }

    /** Fills 0 kg weights on weighted exercises (plans made before starting weights existed) for untouched days. */
    private suspend fun patchMissingWeights(day: Long) {
        val items = dao.planItemsNow(day)
        if (items.any { item -> item.setList.any { it.done } }) return
        val level = prefs.training.value.level
        items.forEach { item ->
            val e = ExerciseRepo.get(item.exerciseId) ?: return@forEach
            val sets = item.setList
            if (e.equipment != Equipment.BODY_ONLY.key && sets.isNotEmpty() && sets.all { !it.timed && it.weightKg == 0f }) {
                val w = engine.startWeight(e, level)
                if (w > 0f) dao.updatePlanItem(item.copy(sets = SetSpec.encode(sets.map { it.copy(weightKg = w) })))
            }
        }
    }

    private var historyCache: Map<String, List<SetLog>> = emptyMap()
    private fun history(id: String) = historyCache[id].orEmpty()

    private suspend fun writeItems(day: Long, planned: List<PlannedExercise>, represcribe: Boolean = true) {
        if (!represcribe) {
            dao.insertPlanItems(planned.mapIndexed { i, pe ->
                PlanItem(epochDay = day, bodyPart = pe.part, exerciseId = pe.exerciseId, position = i, sets = SetSpec.encode(pe.sets))
            })
            return
        }
        historyCache = planned.associate { it.exerciseId to dao.setLogs(it.exerciseId) }
        // Re-prescribe with history so weights progress from the last session.
        val p = prefs.training.value
        val goal = prefs.profile.value.goal
        dao.insertPlanItems(planned.mapIndexed { i, pe ->
            val e = ExerciseRepo.get(pe.exerciseId)
            val sets = if (e != null) engine.prescribe(e, pe.part, p.level, goal, history(pe.exerciseId)) else pe.sets
            PlanItem(epochDay = day, bodyPart = pe.part, exerciseId = pe.exerciseId, position = i, sets = SetSpec.encode(sets))
        })
    }

    fun day(day: Long) = dao.planDayFlow(day)
    fun items(day: Long) = dao.planItems(day)
    fun week(from: Long, to: Long) = dao.planDaysBetween(from, to)
    fun completedDays() = dao.completedDays()
    fun setLogs(exerciseId: String) = dao.setLogsFlow(exerciseId)

    suspend fun regenerate(day: Long, mode: DayMode) = write {
        val existing = dao.planDay(day) ?: return@write
        dao.clearPlanItems(day)
        dao.upsertPlanDay(existing.copy(mode = mode, completedAt = null))
        val p = prefs.training.value
        val goal = prefs.profile.value.goal
        val planned = when (mode) {
            DayMode.FAT_LOSS -> engine.fatLossDay(p, goal, day)
            DayMode.PLAN -> engine.generateDay(BodyPart.parseList(existing.parts), p, goal, day + System.nanoTime() % RESHUFFLE_SEEDS)
        }
        writeItems(day, planned, represcribe = mode == DayMode.PLAN)
    }

    suspend fun addExercise(day: Long, part: BodyPart, exerciseId: String) = write {
        val e = ExerciseRepo.get(exerciseId) ?: return@write
        val items = dao.planItemsNow(day)
        if (dao.planDay(day) == null) dao.upsertPlanDay(PlanDay(day, BodyPart.encode(listOf(part))))
        val p = prefs.training.value
        val sets = engine.prescribe(e, part, p.level, prefs.profile.value.goal, dao.setLogs(exerciseId))
        // Insert after the last item of the same body part so sections stay grouped.
        val pos = (items.filter { it.bodyPart == part }.maxOfOrNull { it.position } ?: items.maxOfOrNull { it.position } ?: -1) + 1
        items.filter { it.position >= pos }.forEach { dao.updatePlanItem(it.copy(position = it.position + 1)) }
        dao.insertPlanItems(listOf(PlanItem(epochDay = day, bodyPart = part, exerciseId = exerciseId, position = pos, sets = SetSpec.encode(sets))))
        val pd = dao.planDay(day)
        if (pd != null && part !in BodyPart.parseList(pd.parts)) dao.upsertPlanDay(pd.copy(parts = BodyPart.encode(BodyPart.parseList(pd.parts) + part), completedAt = null))
    }

    suspend fun removeItem(id: Long) = write { dao.deletePlanItem(id) }

    suspend fun replaceItem(id: Long, newExerciseId: String) = write {
        val item = dao.planItem(id) ?: return@write
        val e = ExerciseRepo.get(newExerciseId) ?: return@write
        val p = prefs.training.value
        val sets = engine.prescribe(e, item.bodyPart, p.level, prefs.profile.value.goal, dao.setLogs(newExerciseId))
        dao.updatePlanItem(item.copy(exerciseId = newExerciseId, sets = SetSpec.encode(sets)))
    }

    suspend fun updateSets(id: Long, sets: List<SetSpec>) = write {
        val item = dao.planItem(id) ?: return@write
        dao.updatePlanItem(item.copy(sets = SetSpec.encode(sets)))
        afterChange(item.epochDay)
    }

    /** Marks set [index] done/undone and keeps the set log in sync. */
    suspend fun toggleSet(id: Long, index: Int, done: Boolean) = write { setDone(id, index, done) }

    suspend fun completeAll(day: Long) = write {
        dao.planItemsNow(day).forEach { item ->
            item.setList.forEachIndexed { i, s -> if (!s.done) setDone(item.id, i, true) }
        }
    }

    /** Must run inside [write]. Re-reads the set first, so a repeated tap is a no-op instead of a second log row. */
    private suspend fun setDone(id: Long, index: Int, done: Boolean) {
        val item = dao.planItem(id) ?: return
        val sets = item.setList.toMutableList()
        if (index !in sets.indices || sets[index].done == done) return
        val s = sets[index]
        sets[index] = s.copy(done = done)
        dao.updatePlanItem(item.copy(sets = SetSpec.encode(sets)))
        if (done) dao.replaceSetLog(SetLog(epochDay = item.epochDay, exerciseId = item.exerciseId, setIndex = index,
            reps = s.reps, weightKg = s.weightKg, seconds = s.seconds))
        else dao.deleteSetLog(item.epochDay, item.exerciseId, index)
        afterChange(item.epochDay)
    }

    /**
     * When every set of the day is done, record the workout once (for streaks, history and charts).
     * Un-completing the day removes that session again, so re-completing never counts twice.
     */
    private suspend fun afterChange(day: Long) {
        val pd = dao.planDay(day) ?: return
        val items = dao.planItemsNow(day)
        val allDone = items.isNotEmpty() && items.all { it.done }
        if (allDone && pd.completedAt == null) {
            val now = System.currentTimeMillis()
            dao.upsertPlanDay(pd.copy(completedAt = now))
            val rest = prefs.training.value.restSec
            val sec = items.sumOf { TrainingEngine.estimateSec(it.setList, rest) }
            val kg = prefs.profile.value.weightKg
            val kcal = items.sumOf { Health.exerciseKcal(it.exerciseId, kg, TrainingEngine.estimateSec(it.setList, rest)) }
            dao.replaceSession(WorkoutSession(workoutId = planWorkoutId(day), title = TrainingEngine.title(BodyPart.parseList(pd.parts)),
                finishedAt = now, epochDay = day, durationSec = sec, calories = kcal))
        } else if (!allDone && pd.completedAt != null) {
            dao.upsertPlanDay(pd.copy(completedAt = null))
            dao.deleteSessionsFor(planWorkoutId(day))
        }
    }

    /** Warm-up for a planned day, playable in the guided player. */
    suspend fun warmUp(day: Long): Workout? {
        val pd = dao.planDay(day) ?: return null
        val ids = engine.warmUpIds(BodyPart.parseList(pd.parts)).filter { ExerciseRepo.get(it) != null }
        if (ids.isEmpty()) return null
        return Workout("warmup:$day", "Warm-Up", "Prepare your body for better performance", Place.HOME, Level.BEGINNER,
            "Warm-up", WARM_UP_REST_SEC, 1, ids.map { Block(it, seconds = WARM_UP_BLOCK_SEC) })
    }

    private companion object {
        /** Spread of random offsets used to reshuffle a regenerated day. */
        const val RESHUFFLE_SEEDS = 97
        const val WARM_UP_REST_SEC = 5
        const val WARM_UP_BLOCK_SEC = 40

        fun planWorkoutId(day: Long) = "plan:$day"
    }
}
