package com.example.core.timer

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.core.model.Subject
import com.example.core.model.TaskPriority
import com.example.core.repository.StudyPlannerRepository
import com.example.core.repository.LocalStudentProfileRepository
import com.example.core.storage.StudentPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Persistent Study Timer Manager.
 * Tracks focus study sessions with no fixed time limit.
 * Uses timestamp-based calculations for background accuracy.
 */
object StudyTimerManager {

    private const val TAG = "StudyTimerManager"
    private const val PREFS_NAME = "nexora_study_timer_v2_prefs"
    private const val KEY_SUBJECT_ID = "key_timer_subject_id"
    private const val KEY_SUBJECT_NAME = "key_timer_subject_name"
    private const val KEY_IS_RUNNING = "key_timer_is_running"
    private const val KEY_IS_PAUSED = "key_timer_is_paused"
    private const val KEY_START_EPOCH = "key_timer_start_epoch"
    private const val KEY_BASE_ELAPSED = "key_timer_base_elapsed"
    private const val KEY_COMPLETED_SESSIONS = "key_timer_completed_sessions"
    private const val KEY_TOTAL_FOCUSED_MINS = "key_timer_total_focused_mins"
    private const val KEY_SESSION_HISTORY = "key_timer_session_history"

    // Points constants
    private const val MINUTES_PER_POINT_BLOCK = 5
    private const val POINTS_PER_BLOCK = 10

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var tickerJob: Job? = null
    private var appContext: Context? = null

    private val _timerState = MutableStateFlow(StudyTimerState())
    val timerState: StateFlow<StudyTimerState> = _timerState.asStateFlow()

    private val _sessionHistory = MutableStateFlow<List<FocusSessionRecord>>(emptyList())
    val sessionHistory: StateFlow<List<FocusSessionRecord>> = _sessionHistory.asStateFlow()

    fun initialize(context: Context) {
        appContext = context.applicationContext
        loadPersistedState()
        loadSessionHistory()
    }

    /**
     * Calculate points based on the rule: 10 points for every 5 completed minutes.
     */
    fun calculatePoints(focusedMins: Int): Int {
        return (focusedMins / MINUTES_PER_POINT_BLOCK) * POINTS_PER_BLOCK
    }

