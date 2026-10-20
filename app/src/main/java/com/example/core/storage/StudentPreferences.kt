package com.example.core.storage

import android.content.Context
import android.content.SharedPreferences
import com.example.core.model.StudentProfile
import com.example.core.util.StudentIdGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Persistent local storage manager for student profile, notification settings,
 * and app preferences in NEXORA LEARN.
 */
object StudentPreferences {

    private const val PREFS_NAME = "nexora_student_prefs"
    private const val KEY_USER_ID = "key_user_id"
    private const val KEY_STUDENT_ID = "key_student_id"
    private const val KEY_STUDENT_NAME = "key_student_name"
    private const val KEY_MEDIUM = "key_medium"
    private const val KEY_CLASS_LEVEL = "key_class_level"
    private const val KEY_TARGET_MARKS = "key_target_marks"
    private const val KEY_TARGET_YEAR = "key_target_year"
    private const val KEY_AVATAR_COLOR = "key_avatar_color"
    private const val KEY_PHOTO_URL = "key_photo_url"
    private const val KEY_THEME_MODE = "key_theme_mode"
    private const val KEY_IS_DARK_MODE = "key_is_dark_mode"
    private const val KEY_IDENTITY_SETUP_DONE = "key_identity_setup_done"
    private const val KEY_STUDENT_ID_LOCKED = "key_student_id_locked"
    private const val KEY_IS_GUEST_MODE = "key_is_guest_mode"
    private const val KEY_ONBOARDING_COMPLETED = "key_onboarding_completed"

    private const val KEY_STUDY_REMINDERS_ENABLED = "key_study_reminders_enabled"
    private const val KEY_MOTIVATION_ALERTS_ENABLED = "key_motivation_alerts_enabled"
    private const val KEY_EXAM_ALERTS_ENABLED = "key_exam_alerts_enabled"
    private const val KEY_FRIEND_NOTIFS_ENABLED = "key_friend_notifs_enabled"
    private const val KEY_REMINDER_HOUR = "key_reminder_hour"
    private const val KEY_REMINDER_MINUTE = "key_reminder_minute"
    private const val KEY_HAPTIC_ENABLED = "key_haptic_enabled"
    private const val KEY_TIME_FORMAT_24H = "key_time_format_24h"
    
    private const val KEY_WEEKLY_SUMMARY_ENABLED = "key_weekly_summary_enabled"
    private const val KEY_SUMMARY_HOUR = "key_summary_hour"
    private const val KEY_SUMMARY_MINUTE = "key_summary_minute"

    private const val KEY_REMINDER_MORNING_ENABLED = "key_reminder_morning_enabled"
    private const val KEY_REMINDER_EVENING_ENABLED = "key_reminder_evening_enabled"
    private const val KEY_REMINDER_NIGHT_ENABLED = "key_reminder_night_enabled"
    private const val KEY_COMPLETED_TODO_COUNT = "key_completed_todo_count"
    private const val KEY_STUDY_STREAK_DAYS = "key_study_streak_days"
    private const val KEY_COMPETITION_POINTS = "key_competition_points"
    private const val KEY_COMPLETED_CHAPTER_IDS = "key_completed_chapter_ids"
    private const val KEY_TOTAL_FOCUSED_MINS = "key_total_focused_mins"
    private const val KEY_COMPLETED_SESSIONS_COUNT = "key_completed_sessions_count"
    private const val KEY_LAST_RESET_MONTH = "key_last_reset_month"

    private val _is24HourFormatFlow = MutableStateFlow(false)
    val is24HourFormatFlow: StateFlow<Boolean> = _is24HourFormatFlow.asStateFlow()

    fun is24HourFormat(context: Context): Boolean {
        val prefs = getPrefs(context)
        val value = if (prefs.contains(KEY_TIME_FORMAT_24H)) {
            prefs.getBoolean(KEY_TIME_FORMAT_24H, false)
        } else {
            android.text.format.DateFormat.is24HourFormat(context)
        }
        _is24HourFormatFlow.value = value
        return value
    }

