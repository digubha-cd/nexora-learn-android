package com.example.core.repository

import android.content.Context
import com.example.core.database.NexoraDatabase
import com.example.core.database.entity.AiChatMessageEntity
import com.example.core.database.entity.AiChatSessionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Repository for managing NEXORA AI chat sessions and smart study mentor guidance for Gujarat Board Class 12 Commerce.
 */
object NexoraAiRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var db: NexoraDatabase? = null

    private val _messages = MutableStateFlow<List<AiMessage>>(emptyList())
    val messages: StateFlow<List<AiMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

    private val _currentSessionTitle = MutableStateFlow("New Session")
    val currentSessionTitle: StateFlow<String> = _currentSessionTitle.asStateFlow()

    private var subjectContext: String? = null
    private var chapterContext: String? = null

    private fun getDb(context: Context): NexoraDatabase {
        return db ?: synchronized(this) {
            val instance = NexoraDatabase.getInstance(context)
            db = instance
            instance
        }
    }

    fun setContext(subject: String?, chapter: String?) {
        subjectContext = subject
        chapterContext = chapter
    }

    suspend fun createNewSession() {
        _currentSessionId.value = UUID.randomUUID().toString()
        _currentSessionTitle.value = "New Session"
        _messages.value = emptyList()
        _errorMessage.value = null
    }

    fun getAllSessions(context: Context): Flow<List<AiChatSessionEntity>> {
        return getDb(context).aiChatDao().getAllSessions()
    }

    suspend fun loadSession(sessionId: String, context: Context) {
        _isLoading.value = true
        _errorMessage.value = null
        try {
            val dao = getDb(context).aiChatDao()
            val session = dao.getSessionById(sessionId)
            if (session != null) {
                _currentSessionId.value = session.sessionId
                _currentSessionTitle.value = session.title
                val entities = dao.getMessagesForSessionSync(sessionId)
                _messages.value = entities.map { it.toAiMessage() }
            }
        } catch (e: Exception) {
            _errorMessage.value = "Failed to load session: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun sendMessage(prompt: String, context: Context) {
        if (prompt.isBlank()) return
        
        val sessionId = _currentSessionId.value ?: run {
            val newId = UUID.randomUUID().toString()
            _currentSessionId.value = newId
            newId
        }

        val userMessage = AiMessage(
            id = UUID.randomUUID().toString(),
            text = prompt,
            isFromUser = true
        )
        
        _messages.value = _messages.value + userMessage
        _isLoading.value = true
        _errorMessage.value = null

        try {
            val dao = getDb(context).aiChatDao()
            if (dao.getSessionById(sessionId) == null) {
                dao.insertSession(AiChatSessionEntity(sessionId = sessionId, title = prompt.take(30)))
                _currentSessionTitle.value = prompt.take(30)
            }
            dao.insertMessage(AiChatMessageEntity.fromAiMessage(sessionId, userMessage))

            // Generate structured GSEB Class 12 Commerce guidance
            delay(400) // Brief natural pause for response generation
            val responseText = generateMentorResponse(prompt, subjectContext, chapterContext)

            val aiMessage = AiMessage(
                id = UUID.randomUUID().toString(),
                text = responseText,
                isFromUser = false
            )

            _messages.value = _messages.value + aiMessage
            dao.insertMessage(AiChatMessageEntity.fromAiMessage(sessionId, aiMessage))
            
            // Update session stats
            dao.updateSessionStats(sessionId, System.currentTimeMillis(), _messages.value.size)

        } catch (e: Exception) {
            _errorMessage.value = "AI Mentor Error: ${e.localizedMessage}"
        } finally {
            _isLoading.value = false
        }
    }

    private fun generateMentorResponse(query: String, subject: String?, chapter: String?): String {
        val q = query.lowercase().trim()
        val subjectInfo = if (!subject.isNullOrBlank()) "[$subject${if (!chapter.isNullOrBlank()) " - $chapter" else ""}]" else ""

        return when {
            q.contains("plan") || q.contains("schedule") || q.contains("routine") || q.contains("timetable") -> {
                """
                |🎯 **GSEB Class 12 Commerce Study Plan for 90+ Marks** $subjectInfo
                |
                |1. **Morning Golden Hours (6:00 AM - 8:30 AM)**:
                |   - Focus on practical subjects like **Elements of Accounts (નામાનાં મૂળતત્વો)** or **Statistics (આંકડાશાસ્ત્ર)**.
                |   - Solve at least 3-4 comprehensive numerical illustrations with working notes.
                |
                |2. **Afternoon Session (2:00 PM - 4:30 PM)**:
                |   - Theory subjects: **Economics (અર્થશાસ્ત્ર)** or **O.C.M. (વાણિજ્ય વ્યવસ્થા)**.
                |   - Highlight points, memorize definitions by renowned economists (Adam Smith, Marshall, Robbins).
                |
                |3. **Evening Focus (7:00 PM - 9:30 PM)**:
                |   - **S.P. & C.C. (વાણિજ્યિક પત્રવ્યવહાર)** and **English / Gujarati**.
                |   - Practice formal letter formats, circulars, and resolution writing.
                |
                |💡 **Pro-Tip**: Review your To-Do checklist in the Planner tab daily before sleeping!
                """.trimMargin()
            }
            q.contains("account") || q.contains("accounts") || q.contains("partnership") || q.contains("share") -> {
                """
                |📊 **Elements of Accounts (Class 12 GSEB Tips)**
                |
                |• **Part 1 Key Focus**:
                |  - **Partnership Final Accounts**: Always prepare Trading A/c, Profit & Loss A/c, P&L Appropriation A/c, Partners' Capital A/c, and Balance Sheet with neat ruling.
                |  - **Admission/Retirement of Partner**: Master Revaluation A/c adjustments, Goodwill treatment (AS-26), and Capital adjustment methods.
                |
                |• **Part 2 Key Focus**:
                |  - **Share Capital Transactions**: Focus on Pro-rata allotment, Calls-in-arrears, and Forfeiture & Re-issue entries.
                |  - **Debentures**: Discount/Loss on issue write-off and redemption reserves.
                |
                |✨ **Golden Rule**: Always show working notes clearly! GSEB examiners allocate specific marks for calculation steps.
                """.trimMargin()
            }
            q.contains("stat") || q.contains("index") || q.contains("probability") || q.contains("correlation") -> {
                """
                |📈 **Statistics (આંકડાશાસ્ત્ર) High-Scoring Guide**
                |
                |• **Chapter 1: Index Numbers**:
                |  - Laspeyre's Index: [Σ p1·q0 / Σ p0·q0] × 100
                |  - Paasche's Index: [Σ p1·q1 / Σ p0·q1] × 100
                |  - Fisher's Ideal Index: √[IL × IP] (Satisfies Time & Factor Reversal tests!)
                |
                |• **Chapter 2 & 3: Linear Correlation & Regression**:
                |  - Karl Pearson's coefficient of correlation (r) and Spearman's Rank method for qualitative data.
                |  - Regression line of Y on X: ŷ = a + bx.
                |
                |• **Part 2**:
                |  - Normal distribution properties (bell-shaped curve, area under curve = 1, mean = median = mode).
                """.trimMargin()
            }
            q.contains("eco") || q.contains("economic") || q.contains("inflation") || q.contains("money") -> {
                """
                |🏛️ **Economics (અર્થશાસ્ત્ર) 90+ Blueprint**
                |
                |• **Money and Inflation (નાણું અને ફુગાવો)**:
                |  - Limitations of Barter System (Mutual adjustment of wants, storage of value).
                |  - Functions of money and types of inflation (Cost-push vs Demand-pull).
                |
                |• **Banking & Monetary Policy**:
                |  - Quantitative tools of RBI: Bank Rate, Repo Rate, Reverse Repo Rate, CRR, SLR.
                |  - Qualitative tools: Margin requirement, credit rationing.
                |
                |• **Presentation Tip**: Draw neat Bar Diagrams and Pie Charts using pencil and scale for Section E 5-mark answers!
                """.trimMargin()
            }
            q.contains("oc") || q.contains("ocm") || q.contains("management") || q.contains("fayol") || q.contains("taylor") -> {
                """
                |💼 **Organization of Commerce & Management (B.A. / O.C.M.)**
                |
                |• **Principles of Management**:
                |  - **Henri Fayol's 14 Principles**: Division of work, Authority & Responsibility, Discipline, Unity of Command, Unity of Direction, Scalar Chain, Esprit de Corps.
                |  - **F.W. Taylor's Scientific Management**: Time study, Motion study, Fatigue study, Differential piece wage system.
                |
                |• **Functions of Management (POSDCORB)**:
                |  - Planning -> Organizing -> Staffing -> Directing -> Coordinating -> Reporting -> Budgeting.
                |
                |📝 **Exam Strategy**: Always write answers with: 1) Definition, 2) Characteristics/Steps in points, 3) Diagrammatic representation.
                """.trimMargin()
            }
            q.contains("gujarat") || q.contains("gujarati") || q.contains("ગુજરાતી") -> {
                """
                |🙏 **નમસ્તે! GSEB ધોરણ ૧૨ કોમર્સ માટે નેક્સોરા AI માર્ગદર્શક**
                |
                |• **બોર્ડ પરીક્ષામાં ૯૦+ ગુણ મેળવવાની શ્રેષ્ઠ વ્યૂહરચના**:
                |  ૧. રોજ સવારે ૧.૫ થી ૨ કલાક નામાનાં મૂળતત્વો અથવા આંકડાશાસ્ત્રના દાખલા ગણો.
                |  ૨. બ્લૂપ્રિન્ટ અનુસાર વિભાગ-E (લાંબા પ્રશ્નો) માટે મુખ્ય પ્રકરણો પર પૂરતો સમય ફાળવો.
                |  ૩. પાઠ્યપુસ્તકના સ્વાધ્યાયના તમામ MCQ અને ૧-વાક્યના પ્રશ્નો કંઠસ્થ કરો.
                |  ૪. નિયમિત 'To-Do List' અને 'Focus Timer' નો ઉપયોગ કરીને અભ્યાસ કરો.
                |
                |તમારો ચોક્કસ પ્રશ્ન પૂછો, હું તમને વિગતવાર સમજાવીશ!
                """.trimMargin()
            }
            else -> {
                """
                |✨ **NEXORA Study Mentor Insight** $subjectInfo
                |
                |Regarding **"$query"**:
                |
                |1. **Key Concept**: In the Gujarat Board Class 12 Commerce curriculum, thoroughly understand the textbook definition first, then understand the practical/commercial application.
                |2. **GSEB Board Pattern**:
                |   - Section A (MCQs) & Section B (One-liners): Direct textbook lines.
                |   - Section C & D: Conceptual short answers with headings.
                |   - Section E: Detailed 5-mark long questions and numerical accounts.
                |3. **Actionable Next Step**: Add a 30-minute practice session for this topic in your **Planner** tab today!
                |
                |Ask me about any specific formula, theorem, account entry, or subject concept!
                """.trimMargin()
            }
        }
    }

    suspend fun retryLastMessage(context: Context) {
        val lastUserMessage = _messages.value.lastOrNull { it.isFromUser }
        if (lastUserMessage != null) {
            _messages.value = _messages.value.takeWhile { it != lastUserMessage } + lastUserMessage
            sendMessage(lastUserMessage.text, context)
        }
    }

    suspend fun deleteSession(sessionId: String, context: Context) {
        getDb(context).aiChatDao().deleteSessionAndMessages(sessionId)
        if (_currentSessionId.value == sessionId) {
            createNewSession()
        }
    }
}
