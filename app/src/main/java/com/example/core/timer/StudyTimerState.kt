package com.example.core.timer

import androidx.compose.ui.graphics.Color
import com.example.core.model.Subject

/**
 * Focus Timer Preset Modes.
 * Note: These are now starting points rather than fixed limits.
 */
enum class StudyTimerPreset(
    val title: String,
    val minutes: Int,
    val subtitle: String
) {
    QUICK_START("Flexible Focus", 0, "No time limit, study as long as you need"),
    POMODORO("25m Goal", 25, "Target a standard Pomodoro block"),
    DEEP_FOCUS("45m Goal", 45, "Target in-depth chapter mastery"),
    EXAM_DRILL("60m Goal", 60, "Target full exam paper practice")
}

/**
 * State representation for the persistent study focus timer.
 * Optimized for open-ended stopwatch-style tracking with persistence.
 */
data class StudyTimerState(
    val subjectId: String = "accounts",
    val subjectName: String = "Elements of Accounts",
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val isCompleted: Boolean = false,
    val startEpochMillis: Long? = null,
    val baseElapsedMillis: Long = 0L,
    val completedSessionsCount: Int = 0,
    val totalFocusedMinutes: Int = 0
) {
    /**
     * Total elapsed study time in seconds, excluding paused intervals.
     */
    val elapsedSeconds: Long
        get() {
            if (!isRunning) return baseElapsedMillis / 1000L
            if (isPaused) return baseElapsedMillis / 1000L
            
            val currentInterval = if (startEpochMillis != null) {
                System.currentTimeMillis() - startEpochMillis
            } else 0L
            
            return (baseElapsedMillis + currentInterval) / 1000L
        }

    /**
     * UI representation of elapsed time (MM:SS or HH:MM:SS)
     */
    val formattedElapsedTime: String
        get() {
            val totalSecs = elapsedSeconds
            val hours = totalSecs / 3600
            val mins = (totalSecs % 3600) / 60
            val secs = totalSecs % 60
            
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, mins, secs)
            } else {
                String.format("%02d:%02d", mins, secs)
            }
        }

    /**
     * For backward compatibility with UI components that expect progress.
     * Since there's no fixed limit, we can't show a meaningful percentage
     * unless we have a "target". For now, we return 0 or just let the UI handle it.
     */
    val progressFraction: Float = 0f
    val remainingSeconds: Long = 0L
    val formattedRemainingTime: String = "∞"
}

/**
 * Record of a completed focus study session.
 */
data class FocusSessionRecord(
    val id: String,
    val subjectId: String,
    val subjectName: String,
    val durationMinutes: Int,
    val timestampEpochMillis: Long,
    val formattedTimestamp: String
)
