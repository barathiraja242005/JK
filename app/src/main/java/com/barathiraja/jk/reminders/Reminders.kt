package com.barathiraja.jk.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.barathiraja.jk.JkApp
import com.barathiraja.jk.MainActivity
import com.barathiraja.jk.R
import com.barathiraja.jk.data.Settings
import java.util.Calendar

object Reminders {
    const val CHANNEL_ID = "jk_reminders"
    internal const val KIND = "kind"
    private const val WORKOUT = "workout"
    private const val WATER = "water"
    private const val WORKOUT_CODE = 1
    private const val WATER_CODE = 2
    private val WATER_EVERY_MS = AlarmManager.INTERVAL_HOUR * 2
    /** Water reminders stay quiet outside these hours. */
    private const val WATER_FROM_HOUR = 8
    private const val WATER_UNTIL_HOUR = 22

    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "Reminders", NotificationManager.IMPORTANCE_DEFAULT)
            .apply { description = "Workout and hydration reminders" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /**
     * (Re)schedules alarms to match [settings]; called on every app start and settings change.
     * One-shot inexact alarms (no special permission) re-armed when they fire, so the workout time follows the
     * wall clock across DST changes. A pending water alarm is kept so app starts don't keep pushing it back.
     */
    fun apply(context: Context, settings: Settings) {
        if (settings.workoutReminder) armWorkout(context, settings) else cancel(context, WORKOUT, WORKOUT_CODE)
        if (!settings.waterReminder) cancel(context, WATER, WATER_CODE)
        else if (existing(context, WATER, WATER_CODE) == null) armWater(context)
    }

    /** Arms the next alarm of [kind] after one fires, if it is still switched on. */
    internal fun rearm(context: Context, kind: String?, settings: Settings) {
        when (kind) {
            WORKOUT -> if (settings.workoutReminder) armWorkout(context, settings)
            WATER -> if (settings.waterReminder) armWater(context)
        }
    }

    private fun armWorkout(context: Context, settings: Settings) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, settings.reminderHour)
            set(Calendar.MINUTE, settings.reminderMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        context.getSystemService(AlarmManager::class.java)
            .set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pending(context, WORKOUT, WORKOUT_CODE))
    }

    private fun armWater(context: Context) {
        context.getSystemService(AlarmManager::class.java)
            .set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + WATER_EVERY_MS, pending(context, WATER, WATER_CODE))
    }

    private fun cancel(context: Context, kind: String, code: Int) {
        val pi = existing(context, kind, code) ?: return
        context.getSystemService(AlarmManager::class.java).cancel(pi)
        pi.cancel()
    }

    private fun intent(context: Context, kind: String) = Intent(context, ReminderReceiver::class.java).putExtra(KIND, kind)

    private fun pending(context: Context, kind: String, code: Int): PendingIntent =
        PendingIntent.getBroadcast(context, code, intent(context, kind), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    /** The alarm's PendingIntent if one is scheduled (a force-stop clears them). */
    private fun existing(context: Context, kind: String, code: Int): PendingIntent? =
        PendingIntent.getBroadcast(context, code, intent(context, kind), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)

    internal fun show(context: Context, kind: String?) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        if (kind == WATER) {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            if (hour < WATER_FROM_HOUR || hour >= WATER_UNTIL_HOUR) return // don't nag at night
        }
        val (title, text) = when (kind) {
            WATER -> "Hydration check" to "Time for a glass of water. Tap to log it."
            else -> "Time to train" to "Your JK workout for today is ready. Let's go!"
        }
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(if (kind == WATER) WATER_CODE else WORKOUT_CODE, n)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getStringExtra(Reminders.KIND)
        Reminders.rearm(context, kind, (context.applicationContext as JkApp).container.prefs.settings.value)
        Reminders.show(context, kind)
    }
}

/** Alarms don't survive a reboot. JkApp re-arms them on start; this receiver just gets the process started. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val app = context.applicationContext as JkApp
            Reminders.apply(context, app.container.prefs.settings.value)
        }
    }
}
