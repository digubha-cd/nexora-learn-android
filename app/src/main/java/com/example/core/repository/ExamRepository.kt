package com.example.core.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.NexoraApplication
import com.example.core.constants.AppConstants
import com.example.core.model.Chapter
import com.example.core.model.ExamDashboardState
import com.example.core.model.ExamItem
import com.example.core.model.ExamPhase
import com.example.core.model.ExamStatus
import com.example.core.model.ExamType
import com.example.core.model.StudyTask
import com.example.core.model.Subject
import com.example.core.model.SubjectExamProgress
import com.example.core.model.SyllabusScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * Repository powering Step 6 Exam System:
 * 1. Two separate exam phases:
 *    - School Exam (22 October 2026): Phase 1 Priority (66 chapters from Step 4 School Exam syllabus).
 *    - Board Exam (25 February 2027): Phase 2 Priority (Complete 94 chapters across all 7 subjects).
 * 2. Dynamic Exam Countdowns:
 *    - "School Exam — X days remaining"
 *    - "Board Exam — Y days remaining"
 *    Calculated automatically from the current reference date.
 * 3. Phase Logic:
 *    - Before 22 October 2026: Prioritize School Exam. Never mix Board Exam exclusive tasks into School Exam.
 *    - After School Exam: Automatically switch priority to Board Exam with the complete Board syllabus.
 * 4. Separate Progress Calculations:
 *    - School Exam progress (% and chapters)
 *    - Board Exam progress (% and chapters)
 *    - Overall 7-subject progress
 *    - Subject-wise preparation breakdown
 * 5. Full two-way reactivity with Step 4 Chapter Checkboxes & Step 5 Study Planner tasks.
 */
object ExamRepository {

    // Anchor reference date (dynamic support)
    var currentDate: LocalDate = StudyPlannerRepository.TODAY

    val SCHOOL_EXAM_DATE: LocalDate get() = StudyPlannerRepository.SCHOOL_EXAM_DATE
    val BOARD_EXAM_DATE: LocalDate get() = StudyPlannerRepository.BOARD_EXAM_DATE

    private val repositoryScope = CoroutineScope(Dispatchers.Default)

    // Manual view-phase override if student wants to inspect Phase 1 vs Phase 2 in UI
    private val _selectedPhaseFilter = MutableStateFlow<ExamPhase?>(null)
    val selectedPhaseFilter: StateFlow<ExamPhase?> = _selectedPhaseFilter

    private val _dashboardState = MutableStateFlow(calculateCurrentState())
    val dashboardState: StateFlow<ExamDashboardState> = _dashboardState

    init {
        kotlinx.coroutines.CoroutineScope(Dispatchers.Default).launch {
            combine(
                SyllabusRepository.completedChapterIds,
                StudyPlannerRepository.tasks,
                _selectedPhaseFilter
            ) { completedIds, allTasks, selectedPhase ->
                calculateDashboardState(completedIds, allTasks, selectedPhase)
            }.collect { newState ->
                _dashboardState.value = newState
            }
        }
    }

    fun setSelectedPhaseFilter(phase: ExamPhase?) {
        _selectedPhaseFilter.value = phase
        refreshDashboardState()
    }

    /**
     * Synchronously computes the current dashboard state snapshot.
     */
    fun calculateCurrentState(): ExamDashboardState {
        return calculateDashboardState(
            SyllabusRepository.completedChapterIds.value,
            StudyPlannerRepository.tasks.value,
            _selectedPhaseFilter.value
        )
    }

    /**
     * Refreshes the active dashboard state immediately.
     */
    fun refreshDashboardState(): ExamDashboardState {
        val newState = calculateCurrentState()
        _dashboardState.value = newState
        return newState
    }

    fun resetForTesting() {
        currentDate = StudyPlannerRepository.TODAY
        _selectedPhaseFilter.value = null
        refreshDashboardState()
    }

