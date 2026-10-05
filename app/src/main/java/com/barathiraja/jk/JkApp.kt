package com.barathiraja.jk

import android.app.Application
import android.content.Context
import android.util.Log
import com.barathiraja.jk.data.ExerciseRepo
import com.barathiraja.jk.data.JkDatabase
import com.barathiraja.jk.data.TrainingRepo
import com.barathiraja.jk.data.UserPrefs
import com.barathiraja.jk.gym.AuthRepo
import com.barathiraja.jk.gym.GymRepo
import com.google.firebase.FirebaseApp
import com.barathiraja.jk.reminders.Reminders
import com.barathiraja.jk.steps.StepTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/** Manual dependency container: one instance of each shared service. */
class AppContainer(context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val prefs = UserPrefs(context)
    val db = JkDatabase.create(context)
    val dao = db.dao()
    val steps = StepTracker(context, prefs, dao, appScope)
    val training = TrainingRepo(db, prefs)

    /** Gym features need google-services.json; without it JK runs as a personal app. */
    val gymEnabled = FirebaseApp.getApps(context).isNotEmpty()
    val auth by lazy { AuthRepo(context.applicationContext) }
    val gym by lazy { GymRepo() }

    private val _currentDay = MutableStateFlow(LocalDate.now().toEpochDay())

    /** The epoch day every "today" screen shows. Ticks at midnight, so a long-lived app never shows yesterday. */
    val currentDay: StateFlow<Long> = _currentDay.asStateFlow()

    /** Re-reads the clock (the midnight tick can be late in deep sleep) and returns today's epoch day. */
    fun today(): Long = LocalDate.now().toEpochDay().also { _currentDay.value = it }

    init {
        appScope.launch {
            while (true) {
                val untilMidnight = Duration.between(LocalDateTime.now(), LocalDate.now().plusDays(1).atStartOfDay())
                delay(untilMidnight.toMillis() + MIDNIGHT_SLACK_MS)
                today()
            }
        }
    }

    private companion object {
        /** Wake a moment after midnight so LocalDate.now() has surely rolled over. */
        const val MIDNIGHT_SLACK_MS = 1_000L
    }
}

class JkApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Reminders.createChannel(this)
        // Alarms die with a force-stop or OEM kill, and settings change on restore or sign-out: keep them in sync.
        container.appScope.launch { container.prefs.settings.collect { Reminders.apply(this@JkApp, it) } }
        // Parse the bundled exercise database off the main thread; the splash screen waits for it.
        Thread {
            runCatching { ExerciseRepo.load(this) }.onFailure { Log.e(TAG, "Exercise database failed to load", it) }
        }.start()
    }

    private companion object {
        const val TAG = "JkApp"
    }
}
