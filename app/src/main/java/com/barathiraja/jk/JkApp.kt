package com.barathiraja.jk

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import com.barathiraja.jk.data.ExerciseRepo
import com.barathiraja.jk.data.JkDatabase
import com.barathiraja.jk.data.TrainingRepo
import com.barathiraja.jk.data.UserPrefs
import com.barathiraja.jk.gym.AuthRepo
import com.barathiraja.jk.gym.GymRepo
import com.barathiraja.jk.gym.TestGym
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
    private val app = context.applicationContext
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

    /**
     * Test mode: the gym screens run on a made-up gym kept in memory ([TestGym]) instead of Firestore. The switch
     * lives in its own file so signing out (which clears the settings) never turns it on or off.
     */
    val testMode: Boolean = app.getSharedPreferences(TEST_PREFS, Context.MODE_PRIVATE).getBoolean(TEST_KEY, false)
    val testGym by lazy { TestGym() }

    /**
     * Turns test mode on or off and restarts the app so every screen starts over on the other data. The real
     * profile and settings are saved on the way in and put back on the way out, so nothing typed in test mode
     * (onboarding as a test member, settings) can reach the real account or its cloud backup.
     */
    fun setTestMode(on: Boolean) {
        val test = app.getSharedPreferences(TEST_PREFS, Context.MODE_PRIVATE)
        if (on && !testMode) {
            test.edit().putString(SNAPSHOT_KEY, JSONObject(prefs.exportBackup()).toString()).commit()
        } else if (!on && testMode) {
            test.getString(SNAPSHOT_KEY, null)?.let { json ->
                val o = JSONObject(json)
                prefs.replaceWith(o.keys().asSequence().associateWith { o.getString(it) })
            }
            test.edit().remove(SNAPSHOT_KEY).commit()
        }
        test.edit().putBoolean(TEST_KEY, on).commit()
        restart()
    }

    /** Starts the app over from its launch screen in a fresh process. */
    fun restart() {
        val intent = app.packageManager.getLaunchIntentForPackage(app.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        app.startActivity(intent)
        Runtime.getRuntime().exit(0)
    }

    /**
     * Removes everything this account left on the phone: its history and settings, the photos it gave JK access to,
     * and Firestore's offline copy of the gym (other members' records and notes). The app must restart afterwards,
     * since Firestore can't be used again in this process.
     */
    suspend fun wipePhone() {
        withContext(Dispatchers.IO) { db.clearAllTables() }
        prefs.clearAll()
        app.contentResolver.persistedUriPermissions.forEach {
            runCatching { app.contentResolver.releasePersistableUriPermission(it.uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
        if (gymEnabled) {
            val fs = FirebaseFirestore.getInstance()
            runCatching { fs.terminate().await(); fs.clearPersistence().await() }
                .onFailure { Log.w(TAG, "Couldn't clear the gym's offline copy", it) }
        }
    }

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
        const val TEST_PREFS = "jk_test_mode"
        const val TEST_KEY = "on"
        const val SNAPSHOT_KEY = "realPrefs"
        const val TAG = "AppContainer"
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
