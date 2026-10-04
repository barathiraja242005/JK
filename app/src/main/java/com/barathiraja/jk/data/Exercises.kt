package com.barathiraja.jk.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import java.util.Locale

/**
 * Exercise from free-exercise-db (github.com/yuhonas/free-exercise-db, public domain / Unlicense).
 * Each exercise has two photos: start (0) and end (1) position, which we cross-fade as a demo.
 */
data class Exercise(
    val id: String,
    val name: String,
    val category: String,
    val equipment: String,
    val level: String,
    val force: String?,
    val mechanic: String?,
    val primary: List<String>,
    val secondary: List<String>,
    val instructions: List<String>,
) {
    val muscles: String get() = primary.joinToString { it.cap() }

    /** Rough metabolic equivalent by category, used for calorie estimates. */
    val met: Float
        get() = when (category) {
            "cardio" -> 8f
            "plyometrics" -> 8f
            "stretching" -> 2.3f
            "olympic weightlifting", "strongman", "powerlifting" -> 6f
            else -> if (equipment == "body only") 5f else 5.5f
        }

    fun image(frame: Int): String =
        if (id in ExerciseRepo.bundled) "file:///android_asset/exercise_images/$id/$frame.jpg"
        else "$REMOTE/$id/$frame.jpg"

    val tutorialUrl: String
        get() = "https://www.youtube.com/results?search_query=" + Uri.encode("$name exercise proper form tutorial")

    companion object {
        const val REMOTE = "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises"
    }
}

fun String.cap() = replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

object ExerciseRepo {
    @Volatile private var all: List<Exercise> = emptyList()
    private var byId: Map<String, Exercise> = emptyMap()
    var bundled: Set<String> = emptySet()
        private set

    /** True once [load] has finished (even if it failed), so the splash screen never hangs. */
    @Volatile var ready = false
        private set

    /** Loads the bundled database. Safe to call repeatedly; only the first call does work. */
    @Synchronized
    fun load(context: Context) {
        if (all.isNotEmpty()) return
        try {
            parse(context)
        } finally {
            ready = true
        }
    }

    private fun parse(context: Context) {
        bundled = context.assets.list("exercise_images")?.toSet() ?: emptySet()
        val text = context.assets.open("exercises.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(text)
        val list = ArrayList<Exercise>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            fun strs(key: String): List<String> {
                val a = o.optJSONArray(key) ?: return emptyList()
                return List(a.length()) { a.getString(it) }
            }
            fun str(key: String) = if (o.isNull(key)) null else o.optString(key)
            list += Exercise(
                id = o.getString("id"),
                name = o.getString("name"),
                category = str("category") ?: "strength",
                equipment = str("equipment") ?: "other",
                level = str("level") ?: "beginner",
                force = str("force"),
                mechanic = str("mechanic"),
                primary = strs("primaryMuscles"),
                secondary = strs("secondaryMuscles"),
                instructions = strs("instructions"),
            )
        }
        all = list
        byId = list.associateBy { it.id }
    }

    fun all(): List<Exercise> = all
    fun get(id: String): Exercise? = byId[id]
    fun require(id: String): Exercise = byId[id] ?: error("Unknown exercise $id")

    val muscles get() = all.flatMap { it.primary }.distinct().sorted()
    val equipment get() = all.map { it.equipment }.distinct().sorted()
    val categories get() = all.map { it.category }.distinct().sorted()

    fun search(query: String, muscle: String?, equipment: String?, level: String?): List<Exercise> {
        val q = query.trim().lowercase()
        return all.filter {
            (q.isEmpty() || it.name.lowercase().contains(q) || it.primary.any { m -> m.contains(q) }) &&
                (muscle == null || muscle in it.primary) &&
                (equipment == null || it.equipment == equipment) &&
                (level == null || it.level == level)
        }
    }
}