    /**
     * Determines the active exam phase based on current date.
     * Before 22 October 2026: Phase 1 (School Exam).
     * On/After 22 October 2026: Phase 2 (Board Exam).
     */
    fun getActiveExamPhase(date: LocalDate = currentDate): ExamPhase {
        return if (date.isBefore(SCHOOL_EXAM_DATE)) {
            ExamPhase.PHASE_1_SCHOOL
        } else {
            ExamPhase.PHASE_2_BOARD
        }
    }

    /**
     * Dynamically calculates days remaining from a given date to School Exam (22 Oct 2026).
     */
    fun getSchoolExamDaysRemaining(from: LocalDate = currentDate): Long {
        val days = ChronoUnit.DAYS.between(from, SCHOOL_EXAM_DATE)
        return maxOf(0L, days)
    }

    /**
     * Dynamically calculates days remaining from a given date to Board Exam (25 Feb 2027).
     */
    fun getBoardExamDaysRemaining(from: LocalDate = currentDate): Long {
        val days = ChronoUnit.DAYS.between(from, BOARD_EXAM_DATE)
        return maxOf(0L, days)
    }

    /**
     * Formats countdown text:
     * e.g., "School Exam — 32 days", on exam date "School Exam — EXAM DAY", never negative.
     */
    fun formatSchoolExamCountdown(from: LocalDate = currentDate): String {
        val days = getSchoolExamDaysRemaining(from)
        return when {
            days <= 0L -> "School Exam — EXAM DAY"
            days == 1L -> "School Exam — 1 day"
            else -> "School Exam — $days days"
        }
    }

    fun formatBoardExamCountdown(from: LocalDate = currentDate): String {
        val days = getBoardExamDaysRemaining(from)
        return when {
            days <= 0L -> "Board Exam — EXAM DAY"
            days == 1L -> "Board Exam — 1 day"
            else -> "Board Exam — $days days"
        }
    }

