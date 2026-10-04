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
    private const val KIND = "kind"
    private const val WORKOUT = "workout"
    private const val WATER = "water"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "Reminders", NotificationManager.IMPORTANCE_DEFAULT)
            .apply { description = "Workout and hydration reminders" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** (Re)schedules alarms to match [settings]. Uses inexact alarms: no special permission needed. */
    fun apply(context: Context, settings: Settings) {
        val am = context.getSystemService(AlarmManager::class.java)
        val workoutPi = pending(context, WORKOUT, 1)
        val waterPi = pending(context, WATER, 2)
        am.cancel(workoutPi)
        am.cancel(waterPi)

        if (settings.workoutReminder) {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, settings.reminderHour)
                set(Calendar.MINUTE, settings.reminderMinute)
                set(Calendar.SECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
            }
            am.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.timeInMillis, AlarmManager.INTERVAL_DAY, workoutPi)
        }
        if (settings.waterReminder) {
            am.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + AlarmManager.INTERVAL_HOUR * 2,
                AlarmManager.INTERVAL_HOUR * 2,
                waterPi,
            )
        }
    }

    private fun pending(context: Context, kind: String, code: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context, code,
            Intent(context, ReminderReceiver::class.java).putExtra(KIND, kind),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    internal fun show(context: Context, kind: String?) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        if (kind == WATER) {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            if (hour < 8 || hour >= 22) return // don't nag at night
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
        NotificationManagerCompat.from(context).notify(if (kind == WATER) 2 else 1, n)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Reminders.show(context, intent.getStringExtra("kind"))
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val app = context.applicationContext as JkApp
            Reminders.apply(context, app.container.prefs.settings.value)
        }
    }
}
