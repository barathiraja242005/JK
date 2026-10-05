package com.barathiraja.jk.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class Sex { MALE, FEMALE }
enum class Goal(val label: String) { LOSE("Lose fat"), MAINTAIN("Stay fit"), GAIN("Build muscle") }
enum class Place(val label: String) { HOME("Home"), GYM("Gym") }
enum class Activity(val label: String, val factor: Double) {
    SEDENTARY("Mostly sitting", 1.2),
    LIGHT("Lightly active", 1.375),
    MODERATE("Moderately active", 1.55),
    VERY("Very active", 1.725),
}
enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class Profile(
    val onboarded: Boolean = false,
    val name: String = "",
    val sex: Sex = Sex.MALE,
    val age: Int = 25,
    val heightCm: Float = 170f,
    val weightKg: Float = 70f,
    val goal: Goal = Goal.MAINTAIN,
    val place: Place = Place.HOME,
    val activity: Activity = Activity.LIGHT,
)

data class Settings(
    val theme: ThemeMode = ThemeMode.LIGHT,
    val voiceCues: Boolean = true,
    val stepGoal: Int = 8000,
    val waterGoalGlasses: Int = 8,
    val workoutReminder: Boolean = false,
    val reminderHour: Int = 18,
    val reminderMinute: Int = 0,
    val waterReminder: Boolean = false,
)

/** Small key-value store for profile and settings, exposed as StateFlows. */
class UserPrefs(context: Context) {
    private val sp = context.getSharedPreferences("jk_prefs", Context.MODE_PRIVATE)

    private val _profile = MutableStateFlow(readProfile())
    val profile: StateFlow<Profile> = _profile.asStateFlow()

    private val _settings = MutableStateFlow(readSettings())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    fun saveProfile(p: Profile) {
        sp.edit()
            .putBoolean("onboarded", p.onboarded)
            .putString("name", p.name)
            .putString("sex", p.sex.name)
            .putInt("age", p.age)
            .putFloat("heightCm", p.heightCm)
            .putFloat("weightKg", p.weightKg)
            .putString("goal", p.goal.name)
            .putString("place", p.place.name)
            .putString("activity", p.activity.name)
            .apply()
        _profile.value = p
    }

    fun saveSettings(s: Settings) {
        sp.edit()
            .putString("theme", s.theme.name)
            .putBoolean("voiceCues", s.voiceCues)
            .putInt("stepGoal", s.stepGoal)
            .putInt("waterGoal", s.waterGoalGlasses)
            .putBoolean("workoutReminder", s.workoutReminder)
            .putInt("reminderHour", s.reminderHour)
            .putInt("reminderMinute", s.reminderMinute)
            .putBoolean("waterReminder", s.waterReminder)
            .apply()
        _settings.value = s
    }

    // Step counter bookkeeping: TYPE_STEP_COUNTER is cumulative since boot.
    var stepBaselineDay: Long
        get() = sp.getLong("stepBaselineDay", -1)
        set(v) = sp.edit().putLong("stepBaselineDay", v).apply()
    var stepBaseline: Float
        get() = sp.getFloat("stepBaseline", -1f)
        set(v) = sp.edit().putFloat("stepBaseline", v).apply()

    private val _training = MutableStateFlow(readTraining())
    val training: StateFlow<TrainingPrefs> = _training.asStateFlow()

    fun saveTraining(t: TrainingPrefs) {
        sp.edit()
            .putBoolean("tSetup", t.setupDone)
            .putString("tLevel", t.level.name)
            .putString("tDays", t.activeDays.joinToString(",") { it.value.toString() })
            .putString("tEquip", t.equipment.joinToString(",") { it.name })
            .putString("tInjuries", t.injuries.joinToString(",") { it.name })
            .putInt("tRest", t.restSec)
            .apply()
        _training.value = t
    }

