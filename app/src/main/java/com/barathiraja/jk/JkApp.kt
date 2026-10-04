package com.barathiraja.jk

import android.app.Application
import android.content.Context
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

/** Manual dependency container: one instance of each shared service. */
class AppContainer(context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val prefs = UserPrefs(context)
    val db = JkDatabase.create(context)
    val dao = db.dao()
    val steps = StepTracker(context, prefs, dao, appScope)
    val training = TrainingRepo(dao, prefs)

    /** Gym features need google-services.json; without it JK runs as a personal app. */
    val gymEnabled = FirebaseApp.getApps(context).isNotEmpty()
    val auth by lazy { AuthRepo(context.applicationContext) }
    val gym by lazy { GymRepo() }
}

class JkApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Reminders.createChannel(this)
        // Parse the bundled exercise database off the main thread; the splash screen waits for it.
        Thread { ExerciseRepo.load(this) }.start()
    }
}
