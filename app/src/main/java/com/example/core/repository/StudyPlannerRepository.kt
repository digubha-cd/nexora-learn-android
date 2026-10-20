package com.example.core.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.NexoraApplication
import com.example.core.model.DailyStudyPlan
import com.example.core.model.DayStudyActivity
import com.example.core.model.ExamType
import com.example.core.model.HeatmapSummaryStats
import com.example.core.model.StudyProgressSummary
import com.example.core.model.StudyStreak
import com.example.core.model.StudyTask
import com.example.core.model.Subject
import com.example.core.model.SubjectProgressItem
import com.example.core.model.TaskPriority
import com.example.core.model.WeeklySummaryData
import com.example.core.reminder.StudyReminderManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID

/**
 * Study Planner Repository:
 * Manages Student-Created To-Do tasks and automatic Exam Countdowns.
 *
 * Requirements:
 * 1. NO automatic task generation. The student creates their own tasks.
 * 2. Allow students to:
 *    - Add a To-Do
 *    - Edit a To-Do
 *    - Delete a To-Do
 *    - Mark Complete with a checkbox
 *    - See Pending and Completed tasks
 *    - Optionally assign a date to a task
 * 3. Keep existing date-based Calendar, showing ONLY student-created tasks, completed tasks,
 *    selected date, and exam dates (no auto-population).
 * 4. Keep exam countdowns completely automatic:
 *    - School Exam — 22 October 2026
 *    - Board Exam — 25 February 2027
 *    - Calculate remaining days from actual current date.
 *    - Automatically decreases by exactly 1 day each calendar day.
 *    - On the exam date show: EXAM DAY
 *    - Never show negative countdown numbers.
 */
object StudyPlannerRepository {

    private const val PREFS_NAME = "nexora_study_planner_prefs"
    private const val KEY_STUDY_TASKS = "key_study_tasks_json"

    private var appContext: Context? = null

    val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
    val ISO_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    // Configurable/testable reference date; defaults dynamically to Indian Standard Time (IST - Asia/Kolkata)
    var currentDateOverride: LocalDate? = null
    val TODAY: LocalDate get() = currentDateOverride ?: com.example.core.util.IstTimeUtil.getCurrentLocalDate()
    val TODAY_STR: String get() = TODAY.format(ISO_FORMATTER)
    val TODAY_FORMATTED: String get() = TODAY.format(DATE_FORMATTER)

    // Milestone Exam Dates
    val SCHOOL_EXAM_DATE: LocalDate = LocalDate.of(2026, 10, 22)
    val SCHOOL_EXAM_DATE_STR: String = SCHOOL_EXAM_DATE.format(ISO_FORMATTER)
    val SCHOOL_EXAM_FORMATTED: String = SCHOOL_EXAM_DATE.format(DATE_FORMATTER)

    val BOARD_EXAM_DATE: LocalDate = LocalDate.of(2027, 2, 25)
    val BOARD_EXAM_DATE_STR: String = BOARD_EXAM_DATE.format(ISO_FORMATTER)
    val BOARD_EXAM_FORMATTED: String = BOARD_EXAM_DATE.format(DATE_FORMATTER)

    // Standard academic ordering for the 7 Class 12 Commerce subjects
    val ORDERED_SUBJECT_IDS: List<String> = listOf(
        "accounts",
        "stat",
        "economics",
        "ba",
        "sp_cc",
        "english",
        "gujarati"
    )

    // Student-created tasks list. Starts empty - NO automatic generation!
    private val _tasks = MutableStateFlow<List<StudyTask>>(emptyList())
    val tasks: StateFlow<List<StudyTask>> = _tasks.asStateFlow()

    init {
        // Listen to syllabus chapter completion events to synchronize if a student-created task references a chapter
        SyllabusRepository.onChapterCompletionChanged = { chapterId, isCompleted ->
            syncFromChapter(chapterId, isCompleted)
        }
    }

    fun initialize(context: Context) {
        appContext = context.applicationContext
        loadPersistedTasks()
    }

    private fun getPrefs(): SharedPreferences? {
        if (appContext == null) {
            try {
                appContext = NexoraApplication.appContext
            } catch (_: Throwable) {}
        }
        return appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun loadPersistedTasks() {
        val prefs = getPrefs() ?: return
        val jsonStr = prefs.getString(KEY_STUDY_TASKS, null) ?: return
        if (jsonStr.isNotBlank()) {
            val loaded = deserializeTasks(jsonStr)
            _tasks.value = loaded
        }
    }

    private fun persistTasks(): Boolean {
        val prefs = getPrefs() ?: return false
        val serialized = serializeTasks(_tasks.value)
        val success = prefs.edit().putString(KEY_STUDY_TASKS, serialized).commit()
        if (success && appContext != null) {
            val ctx = appContext!!
            if (com.example.core.supabase.SupabaseClientManager.isConfigured(ctx)) {
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    try {
                        val repo = com.example.core.supabase.SupabaseAuthRepository(ctx)
                        if (repo.isUserSignedIn()) {
                            repo.syncTasksToCloud(_tasks.value)
                        }
                    } catch (_: Exception) {}
                }
            }
        }
        return success
    }