    /**
     * Core progress calculation engine.
     */
    private fun calculateDashboardState(
        completedIds: Set<String>,
        tasksList: List<StudyTask>,
        selectedPhase: ExamPhase?
    ): ExamDashboardState {
        val activePhase = selectedPhase ?: getActiveExamPhase(currentDate)
        val isSchoolPriority = currentDate.isBefore(SCHOOL_EXAM_DATE)

        // 1. Gather all chapters across all 7 subjects
        val allBoardChapters = mutableListOf<Chapter>()
        val allSchoolChapters = mutableListOf<Chapter>()

        val subjectProgress = mutableListOf<SubjectExamProgress>()

        StudyPlannerRepository.ORDERED_SUBJECT_IDS.forEach { subId ->
            val boardChapters = SyllabusRepository.getChapters(subId)
            val schoolChapters = SyllabusRepository.getChaptersByScope(subId, SyllabusScope.SCHOOL_EXAM)

            allBoardChapters.addAll(boardChapters)
            allSchoolChapters.addAll(schoolChapters)

            val schoolCompleted = schoolChapters.count { completedIds.contains(it.id) }
            val schoolTotal = schoolChapters.size
            val schoolPercent = if (schoolTotal > 0) (schoolCompleted.toFloat() / schoolTotal) * 100f else 0.0f

            val boardCompleted = boardChapters.count { completedIds.contains(it.id) }
            val boardTotal = boardChapters.size
            val boardPercent = if (boardTotal > 0) (boardCompleted.toFloat() / boardTotal) * 100f else 0.0f

            val subject = Subject.OFFICIAL_SUBJECTS.firstOrNull { it.id == subId }
            val gujaratiName = when (subId) {
                "accounts" -> "નામાનાં મૂળતત્ત્વો"
                "stat" -> "આંકડાશાસ્ત્ર"
                "economics" -> "અર્થશાસ્ત્ર"
                "ba" -> "વાણિજ્ય વ્યવસ્થા અને સંચાલન"
                "sp_cc" -> "એસ.પી. અને સી.સી."
                "english" -> "અંગ્રેજી (English)"
                "gujarati" -> "ગુજરાતી (પ્રથમ ભાષા)"
                else -> subject?.name ?: subId
            }

            subjectProgress.add(
                SubjectExamProgress(
                    subjectId = subId,
                    subjectName = subject?.name ?: subId,
                    gujaratiName = gujaratiName,
                    schoolExamCompleted = schoolCompleted,
                    schoolExamTotal = schoolTotal,
                    schoolExamPercent = schoolPercent,
                    boardExamCompleted = boardCompleted,
                    boardExamTotal = boardTotal,
                    boardExamPercent = boardPercent
                )
            )
        }

        // 2. Separate exam progress calculations
        val schoolCompletedCount = allSchoolChapters.count { completedIds.contains(it.id) }
        val schoolTotalCount = allSchoolChapters.size // 66 chapters
        val schoolProgressPercent = if (schoolTotalCount > 0) {
            (schoolCompletedCount.toFloat() / schoolTotalCount) * 100f
        } else 0.0f

        val boardCompletedCount = allBoardChapters.count { completedIds.contains(it.id) }
        val boardTotalCount = allBoardChapters.size // 94 chapters
        val boardProgressPercent = if (boardTotalCount > 0) {
            (boardCompletedCount.toFloat() / boardTotalCount) * 100f
        } else 0.0f

        // Overall progress reflects active phase or blended target towards 90+
        val overallProgressPercent = if (isSchoolPriority) {
            schoolProgressPercent
        } else {
            boardProgressPercent
        }

        // 3. Chapter pending and completed counts (for active phase scope)
        val targetScopeChapters = if (activePhase == ExamPhase.PHASE_1_SCHOOL) allSchoolChapters else allBoardChapters
        val completedCount = targetScopeChapters.count { completedIds.contains(it.id) }
        val pendingCount = targetScopeChapters.size - completedCount

        // 4. Missed tasks
        val missedTasks = tasksList.filter { it.isMissed && !it.isCompleted }

        return ExamDashboardState(
            targetMarks = AppConstants.DEFAULT_TARGET_MARKS,
            activePhase = activePhase,
            isSchoolExamPriority = isSchoolPriority,
            schoolExamDaysRemaining = getSchoolExamDaysRemaining(),
            schoolExamCountdownText = formatSchoolExamCountdown(),
            boardExamDaysRemaining = getBoardExamDaysRemaining(),
            boardExamCountdownText = formatBoardExamCountdown(),
            schoolExamCompletedCount = schoolCompletedCount,
            schoolExamTotalCount = schoolTotalCount,
            schoolExamProgressPercent = schoolProgressPercent,
            boardExamCompletedCount = boardCompletedCount,
            boardExamTotalCount = boardTotalCount,
            boardExamProgressPercent = boardProgressPercent,
            overallProgressPercent = overallProgressPercent,
            subjectProgressList = subjectProgress,
            completedChaptersCount = completedCount,
            pendingChaptersCount = pendingCount,
            missedTasksCount = missedTasks.size,
            missedTasks = missedTasks
        )
    }

    /**
     * Retrieve completed chapters for a given exam scope.
     */
    fun getCompletedChapters(scope: SyllabusScope): List<Chapter> {
        val completedIds = SyllabusRepository.completedChapterIds.value
        val chapters = mutableListOf<Chapter>()
        StudyPlannerRepository.ORDERED_SUBJECT_IDS.forEach { subId ->
            val list = SyllabusRepository.getChaptersByScope(subId, scope)
            chapters.addAll(list.filter { completedIds.contains(it.id) })
        }
        return chapters
    }

    /**
     * Retrieve pending chapters for a given exam scope.
     */
    fun getPendingChapters(scope: SyllabusScope): List<Chapter> {
        val completedIds = SyllabusRepository.completedChapterIds.value
        val chapters = mutableListOf<Chapter>()
        StudyPlannerRepository.ORDERED_SUBJECT_IDS.forEach { subId ->
            val list = SyllabusRepository.getChaptersByScope(subId, scope)
            chapters.addAll(list.filter { !completedIds.contains(it.id) })
        }
        return chapters
    }

