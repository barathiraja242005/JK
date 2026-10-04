package com.barathiraja.jk

import android.app.Application
import android.content.Context
import com.barathiraja.jk.data.ExerciseRepo
import com.barathiraja.jk.data.JkDatabase
import com.barathiraja.jk.data.TrainingRepo
import com.barathiraja.jk.data.UserPrefs
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