    private fun recordCompletedSession(subjectId: String, subjectName: String, durationMins: Int) {
        val now = System.currentTimeMillis()
        val formatter = java.time.format.DateTimeFormatter.ofPattern("d MMM, hh:mm a", java.util.Locale.ENGLISH)
        val timeStr = java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(now), com.example.core.util.IstTimeUtil.IST_ZONE_ID).format(formatter)
        val record = FocusSessionRecord(
            id = java.util.UUID.randomUUID().toString(),
            subjectId = subjectId,
            subjectName = subjectName,
            durationMinutes = durationMins,
            timestampEpochMillis = now,
            formattedTimestamp = timeStr
        )
        val updated = listOf(record) + _sessionHistory.value
        _sessionHistory.value = updated.take(50)
        persistSessionHistory()
    }

    private fun persistSessionHistory() {
        val prefs = getPrefs() ?: return
        try {
            val array = org.json.JSONArray()
            for (r in _sessionHistory.value) {
                val obj = org.json.JSONObject()
                obj.put("id", r.id)
                obj.put("subjectId", r.subjectId)
                obj.put("subjectName", r.subjectName)
                obj.put("durationMinutes", r.durationMinutes)
                obj.put("timestampEpochMillis", r.timestampEpochMillis)
                obj.put("formattedTimestamp", r.formattedTimestamp)
                array.put(obj)
            }
            prefs.edit().putString(KEY_SESSION_HISTORY, array.toString()).apply()
        } catch (_: Exception) {}
    }

    private fun loadSessionHistory() {
        val prefs = getPrefs() ?: return
        val jsonStr = prefs.getString(KEY_SESSION_HISTORY, null) ?: return
        try {
            val array = org.json.JSONArray(jsonStr)
            val list = mutableListOf<FocusSessionRecord>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    FocusSessionRecord(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        subjectId = obj.optString("subjectId", "accounts"),
                        subjectName = obj.optString("subjectName", "Elements of Accounts"),
                        durationMinutes = obj.optInt("durationMinutes", 0),
                        timestampEpochMillis = obj.optLong("timestampEpochMillis", System.currentTimeMillis()),
                        formattedTimestamp = obj.optString("formattedTimestamp", "")
                    )
                )
            }
            _sessionHistory.value = list
        } catch (_: Exception) {}
    }

    private fun getPrefs(): SharedPreferences? {
        return appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun loadPersistedState() {
        val prefs = getPrefs() ?: return
        val subjectId = prefs.getString(KEY_SUBJECT_ID, "accounts") ?: "accounts"
        val subjectName = prefs.getString(KEY_SUBJECT_NAME, "Elements of Accounts") ?: "Elements of Accounts"
        val isRunning = prefs.getBoolean(KEY_IS_RUNNING, false)
        val isPaused = prefs.getBoolean(KEY_IS_PAUSED, false)
        val startEpoch = if (prefs.contains(KEY_START_EPOCH)) prefs.getLong(KEY_START_EPOCH, 0L) else null
        val baseElapsed = prefs.getLong(KEY_BASE_ELAPSED, 0L)
        val completedSessions = prefs.getInt(KEY_COMPLETED_SESSIONS, 0)
        val totalFocusedMins = prefs.getInt(KEY_TOTAL_FOCUSED_MINS, 0)

        _timerState.value = StudyTimerState(
            subjectId = subjectId,
            subjectName = subjectName,
            isRunning = isRunning,
            isPaused = isPaused,
            startEpochMillis = startEpoch,
            baseElapsedMillis = baseElapsed,
            completedSessionsCount = completedSessions,
            totalFocusedMinutes = totalFocusedMins
        )

        if (isRunning && !isPaused) {
            startTicker()
        }
    }

    private fun persistState() {
        val prefs = getPrefs() ?: return
        val state = _timerState.value
        prefs.edit()
            .putString(KEY_SUBJECT_ID, state.subjectId)
            .putString(KEY_SUBJECT_NAME, state.subjectName)
            .putBoolean(KEY_IS_RUNNING, state.isRunning)
            .putBoolean(KEY_IS_PAUSED, state.isPaused)
            .putLong(KEY_BASE_ELAPSED, state.baseElapsedMillis)
            .apply {
                if (state.startEpochMillis != null) putLong(KEY_START_EPOCH, state.startEpochMillis) else remove(KEY_START_EPOCH)
            }
            .putInt(KEY_COMPLETED_SESSIONS, state.completedSessionsCount)
            .putInt(KEY_TOTAL_FOCUSED_MINS, state.totalFocusedMinutes)
            .apply()
    }

    fun selectSubject(subject: Subject) {
        if (_timerState.value.isRunning) return
        _timerState.update {
            it.copy(
                subjectId = subject.id,
                subjectName = subject.name
            )
        }
        persistState()
    }

    fun startTimer() {
        _timerState.update {
            it.copy(
                isRunning = true,
                isPaused = false,
                isCompleted = false,
                startEpochMillis = System.currentTimeMillis(),
                baseElapsedMillis = 0L
            )
        }
        persistState()
        startTicker()
    }

    fun pauseTimer() {
        tickerJob?.cancel()
        tickerJob = null
        val now = System.currentTimeMillis()
        _timerState.update { state ->
            val addedTime = if (state.startEpochMillis != null) now - state.startEpochMillis else 0L
            state.copy(
                isRunning = true,
                isPaused = true,
                startEpochMillis = null,
                baseElapsedMillis = state.baseElapsedMillis + addedTime
            )
        }
        persistState()
    }

    fun resumeTimer() {
        _timerState.update {
            it.copy(
                isRunning = true,
                isPaused = false,
                startEpochMillis = System.currentTimeMillis()
            )
        }
        persistState()
        startTicker()
    }

    fun resetTimer() {
        tickerJob?.cancel()
        tickerJob = null
        _timerState.update {
            it.copy(
                isRunning = false,
                isPaused = false,
                isCompleted = false,
                startEpochMillis = null,
                baseElapsedMillis = 0L
            )
        }
        persistState()
    }

    /**
     * Finishes the current session, records activity and awards points.
     */
    fun finishSession() {
        tickerJob?.cancel()
        tickerJob = null
        val now = System.currentTimeMillis()
        val current = _timerState.value
        
        // Calculate total study time
        val sessionElapsedMillis = if (current.isRunning && !current.isPaused && current.startEpochMillis != null) {
            current.baseElapsedMillis + (now - current.startEpochMillis)
        } else {
            current.baseElapsedMillis
        }
        
        val focusedMins = (sessionElapsedMillis / 60000).toInt()
        
        if (focusedMins > 0) {
            // 1. Log in Planner
            try {
                StudyPlannerRepository.addTask(
                    title = "Focus Session: ${current.subjectName}",
                    description = "Completed $focusedMins min deep study focus session",
                    subjectId = current.subjectId,
                    scheduledDate = StudyPlannerRepository.TODAY,
                    priority = TaskPriority.HIGH,
                    durationMinutes = focusedMins
                )
            } catch (_: Exception) {}

            // 2. Record in History
            recordCompletedSession(current.subjectId, current.subjectName, focusedMins)

            // 3. Award points: 10 points per 5 minutes
            val points = calculatePoints(focusedMins)
            if (points > 0 && appContext != null) {
                val ctx = appContext!!
                val profile = StudentPreferences.getStudentProfile(ctx)
                if (profile.userId.isNotBlank()) {
                    scope.launch(Dispatchers.IO) {
                        // Use a session-specific action key to ensure idempotency
                        val sessionActionKey = "timer_session_${java.util.UUID.randomUUID()}"
                        val repo = LocalStudentProfileRepository(ctx)
                        repo.awardPointsIdempotent(
                            profile.userId,
                            sessionActionKey,
                            points
                        )
                        // Also award daily streak point if not already awarded today
                        val todayStr = java.time.LocalDate.now(com.example.core.util.IstTimeUtil.IST_ZONE_ID).toString()
                        repo.awardPointsIdempotent(profile.userId, "streak_$todayStr", 5)
                        Log.d(TAG, "Awarded $points points for $focusedMins min focus session.")
                    }
                }
            }
        }

        _timerState.update {
            it.copy(
                isRunning = false,
                isPaused = false,
                isCompleted = true,
                startEpochMillis = null,
                baseElapsedMillis = 0L,
                completedSessionsCount = it.completedSessionsCount + (if (focusedMins > 0) 1 else 0),
                totalFocusedMinutes = it.totalFocusedMinutes + focusedMins
            )
        }
        persistState()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                delay(1000L)
                val current = _timerState.value
                if (!current.isRunning || current.isPaused) break
                
                // Just trigger a re-emission for UI observers if needed,
                // though usually StateFlow values remain same if calculation is dynamic.
                // We update the state slightly to force Compose recomposition of timer text.
                _timerState.update { it.copy() }
            }
        }
    }

    /**
     * Merge and update focus session stats from cloud backup.
     */
    fun updateSessionStats(sessions: Int, focusedMins: Int) {
        _timerState.update {
            it.copy(
                completedSessionsCount = maxOf(it.completedSessionsCount, sessions),
                totalFocusedMinutes = maxOf(it.totalFocusedMinutes, focusedMins)
            )
        }
        persistState()
    }

    fun resetForTesting() {
        tickerJob?.cancel()
        tickerJob = null
        _timerState.value = StudyTimerState()
        _sessionHistory.value = emptyList()
        val prefs = getPrefs()
        prefs?.edit()?.clear()?.apply()
    }
}