    private fun readTraining(): TrainingPrefs {
        val d = TrainingPrefs()
        return TrainingPrefs(
            setupDone = sp.getBoolean("tSetup", false),
            level = enumOr(sp.getString("tLevel", null), d.level),
            activeDays = sp.getString("tDays", null)?.split(',')?.mapNotNull { it.toIntOrNull()?.let(java.time.DayOfWeek::of) }?.toSet()
                ?: d.activeDays,
            equipment = sp.getString("tEquip", null)?.split(',')?.mapNotNull { n -> Equipment.entries.firstOrNull { it.name == n } }?.toSet()
                ?: d.equipment,
            injuries = sp.getString("tInjuries", null)?.split(',')?.mapNotNull { n -> Injury.entries.firstOrNull { it.name == n } }?.toSet()
                ?: d.injuries,
            restSec = sp.getInt("tRest", d.restSec),
        )
    }

    /**
     * Profile, settings and training preferences as typed strings ("i:25"), for the signed-in account's private
     * cloud backup. Phone-specific keys (step counter baselines, photo URI, popups) stay on the phone.
     */
    fun exportBackup(): Map<String, String> = sp.all.filterKeys { it !in deviceKeys }.mapNotNull { (k, v) ->
        when (v) {
            is Boolean -> k to "b:$v"
            is Int -> k to "i:$v"
            is Long -> k to "l:$v"
            is Float -> k to "f:$v"
            is String -> k to "s:$v"
            else -> null
        }
    }.toMap()

    fun importBackup(backup: Map<String, String>) {
        val e = sp.edit()
        backup.forEach { (k, raw) ->
            if (k in deviceKeys) return@forEach
            val v = raw.drop(2)
            when (raw.take(2)) {
                "b:" -> e.putBoolean(k, v == "true")
                "i:" -> v.toIntOrNull()?.let { e.putInt(k, it) }
                "l:" -> v.toLongOrNull()?.let { e.putLong(k, it) }
                "f:" -> v.toFloatOrNull()?.let { e.putFloat(k, it) }
                "s:" -> e.putString(k, v)
            }
        }
        e.commit()
        _profile.value = readProfile()
        _settings.value = readSettings()
        _training.value = readTraining()
    }

    private val deviceKeys = setOf("stepBaselineDay", "stepBaseline", "avatarUri", "planDialogDay")

    /** Wipes everything stored on this phone (used when a gym account signs out). */
    fun clearAll() {
        sp.edit().clear().commit()
        _profile.value = readProfile()
        _settings.value = readSettings()
        _training.value = readTraining()
    }

    /** Last day the "Today's plan" popup was shown. */
    var planDialogDay: Long
        get() = sp.getLong("planDialogDay", -1)
        set(v) = sp.edit().putLong("planDialogDay", v).apply()

    var avatarUri: String?
        get() = sp.getString("avatarUri", null)
        set(v) = sp.edit().putString("avatarUri", v).apply()

    var vegOnly: Boolean
        get() = sp.getBoolean("vegOnly", false)
        set(v) = sp.edit().putBoolean("vegOnly", v).apply()

    private fun readProfile() = Profile(
        onboarded = sp.getBoolean("onboarded", false),
        name = sp.getString("name", "") ?: "",
        sex = enumOr(sp.getString("sex", null), Sex.MALE),
        age = sp.getInt("age", 25),
        heightCm = sp.getFloat("heightCm", 170f),
        weightKg = sp.getFloat("weightKg", 70f),
        goal = enumOr(sp.getString("goal", null), Goal.MAINTAIN),
        place = enumOr(sp.getString("place", null), Place.HOME),
        activity = enumOr(sp.getString("activity", null), Activity.LIGHT),
    )

    private fun readSettings() = Settings(
        theme = enumOr(sp.getString("theme", null), ThemeMode.LIGHT),
        voiceCues = sp.getBoolean("voiceCues", true),
        stepGoal = sp.getInt("stepGoal", 8000),
        waterGoalGlasses = sp.getInt("waterGoal", 8),
        workoutReminder = sp.getBoolean("workoutReminder", false),
        reminderHour = sp.getInt("reminderHour", 18),
        reminderMinute = sp.getInt("reminderMinute", 0),
        waterReminder = sp.getBoolean("waterReminder", false),
    )

    private inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
        name?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default
}