    /**
     * Toggle chapter completion from Exam Dashboard.
     * Propagates to SyllabusRepository and StudyPlannerRepository.
     */
    fun toggleChapterCompletion(chapterId: String) {
        SyllabusRepository.toggleChapterCompletion(chapterId)
        refreshDashboardState()
    }

    /**
     * Reschedule missed task to today.
     */
    fun rescheduleMissedTask(taskId: String) {
        StudyPlannerRepository.rescheduleTask(taskId, StudyPlannerRepository.TODAY)
        refreshDashboardState()
    }

    /**
     * Reschedule all missed tasks to today.
     */
    fun rescheduleAllMissedToToday() {
        StudyPlannerRepository.rescheduleAllMissedToToday()
        refreshDashboardState()
    }

    /**
     * Toggle task completion.
     */
    fun toggleTaskCompletion(taskId: String) {
        StudyPlannerRepository.toggleTaskCompletion(taskId)
        refreshDashboardState()
    }

    // --- Custom Exam Management & Persistence ---
    private const val PREFS_NAME = "nexora_exams_prefs"
    private const val KEY_EXAMS_JSON = "key_exams_json"

    private var appContext: Context? = null

    val DEFAULT_SCHOOL_EXAM = ExamItem(
        id = "default_school_exam",
        name = "School Exam",
        type = ExamType.SCHOOL_EXAM,
        date = LocalDate.of(2026, 10, 22),
        startTime = "10:30 AM",
        endTime = "01:30 PM",
        location = "School Examination Hall",
        notes = "GSHSEB Class 12 Commerce - Phase 1 School Exam (66 Chapters)",
        subjectIds = Subject.OFFICIAL_SUBJECTS.map { it.id },
        isDefault = true
    )

    val DEFAULT_BOARD_EXAM = ExamItem(
        id = "default_board_exam",
        name = "Board Exam",
        type = ExamType.BOARD_EXAM,
        date = LocalDate.of(2027, 2, 25),
        startTime = "10:30 AM",
        endTime = "01:30 PM",
        location = "GSEB Board Examination Center",
        notes = "Gujarat Board Final Exam - Phase 2 (All 7 Subjects • 94 Chapters)",
        subjectIds = Subject.OFFICIAL_SUBJECTS.map { it.id },
        isDefault = true
    )

    private val _exams = MutableStateFlow<List<ExamItem>>(listOf(DEFAULT_SCHOOL_EXAM, DEFAULT_BOARD_EXAM))
    val exams: StateFlow<List<ExamItem>> = _exams.asStateFlow()

    fun initialize(context: Context) {
        appContext = context.applicationContext
        loadPersistedExams()
    }

