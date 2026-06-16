package com.example.infrastructure.adapters.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.domain.Cadence
import com.example.core.domain.Habit
import com.example.core.domain.LifeDomain
import com.example.core.ports.StoragePort
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

import com.example.core.domain.ActivityCategory
import com.example.core.domain.ActivityLog
import com.example.infrastructure.adapters.notifications.NotificationScheduler
import com.example.infrastructure.adapters.database.LogEntity
import org.json.JSONArray
import org.json.JSONObject

enum class ThemeMode {
    CYBERPUNK,
    SUNSET,
    MONOCHROME
}

data class CelebrationState(
    val habitId: String,
    val routineText: String,
    val rewardText: String,
    val x: Float,
    val y: Float,
    val timestamp: Long = System.currentTimeMillis()
)

data class MainUiState(
    val habits: List<Habit> = emptyList(),
    val logs: Map<String, Map<String, Boolean>> = emptyMap(),
    val selectedDate: String = "",
    val themeMode: ThemeMode = ThemeMode.CYBERPUNK,
    val isLoading: Boolean = true,
    val celebration: CelebrationState? = null,
    val activityLogs: List<ActivityLog> = emptyList(),
    val activeProfileId: String = "main",
    val unlockedThemes: Set<ThemeMode> = setOf(ThemeMode.CYBERPUNK)
)

