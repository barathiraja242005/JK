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

/** What the tracker remembers between sensor readings: the day, its baseline and the last counter value seen. */
data class StepState(val day: Long, val baseline: Float, val lastTotal: Float) {
    companion object {
        /** Nothing recorded yet (fresh install or cleared prefs). */
        val NONE = StepState(day = -1, baseline = 0f, lastTotal = -1f)
    }
}

/** Result of one reading: the new state, today's steps and, on a rollover, the final count for the day that ended. */
data class StepUpdate(val state: StepState, val steps: Int, val finished: StepDay? = null)

/**
 * Turns a cumulative-since-boot counter reading into "steps today". Pure so it can be unit tested.
 * On a new day the previous day keeps everything up to the last reading and today starts from there, so
 * steps walked between the last reading and the first one after midnight are not lost.
 */
fun nextSteps(prev: StepState, day: Long, total: Float): StepUpdate {
    val seen = prev.day >= 0 && prev.lastTotal >= 0f
    val rebooted = seen && total < prev.lastTotal
    val baseline = when {
        prev.day < 0 -> total // first reading ever
        prev.day != day -> when {
            // Next day, same boot: carry on from the last reading. After a gap or reboot we can't split steps by day.
            seen && !rebooted && day == prev.day + 1 -> prev.lastTotal
            rebooted && day == prev.day + 1 -> 0f
            else -> total
        }
        // Same day after a reboot (counter restarts at 0): today's count so far plus everything since boot.
        rebooted -> -(prev.lastTotal - prev.baseline)
        else -> prev.baseline
    }
    val finished = if (prev.day >= 0 && prev.day != day && seen) {
        StepDay(prev.day, (prev.lastTotal - prev.baseline).toInt().coerceAtLeast(0))
    } else null
    val steps = (total - baseline).toInt().coerceAtLeast(0)
    return StepUpdate(StepState(day, baseline, total), steps, finished)
}

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
        val update = nextSteps(prefs.stepState, LocalDate.now().toEpochDay(), total)
        prefs.stepState = update.state
        update.finished?.let { scope.launch { dao.upsertSteps(it) } }
        if (update.steps != _today.value || update.finished != null) {
            _today.value = update.steps
            scope.launch { dao.upsertSteps(StepDay(update.state.day, update.steps)) }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
