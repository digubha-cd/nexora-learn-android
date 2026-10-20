package com.example.core.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.NexoraApplication
import com.example.core.model.CalendarEvent
import com.example.core.model.StudyTask
import com.example.core.model.TaskPriority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Repository for Calendar Events:
 * - Persists custom events to SharedPreferences
 * - Provides Month, Week, Day event lookups
 * - Synchronizes with StudyPlannerRepository (preventing duplicates via taskId linkage)
 * - Safe handling of dates, boundaries, and persistence across app restarts
 */
object CalendarRepository {

    private const val PREFS_NAME = "nexora_calendar_prefs"
    private const val KEY_CALENDAR_EVENTS = "key_calendar_events_json"

    private var appContext: Context? = null

    private val _events = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val events: StateFlow<List<CalendarEvent>> = _events.asStateFlow()

    init {
        try {
            appContext = NexoraApplication.appContext
            loadPersistedEvents()
        } catch (_: Throwable) {}
    }

    fun initialize(context: Context) {
        appContext = context.applicationContext
        loadPersistedEvents()
    }

    private fun getPrefs(): SharedPreferences? {
        if (appContext == null) {
            try {
                appContext = NexoraApplication.appContext
            } catch (_: Throwable) {}
        }
        return appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun loadPersistedEvents() {
        val prefs = getPrefs() ?: return
        val jsonStr = prefs.getString(KEY_CALENDAR_EVENTS, null) ?: return
        if (jsonStr.isNotBlank()) {
            _events.value = deserializeEvents(jsonStr)
        }
    }

    private fun persistEvents(): Boolean {
        val prefs = getPrefs() ?: return false
        val serialized = serializeEvents(_events.value)
        return prefs.edit().putString(KEY_CALENDAR_EVENTS, serialized).commit()
    }

    fun addEvent(
        event: CalendarEvent,
        syncToPlanner: Boolean = false,
        context: Context? = null
    ): CalendarEvent? {
        var finalEvent = event
        val effectiveContext = context ?: appContext

        // If user wants to synchronize to Study Planner and it's not already linked to a task
        if (syncToPlanner && finalEvent.taskId == null) {
            val newTaskId = UUID.randomUUID().toString()
            val newTask = StudyPlannerRepository.addTask(
                title = finalEvent.title,
                description = finalEvent.description,
                subjectId = finalEvent.subjectId ?: "accounts",
                scheduledDate = finalEvent.date,
                priority = TaskPriority.MEDIUM,
                reminderTime = finalEvent.startTime,
                reminderDateStr = finalEvent.date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                reminderEpochMillis = finalEvent.reminderEpochMillis,
                chapterIds = if (finalEvent.chapterId != null) listOf(finalEvent.chapterId) else emptyList(),
                context = effectiveContext
            )
            finalEvent = finalEvent.copy(taskId = newTask.taskId)
        }

        _events.value = _events.value + finalEvent
        if (!persistEvents()) return null // Indicate failure
        return finalEvent
    }

    fun updateEvent(event: CalendarEvent, context: Context? = null) {
        val effectiveContext = context ?: appContext
        _events.value = _events.value.map { if (it.id == event.id) event else it }
        persistEvents()

        // Sync updates to linked StudyPlanner task if present
        if (event.taskId != null) {
            StudyPlannerRepository.getTaskById(event.taskId)?.let { existingTask ->
                StudyPlannerRepository.updateTask(
                    taskId = existingTask.taskId,
                    title = event.title,
                    description = event.description,
                    subjectId = event.subjectId ?: existingTask.subjectId,
                    scheduledDate = event.date,
                    priority = existingTask.priority,
                    reminderTime = event.startTime ?: existingTask.reminderTime,
                    reminderDateStr = event.date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    reminderEpochMillis = event.reminderEpochMillis ?: existingTask.reminderEpochMillis,
                    chapterIds = if (event.chapterId != null) listOf(event.chapterId) else existingTask.chapterIds,
                    context = effectiveContext
                )
            }
        }
    }

    fun deleteEvent(eventId: String, context: Context? = null) {
        val effectiveContext = context ?: appContext
        val target = _events.value.find { it.id == eventId }
        _events.value = _events.value.filter { it.id != eventId }
        persistEvents()

        // If linked to a study task, delete from StudyPlannerRepository as well
        if (target?.taskId != null) {
            StudyPlannerRepository.deleteTask(target.taskId, effectiveContext)
        }
    }

    fun getEventsForDate(date: LocalDate): List<CalendarEvent> {
        return _events.value.filter { it.date == date }
    }

    fun getEventById(eventId: String): CalendarEvent? {
        return _events.value.find { it.id == eventId }
    }

    private fun serializeEvents(events: List<CalendarEvent>): String {
        val array = JSONArray()
        for (e in events) {
            val obj = JSONObject().apply {
                put("id", e.id)
                put("title", e.title)
                put("description", e.description)
                put("date", e.date.format(DateTimeFormatter.ISO_LOCAL_DATE))
                if (e.startTime != null) put("startTime", e.startTime)
                if (e.endTime != null) put("endTime", e.endTime)
                put("isAllDay", e.isAllDay)
                if (e.subjectId != null) put("subjectId", e.subjectId)
                if (e.chapterId != null) put("chapterId", e.chapterId)
                if (e.reminderEpochMillis != null) put("reminderEpochMillis", e.reminderEpochMillis)
                if (e.taskId != null) put("taskId", e.taskId)
                if (e.examId != null) put("examId", e.examId)
            }
            array.put(obj)
        }
        return array.toString()
    }

    private fun deserializeEvents(jsonStr: String): List<CalendarEvent> {
        if (jsonStr.isBlank()) return emptyList()
        val list = mutableListOf<CalendarEvent>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id", UUID.randomUUID().toString())
                val title = obj.optString("title", "")
                val description = obj.optString("description", "")
                val dateStr = obj.optString("date", "")
                val date = if (dateStr.isNotBlank()) {
                    try { LocalDate.parse(dateStr) } catch (_: Exception) { com.example.core.util.IstTimeUtil.getCurrentLocalDate() }
                } else com.example.core.util.IstTimeUtil.getCurrentLocalDate()
                val startTime = if (obj.has("startTime")) obj.getString("startTime") else null
                val endTime = if (obj.has("endTime")) obj.getString("endTime") else null
                val isAllDay = obj.optBoolean("isAllDay", true)
                val subjectId = if (obj.has("subjectId")) obj.getString("subjectId") else null
                val chapterId = if (obj.has("chapterId")) obj.getString("chapterId") else null
                val reminderEpochMillis = if (obj.has("reminderEpochMillis")) obj.getLong("reminderEpochMillis") else null
                val taskId = if (obj.has("taskId")) obj.getString("taskId") else null
                val examId = if (obj.has("examId")) obj.getString("examId") else null

                list.add(
                    CalendarEvent(
                        id = id,
                        title = title,
                        description = description,
                        date = date,
                        startTime = startTime,
                        endTime = endTime,
                        isAllDay = isAllDay,
                        subjectId = subjectId,
                        chapterId = chapterId,
                        reminderEpochMillis = reminderEpochMillis,
                        taskId = taskId,
                        examId = examId
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }
}
