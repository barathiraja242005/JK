package com.barathiraja.jk.steps

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.barathiraja.jk.data.JkDao
import com.barathiraja.jk.data.StepDay
import com.barathiraja.jk.data.UserPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

/**
 * Reads the hardware step counter (cumulative since boot) and converts it to
 * "steps today" by remembering a per-day baseline.
 */
class StepTracker(
    context: Context,
    private val prefs: UserPrefs,
    private val dao: JkDao,
    private val scope: CoroutineScope,
) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    val available: Boolean get() = sensor != null

    private val _today = MutableStateFlow(0)
    val today: StateFlow<Int> = _today.asStateFlow()

    private var listening = false

    fun start() {
        if (listening || sensor == null) return
        listening = sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
    }

    fun stop() {
        if (!listening) return
        sensorManager.unregisterListener(this)
        listening = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        val total = event.values.firstOrNull() ?: return
        val day = LocalDate.now().toEpochDay()
        // New day, first run, or device rebooted (counter reset): rebase, keeping today's steps.
        if (prefs.stepBaselineDay != day || prefs.stepBaseline < 0f) {
            prefs.stepBaselineDay = day
            prefs.stepBaseline = total
        } else if (total < prefs.stepBaseline) {
            prefs.stepBaseline = total - _today.value
        }
        val steps = (total - prefs.stepBaseline).toInt().coerceAtLeast(0)
        if (steps != _today.value) {
            _today.value = steps
            scope.launch { dao.upsertSteps(StepDay(day, steps)) }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
