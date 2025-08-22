// --- Файл: notifications/AlarmReceiver.kt ---
package com.gemini.bibliverse.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.gemini.bibliverse.MainActivity
import com.gemini.bibliverse.R
import com.gemini.bibliverse.data.DataStoreManager
import com.gemini.bibliverse.data.Verse
import com.gemini.bibliverse.data.VerseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val repository = VerseRepository(context)
        val dataStoreManager = DataStoreManager(context)

        CoroutineScope(Dispatchers.IO).launch {
            val affirmationsEnabled = dataStoreManager.getAffirmationsEnabled().first()

            val verses = if (affirmationsEnabled) {
                repository.loadVerses()
            } else {
                repository.loadBibleVerses()
            }

            if (verses.isNotEmpty()) {
                val randomVerse = verses[Random.nextInt(verses.size)]
                showNotification(context, randomVerse)
            }
        }
    }

    // FIX 1.4: Передаем весь объект Verse
    private fun showNotification(context: Context, verse: Verse) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "daily_verse_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Daily Verses", NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Channel for daily Bible verses"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // FIX 1.4: Создаем интент для открытия MainActivity
        val activityIntent = Intent(context, MainActivity::class.java).apply {
            action = "SHOW_VERSE_ACTION"
            putExtra("verse_text", verse.text)
            putExtra("verse_reference", verse.reference)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 1, activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Your Verse for the Day")
            .setContentText(verse.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText("\"${verse.text}\" — ${verse.reference}"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(101, notification)
    }
}
