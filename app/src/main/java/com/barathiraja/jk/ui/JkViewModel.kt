package com.barathiraja.jk.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.barathiraja.jk.AppContainer
import com.barathiraja.jk.data.Block
import com.barathiraja.jk.data.BodyPhoto
import com.barathiraja.jk.data.Catalog
import com.barathiraja.jk.data.ChallengeDay
import com.barathiraja.jk.data.CustomWorkout
import com.barathiraja.jk.data.ExerciseFlag
import com.barathiraja.jk.data.ExerciseRepo
import com.barathiraja.jk.data.Fast
import com.barathiraja.jk.data.Food
import com.barathiraja.jk.data.FoodEntry
import com.barathiraja.jk.data.Level
import com.barathiraja.jk.data.Meal
import com.barathiraja.jk.data.MindSession
import com.barathiraja.jk.data.WalkSession
import com.barathiraja.jk.data.Workout
import com.barathiraja.jk.data.Profile
import com.barathiraja.jk.data.Settings
import com.barathiraja.jk.data.WaterDay
import com.barathiraja.jk.data.WeightEntry
import com.barathiraja.jk.data.WorkoutSession
import com.barathiraja.jk.domain.Health
import com.barathiraja.jk.reminders.Reminders
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** App-wide state shared by all tabs. Room flows are exposed as StateFlows. */
class JkViewModel(private val c: AppContainer, private val app: android.app.Application) : ViewModel() {
    private val today get() = LocalDate.now().toEpochDay()
    private val weekStart = LocalDate.now().toEpochDay() - 6

    val profile: StateFlow<Profile> = c.prefs.profile
    val settings: StateFlow<Settings> = c.prefs.settings
    val stepsToday: StateFlow<Int> = c.steps.today
    val stepSensorAvailable: Boolean get() = c.steps.available

    val waterToday: StateFlow<Int> = c.dao.water(today).map { it?.glasses ?: 0 }.state(0)
    val sessions: StateFlow<List<WorkoutSession>> = c.dao.sessions().state(emptyList())
    val weekSessions = c.dao.sessionsSince(weekStart).state(emptyList())
    val weekSteps = c.dao.stepsSince(weekStart).state(emptyList())
    val weekWater = c.dao.waterSince(weekStart).state(emptyList())
    val weights: StateFlow<List<WeightEntry>> = c.dao.weights().state(emptyList())
    val activeFast: StateFlow<Fast?> = c.dao.activeFast().state(null)
    val pastFasts = c.dao.pastFasts().state(emptyList())
    val streak: StateFlow<Int> = c.dao.activeDays().map { Health.streak(it, LocalDate.now().toEpochDay()) }.state(0)

    fun saveProfile(p: Profile, logWeight: Boolean = true) {
        val first = !c.prefs.profile.value.onboarded
        c.prefs.saveProfile(p)
        if (first) {
            c.prefs.saveSettings(settings.value.copy(waterGoalGlasses = Health.waterGoalGlasses(p.weightKg)))
        }
        if (logWeight) viewModelScope.launch { c.dao.upsertWeight(WeightEntry(today, p.weightKg)) }
    }

    fun saveSettings(s: Settings) {
        c.prefs.saveSettings(s)
        Reminders.apply(app, s)
    }

    fun addWater(delta: Int) = viewModelScope.launch {
        val current = c.dao.water(today).first()?.glasses ?: 0
        c.dao.upsertWater(WaterDay(today, (current + delta).coerceIn(0, 40)))
    }

    fun logWeight(kg: Float) = viewModelScope.launch {
        c.dao.upsertWeight(WeightEntry(today, kg))
        c.prefs.saveProfile(profile.value.copy(weightKg = kg))
    }

    fun deleteWeight(day: Long) = viewModelScope.launch { c.dao.deleteWeight(day) }

    fun startFast(hours: Int) = viewModelScope.launch {
        if (c.dao.activeFast().first() == null) {
            c.dao.insertFast(Fast(startedAt = System.currentTimeMillis(), targetHours = hours))
        }
    }

    fun endFast() = viewModelScope.launch {
        c.dao.activeFast().first()?.let { c.dao.updateFast(it.copy(endedAt = System.currentTimeMillis())) }
    }


    fun startSteps() = c.steps.start()

    // ---- Exercise flags (likes / saves) ----
    val flags = c.dao.flags().map { list -> list.associateBy { it.exerciseId } }.state(emptyMap())

    fun toggleLike(id: String) = viewModelScope.launch {
        val f = c.dao.flag(id) ?: ExerciseFlag(id)
        c.dao.upsertFlag(f.copy(liked = !f.liked))
    }

