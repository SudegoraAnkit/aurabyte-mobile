package com.example.core.ports

import com.example.core.domain.ActivityLog
import com.example.core.domain.Habit
import com.example.core.domain.TrackerState
import kotlinx.coroutines.flow.Flow

interface StoragePort {
    fun loadTrackerState(): Flow<TrackerState>
    fun loadTrackerStateForProfile(profileId: String): Flow<TrackerState>
    suspend fun saveHabit(habit: Habit)
    suspend fun toggleLogEntry(date: String, habitId: String, currentStatus: Boolean)
    suspend fun deleteHabit(habitId: String)
    fun loadActivityLogs(): Flow<List<ActivityLog>>
    suspend fun saveActivityLog(log: ActivityLog)
    suspend fun deleteActivityLog(id: String)
    suspend fun restoreBackup(
        habits: List<Habit>,
        logs: List<com.example.infrastructure.adapters.database.LogEntity>,
        activityLogs: List<ActivityLog>
    )
}