    fun set24HourFormat(context: Context, is24Hour: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_TIME_FORMAT_24H, is24Hour).apply()
        _is24HourFormatFlow.value = is24Hour
    }

    private val _currentProfile = MutableStateFlow<StudentProfile?>(null)
    val currentProfile: StateFlow<StudentProfile?> = _currentProfile.asStateFlow()

    private val _isDarkModeFlow = MutableStateFlow(false)
    val isDarkModeFlow: StateFlow<Boolean> = _isDarkModeFlow.asStateFlow()

    private val _themeModeFlow = MutableStateFlow("LIGHT")
    val themeModeFlow: StateFlow<String> = _themeModeFlow.asStateFlow()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Get current theme mode ("LIGHT", "DEEP_NAVY", "NAVY_NIGHT").
     */
    fun getThemeMode(context: Context): String {
        val mode = getPrefs(context).getString(KEY_THEME_MODE, "LIGHT") ?: "LIGHT"
        _themeModeFlow.value = mode
        return mode
    }

    /**
     * Set theme mode ("LIGHT", "DEEP_NAVY", "NAVY_NIGHT").
     */
    fun setThemeMode(context: Context, mode: String) {
        getPrefs(context).edit().putString(KEY_THEME_MODE, mode).apply()
        _themeModeFlow.value = mode
        // Also keep isDarkMode in sync for legacy compatibility
        val isDark = mode != "LIGHT"
        getPrefs(context).edit().putBoolean(KEY_IS_DARK_MODE, isDark).apply()
        _isDarkModeFlow.value = isDark
    }

    /**
     * Check if dark mode is active.
     */
    fun isDarkMode(context: Context): Boolean {
        val mode = getThemeMode(context)
        return mode == "DEEP_NAVY" || mode == "NAVY_NIGHT"
    }

    /**
     * Set dark mode.
     */
    fun setDarkMode(context: Context, isDark: Boolean) {
        val mode = if (isDark) "DEEP_NAVY" else "LIGHT"
        setThemeMode(context, mode)
    }

    /**
     * Check if user is browsing as guest.
     */
    fun isGuestMode(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_GUEST_MODE, false)
    }

    /**
     * Set guest mode preference.
     */
    fun setGuestMode(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_IS_GUEST_MODE, enabled).apply()
    }

    // Notification Preference Helpers
    fun isStudyRemindersEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_STUDY_REMINDERS_ENABLED, true)

    fun setStudyRemindersEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_STUDY_REMINDERS_ENABLED, enabled).apply()
    }

    fun isMotivationAlertsEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_MOTIVATION_ALERTS_ENABLED, true)

    fun setMotivationAlertsEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_MOTIVATION_ALERTS_ENABLED, enabled).apply()
    }

    fun isExamAlertsEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_EXAM_ALERTS_ENABLED, true)

    fun setExamAlertsEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_EXAM_ALERTS_ENABLED, enabled).apply()
    }

    fun isFriendNotifsEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_FRIEND_NOTIFS_ENABLED, true)

    fun setFriendNotifsEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_FRIEND_NOTIFS_ENABLED, enabled).apply()
    }

    fun isHapticEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_HAPTIC_ENABLED, true)

    fun setHapticEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_HAPTIC_ENABLED, enabled).apply()
    }

    fun getReminderTimeHour(context: Context): Int =
        getPrefs(context).getInt(KEY_REMINDER_HOUR, 19) // Default 7:00 PM

    fun getReminderTimeMinute(context: Context): Int =
        getPrefs(context).getInt(KEY_REMINDER_MINUTE, 0)

    fun setReminderTime(context: Context, hour: Int, minute: Int) {
        getPrefs(context).edit()
            .putInt(KEY_REMINDER_HOUR, hour)
            .putInt(KEY_REMINDER_MINUTE, minute)
            .apply()
    }

    fun isWeeklySummaryEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_WEEKLY_SUMMARY_ENABLED, true)

    fun setWeeklySummaryEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_WEEKLY_SUMMARY_ENABLED, enabled).apply()
    }

    fun getSummaryTimeHour(context: Context): Int =
        getPrefs(context).getInt(KEY_SUMMARY_HOUR, 19)

    fun getSummaryTimeMinute(context: Context): Int =
        getPrefs(context).getInt(KEY_SUMMARY_MINUTE, 0)

    fun setSummaryTime(context: Context, hour: Int, minute: Int) {
        getPrefs(context).edit()
            .putInt(KEY_SUMMARY_HOUR, hour)
            .putInt(KEY_SUMMARY_MINUTE, minute)
            .apply()
    }

    fun isMorningReminderEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_REMINDER_MORNING_ENABLED, true)

    fun setMorningReminderEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_REMINDER_MORNING_ENABLED, enabled).apply()
    }

    fun isEveningReminderEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_REMINDER_EVENING_ENABLED, true)

    fun setEveningReminderEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_REMINDER_EVENING_ENABLED, enabled).apply()
    }

    fun isNightReminderEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_REMINDER_NIGHT_ENABLED, true)

    fun setNightReminderEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_REMINDER_NIGHT_ENABLED, enabled).apply()
    }

    fun hasCompletedOnboarding(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun setOnboardingCompleted(context: Context, completed: Boolean = true) {
        getPrefs(context).edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    /**
     * Check if the one-time Student Identity Setup has been completed.
     */
    fun hasCompletedSetup(context: Context): Boolean {
        val prefs = getPrefs(context)
        val isExplicitlyCompleted = prefs.getBoolean(KEY_IDENTITY_SETUP_DONE, false)
        if (isExplicitlyCompleted) return true
        val name = prefs.getString(KEY_STUDENT_NAME, "")?.trim().orEmpty()
        val isLocked = prefs.getBoolean(KEY_STUDENT_ID_LOCKED, false)
        return isLocked && name.isNotBlank()
    }

    /**
     * Check if Student ID is immutable and permanently locked.
     */
    fun isStudentIdImmutable(context: Context): Boolean {
        val prefs = getPrefs(context)
        return prefs.getBoolean(KEY_STUDENT_ID_LOCKED, false) || hasCompletedSetup(context)
    }

    /**
     * Complete the first-launch Student Identity Setup.
     * Atomically saves the student's name and permanent Student ID,
     * locks the Student ID so it becomes immutable, and marks setup as completed.
     */
    fun completeIdentitySetup(context: Context, name: String, studentId: String): StudentProfile {
        val trimmedName = name.trim()
        val normalizedId = StudentIdGenerator.normalizeStudentId(studentId) ?: studentId.trim().uppercase()
        val prefs = getPrefs(context)
        prefs.edit()
            .putString(KEY_STUDENT_NAME, trimmedName)
            .putString(KEY_STUDENT_ID, normalizedId)
            .putBoolean(KEY_IDENTITY_SETUP_DONE, true)
            .putBoolean(KEY_STUDENT_ID_LOCKED, true)
            .apply()

        val existing = getStudentProfile(context)
        val updatedProfile = existing.copy(
            studentName = if (trimmedName.isNotBlank()) trimmedName else "Class 12 Student",
            studentId = normalizedId,
            updatedAt = System.currentTimeMillis()
        )
        _currentProfile.value = updatedProfile
        return updatedProfile
    }

    /**
     * Check if a student name has already been entered and saved locally.
     */
    fun hasSavedName(context: Context): Boolean {
        val name = getPrefs(context).getString(KEY_STUDENT_NAME, "")?.trim().orEmpty()
        return name.isNotBlank()
    }

    /**
     * Get the student's locally saved name.
     */
    fun getSavedStudentName(context: Context): String {
        return getPrefs(context).getString(KEY_STUDENT_NAME, "")?.trim().orEmpty()
    }

    /**
     * Load or initialize the persistent local StudentProfile.
     */
    fun getStudentProfile(context: Context): StudentProfile {
        val prefs = getPrefs(context)
        var userId = prefs.getString(KEY_USER_ID, null)
        if (userId.isNullOrBlank()) {
            userId = if (isGuestMode(context)) "GUEST" else ""
            if (userId.isNotBlank()) {
                prefs.edit().putString(KEY_USER_ID, userId).apply()
            }
        }

        var studentId = prefs.getString(KEY_STUDENT_ID, null)
        if (studentId.isNullOrBlank()) {
            studentId = if (isGuestMode(context)) "GUEST" else StudentIdGenerator.generateCandidateId()
            prefs.edit().putString(KEY_STUDENT_ID, studentId).apply()
        } else if (studentId != "GUEST" && isGuestMode(context)) {
            // If they were a real user and somehow became a guest (unlikely but safe)
            // or if we want to ensure guests always see 'GUEST'
        }

        val name = prefs.getString(KEY_STUDENT_NAME, "")?.trim().orEmpty()
        val medium = prefs.getString(KEY_MEDIUM, "Gujarati Medium") ?: "Gujarati Medium"
        val classLevel = prefs.getString(KEY_CLASS_LEVEL, "Class 12 Commerce") ?: "Class 12 Commerce"
        val targetMarks = prefs.getString(KEY_TARGET_MARKS, "90+ Marks") ?: "90+ Marks"
        val avatarColor = prefs.getLong(KEY_AVATAR_COLOR, 0xFF00E5FF)
        val photoUrl = prefs.getString(KEY_PHOTO_URL, "") ?: ""
        
        var completedTodos = prefs.getInt(KEY_COMPLETED_TODO_COUNT, 0)
        var streakDays = prefs.getInt(KEY_STUDY_STREAK_DAYS, 0)
        var points = prefs.getInt(KEY_COMPETITION_POINTS, 0)
        val chaptersStr = prefs.getString(KEY_COMPLETED_CHAPTER_IDS, "") ?: ""
        val chapters = if (chaptersStr.isNotBlank()) chaptersStr.split(",").filter { it.isNotBlank() } else emptyList()
        val focusedMins = prefs.getInt(KEY_TOTAL_FOCUSED_MINS, 0)
        val sessionCount = prefs.getInt(KEY_COMPLETED_SESSIONS_COUNT, 0)

        // Monthly Reset Logic
        val currentMonthStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"))
        val lastResetMonth = prefs.getString(KEY_LAST_RESET_MONTH, "") ?: ""
        if (lastResetMonth.isNotBlank() && lastResetMonth != currentMonthStr) {
            // New month detected! Reset points for the new monthly leaderboard.
            points = 0
            prefs.edit()
                .putInt(KEY_COMPETITION_POINTS, 0)
                .putString(KEY_LAST_RESET_MONTH, currentMonthStr)
                .apply()
            android.util.Log.d("StudentPrefs", "Monthly points reset from $lastResetMonth to $currentMonthStr")
        } else if (lastResetMonth.isBlank()) {
            prefs.edit().putString(KEY_LAST_RESET_MONTH, currentMonthStr).apply()
        }

        val profile = StudentProfile(
            userId = userId,
            studentId = if (isGuestMode(context)) "GUEST" else studentId,
            studentName = if (name.isNotBlank()) name else "Class 12 Student",
            photoUrl = photoUrl,
            medium = medium,
            classLevel = classLevel,
            targetMarks = targetMarks,
            avatarColorHex = String.format("#%06X", (0xFFFFFFL and avatarColor)),
            completedTodoCount = completedTodos,
            studyStreakDays = streakDays,
            competitionPoints = points,
            completedChapterIds = chapters,
            totalFocusedMinutes = focusedMins,
            completedSessionsCount = sessionCount
        )

        _currentProfile.value = profile
        return profile
    }

    /**
     * Save the student's name on first setup or update.
     */
    fun saveStudentName(context: Context, name: String): StudentProfile {
        val trimmedName = name.trim()
        val prefs = getPrefs(context)
        prefs.edit().putString(KEY_STUDENT_NAME, trimmedName).apply()

        val updatedProfile = getStudentProfile(context).copy(
            studentName = if (trimmedName.isNotBlank()) trimmedName else "Class 12 Student",
            updatedAt = System.currentTimeMillis()
        )
        _currentProfile.value = updatedProfile
        return updatedProfile
    }

    /**
     * Save the student's photoUrl locally.
     */
    fun savePhotoUrl(context: Context, photoUrl: String): StudentProfile {
        val prefs = getPrefs(context)
        prefs.edit().putString(KEY_PHOTO_URL, photoUrl).apply()

        val updatedProfile = getStudentProfile(context).copy(
            photoUrl = photoUrl,
            updatedAt = System.currentTimeMillis()
        )
        _currentProfile.value = updatedProfile
        return updatedProfile
    }

    /**
     * Save student with entered name and student ID.
     */
    fun saveStudentProfileWithId(context: Context, name: String, studentId: String): StudentProfile {
        val trimmedName = name.trim()
        val normalizedId = StudentIdGenerator.normalizeStudentId(studentId) ?: studentId.trim().uppercase()
        val prefs = getPrefs(context)
        prefs.edit()
            .putString(KEY_STUDENT_NAME, trimmedName)
            .putString(KEY_STUDENT_ID, normalizedId)
            .putBoolean(KEY_IDENTITY_SETUP_DONE, true)
            .putBoolean(KEY_STUDENT_ID_LOCKED, true)
            .apply()

        val existing = getStudentProfile(context)
        val updatedProfile = existing.copy(
            studentName = if (trimmedName.isNotBlank()) trimmedName else "Class 12 Student",
            studentId = normalizedId,
            updatedAt = System.currentTimeMillis()
        )
        _currentProfile.value = updatedProfile
        return updatedProfile
    }

    /**
     * Update full student profile locally.
     */
    fun updateStudentProfile(context: Context, profile: StudentProfile) {
        val prefs = getPrefs(context)
        prefs.edit()
            .putString(KEY_USER_ID, profile.userId)
            .putString(KEY_STUDENT_ID, profile.studentId)
            .putString(KEY_STUDENT_NAME, profile.studentName)
            .putString(KEY_PHOTO_URL, profile.photoUrl)
            .putString(KEY_MEDIUM, profile.medium)
            .putString(KEY_CLASS_LEVEL, profile.classLevel)
            .putString(KEY_TARGET_MARKS, profile.targetMarks)
            .putLong(KEY_AVATAR_COLOR, try { java.lang.Long.parseLong(profile.avatarColorHex.removePrefix("#"), 16) } catch (_: Exception) { 0x00E5FFL })
            .putInt(KEY_COMPLETED_TODO_COUNT, profile.completedTodoCount)
            .putInt(KEY_STUDY_STREAK_DAYS, profile.studyStreakDays)
            .putInt(KEY_COMPETITION_POINTS, profile.competitionPoints)
            .putString(KEY_COMPLETED_CHAPTER_IDS, profile.completedChapterIds.joinToString(","))
            .putInt(KEY_TOTAL_FOCUSED_MINS, profile.totalFocusedMinutes)
            .putInt(KEY_COMPLETED_SESSIONS_COUNT, profile.completedSessionsCount)
            .apply()

        _currentProfile.value = profile
    }

    private const val KEY_SUBJECT_GOAL_PREFIX = "key_subject_goal_"
    private const val KEY_WEEKLY_STUDY_GOAL = "key_weekly_study_goal"

    fun getSubjectGoal(context: Context, subjectId: String, defaultGoal: Int = 90): Int {
        return getPrefs(context).getInt("$KEY_SUBJECT_GOAL_PREFIX$subjectId", defaultGoal)
    }

    fun setSubjectGoal(context: Context, subjectId: String, goal: Int) {
        getPrefs(context).edit().putInt("$KEY_SUBJECT_GOAL_PREFIX$subjectId", goal.coerceIn(10, 100)).apply()
    }

    fun getWeeklyStudyGoalHours(context: Context): Int {
        return getPrefs(context).getInt(KEY_WEEKLY_STUDY_GOAL, 35) // Default 35 hours/week
    }

    fun setWeeklyStudyGoalHours(context: Context, hours: Int) {
        getPrefs(context).edit().putInt(KEY_WEEKLY_STUDY_GOAL, hours.coerceIn(1, 168)).apply()
    }

    fun getTargetMarks(context: Context): String {
        return getPrefs(context).getString(KEY_TARGET_MARKS, "90+ Marks") ?: "90+ Marks"
    }

    fun setTargetMarks(context: Context, marks: String) {
        getPrefs(context).edit().putString(KEY_TARGET_MARKS, marks).apply()
        val profile = getStudentProfile(context)
        _currentProfile.value = profile.copy(targetMarks = marks)
    }

    fun getTargetYear(context: Context): String {
        return getPrefs(context).getString(KEY_TARGET_YEAR, "2027") ?: "2027"
    }

    fun setTargetYear(context: Context, year: String) {
        getPrefs(context).edit().putString(KEY_TARGET_YEAR, year).apply()
    }
}