    fun toggleSave(id: String) = viewModelScope.launch {
        val f = c.dao.flag(id) ?: ExerciseFlag(id)
        c.dao.upsertFlag(f.copy(saved = !f.saved))
    }

    // ---- Custom workouts ----
    val customWorkouts = c.dao.customWorkouts().state(emptyList())

    fun saveCustom(w: CustomWorkout, onSaved: (Long) -> Unit = {}) = viewModelScope.launch {
        val id = c.dao.upsertCustom(w)
        onSaved(if (w.id != 0L) w.id else id)
    }

    fun deleteCustom(id: Long) = viewModelScope.launch { c.dao.deleteCustom(id) }

    /** Resolves any workout id: built-in, "challenge:<id>:<day>" or "custom:<id>". */
    suspend fun resolveWorkout(id: String): Workout? {
        if (id.startsWith("warmup:")) return c.training.warmUp(id.removePrefix("warmup:").toLongOrNull() ?: return null)
        if (id.startsWith("custom:")) {
            val cw = c.dao.customWorkout(id.removePrefix("custom:").toLongOrNull() ?: return null) ?: return null
            val blocks = Block.decodeAll(cw.blocks).filter { ExerciseRepo.get(it.exerciseId) != null }
            if (blocks.isEmpty()) return null
            return Workout(id, cw.name, "Your custom workout", profile.value.place, Level.BEGINNER, "Custom",
                cw.restSec, cw.rounds, blocks)
        }
        return Catalog.resolve(id)
    }

    // ---- Challenges ----
    val challengeDays = c.dao.challengeDays().map { list -> list.groupBy({ it.challengeId }, { it.day }).mapValues { it.value.toSet() } }
        .state(emptyMap())

    fun resetChallenge(id: String) = viewModelScope.launch { c.dao.resetChallenge(id) }

    /** Saves a finished session; challenge workouts also tick off their day. */
    fun completeWorkout(s: WorkoutSession) = viewModelScope.launch {
        c.dao.insertSession(s)
        if (s.workoutId.startsWith("challenge:")) {
            val p = s.workoutId.split(':')
            val day = p.getOrNull(2)?.toIntOrNull()
            if (day != null) c.dao.markChallengeDay(ChallengeDay(p[1], day, s.finishedAt))
        }
    }

    // ---- Food ----
    val foodToday = c.dao.foodOn(today).state(emptyList())

    fun logFood(food: Food, meal: Meal, servings: Float) = viewModelScope.launch {
        c.dao.insertFood(
            FoodEntry(epochDay = today, meal = meal, name = food.name, servings = servings,
                kcal = (food.kcal * servings).toInt(), protein = food.protein * servings,
                carbs = food.carbs * servings, fat = food.fat * servings)
        )
    }

    fun logCustomFood(name: String, kcal: Int, meal: Meal) = viewModelScope.launch {
        c.dao.insertFood(FoodEntry(epochDay = today, meal = meal, name = name, servings = 1f, kcal = kcal,
            protein = 0f, carbs = 0f, fat = 0f))
    }

    fun deleteFood(id: Long) = viewModelScope.launch { c.dao.deleteFood(id) }

    // ---- Body photos ----
    val photos = c.dao.photos().state(emptyList())

    fun addPhoto(uri: String) = viewModelScope.launch {
        c.dao.insertPhoto(BodyPhoto(epochDay = today, uri = uri, weightKg = profile.value.weightKg))
    }

    fun deletePhoto(id: Long) = viewModelScope.launch { c.dao.deletePhoto(id) }

    // ---- Walks & mindfulness ----
    val walks = c.dao.walks().state(emptyList())
    val mindSessions = c.dao.mindSessions().state(emptyList())

    fun saveWalk(w: WalkSession) = viewModelScope.launch { c.dao.insertWalk(w) }
    fun saveMind(title: String, sec: Int) = viewModelScope.launch {
        if (sec >= 30) c.dao.insertMind(MindSession(epochDay = today, title = title, durationSec = sec))
    }

    var vegOnly: Boolean
        get() = c.prefs.vegOnly
        set(v) { c.prefs.vegOnly = v }

    // ---- Avatar ----
    val avatar = MutableStateFlow(c.prefs.avatarUri)
    fun setAvatar(uri: String?) { c.prefs.avatarUri = uri; avatar.value = uri }

    private fun <T> kotlinx.coroutines.flow.Flow<T>.state(initial: T) =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    companion object {
        fun factory(c: AppContainer, app: android.app.Application) = viewModelFactory {
            initializer { JkViewModel(c, app) }
        }
    }
}
