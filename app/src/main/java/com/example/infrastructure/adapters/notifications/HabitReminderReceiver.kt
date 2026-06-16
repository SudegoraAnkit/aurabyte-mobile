package com.example.infrastructure.adapters.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.example.MainActivity
import com.example.core.domain.isApplicableOn
import com.example.infrastructure.adapters.database.AppDatabase
import com.example.infrastructure.adapters.database.RoomStorageAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HabitReminderReceiver : BroadcastReceiver() {

    private const val CHANNEL_ID = "habit_reminders_channel"
    private const val CHANNEL_NAME = "Habit Reminders"

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        
        if (action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "habitengine_habits_ledger.db"
                    ).build()
                    val adapter = RoomStorageAdapter(db)
                    val state = adapter.loadTrackerState().first()
                    
                    state.habits.forEach { habit ->
                        if (habit.reminderHour != null && habit.reminderMinute != null) {
                            NotificationScheduler.scheduleHabitReminder(context, habit)
                        }
                    }
                    NotificationScheduler.scheduleGeneralReminders(context)
                    db.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        } else {
            createNotificationChannel(context)
            when (action) {
                "com.example.HABIT_REMINDER_ACTION" -> {
                    val cue = intent.getStringExtra("habit_cue") ?: ""
                    val routine = intent.getStringExtra("habit_routine") ?: ""
                    val habitId = intent.getStringExtra("habit_id") ?: ""
                    
                    showNotification(
                        context,
                        habitId.hashCode(),
                        "Habit Reminder: $cue",
                        "Routine: $routine"
                    )
                }
                "com.example.GENERAL_REMINDER_ACTION" -> {
                    val type = intent.getStringExtra("reminder_type") ?: ""
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val db = Room.databaseBuilder(
                                context.applicationContext,
                                AppDatabase::class.java,
                                "habitengine_habits_ledger.db"
                            ).build()
                            val adapter = RoomStorageAdapter(db)
                            val state = adapter.loadTrackerState().first()
                            
                            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                            val completions = state.logs[todayStr] ?: emptyMap()
                            
                            val incompleteCount = state.habits.count { habit ->
                                habit.cadence.isApplicableOn(todayStr) && completions[habit.id] != true
                            }
                            
                            db.close()
                            
                            if (incompleteCount > 0) {
                                if (type == "morning") {
                                    showNotification(
                                        context,
                                        999,
                                        "Morning Habit Review",
                                        "You have $incompleteCount incomplete habits today. Keep your streaks alive!"
                                    )
                                } else {
                                    showNotification(
                                        context,
                                        998,
                                        "Evening Habit Tracker",
                                        "Don't forget to log your habits! $incompleteCount tasks are still outstanding."
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }
        }
    }

    private fun showNotification(context: Context, id: Int, title: String, text: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        
        val intent = Intent(context, MainActivity::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(context, id, intent, flags)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(id, notification)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
        }
    }
}
