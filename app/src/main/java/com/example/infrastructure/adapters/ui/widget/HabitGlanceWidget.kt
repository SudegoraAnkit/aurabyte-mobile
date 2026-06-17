package com.example.infrastructure.adapters.ui.widget

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.DisposableEffect
import kotlinx.coroutines.launch
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.room.Room
import com.example.core.domain.isApplicableOn
import com.example.infrastructure.adapters.database.AppDatabase
import com.example.infrastructure.adapters.database.LogEntity
import com.example.infrastructure.adapters.database.RoomStorageAdapter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HabitGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val db = remember {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "habitengine_habits_ledger.db"
                ).build()
            }
            val adapter = remember(db) { RoomStorageAdapter(db) }
            val state by remember(adapter) { adapter.loadTrackerState() }.collectAsState(initial = null)

            DisposableEffect(db) {
                onDispose {
                    db.close()
                }
            }

            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val habitsForToday = state?.habits?.filter { it.cadence.isApplicableOn(todayStr) && it.profileId == "main" } ?: emptyList()
            val completions = state?.logs?.get(todayStr) ?: emptyMap()

            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val isEvening = hour >= 18

            val incompleteCount = habitsForToday.count { completions[it.id] != true }
            
            val widgetBgColor = if (isEvening && incompleteCount > 0) {
                ColorProvider(Color(0xFF3E1A1A))
            } else {
                ColorProvider(Color(0xFF0D1220))
            }

            val accentColor = ColorProvider(Color(0xFF22C55E))
            val textColor = ColorProvider(Color(0xFFF9FAFB))
            val mutedColor = ColorProvider(Color(0xFF6B7280))

            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(widgetBgColor)
                    .padding(8.dp),
                horizontalAlignment = Alignment.Horizontal.Start,
                verticalAlignment = Alignment.Vertical.Top
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Text(
                        text = "⚡ HabitEngine",
                        style = TextStyle(
                            color = textColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Text(
                        text = "$incompleteCount Left",
                        style = TextStyle(
                            color = if (incompleteCount > 0) ColorProvider(Color(0xFFEF4444)) else accentColor,
                            fontSize = 11.sp
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.height(4.dp))

                if (habitsForToday.isEmpty()) {
                    Text(
                        text = "No habits scheduled today.",
                        style = TextStyle(color = mutedColor, fontSize = 11.sp)
                    )
                } else {
                    habitsForToday.take(3).forEach { habit ->
                        val isCompleted = completions[habit.id] == true
                        val habitParam = actionParametersOf(
                            ActionParameters.Key<String>("habit_id") to habit.id,
                            ActionParameters.Key<Boolean>("current_status") to isCompleted
                        )

                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.Vertical.CenterVertically
                        ) {
                            androidx.glance.appwidget.components.CircleIconButton(
                                imageProvider = androidx.glance.ImageProvider(
                                    if (isCompleted) android.R.drawable.checkbox_on_background else android.R.drawable.checkbox_off_background
                                ),
                                contentDescription = "Toggle",
                                onClick = actionRunCallback<ToggleHabitActionCallback>(habitParam),
                                modifier = GlanceModifier.width(20.dp).height(20.dp)
                            )
                            Spacer(modifier = GlanceModifier.width(6.dp))
                            Text(
                                text = habit.routineText,
                                style = TextStyle(
                                    color = if (isCompleted) mutedColor else textColor,
                                    fontSize = 12.sp
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

class ToggleHabitActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val habitId = parameters[ActionParameters.Key<String>("habit_id")] ?: return
        val currentStatus = parameters[ActionParameters.Key<Boolean>("current_status")] ?: return

        val db = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "habitengine_habits_ledger.db"
        ).build()
        
        try {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            db.logDao().insertLog(
                LogEntity(
                    date = todayStr,
                    habitId = habitId,
                    completed = !currentStatus
                )
            )
            HabitGlanceWidget().updateAll(context)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.close()
        }
    }
}

class HabitGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget
        get() = HabitGlanceWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        if (action == "android.appwidget.action.APPWIDGET_UPDATE") {
            val pendingResult = goAsync()
            val resultReceiver = this
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                try {
                    HabitGlanceWidget().updateAll(context)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