class HabitViewModel(
    private val storagePort: StoragePort,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _selectedDate = savedStateHandle.getStateFlow("selectedDate", getTodayDateString())
    private val _themeMode = savedStateHandle.getStateFlow("themeMode", ThemeMode.CYBERPUNK)
    private val _activeProfileId = savedStateHandle.getStateFlow("activeProfileId", "main")
    private val _celebration = MutableStateFlow<CelebrationState?>(null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<MainUiState> = combine(
        _activeProfileId.flatMapLatest { profileId ->
            storagePort.loadTrackerStateForProfile(profileId)
        },
        storagePort.loadActivityLogs(),
        _selectedDate,
        _themeMode,
        _celebration
    ) { trackerState, activities, date, theme, celeb ->
        val profile = _activeProfileId.value
        val unlocked = mutableSetOf(ThemeMode.CYBERPUNK)
        trackerState.habits.forEach { habit ->
            val stats = habit.getStreakStats(trackerState.logs, date)
            if (stats.currentStreak >= 7 || stats.longestStreak >= 7) {
                unlocked.add(ThemeMode.SUNSET)
            }
            if (stats.currentStreak >= 30 || stats.longestStreak >= 30) {
                unlocked.add(ThemeMode.MONOCHROME)
            }
        }

        val resolvedTheme = if (theme in unlocked) theme else ThemeMode.CYBERPUNK

        MainUiState(
            habits = trackerState.habits,
            logs = trackerState.logs,
            selectedDate = date,
            themeMode = resolvedTheme,
            isLoading = false,
            celebration = celeb,
            activityLogs = activities,
            activeProfileId = profile,
            unlockedThemes = unlocked
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState(selectedDate = getTodayDateString())
    )

    fun selectDate(dateString: String) {
        savedStateHandle["selectedDate"] = dateString
    }

    fun selectProfile(profileId: String) {
        savedStateHandle["activeProfileId"] = profileId
    }

    fun toggleTheme() {
        val nextTheme = when (_themeMode.value) {
            ThemeMode.CYBERPUNK -> ThemeMode.SUNSET
            ThemeMode.SUNSET -> ThemeMode.MONOCHROME
            ThemeMode.MONOCHROME -> ThemeMode.CYBERPUNK
        }
        savedStateHandle["themeMode"] = nextTheme
    }

    fun setTheme(theme: ThemeMode) {
        savedStateHandle["themeMode"] = theme
    }

    fun createHabit(
        context: android.content.Context,
        domain: LifeDomain,
        cadence: Cadence,
        cueText: String,
        routineText: String,
        rewardText: String,
        notes: String = "",
        isBad: Boolean = false,
        targetMilestone: Int = 0,
        restartOnMiss: Boolean = false,
        reminderHour: Int? = null,
        reminderMinute: Int? = null,
        profileId: String = "main"
    ) {
        viewModelScope.launch {
            val habit = Habit(
                id = UUID.randomUUID().toString(),
                domain = domain,
                cadence = cadence,
                cueText = cueText.trim(),
                routineText = routineText.trim(),
                rewardText = rewardText.trim(),
                notes = notes.trim(),
                isBad = isBad,
                targetMilestone = targetMilestone,
                restartOnMiss = restartOnMiss,
                reminderHour = reminderHour,
                reminderMinute = reminderMinute,
                profileId = profileId
            )
            storagePort.saveHabit(habit)
            if (reminderHour != null && reminderMinute != null) {
                NotificationScheduler.scheduleHabitReminder(context.applicationContext, habit)
            }
        }
    }

    fun updateHabit(
        context: android.content.Context,
        habitId: String,
        domain: LifeDomain,
        cadence: Cadence,
        cueText: String,
        routineText: String,
        rewardText: String,
        createdAt: Long,
        notes: String = "",
        isBad: Boolean = false,
        targetMilestone: Int = 0,
        restartOnMiss: Boolean = false,
        reminderHour: Int? = null,
        reminderMinute: Int? = null,
        profileId: String = "main"
    ) {
        viewModelScope.launch {
            val habit = Habit(
                id = habitId,
                domain = domain,
                cadence = cadence,
                cueText = cueText.trim(),
                routineText = routineText.trim(),
                rewardText = rewardText.trim(),
                createdAt = createdAt,
                notes = notes.trim(),
                isBad = isBad,
                targetMilestone = targetMilestone,
                restartOnMiss = restartOnMiss,
                reminderHour = reminderHour,
                reminderMinute = reminderMinute,
                profileId = profileId
            )
            storagePort.saveHabit(habit)
            NotificationScheduler.cancelHabitReminder(context.applicationContext, habitId)
            if (reminderHour != null && reminderMinute != null) {
                NotificationScheduler.scheduleHabitReminder(context.applicationContext, habit)
            }
        }
    }

    fun createActivityLog(
        description: String,
        category: ActivityCategory,
        durationMinutes: Int
    ) {
        viewModelScope.launch {
            val log = ActivityLog(
                id = UUID.randomUUID().toString(),
                description = description.trim(),
                category = category,
                timestamp = System.currentTimeMillis(),
                durationMinutes = durationMinutes
            )
            storagePort.saveActivityLog(log)
        }
    }

    fun deleteActivityLog(id: String) {
        viewModelScope.launch {
            storagePort.deleteActivityLog(id)
        }
    }

    fun toggleHabitCompletion(habitId: String, currentStatus: Boolean, clickX: Float, clickY: Float) {
        viewModelScope.launch {
            val date = _selectedDate.value
            storagePort.toggleLogEntry(date, habitId, currentStatus)

            if (!currentStatus) {
                val state = uiState.value
                val habit = state.habits.find { it.id == habitId }
                if (habit != null) {
                    _celebration.value = CelebrationState(
                        habitId = habitId,
                        routineText = habit.routineText,
                        rewardText = habit.rewardText,
                        x = clickX,
                        y = clickY
                    )
                }
            }
        }
    }

    fun dismissCelebration() {
        _celebration.value = null
    }

    fun deleteHabit(context: android.content.Context, habitId: String) {
        viewModelScope.launch {
            storagePort.deleteHabit(habitId)
            NotificationScheduler.cancelHabitReminder(context.applicationContext, habitId)
        }
    }

    fun exportBackupAsJson(): String {
        return try {
            val state = uiState.value
            val backupObj = JSONObject()
            
            val habitsArr = JSONArray()
            state.habits.forEach { habit ->
                val habitObj = JSONObject().apply {
                    put("id", habit.id)
                    put("domain", habit.domain.name)
                    put("cadence", habit.cadence.name)
                    put("cueText", habit.cueText)
                    put("routineText", habit.routineText)
                    put("rewardText", habit.rewardText)
                    put("createdAt", habit.createdAt)
                    put("notes", habit.notes)
                    put("isBad", habit.isBad)
                    put("targetMilestone", habit.targetMilestone)
                    put("restartOnMiss", habit.restartOnMiss)
                    if (habit.reminderHour != null) put("reminderHour", habit.reminderHour)
                    if (habit.reminderMinute != null) put("reminderMinute", habit.reminderMinute)
                    put("profileId", habit.profileId)
                }
                habitsArr.put(habitObj)
            }
            backupObj.put("habits", habitsArr)
            
            val logsObj = JSONObject()
            state.logs.forEach { dateKey, habitMap ->
                val habitMapObj = JSONObject()
                habitMap.forEach { habitId, completed ->
                    habitMapObj.put(habitId, completed)
                }
                logsObj.put(dateKey, habitMapObj)
            }
            backupObj.put("logs", logsObj)
            
            val activitiesArr = JSONArray()
            state.activityLogs.forEach { log ->
                val logObj = JSONObject().apply {
                    put("id", log.id)
                    put("description", log.description)
                    put("category", log.category.name)
                    put("timestamp", log.timestamp)
                    put("durationMinutes", log.durationMinutes)
                }
                activitiesArr.put(logObj)
            }
            backupObj.put("activityLogs", activitiesArr)
            
            backupObj.toString(4)
        } catch (e: Exception) {
            ""
        }
    }

    fun restoreBackupFromJson(jsonString: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val backupObj = JSONObject(jsonString)
                
                val habitsList = mutableListOf<Habit>()
                if (backupObj.has("habits")) {
                    val habitsArr = backupObj.getJSONArray("habits")
                    for (i in 0 until habitsArr.length()) {
                        val obj = habitsArr.getJSONObject(i)
                        habitsList.add(
                            Habit(
                                id = obj.getString("id"),
                                domain = try { LifeDomain.valueOf(obj.getString("domain")) } catch(e: Exception) { LifeDomain.PERSONAL },
                                cadence = try { Cadence.valueOf(obj.getString("cadence")) } catch(e: Exception) { Cadence.DAILY },
                                cueText = obj.optString("cueText", ""),
                                routineText = obj.getString("routineText"),
                                rewardText = obj.optString("rewardText", ""),
                                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                                notes = obj.optString("notes", ""),
                                isBad = obj.optBoolean("isBad", false),
                                targetMilestone = obj.optInt("targetMilestone", 0),
                                restartOnMiss = obj.optBoolean("restartOnMiss", false),
                                reminderHour = if (obj.has("reminderHour")) obj.getInt("reminderHour") else null,
                                reminderMinute = if (obj.has("reminderMinute")) obj.getInt("reminderMinute") else null,
                                profileId = obj.optString("profileId", "main")
                            )
                        )
                    }
                }
                
                val parsedLogs = mutableListOf<LogEntity>()
                if (backupObj.has("logs")) {
                    val logsObj = backupObj.getJSONObject("logs")
                    val dates = logsObj.keys()
                    while (dates.hasNext()) {
                        val date = dates.next()
                        val habitMapObj = logsObj.getJSONObject(date)
                        val habitIds = habitMapObj.keys()
                        while (habitIds.hasNext()) {
                            val habitId = habitIds.next()
                            val completed = habitMapObj.getBoolean(habitId)
                            parsedLogs.add(
                                LogEntity(
                                    date = date,
                                    habitId = habitId,
                                    completed = completed
                                )
                            )
                        }
                    }
                }
                
                val parsedActivities = mutableListOf<ActivityLog>()
                if (backupObj.has("activityLogs")) {
                    val activitiesArr = backupObj.getJSONArray("activityLogs")
                    for (i in 0 until activitiesArr.length()) {
                        val obj = activitiesArr.getJSONObject(i)
                        parsedActivities.add(
                            ActivityLog(
                                id = obj.getString("id"),
                                description = obj.getString("description"),
                                category = try { ActivityCategory.valueOf(obj.getString("category")) } catch(e: Exception) { ActivityCategory.NEUTRAL },
                                timestamp = obj.getLong("timestamp"),
                                durationMinutes = obj.optInt("durationMinutes", 0)
                            )
                        )
                    }
                }
                
                storagePort.restoreBackup(habitsList, parsedLogs, parsedActivities)
                callback(true)
            } catch (e: Exception) {
                e.printStackTrace()
                callback(false)
            }
        }
    }

    companion object {
        fun getTodayDateString(): String {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return formatter.format(Date())
        }
    }
}