    private fun getPrefs(): SharedPreferences? {
        if (appContext == null) {
            try {
                appContext = NexoraApplication.appContext
            } catch (_: Throwable) {}
        }
        return appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun loadPersistedExams() {
        val prefs = getPrefs() ?: return
        val jsonStr = prefs.getString(KEY_EXAMS_JSON, null) ?: return
        if (jsonStr.isNotBlank()) {
            val loaded = deserializeExams(jsonStr)
            if (loaded.isNotEmpty()) {
                val list = loaded.toMutableList()
                if (list.none { it.id == DEFAULT_SCHOOL_EXAM.id || (it.isDefault && it.type == ExamType.SCHOOL_EXAM) }) {
                    list.add(0, DEFAULT_SCHOOL_EXAM)
                }
                if (list.none { it.id == DEFAULT_BOARD_EXAM.id || (it.isDefault && it.type == ExamType.BOARD_EXAM) }) {
                    list.add(DEFAULT_BOARD_EXAM)
                }
                _exams.value = list.sortedBy { it.date }
            }
        }
    }

    private fun persistExams() {
        val prefs = getPrefs() ?: return
        val serialized = serializeExams(_exams.value)
        prefs.edit().putString(KEY_EXAMS_JSON, serialized).apply()
    }

    fun addExam(exam: ExamItem, context: Context? = null): ExamItem {
        val effectiveContext = context ?: appContext
        val updated = (_exams.value + exam).sortedBy { it.date }
        _exams.value = updated
        persistExams()
        refreshDashboardState()
        return exam
    }

    fun updateExam(exam: ExamItem, context: Context? = null) {
        val effectiveContext = context ?: appContext
        val updated = _exams.value.map { if (it.id == exam.id) exam else it }.sortedBy { it.date }
        _exams.value = updated
        persistExams()
        refreshDashboardState()
    }

    fun deleteExam(examId: String, context: Context? = null): Boolean {
        val target = _exams.value.find { it.id == examId }
        if (target != null && target.isDefault) {
            return false // Default exams cannot be deleted, only edited
        }
        _exams.value = _exams.value.filter { it.id != examId }
        persistExams()
        refreshDashboardState()
        return true
    }

    fun getExamById(examId: String): ExamItem? {
        return _exams.value.find { it.id == examId }
    }

    fun getExamsForDate(date: LocalDate): List<ExamItem> {
        return _exams.value.filter { it.date == date }
    }

    private fun serializeExams(list: List<ExamItem>): String {
        val array = JSONArray()
        for (e in list) {
            val obj = JSONObject().apply {
                put("id", e.id)
                put("name", e.name)
                put("type", e.type.name)
                put("date", e.date.format(DateTimeFormatter.ISO_LOCAL_DATE))
                if (e.startTime != null) put("startTime", e.startTime)
                if (e.endTime != null) put("endTime", e.endTime)
                if (e.location != null) put("location", e.location)
                if (e.notes != null) put("notes", e.notes)
                val subArray = JSONArray()
                for (s in e.subjectIds) subArray.put(s)
                put("subjectIds", subArray)
                if (e.reminderEpochMillis != null) put("reminderEpochMillis", e.reminderEpochMillis)
                put("isDefault", e.isDefault)
            }
            array.put(obj)
        }
        return array.toString()
    }

    private fun deserializeExams(jsonStr: String): List<ExamItem> {
        if (jsonStr.isBlank()) return emptyList()
        val list = mutableListOf<ExamItem>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id", UUID.randomUUID().toString())
                val name = obj.optString("name", "Exam")
                val typeStr = obj.optString("type", ExamType.OTHER.name)
                val type = try { ExamType.valueOf(typeStr) } catch (_: Exception) { ExamType.OTHER }
                val dateStr = obj.optString("date", "")
                val date = if (dateStr.isNotBlank()) {
                    try { LocalDate.parse(dateStr) } catch (_: Exception) { com.example.core.util.IstTimeUtil.getCurrentLocalDate() }
                } else com.example.core.util.IstTimeUtil.getCurrentLocalDate()
                val startTime = if (obj.has("startTime")) obj.getString("startTime") else null
                val endTime = if (obj.has("endTime")) obj.getString("endTime") else null
                val location = if (obj.has("location")) obj.getString("location") else null
                val notes = if (obj.has("notes")) obj.getString("notes") else null
                val subList = mutableListOf<String>()
                if (obj.has("subjectIds")) {
                    val subArr = obj.getJSONArray("subjectIds")
                    for (j in 0 until subArr.length()) {
                        subList.add(subArr.getString(j))
                    }
                }
                val reminderEpochMillis = if (obj.has("reminderEpochMillis")) obj.getLong("reminderEpochMillis") else null
                val isDefault = obj.optBoolean("isDefault", false)

                list.add(
                    ExamItem(
                        id = id,
                        name = name,
                        type = type,
                        date = date,
                        startTime = startTime,
                        endTime = endTime,
                        location = location,
                        notes = notes,
                        subjectIds = subList,
                        reminderEpochMillis = reminderEpochMillis,
                        isDefault = isDefault
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }
}
