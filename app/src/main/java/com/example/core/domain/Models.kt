package com.example.core.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

enum class LifeDomain {
    HEALTH,
    PROFESSIONAL,
    PERSONAL,
    FAMILY;

    val displayName: String
        get() = when (this) {
            HEALTH -> "Health"
            PROFESSIONAL -> "Professional"
            PERSONAL -> "Personal"
            FAMILY -> "Social"
        }
}

enum class Cadence {
    DAILY,
    WEEKDAYS,
    WEEKENDS,
    MONTHLY;

    val displayName: String
        get() = when (this) {
            DAILY -> "Daily"
            WEEKDAYS -> "Weekdays"
            WEEKENDS -> "Weekends"
            MONTHLY -> "Monthly"
        }
}

data class StreakStats(
    val currentStreak: Int,
    val longestStreak: Int,
    val milestoneReached: Boolean,
    val completionPercentage: Int,
    val historyGrid: List<Boolean>
)

data class Habit(
    val id: String,
    val domain: LifeDomain,
    val cadence: Cadence,
    val cueText: String,
    val routineText: String,
    val rewardText: String,
    val createdAt: Long = System.currentTimeMillis(),
    val notes: String = "",
    val isBad: Boolean = false,
    val targetMilestone: Int = 0,
    val restartOnMiss: Boolean = false,
    val reminderHour: Int? = null,
    val reminderMinute: Int? = null,
    val profileId: String = "main"
) {
    fun getStreakStats(
        logs: Map<String, Map<String, Boolean>>,
        todayStr: String = LocalDate.now(ZoneId.systemDefault()).toString()
    ): StreakStats {
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        val startLocalDate = try {
            Instant.ofEpochMilli(createdAt)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        } catch (e: Exception) {
            LocalDate.now(ZoneId.systemDefault()).minusDays(30)
        }
        
        val today = try {
            LocalDate.parse(todayStr, formatter)
        } catch (e: Exception) {
            LocalDate.now(ZoneId.systemDefault())
        }

        val dateList = mutableListOf<LocalDate>()
        var curr = startLocalDate
        while (!curr.isAfter(today)) {
            if (cadence.isApplicableOn(curr.toString())) {
                dateList.add(curr)
            }
            curr = curr.plusDays(1)
        }

        var currentRun = 0
        var longestRun = 0
        var totalCompletions = 0
        val totalOpportunities = dateList.size

        dateList.forEach { date ->
            val dateStr = date.toString()
            val isCompleted = logs[dateStr]?.get(id) == true
            
            if (isCompleted) {
                currentRun++
                totalCompletions++
                if (currentRun > longestRun) {
                    longestRun = currentRun
                }
            } else {
                val isToday = date.isEqual(today)
                if (restartOnMiss) {
                    if (!isToday) {
                        currentRun = 0
                    }
                }
            }
        }

        val gridList = mutableListOf<Boolean>()
        for (i in 27 downTo 0) {
            val d = today.minusDays(i.toLong())
            val dStr = d.toString()
            val completed = logs[dStr]?.get(id) == true
            gridList.add(completed)
        }

        val percent = if (totalOpportunities == 0) 0 else ((totalCompletions.toFloat() / totalOpportunities) * 100).toInt()
        val milestoneReached = targetMilestone > 0 && (if (restartOnMiss) currentRun else totalCompletions) >= targetMilestone

        return StreakStats(
            currentStreak = currentRun,
            longestStreak = longestRun,
            milestoneReached = milestoneReached,
            completionPercentage = percent,
            historyGrid = gridList
        )
    }
}

enum class ActivityCategory {
    IMPORTANT,
    TIME_WASTER,
    NEUTRAL;

    val displayName: String
        get() = when (this) {
            IMPORTANT -> "Important"
            TIME_WASTER -> "Time Waster"
            NEUTRAL -> "Neutral / Routine"
        }
}

data class ActivityLog(
    val id: String,
    val description: String,
    val category: ActivityCategory,
    val timestamp: Long,
    val durationMinutes: Int = 0
)

data class DayLog(
    val date: String,
    val completions: Map<String, Boolean>
)

data class TrackerState(
    val habits: List<Habit> = emptyList(),
    val logs: Map<String, Map<String, Boolean>> = emptyMap()
)

fun Cadence.isApplicableOn(dateStr: String): Boolean {
    return try {
        val localDate = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
        val dayOfWeek = localDate.dayOfWeek
        when (this) {
            Cadence.DAILY -> true
            Cadence.WEEKDAYS -> {
                dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY
            }
            Cadence.WEEKENDS -> {
                dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY
            }
            Cadence.MONTHLY -> {
                localDate.dayOfMonth == 1
            }
        }
    } catch (e: Exception) {
        true
    }
}