    fun syncWithSupabase(context: Context) {
        if (!com.example.core.supabase.SupabaseClientManager.isConfigured(context)) return
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val repo = com.example.core.supabase.SupabaseAuthRepository(context)
                if (repo.isUserSignedIn()) {
                    repo.syncTasksToCloud(_tasks.value)
                    val remoteResult = repo.pullTasksFromCloud()
                    if (remoteResult.isSuccess) {
                        val remoteTasks = remoteResult.getOrNull() ?: emptyList()
                        if (remoteTasks.isNotEmpty()) {
                            val currentMap = _tasks.value.associateBy { it.taskId }.toMutableMap()
                            for (task in remoteTasks) {
                                if (!currentMap.containsKey(task.taskId)) {
                                    currentMap[task.taskId] = task
                                }
                            }
                            _tasks.value = currentMap.values.toList()
                            getPrefs()?.edit()?.putString(KEY_STUDY_TASKS, serializeTasks(_tasks.value))?.apply()
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("StudyPlannerRepo", "Supabase sync: ${e.message}")
            }
        }
    }

    private fun serializeTasks(taskList: List<StudyTask>): String {
        val array = JSONArray()
        for (t in taskList) {
            val obj = JSONObject()
            obj.put("taskId", t.taskId)
            obj.put("title", t.title)
            obj.put("description", t.description)
            obj.put("subjectId", t.subjectId)
            obj.put("subjectName", t.subjectName)
            obj.put("chapterId", t.chapterId)
            obj.put("chapterTitle", t.chapterTitle)
            val chArray = JSONArray()
            for (ch in t.chapterIds) {
                chArray.put(ch)
            }
            obj.put("chapterIds", chArray)
            obj.put("examPhase", t.examPhase?.name)
            obj.put("scheduledDateStr", t.scheduledDateStr)
            obj.put("scheduledDateFormatted", t.scheduledDateFormatted)
            obj.put("isCompleted", t.isCompleted)
            if (t.completedAt != null) obj.put("completedAt", t.completedAt)
            obj.put("priority", t.priority.name)
            obj.put("durationMinutes", t.durationMinutes)
            obj.put("notes", t.notes)
            if (t.reminderTime != null) obj.put("reminderTime", t.reminderTime)
            if (t.reminderDateStr != null) obj.put("reminderDateStr", t.reminderDateStr)
            if (t.reminderEpochMillis != null) obj.put("reminderEpochMillis", t.reminderEpochMillis)
            obj.put("isMissed", t.isMissed)
            obj.put("rescheduled", t.rescheduled)
            obj.put("createdAt", t.createdAt)
            obj.put("updatedAt", t.updatedAt)
            array.put(obj)
        }
        return array.toString()
    }

    private fun deserializeTasks(jsonStr: String): List<StudyTask> {
        if (jsonStr.isBlank()) return emptyList()
        val list = mutableListOf<StudyTask>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val taskId = obj.optString("taskId", UUID.randomUUID().toString())
                val title = obj.optString("title", "")
                val description = obj.optString("description", "")
                val subjectId = obj.optString("subjectId", "")
                val subjectName = obj.optString("subjectName", "General")
                val chapterId = obj.optString("chapterId", "")
                val chapterTitle = obj.optString("chapterTitle", title)
                val chapterIds = mutableListOf<String>()
                val chArray = obj.optJSONArray("chapterIds")
                if (chArray != null) {
                    for (j in 0 until chArray.length()) {
                        chapterIds.add(chArray.getString(j))
                    }
                } else if (chapterId.isNotBlank()) {
                    chapterIds.add(chapterId)
                }
                val phaseStr = obj.optString("examPhase", ExamType.SCHOOL_EXAM.name)
                val examPhase = try { ExamType.valueOf(phaseStr) } catch (_: Exception) { ExamType.SCHOOL_EXAM }
                val scheduledDateStr = obj.optString("scheduledDateStr", "")
                val scheduledDateFormatted = obj.optString("scheduledDateFormatted", if (scheduledDateStr.isNotBlank()) scheduledDateStr else "No date assigned")
                val isCompleted = obj.optBoolean("isCompleted", false)
                val completedAt = if (obj.has("completedAt") && !obj.isNull("completedAt")) obj.getLong("completedAt") else null
                val priorityStr = obj.optString("priority", TaskPriority.MEDIUM.name)
                val priority = try { TaskPriority.valueOf(priorityStr) } catch (_: Exception) { TaskPriority.MEDIUM }
                val durationMinutes = obj.optInt("durationMinutes", 30)
                val notes = obj.optString("notes", "")
                val reminderTime = if (obj.has("reminderTime") && !obj.isNull("reminderTime")) obj.optString("reminderTime") else null
                val reminderDateStr = if (obj.has("reminderDateStr") && !obj.isNull("reminderDateStr")) obj.optString("reminderDateStr") else null
                val reminderEpochMillis = if (obj.has("reminderEpochMillis") && !obj.isNull("reminderEpochMillis")) obj.getLong("reminderEpochMillis") else null
                val isMissed = obj.optBoolean("isMissed", false)
                val rescheduled = obj.optBoolean("rescheduled", false)
                val createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                val updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())

                list.add(
                    StudyTask(
                        taskId = taskId,
                        title = title,
                        description = description,
                        subjectId = subjectId,
                        subjectName = subjectName,
                        chapterId = chapterId,
                        chapterTitle = chapterTitle,
                        chapterIds = chapterIds,
                        examPhase = examPhase,
                        scheduledDateStr = scheduledDateStr,
                        scheduledDateFormatted = scheduledDateFormatted,
                        isCompleted = isCompleted,
                        completedAt = completedAt,
                        priority = priority,
                        durationMinutes = durationMinutes,
                        notes = notes,
                        reminderTime = reminderTime,
                        reminderDateStr = reminderDateStr,
                        reminderEpochMillis = reminderEpochMillis,
                        isMissed = isMissed,
                        rescheduled = rescheduled,
                        createdAt = createdAt,
                        updatedAt = updatedAt
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("StudyPlannerRepo", "Error deserializing tasks: ${e.message}")
        }
        return list
    }

    /**
     * Study duration distribution recommendation for the 7 subjects.
     */
    fun getSubjectDailyDuration(subjectId: String): Int {
        return when (subjectId) {
            "accounts" -> 50
            "stat" -> 45
            "economics" -> 45
            "ba" -> 45
            "sp_cc" -> 40
            "english" -> 40
            "gujarati" -> 35
            else -> 30
        }
    }

    // ==========================================
    // EXAM COUNTDOWN ENGINE
    // ==========================================

    /**
     * Dynamically calculates days remaining until School Exam (22 October 2026).
     * Never returns negative numbers (clamped to 0).
     */
    fun getSchoolExamDaysRemaining(from: LocalDate = TODAY): Long {
        val days = ChronoUnit.DAYS.between(from, SCHOOL_EXAM_DATE)
        return maxOf(0L, days)
    }

    /**
     * Dynamically calculates days remaining until Board Exam (25 February 2027).
     * Never returns negative numbers (clamped to 0).
     */
    fun getBoardExamDaysRemaining(from: LocalDate = TODAY): Long {
        val days = ChronoUnit.DAYS.between(from, BOARD_EXAM_DATE)
        return maxOf(0L, days)
    }

    /**
     * Formats days remaining label.
     * On exam day: "EXAM DAY"
     * For 1 day: "1 day"
     * For > 1: "$days days"
     * Never shows negative numbers.
     */
    fun formatDays(days: Long): String {
        return when {
            days <= 0L -> "EXAM DAY"
            days == 1L -> "1 day"
            else -> "$days days"
        }
    }

    /**
     * Format exam countdown with optional exam name prefix:
     * e.g., "School Exam: 32 days" or "School Exam — EXAM DAY"
     */
    fun formatExamCountdown(days: Long, examName: String? = null): String {
        val countText = formatDays(days)
        return if (examName != null) {
            if (countText == "EXAM DAY") "$examName — EXAM DAY" else "$examName: $countText"
        } else {
            countText
        }
    }

    // ==========================================
    // STUDENT-CREATED TO-DO CRUD OPERATIONS
    // ==========================================

    /**
     * Student adds a new To-Do task.
     * Date is optional.
     */
    fun addTask(
        title: String,
        description: String = "",
        subjectId: String = "",
        scheduledDate: LocalDate? = null,
        priority: TaskPriority = TaskPriority.MEDIUM,
        durationMinutes: Int = 30,
        notes: String = "",
        reminderTime: String? = null,
        chapterId: String = "",
        chapterIds: List<String> = emptyList(),
        examPhase: ExamType? = null,
        reminderDateStr: String? = null,
        reminderEpochMillis: Long? = null,
        context: Context? = null
    ): StudyTask {
        val dateStr = scheduledDate?.format(ISO_FORMATTER) ?: ""
        val dateFormatted = scheduledDate?.format(DATE_FORMATTER) ?: "No date assigned"
        val subject = Subject.OFFICIAL_SUBJECTS.firstOrNull { it.id == subjectId }
        val subjectName = subject?.name ?: if (subjectId.isNotBlank()) subjectId else "General"
        val resolvedPhase = examPhase ?: if (scheduledDate != null) getActiveExamPhase(scheduledDate) else ExamType.SCHOOL_EXAM

        val finalChapterIds = if (chapterIds.isNotEmpty()) {
            chapterIds
        } else if (chapterId.isNotBlank()) {
            listOf(chapterId)
        } else {
            emptyList()
        }
        val finalChapterId = if (chapterId.isNotBlank()) chapterId else finalChapterIds.firstOrNull() ?: ""

        val newTask = StudyTask(
            taskId = UUID.randomUUID().toString(),
            title = title.trim(),
            description = description.trim(),
            subjectId = subjectId,
            subjectName = subjectName,
            chapterId = finalChapterId,
            chapterTitle = title.trim(),
            chapterIds = finalChapterIds,
            examPhase = resolvedPhase,
            scheduledDateStr = dateStr,
            scheduledDateFormatted = dateFormatted,
            isCompleted = false,
            priority = priority,
            durationMinutes = durationMinutes,
            notes = notes.trim(),
            reminderTime = reminderTime,
            reminderDateStr = reminderDateStr,
            reminderEpochMillis = reminderEpochMillis,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        _tasks.value = _tasks.value + newTask
        persistTasks()

        // If a reminder was set and context is provided, schedule the Android notification alarm
        if (context != null && reminderEpochMillis != null) {
            StudyReminderManager.scheduleReminder(context, newTask, reminderEpochMillis)
        }

        return newTask
    }

    /**
     * Coroutine-based suspending task creation with explicit logging and Firestore write verification.
     */
    suspend fun addTaskAsync(
        title: String,
        description: String = "",
        subjectId: String = "",
        scheduledDate: LocalDate? = null,
        priority: TaskPriority = TaskPriority.MEDIUM,
        durationMinutes: Int = 30,
        notes: String = "",
        reminderTime: String? = null,
        chapterId: String = "",
        chapterIds: List<String> = emptyList(),
        examPhase: ExamType? = null,
        reminderDateStr: String? = null,
        reminderEpochMillis: Long? = null,
        context: Context? = null
    ): Result<StudyTask> = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("StudyPlannerRepo", "addTaskAsync: Starting persistence for title='${title.trim()}', subjectId='$subjectId'")
            val task = addTask(
                title = title,
                description = description,
                subjectId = subjectId,
                scheduledDate = scheduledDate,
                priority = priority,
                durationMinutes = durationMinutes,
                notes = notes,
                reminderTime = reminderTime,
                chapterId = chapterId,
                chapterIds = chapterIds,
                examPhase = examPhase,
                reminderDateStr = reminderDateStr,
                reminderEpochMillis = reminderEpochMillis,
                context = context
            )

            android.util.Log.d("StudyPlannerRepo", "addTaskAsync: Successfully completed task creation: ${task.taskId}")
            Result.success(task)
        } catch (e: Exception) {
            android.util.Log.e("StudyPlannerRepo", "addTaskAsync: Failed to save task: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Student edits an existing To-Do task.
     * Also updates or cancels any scheduled reminder.
     */
    fun updateTask(
        taskId: String,
        title: String,
        description: String = "",
        subjectId: String = "",
        scheduledDate: LocalDate? = null,
        priority: TaskPriority = TaskPriority.MEDIUM,
        durationMinutes: Int = 30,
        notes: String = "",
        reminderTime: String? = null,
        reminderDateStr: String? = null,
        reminderEpochMillis: Long? = null,
        chapterIds: List<String>? = null,
        context: Context? = null
    ): Boolean {
        val dateStr = scheduledDate?.format(ISO_FORMATTER) ?: ""
        val dateFormatted = scheduledDate?.format(DATE_FORMATTER) ?: "No date assigned"
        val subject = Subject.OFFICIAL_SUBJECTS.firstOrNull { it.id == subjectId }
        val subjectName = subject?.name ?: if (subjectId.isNotBlank()) subjectId else "General"

        var updatedTask: StudyTask? = null

        _tasks.value = _tasks.value.map { task ->
            if (task.taskId == taskId) {
                val newChapterIds = chapterIds ?: task.chapterIds
                val updated = task.copy(
                    title = title.trim(),
                    description = description.trim(),
                    subjectId = subjectId,
                    subjectName = subjectName,
                    chapterIds = newChapterIds,
                    chapterId = if (newChapterIds.isNotEmpty()) newChapterIds.first() else task.chapterId,
                    scheduledDateStr = dateStr,
                    scheduledDateFormatted = dateFormatted,
                    priority = priority,
                    durationMinutes = durationMinutes,
                    notes = notes.trim(),
                    reminderTime = reminderTime,
                    reminderDateStr = reminderDateStr,
                    reminderEpochMillis = reminderEpochMillis,
                    updatedAt = System.currentTimeMillis()
                )
                updatedTask = updated
                updated
            } else {
                task
            }
        }
        if (!persistTasks()) return false

        if (context != null) {
            if (reminderEpochMillis != null && updatedTask != null) {
                StudyReminderManager.scheduleReminder(context, updatedTask!!, reminderEpochMillis)
            } else if (reminderEpochMillis == null && reminderTime.isNullOrBlank()) {
                StudyReminderManager.cancelReminder(context, taskId)
            }
        }
        return true
    }

    /**
     * Deletes multiple To-Do tasks at once.
     */
    fun deleteTasks(taskIds: List<String>, context: Context? = null) {
        context?.let { ctx ->
            taskIds.forEach { taskId -> StudyReminderManager.cancelReminder(ctx, taskId) }
        }
        _tasks.value = _tasks.value.filterNot { taskIds.contains(it.taskId) }
        persistTasks()
    }

    /**
     * Mark multiple To-Do tasks as completed or pending at once.
     */
    fun batchSetCompleted(taskIds: List<String>, isCompleted: Boolean = true) {
        val currentList = _tasks.value.toMutableList()
        val now = System.currentTimeMillis()
        var modified = false
        val chaptersToSync = mutableListOf<String>()
        currentList.forEachIndexed { index, task ->
            if (taskIds.contains(task.taskId) && task.isCompleted != isCompleted) {
                currentList[index] = task.copy(
                    isCompleted = isCompleted,
                    completedAt = if (isCompleted) now else null,
                    isMissed = if (isCompleted) false else task.isMissed,
                    updatedAt = now
                )
                modified = true
                if (task.chapterIds.isNotEmpty()) {
                    chaptersToSync.addAll(task.chapterIds)
                } else if (!task.chapterId.isNullOrBlank()) {
                    chaptersToSync.add(task.chapterId)
                }
            }
        }
        if (modified) {
            _tasks.value = currentList
            persistTasks()
            chaptersToSync.forEach { chId ->
                SyllabusRepository.setChapterCompleted(chId, isCompleted)
            }
        }
    }

    /**
     * Reschedule multiple tasks to a target date (defaults to today).
     */
    fun batchReschedule(taskIds: List<String>, targetDate: LocalDate = TODAY) {
        val currentList = _tasks.value.toMutableList()
        val dateStr = targetDate.format(ISO_FORMATTER)
        val dateFormatted = targetDate.format(DATE_FORMATTER)
        val now = System.currentTimeMillis()
        var modified = false
        currentList.forEachIndexed { index, task ->
            if (taskIds.contains(task.taskId)) {
                currentList[index] = task.copy(
                    scheduledDateStr = dateStr,
                    scheduledDateFormatted = dateFormatted,
                    isMissed = false,
                    rescheduled = true,
                    updatedAt = now
                )
                modified = true
            }
        }
        if (modified) {
            _tasks.value = currentList
            persistTasks()
        }
    }

    /**
     * Student deletes a To-Do task.
     * Also cancels any active alarm reminder.
     */
    fun deleteTask(taskId: String, context: Context? = null) {
        context?.let { StudyReminderManager.cancelReminder(it, taskId) }
        _tasks.value = _tasks.value.filterNot { it.taskId == taskId }
        persistTasks()
    }

    /**
     * Clear reminder from a task without deleting the task.
     */
    fun deleteReminder(taskId: String, context: Context? = null) {
        context?.let { StudyReminderManager.cancelReminder(it, taskId) }
        _tasks.value = _tasks.value.map { task ->
            if (task.taskId == taskId) {
                task.copy(
                    reminderTime = null,
                    reminderDateStr = null,
                    reminderEpochMillis = null,
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                task
            }
        }
        persistTasks()
    }

    /**
     * Toggle completion of a study task with a checkbox.
     * When completed, marks all linked chapters as completed in SyllabusRepository,
     * updating subject progress, profile progress, chapter points, and removing them from remaining reminders.
     */
    fun toggleTaskCompletion(taskId: String) {
        val currentList = _tasks.value.toMutableList()
        val index = currentList.indexOfFirst { it.taskId == taskId }
        if (index != -1) {
            val task = currentList[index]
            val newCompleted = !task.isCompleted
            val updated = task.copy(
                isCompleted = newCompleted,
                completedAt = if (newCompleted) System.currentTimeMillis() else null,
                isMissed = if (newCompleted) false else task.isMissed,
                updatedAt = System.currentTimeMillis()
            )
            currentList[index] = updated
            _tasks.value = currentList
            persistTasks()

            // If this task is associated with chapter(s), sync to syllabus
            val chaptersToSync = if (task.chapterIds.isNotEmpty()) {
                task.chapterIds
            } else if (!task.chapterId.isNullOrBlank()) {
                listOf(task.chapterId)
            } else {
                emptyList()
            }

            chaptersToSync.forEach { chId ->
                SyllabusRepository.setChapterCompleted(chId, newCompleted)
            }

            if (newCompleted && appContext != null) {
                val context = appContext!!
                val profile = com.example.core.storage.StudentPreferences.getStudentProfile(context)
                if (profile.userId.isNotBlank()) {
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        val repo = LocalStudentProfileRepository(context)
                        repo.awardPointsIdempotent(profile.userId, "todo_${task.taskId}", 10)
                        repo.awardPointsIdempotent(profile.userId, "streak_${TODAY_STR}", 5)
                    }
                }
            }
        }
    }

    /**
     * Reschedule a task to a target date.
     */
    fun rescheduleTask(taskId: String, targetDate: LocalDate = TODAY) {
        val currentList = _tasks.value.toMutableList()
        val index = currentList.indexOfFirst { it.taskId == taskId }
        if (index != -1) {
            val task = currentList[index]
            val updated = task.copy(
                scheduledDateStr = targetDate.format(ISO_FORMATTER),
                scheduledDateFormatted = targetDate.format(DATE_FORMATTER),
                isMissed = false,
                rescheduled = true,
                updatedAt = System.currentTimeMillis()
            )
            currentList[index] = updated
            _tasks.value = currentList
            persistTasks()
        }
    }

    /**
     * Reschedule all currently missed tasks to today.
     */
    fun rescheduleAllMissedToToday() {
        val currentList = _tasks.value.toMutableList()
        var modified = false
        currentList.forEachIndexed { index, task ->
            if (task.isMissed && !task.isCompleted) {
                currentList[index] = task.copy(
                    scheduledDateStr = TODAY_STR,
                    scheduledDateFormatted = TODAY_FORMATTED,
                    isMissed = false,
                    rescheduled = true,
                    updatedAt = System.currentTimeMillis()
                )
                modified = true
            }
        }
        if (modified) {
            _tasks.value = currentList
            persistTasks()
        }
    }

    /**
     * Synchronize completion when user checks a chapter checkbox directly in SubjectDetailScreen.
     */
    private fun syncFromChapter(chapterId: String, isCompleted: Boolean) {
        val currentList = _tasks.value.toMutableList()
        var modified = false
        currentList.forEachIndexed { index, task ->
            val matches = task.chapterId == chapterId || task.chapterIds.contains(chapterId)
            if (matches && task.isCompleted != isCompleted) {
                currentList[index] = task.copy(
                    isCompleted = isCompleted,
                    completedAt = if (isCompleted) System.currentTimeMillis() else null,
                    isMissed = if (isCompleted) false else task.isMissed,
                    updatedAt = System.currentTimeMillis()
                )
                modified = true
            }
        }
        if (modified) {
            _tasks.value = currentList
            persistTasks()
        }
    }

    /**
     * Mark or unmark a task as missed.
     */
    fun setTaskMissed(taskId: String, isMissed: Boolean) {
        val currentList = _tasks.value.toMutableList()
        val index = currentList.indexOfFirst { it.taskId == taskId }
        if (index != -1) {
            val task = currentList[index]
            val updated = task.copy(
                isMissed = isMissed,
                isCompleted = if (isMissed) false else task.isCompleted,
                updatedAt = System.currentTimeMillis()
            )
            currentList[index] = updated
            _tasks.value = currentList
            persistTasks()
            if (isMissed && !task.chapterId.isNullOrBlank()) {
                SyllabusRepository.setChapterCompleted(task.chapterId, false)
            }
        }
    }

    // ==========================================
    // QUERY FILTERS FOR STUDENT TO-DO
    // ==========================================

    fun getPendingTasks(): List<StudyTask> {
        return _tasks.value.filter { !it.isCompleted }
    }

    fun getCompletedTasks(): List<StudyTask> {
        return _tasks.value.filter { it.isCompleted }
    }

    fun getTodayTasks(): List<StudyTask> {
        val todayStr = TODAY_STR
        return _tasks.value.filter { it.scheduledDateStr == todayStr }
    }

    fun getUpcomingTasks(): List<StudyTask> {
        val today = TODAY
        return _tasks.value.filter { task ->
            if (task.scheduledDateStr.isBlank() || task.isCompleted) false
            else {
                try {
                    val date = LocalDate.parse(task.scheduledDateStr, ISO_FORMATTER)
                    date.isAfter(today)
                } catch (e: Exception) {
                    false
                }
            }
        }.sortedBy { it.scheduledDateStr }
    }

    fun getMissedTasks(): List<StudyTask> {
        val today = TODAY
        return _tasks.value.filter { task ->
            if (task.scheduledDateStr.isBlank() || task.isCompleted) false
            else {
                try {
                    val date = LocalDate.parse(task.scheduledDateStr, ISO_FORMATTER)
                    date.isBefore(today)
                } catch (e: Exception) {
                    false
                }
            }
        }
    }

    fun getTasksWithoutDate(): List<StudyTask> {
        return _tasks.value.filter { it.scheduledDateStr.isBlank() }
    }

    fun getTasksForDate(dateStr: String): List<StudyTask> {
        return _tasks.value.filter { it.scheduledDateStr == dateStr }
    }

    fun hasTasksOnDate(dateStr: String): Boolean {
        return _tasks.value.any { it.scheduledDateStr == dateStr }
    }

    fun hasCompletedOnDate(dateStr: String): Boolean {
        return _tasks.value.any { it.scheduledDateStr == dateStr && it.isCompleted }
    }

    fun hasMissedOnDate(dateStr: String): Boolean {
        return _tasks.value.any { it.scheduledDateStr == dateStr && it.isMissed && !it.isCompleted }
    }

    fun hasUpcomingOnDate(dateStr: String): Boolean {
        return _tasks.value.any { it.scheduledDateStr == dateStr && !it.isCompleted && !it.isMissed }
    }

    fun getMilestoneOnDate(dateStr: String): String? {
        return when (dateStr) {
            SCHOOL_EXAM_DATE_STR -> "School Exam — 22 October 2026"
            BOARD_EXAM_DATE_STR -> "Board Exam — 25 February 2027"
            else -> null
        }
    }

    /**
     * Returns student tasks for the given calendar date with exam countdowns.
     * Does NOT generate automatic study tasks.
     */
    fun getDailyStudyPlan(date: LocalDate): DailyStudyPlan {
        val dateStr = date.format(ISO_FORMATTER)
        val dateFormatted = date.format(DATE_FORMATTER)
        val dayTasks = getTasksForDate(dateStr)
        val schoolRemaining = getSchoolExamDaysRemaining(date)
        val boardRemaining = getBoardExamDaysRemaining(date)
        val totalMins = dayTasks.sumOf { it.durationMinutes }

        return DailyStudyPlan(
            dateStr = dateStr,
            dateFormatted = dateFormatted,
            totalHours = totalMins / 60,
            totalMinutes = totalMins,
            tasks = dayTasks,
            schoolExamDaysRemaining = schoolRemaining,
            boardExamDaysRemaining = boardRemaining
        )
    }

    fun getActiveExamPhase(date: LocalDate = TODAY): ExamType {
        return if (date.isBefore(SCHOOL_EXAM_DATE)) ExamType.SCHOOL_EXAM else ExamType.BOARD_EXAM
    }

    fun getSchoolExamTasks(): List<StudyTask> {
        return _tasks.value.filter { it.examPhase == ExamType.SCHOOL_EXAM }
    }

    fun getBoardExamTasks(): List<StudyTask> {
        return _tasks.value.filter { it.examPhase == ExamType.BOARD_EXAM }
    }

    fun getTasksBySubject(subjectId: String): List<StudyTask> {
        return _tasks.value.filter { it.subjectId == subjectId }
    }

    fun getSubjectTotalCount(subjectId: String): Int {
        return getTasksBySubject(subjectId).size
    }

    fun getSubjectCompletedCount(subjectId: String): Int {
        return getTasksBySubject(subjectId).count { it.isCompleted }
    }

    fun getSubjectProgress(subjectId: String): Float {
        val total = getSubjectTotalCount(subjectId)
        if (total == 0) return 0.0f
        return getSubjectCompletedCount(subjectId).toFloat() / total
    }

    fun getDailyProgress(): Float {
        val todayList = getTodayTasks()
        if (todayList.isEmpty()) return 0.0f
        val completed = todayList.count { it.isCompleted }
        return completed.toFloat() / todayList.size
    }

    fun getTodayCompletedCount(): Int = getTodayTasks().count { it.isCompleted }
    fun getTodayTotalCount(): Int = getTodayTasks().size

    fun getMissedCount(): Int = getMissedTasks().size

    fun getSchoolExamProgress(): Float {
        val schoolTasks = _tasks.value.filter { it.examPhase == ExamType.SCHOOL_EXAM }
        if (schoolTasks.isEmpty()) return 0.0f
        val completed = schoolTasks.count { it.isCompleted }
        return completed.toFloat() / schoolTasks.size
    }

    fun getOverallBoardProgress(): Float {
        if (_tasks.value.isEmpty()) return 0.0f
        val completed = _tasks.value.count { it.isCompleted }
        return completed.toFloat() / _tasks.value.size
    }

    // ==========================================
    // PROGRESS & STUDY STREAK (STEP 7)
    // ==========================================

    fun getCompletedCount(): Int = _tasks.value.count { it.isCompleted }

    fun getPendingCount(): Int = _tasks.value.count { !it.isCompleted }

    fun getTotalCount(): Int = _tasks.value.size

    /**
     * Daily completion percentage based solely on student-created To-Do tasks for TODAY.
     */
    fun getDailyCompletionPercentage(): Int {
        val todayTasks = getTodayTasks()
        if (todayTasks.isEmpty()) {
            val completedToday = _tasks.value.count { task ->
                task.isCompleted && task.completedAt != null && run {
                    try {
                        val compDate = java.time.Instant.ofEpochMilli(task.completedAt)
                            .atZone(com.example.core.util.IstTimeUtil.IST_ZONE_ID)
                            .toLocalDate()
                        compDate == TODAY
                    } catch (_: Exception) {
                        false
                    }
                }
            }
            return if (completedToday > 0) 100 else 0
        }
        val completedCount = todayTasks.count { it.isCompleted }
        return (completedCount * 100) / todayTasks.size
    }

    /**
     * Weekly completion percentage for the current calendar week (Monday to Sunday).
     */
    fun getWeeklyCompletionPercentage(): Int {
        val today = TODAY
        val startOfWeek = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
        val endOfWeek = today.with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY))

        val weekTasks = _tasks.value.filter { task ->
            task.scheduledDate?.let { !it.isBefore(startOfWeek) && !it.isAfter(endOfWeek) } ?: false
        }

        if (weekTasks.isEmpty()) {
            val completedThisWeek = _tasks.value.count { task ->
                task.isCompleted && task.completedAt != null && run {
                    try {
                        val compDate = java.time.Instant.ofEpochMilli(task.completedAt)
                            .atZone(com.example.core.util.IstTimeUtil.IST_ZONE_ID)
                            .toLocalDate()
                        !compDate.isBefore(startOfWeek) && !compDate.isAfter(endOfWeek)
                    } catch (_: Exception) {
                        false
                    }
                }
            }
            return if (completedThisWeek > 0) 100 else 0
        }

        val completedCount = weekTasks.count { it.isCompleted }
        return (completedCount * 100) / weekTasks.size
    }

    /**
     * Overall study progress percentage based on all student-created To-Do tasks.
     */
    fun getOverallCompletionPercentage(): Int {
        if (_tasks.value.isEmpty()) return 0
        val completed = _tasks.value.count { it.isCompleted }
        return (completed * 100) / _tasks.value.size
    }

    /**
     * Subject-wise completed tasks for all 7 official Class 12 Commerce subjects.
     * Calculated strictly from the student's own To-Do items.
     */
    fun getSubjectWiseProgressList(): List<SubjectProgressItem> {
        return Subject.OFFICIAL_SUBJECTS.map { sub ->
            val subTasks = _tasks.value.filter { it.subjectId == sub.id }
            val total = subTasks.size
            val completed = subTasks.count { it.isCompleted }
            val pending = total - completed
            val pct = if (total > 0) (completed * 100 / total) else 0
            SubjectProgressItem(
                subjectId = sub.id,
                subjectName = sub.name,
                completedCount = completed,
                pendingCount = pending,
                totalCount = total,
                progressPercentage = pct
            )
        }
    }

    /**
     * Study streak calculation based on completing at least one student-created To-Do on a day.
     * Requirements:
     * - Current streak
     * - Best streak
     * - Today's completion status ("Completed today" or "Pending today")
     * - Does NOT punish the student with automatic missed tasks.
     */
    fun getStudyStreak(): StudyStreak {
        val completedTasks = _tasks.value.filter { it.isCompleted }
        if (completedTasks.isEmpty()) {
            return StudyStreak(
                currentStreak = 0,
                bestStreak = 0,
                isCompletedToday = false,
                todayStatusText = "Pending today",
                streakDays = emptyList()
            )
        }

        val completedDates = mutableSetOf<LocalDate>()
        for (task in completedTasks) {
            if (task.completedAt != null) {
                try {
                    val date = java.time.Instant.ofEpochMilli(task.completedAt)
                        .atZone(com.example.core.util.IstTimeUtil.IST_ZONE_ID)
                        .toLocalDate()
                    completedDates.add(date)
                } catch (_: Exception) {}
            }
            task.scheduledDate?.let { completedDates.add(it) }
        }

        // Include completed Focus Timer study sessions
        try {
            for (session in com.example.core.timer.StudyTimerManager.sessionHistory.value) {
                val date = java.time.Instant.ofEpochMilli(session.timestampEpochMillis)
                    .atZone(com.example.core.util.IstTimeUtil.IST_ZONE_ID)
                    .toLocalDate()
                completedDates.add(date)
            }
        } catch (_: Exception) {}

        val today = TODAY
        val isCompletedToday = completedDates.contains(today)
        val todayStatusText = if (isCompletedToday) "Completed today" else "Pending today"

        // Current streak:
        // If completed today: streak counts today, today-1, today-2...
        // If not completed today: if yesterday was completed, streak is active/maintained!
        var currentStreak = 0
        var checkDate = if (isCompletedToday) today else today.minusDays(1)
        while (completedDates.contains(checkDate)) {
            currentStreak++
            checkDate = checkDate.minusDays(1)
        }

        // Best streak:
        val sortedDates = completedDates.sorted()
        var bestStreak = 0
        var runningStreak = 0
        var prevDate: LocalDate? = null

        for (d in sortedDates) {
            if (prevDate == null || d == prevDate.plusDays(1)) {
                runningStreak++
            } else if (d != prevDate) {
                runningStreak = 1
            }
            prevDate = d
            if (runningStreak > bestStreak) {
                bestStreak = runningStreak
            }
        }
        bestStreak = maxOf(bestStreak, currentStreak)

        return StudyStreak(
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            isCompletedToday = isCompletedToday,
            todayStatusText = todayStatusText,
            streakDays = sortedDates
        )
    }

    /**
     * Retrieves daily study activity for the past 7 days (from TODAY - 6 days to TODAY inclusive).
     * Calculates real completed tasks, scheduled tasks, and total study duration for each day.
     */
    fun getPast7DaysActivity(): List<com.example.core.model.DayStudyActivity> {
        val today = TODAY
        val dayFormatter = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)
        val shortDateFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

        return (6 downTo 0).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            val dateStr = date.format(ISO_FORMATTER)
            val dayLabel = date.format(dayFormatter)
            val dayNumberFormatted = date.format(shortDateFormatter)
            val isToday = (date == today)

            // Tasks scheduled for this date, or if unscheduled, completed on this date
            val tasksOnDay = _tasks.value.filter { task ->
                if (task.scheduledDateStr.isNotBlank()) {
                    task.scheduledDateStr == dateStr
                } else {
                    task.completedAt?.let { epoch ->
                        try {
                            java.time.Instant.ofEpochMilli(epoch)
                                .atZone(com.example.core.util.IstTimeUtil.IST_ZONE_ID)
                                .toLocalDate() == date
                        } catch (_: Exception) {
                            false
                        }
                    } ?: false
                }
            }

            val sessionsOnDay = try {
                com.example.core.timer.StudyTimerManager.sessionHistory.value.filter { session ->
                    java.time.Instant.ofEpochMilli(session.timestampEpochMillis)
                        .atZone(com.example.core.util.IstTimeUtil.IST_ZONE_ID)
                        .toLocalDate() == date
                }
            } catch (_: Exception) {
                emptyList()
            }

            val completedTasks = tasksOnDay.filter { it.isCompleted }
            val completedCount = completedTasks.size + sessionsOnDay.size
            val totalCount = tasksOnDay.size + sessionsOnDay.size
            val durationMinutes = completedTasks.sumOf { it.durationMinutes } + sessionsOnDay.sumOf { it.durationMinutes }

            com.example.core.model.DayStudyActivity(
                date = date,
                dateStr = dateStr,
                dayLabel = dayLabel,
                dayNumberFormatted = dayNumberFormatted,
                completedCount = completedCount,
                totalCount = totalCount,
                durationMinutes = durationMinutes,
                isStreakAchieved = completedCount > 0,
                isToday = isToday
            )
        }
    }

    /**
     * Retrieves daily study activity for a specified number of past days for heatmap visualization.
     */
    fun getStudyActivityForDays(daysCount: Int = 84): List<DayStudyActivity> {
        val today = TODAY
        val dayFormatter = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)
        val shortDateFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

        return (daysCount downTo 0).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            calculateDayStudyActivity(date, today, dayFormatter, shortDateFormatter)
        }
    }

    /**
     * Retrieves aligned 52-53 weeks of daily study activity (365+ days) for the GitHub-style heatmap.
     * Aligns to Monday-Sunday week bounds so every week column has 7 days.
     */
    fun get365DaysHeatmapData(): List<DayStudyActivity> {
        val today = TODAY
        val dayFormatter = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)
        val shortDateFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

        // Find Monday of the current week
        val currentWeekMonday = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
        // Go back 52 weeks to show a full 53-week span (~371 days, encompassing the last 365 days)
        val startMonday = currentWeekMonday.minusWeeks(52)
        val endSunday = currentWeekMonday.plusDays(6)

        val totalDays = java.time.temporal.ChronoUnit.DAYS.between(startMonday, endSunday).toInt()

        return (0..totalDays).map { dayOffset ->
            val date = startMonday.plusDays(dayOffset.toLong())
            calculateDayStudyActivity(date, today, dayFormatter, shortDateFormatter)
        }
    }

    /**
     * Calculates authentic DayStudyActivity for a specific date using real tasks and focus sessions.
     */
    private fun calculateDayStudyActivity(
        date: LocalDate,
        today: LocalDate,
        dayFormatter: DateTimeFormatter,
        shortDateFormatter: DateTimeFormatter
    ): DayStudyActivity {
        val dateStr = date.format(ISO_FORMATTER)
        val dayLabel = date.format(dayFormatter)
        val dayNumberFormatted = date.format(shortDateFormatter)
        val isToday = (date == today)

        // Match tasks without double counting (scheduled date first, fallback to completedAt if unscheduled)
        val tasksOnDay = _tasks.value.filter { task ->
            if (task.scheduledDateStr.isNotBlank()) {
                task.scheduledDateStr == dateStr
            } else {
                task.completedAt?.let { epoch ->
                    try {
                        java.time.Instant.ofEpochMilli(epoch)
                            .atZone(com.example.core.util.IstTimeUtil.IST_ZONE_ID)
                            .toLocalDate() == date
                    } catch (_: Exception) {
                        false
                    }
                } ?: false
            }
        }

        val sessionsOnDay = try {
            com.example.core.timer.StudyTimerManager.sessionHistory.value.filter { session ->
                java.time.Instant.ofEpochMilli(session.timestampEpochMillis)
                    .atZone(com.example.core.util.IstTimeUtil.IST_ZONE_ID)
                    .toLocalDate() == date
            }
        } catch (_: Exception) {
            emptyList()
        }

        val completedTasks = tasksOnDay.filter { it.isCompleted }
        val sessionMinutes = sessionsOnDay.sumOf { it.durationMinutes }
        val taskMinutes = completedTasks.sumOf { it.durationMinutes }
        val durationMinutes = sessionMinutes + taskMinutes
        val completedCount = completedTasks.size + sessionsOnDay.size
        val totalCount = tasksOnDay.size + sessionsOnDay.size

        return DayStudyActivity(
            date = date,
            dateStr = dateStr,
            dayLabel = dayLabel,
            dayNumberFormatted = dayNumberFormatted,
            completedCount = completedCount,
            totalCount = totalCount,
            durationMinutes = durationMinutes,
            isStreakAchieved = completedCount > 0,
            isToday = isToday,
            completedTaskCount = completedTasks.size,
            sessionCount = sessionsOnDay.size,
            sessionMinutes = sessionMinutes
        )
    }

    /**
     * Retrieves summary statistics for the 365-day consistency heatmap.
     */
    fun getHeatmapSummaryStats(heatmapData: List<DayStudyActivity>): HeatmapSummaryStats {
        val today = TODAY
        val pastAndTodayDays = heatmapData.filter { it.date <= today }
        val activeDaysCount = pastAndTodayDays.count { it.activityLevel > 0 }
        val totalMinutes = pastAndTodayDays.sumOf { it.durationMinutes.toLong() }
        val streak = getStudyStreak()

        return HeatmapSummaryStats(
            activeDaysCount = activeDaysCount,
            totalStudyMinutes = totalMinutes,
            currentStreak = streak.currentStreak,
            bestStreak = streak.bestStreak
        )
    }

    /**
     * Retrieves detailed tasks and focus sessions for a specific date (e.g. for the bottom sheet).
     */
    fun getStudyDetailsForDate(date: LocalDate): Pair<List<StudyTask>, List<com.example.core.timer.FocusSessionRecord>> {
        val dateStr = date.format(ISO_FORMATTER)
        val tasks = _tasks.value.filter { task ->
            if (task.scheduledDateStr.isNotBlank()) {
                task.scheduledDateStr == dateStr
            } else {
                task.completedAt?.let { epoch ->
                    try {
                        java.time.Instant.ofEpochMilli(epoch)
                            .atZone(com.example.core.util.IstTimeUtil.IST_ZONE_ID)
                            .toLocalDate() == date
                    } catch (_: Exception) {
                        false
                    }
                } ?: false
            }
        }

        val sessions = try {
            com.example.core.timer.StudyTimerManager.sessionHistory.value.filter { session ->
                java.time.Instant.ofEpochMilli(session.timestampEpochMillis)
                    .atZone(com.example.core.util.IstTimeUtil.IST_ZONE_ID)
                    .toLocalDate() == date
            }
        } catch (_: Exception) {
            emptyList()
        }

        return Pair(tasks, sessions)
    }

    /**
     * Full summary for the Progress section.
     */
    fun getProgressSummary(): StudyProgressSummary {
        val completed = getCompletedCount()
        val pending = getPendingCount()
        val missed = getMissedCount()
        val total = getTotalCount()
        val dailyPct = getDailyCompletionPercentage()
        val weeklyPct = getWeeklyCompletionPercentage()
        val overallPct = getOverallCompletionPercentage()
        val subjects = getSubjectWiseProgressList()
        val streak = getStudyStreak()

        return StudyProgressSummary(
            completedCount = completed,
            pendingCount = pending,
            missedCount = missed,
            totalCount = total,
            dailyPercentage = dailyPct,
            weeklyPercentage = weeklyPct,
            overallPercentage = overallPct,
            subjectProgressList = subjects,
            streak = streak
        )
    }

    /**
     * Get real remaining (uncompleted) chapters for a subject from SyllabusRepository.
     */
    fun getRemainingChaptersForSubject(subjectId: String): List<com.example.core.model.Chapter> {
        return SyllabusRepository.getPendingChapters(subjectId)
    }

    /**
     * Get real completed chapters for a subject from SyllabusRepository.
     */
    fun getCompletedChaptersForSubject(subjectId: String): List<com.example.core.model.Chapter> {
        return SyllabusRepository.getCompletedChapters(subjectId)
    }

    /**
     * Retrieve a task by its unique ID.
     */
    fun getTaskById(taskId: String): StudyTask? {
        return _tasks.value.find { it.taskId == taskId }
    }

    /**
     * Aggregates study activity for the past 7 days for the weekly notification.
     */
    fun getWeeklySummaryStats(): WeeklySummaryData {
        val today = TODAY
        val sevenDaysAgo = today.minusDays(7)
        
        val tasks = _tasks.value.filter { task ->
            val taskDate = task.completedAt?.let { epoch ->
                try {
                    java.time.Instant.ofEpochMilli(epoch)
                        .atZone(com.example.core.util.IstTimeUtil.IST_ZONE_ID)
                        .toLocalDate()
                } catch (_: Exception) {
                    null
                }
            } ?: task.scheduledDate
            taskDate != null && taskDate.isAfter(sevenDaysAgo) && taskDate.isBefore(today.plusDays(1))
        }
        
        val sessions = com.example.core.timer.StudyTimerManager.sessionHistory.value.filter { session ->
            val sessionDate = java.time.Instant.ofEpochMilli(session.timestampEpochMillis)
                .atZone(com.example.core.util.IstTimeUtil.IST_ZONE_ID)
                .toLocalDate()
            sessionDate.isAfter(sevenDaysAgo) && sessionDate.isBefore(today.plusDays(1))
        }
        
        val totalMins = tasks.filter { it.isCompleted }.sumOf { it.durationMinutes }.toLong() + sessions.sumOf { it.durationMinutes }.toLong()
        
        val subjectBreakdown = mutableMapOf<String, Long>()
        tasks.filter { it.isCompleted }.forEach { 
            subjectBreakdown[it.subjectId] = subjectBreakdown.getOrDefault(it.subjectId, 0L) + it.durationMinutes
        }
        sessions.forEach {
            subjectBreakdown[it.subjectId] = subjectBreakdown.getOrDefault(it.subjectId, 0L) + it.durationMinutes
        }
        
        val streak = getStudyStreak().currentStreak
        
        val nextWeekTasks = _tasks.value.filter { task ->
            val taskDate = task.scheduledDate
            taskDate != null && taskDate.isAfter(today) && taskDate.isBefore(today.plusDays(8))
        }
        
        return WeeklySummaryData(
            totalMinutes = totalMins,
            subjectBreakdown = subjectBreakdown,
            currentStreak = streak,
            nextWeekGoalCount = nextWeekTasks.size
        )
    }

    /**
     * Resets repository state for isolated testing.
     */
    fun resetForTesting() {
        _tasks.value = emptyList()
        currentDateOverride = null
        val prefs = getPrefs()
        prefs?.edit()?.remove(KEY_STUDY_TASKS)?.apply()
        com.example.core.timer.StudyTimerManager.resetForTesting()
    }
}
