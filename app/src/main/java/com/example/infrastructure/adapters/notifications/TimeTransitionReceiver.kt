package com.example.infrastructure.adapters.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.infrastructure.adapters.ui.widget.HabitGlanceWidgetReceiver

class TimeTransitionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        
        if (action == Intent.ACTION_TIMEZONE_CHANGED || 
            action == Intent.ACTION_DATE_CHANGED || 
            action == Intent.ACTION_TIME_CHANGED) {
            
            val widgetIntent = Intent(context, HabitGlanceWidgetReceiver::class.java).apply {
                this.action = "android.appwidget.action.APPWIDGET_UPDATE"
            }
            context.sendBroadcast(widgetIntent)
            
            NotificationScheduler.scheduleGeneralReminders(context)
        }
    }
}
