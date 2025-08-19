package com.gemini.bibliverse.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.util.Calendar

class NotificationScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // FIX 1.1: Добавлена аннотация для проверки версии API
    @RequiresApi(Build.VERSION_CODES.S)
    fun scheduleDailyVerse(hour: Int, minute: Int) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Проверка на возможность установки точных будильников
        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
            Log.d("BibliverseScheduler", "Exact alarm scheduled for: $hour:$minute")
        } else {
            // Запасной вариант
            alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                15 * 60 * 1000, // 15-минутное окно
                pendingIntent
            )
            Log.d("BibliverseScheduler", "Inexact alarm scheduled for: $hour:$minute")
        }
    }

    fun cancelDailyVerse() {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            Log.d("BibliverseScheduler", "Alarm canceled.")
        }
    }

    companion object {
        private const val ALARM_REQUEST_CODE = 100
    }
}
