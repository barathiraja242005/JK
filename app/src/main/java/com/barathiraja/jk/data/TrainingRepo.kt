package com.barathiraja.jk.data

import com.barathiraja.jk.domain.Health
import com.barathiraja.jk.domain.TrainingEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.DayOfWeek
import java.time.LocalDate

/** Owns the weekly split and daily plans: generation, editing, set logging and completion. */
class TrainingRepo(private val dao: JkDao, private val prefs: UserPrefs) {
    val engine = TrainingEngine { ExerciseRepo.get(it) }
    private val lock = Mutex()

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
    private suspend fun resetUpcoming() = lock.withLock {
        val todayItems = dao.planItemsNow(today)
        val todayStarted = todayItems.any { item -> item.setList.any { it.done } }
        val from = if (todayStarted) today + 1 else today
        dao.clearFutureItems(from)
        dao.clearFuturePlans(from)
    }

    /** Generates the plan for [day] if needed (today or later). */
    suspend fun ensureDay(day: Long) = lock.withLock {
        if (!prefs.training.value.setupDone || day < today) return@withLock
        if (dao.planDay(day) != null) {
            patchMissingWeights(day)
            return@withLock
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

    private suspend fun writeItems(day: Long, planned: List<com.barathiraja.jk.domain.PlannedExercise>, represcribe: Boolean = true) {
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
    fun weekItems(from: Long, to: Long) = dao.planItemsBetween(from, to)
    fun completedDays() = dao.completedDays()
    fun setLogs(exerciseId: String) = dao.setLogsFlow(exerciseId)

    suspend fun regenerate(day: Long, mode: DayMode) = lock.withLock {
        val existing = dao.planDay(day) ?: return@withLock
        dao.clearPlanItems(day)
        dao.upsertPlanDay(existing.copy(mode = mode, completedAt = null))
        val p = prefs.training.value
        val goal = prefs.profile.value.goal
        val planned = when (mode) {
            DayMode.FAT_LOSS -> engine.fatLossDay(p, goal, day)
            DayMode.PLAN -> engine.generateDay(BodyPart.parseList(existing.parts), p, goal, day + System.nanoTime() % 97)
        }
        writeItems(day, planned, represcribe = mode == DayMode.PLAN)
    }

    suspend fun addExercise(day: Long, part: BodyPart, exerciseId: String) {
        val items = dao.planItemsNow(day)
        if (dao.planDay(day) == null) dao.upsertPlanDay(PlanDay(day, BodyPart.encode(listOf(part))))
        val p = prefs.training.value
        val e = ExerciseRepo.get(exerciseId) ?: return
        val sets = engine.prescribe(e, part, p.level, prefs.profile.value.goal, dao.setLogs(exerciseId))
        // Insert after the last item of the same body part so sections stay grouped.
        val pos = (items.filter { it.bodyPart == part }.maxOfOrNull { it.position } ?: items.maxOfOrNull { it.position } ?: -1) + 1
        items.filter { it.position >= pos }.forEach { dao.updatePlanItem(it.copy(position = it.position + 1)) }
        dao.insertPlanItems(listOf(PlanItem(epochDay = day, bodyPart = part, exerciseId = exerciseId, position = pos, sets = SetSpec.encode(sets))))
        val pd = dao.planDay(day)
        if (pd != null && part !in BodyPart.parseList(pd.parts)) dao.upsertPlanDay(pd.copy(parts = BodyPart.encode(BodyPart.parseList(pd.parts) + part), completedAt = null))
    }

    suspend fun removeItem(id: Long) = dao.deletePlanItem(id)

    suspend fun replaceItem(id: Long, newExerciseId: String) {
        val item = dao.planItem(id) ?: return
        val e = ExerciseRepo.get(newExerciseId) ?: return
        val p = prefs.training.value
        val sets = engine.prescribe(e, item.bodyPart, p.level, prefs.profile.value.goal, dao.setLogs(newExerciseId))
        dao.updatePlanItem(item.copy(exerciseId = newExerciseId, sets = SetSpec.encode(sets)))
    }

    suspend fun updateSets(id: Long, sets: List<SetSpec>) {
        val item = dao.planItem(id) ?: return
        dao.updatePlanItem(item.copy(sets = SetSpec.encode(sets)))
        afterChange(item.epochDay)
    }

    /** Marks set [index] done/undone and keeps the set log in sync. */
    suspend fun toggleSet(id: Long, index: Int, done: Boolean) {
        val item = dao.planItem(id) ?: return
        val sets = item.setList.toMutableList()
        if (index !in sets.indices) return
        val s = sets[index]
        sets[index] = s.copy(done = done)
        dao.updatePlanItem(item.copy(sets = SetSpec.encode(sets)))
        if (done) dao.insertSetLog(SetLog(epochDay = item.epochDay, exerciseId = item.exerciseId, setIndex = index,
            reps = s.reps, weightKg = s.weightKg, seconds = s.seconds))
        else dao.deleteSetLog(item.epochDay, item.exerciseId, index)
        afterChange(item.epochDay)
    }

    suspend fun completeAll(day: Long) {
        dao.planItemsNow(day).forEach { item ->
            item.setList.forEachIndexed { i, s -> if (!s.done) toggleSet(item.id, i, true) }
        }
    }

    /** When every set of the day is done, record the workout once (for streaks, history and charts). */
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
            val kcal = items.sumOf { item ->
                val met = ExerciseRepo.get(item.exerciseId)?.met ?: 5f
                Health.caloriesBurned(met, kg, TrainingEngine.estimateSec(item.setList, rest))
            }
            dao.insertSession(WorkoutSession(workoutId = "plan:$day", title = TrainingEngine.title(BodyPart.parseList(pd.parts)),
                finishedAt = now, epochDay = day, durationSec = sec, calories = kcal))
        } else if (!allDone && pd.completedAt != null) {
            dao.upsertPlanDay(pd.copy(completedAt = null))
        }
    }

    /** Warm-up for a planned day, playable in the guided player. */
    suspend fun warmUp(day: Long): Workout? {
        val pd = dao.planDay(day) ?: return null
        val ids = engine.warmUpIds(BodyPart.parseList(pd.parts)).filter { ExerciseRepo.get(it) != null }
        if (ids.isEmpty()) return null
        return Workout("warmup:$day", "Warm-Up", "Prepare your body for better performance", Place.HOME, Level.BEGINNER,
            "Warm-up", 5, 1, ids.map { Block(it, seconds = 40) })
    }
}
