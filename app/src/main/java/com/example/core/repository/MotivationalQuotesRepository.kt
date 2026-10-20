package com.example.core.repository

import android.content.Context
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository and Manager for Daily Motivational Quotes.
 * Provides 100+ unique student-focused motivational quotes and handles
 * stable time-slot scheduling (4 times a day: Morning 6 AM, Late Morning 11 AM,
 * Afternoon 4 PM, Evening 9 PM), local persistence, duplicate prevention in
 * consecutive slots, and offline support.
 */
object MotivationalQuotesRepository {

    private const val PREFS_NAME = "nexora_motivation_prefs"
    private const val KEY_LAST_SLOT_ID = "key_last_slot_id"
    private const val KEY_CURRENT_QUOTE_INDEX = "key_current_quote_index"
    private const val KEY_PREVIOUS_QUOTE_INDEX = "key_previous_quote_index"
    private const val KEY_LAST_MOTIVATION_TEXT = "key_last_motivation_text"
    private const val KEY_LAST_MOTIVATION_TIMESTAMP = "key_last_motivation_timestamp"

    data class QuoteSlotInfo(
        val quote: String,
        val slotTitle: String,
        val dateKey: Long
    )

    private val _currentQuote = MutableStateFlow("Small steps every day lead to big achievements.")
    val currentQuote: StateFlow<String> = _currentQuote.asStateFlow()

    private val _currentSlotTitle = MutableStateFlow("Morning Motivation")
    val currentSlotTitle: StateFlow<String> = _currentSlotTitle.asStateFlow()

    /**
     * Complete collection of 100+ original, high-quality, student-friendly motivational quotes.
     */
    val quotesList = listOf(
        // Studying Consistently & Small Daily Improvements (1-20)
        "Small steps every day lead to big achievements.",
        "Your future grows with every lesson you learn.",
        "Focus on progress, not perfection.",
        "One focused study session is a step forward.",
        "Consistency turns effort into achievement.",
        "You do not have to be perfect; you have to keep learning.",
        "Every question you practise makes you more prepared.",
        "Small habits repeated daily create extraordinary results.",
        "An hour of deep focus beats three hours of distracted reading.",
        "Show up for your studies even on days when motivation is low.",
        "Success is the sum of small efforts repeated day in and day out.",
        "Make today's study session better than yesterday's.",
        "Your consistency is your superpower in exams.",
        "Little by little, a little becomes a lot.",
        "Build your knowledge brick by brick with daily dedication.",
        "A disciplined study routine is the bridge to your dream career.",
        "Keep your momentum alive with one page, one concept at a time.",
        "Dedication to your books today shapes your freedom tomorrow.",
        "The secret of getting ahead is getting started every single day.",
        "Your daily study habit is an investment in your limitless potential.",

        // Exam Preparation & Confidence (21-40)
        "Approach every exam with calm confidence and thorough preparation.",
        "Preparation eliminates anxiety and breeds absolute confidence.",
        "Trust the hours of practice you have put into your subjects.",
        "Examinations test not just memory, but your resilience and clarity.",
        "Review your mistakes today so you never repeat them in the exam hall.",
        "Read each question carefully and answer with structured precision.",
        "Your exam score reflects your dedication, focus, and perseverance.",
        "Stay calm under pressure; you have prepared for this moment.",
        "Turn complex chapters into simple, understandable concepts.",
        "Practice past papers until solving them feels second nature.",
        "Your mind is sharp, focused, and ready for any challenge.",
        "Believe in your ability to master even the hardest subjects.",
        "Confidence comes from discipline and rigorous revision.",
        "Let your hard work speak louder than your exam stress.",
        "Every revision round sharpens your retention and clarity.",
        "You are fully equipped to conquer your board examinations.",
        "Keep your revision notes organized and your mind clear.",
        "Anxious thoughts dissolve when you dive deep into productive study.",
        "Success in examinations belongs to those who prepare with purpose.",
        "Turn difficult syllabus topics into your greatest strengths.",

        // Discipline, Focus & Time Management (41-60)
        "Discipline is choosing between what you want now and what you want most.",
        "Guard your focus fiercely; distractions steal your precious time.",
        "Master your schedule before your schedule masters you.",
        "Turn off notifications and dive into deep, uninterrupted study.",
        "Time management is life management. Spend your hours wisely.",
        "A distraction-free desk is the launchpad for high productivity.",
        "Prioritize your hardest subjects when your energy is highest.",
        "Study with intention, not just to pass the time.",
        "Break large chapters into manageable 25-minute focus blocks.",
        "Self-discipline is the silent architect of academic excellence.",
        "Focus is not saying yes to studying; it's saying no to distractions.",
        "Plan your study day the night before for maximum efficiency.",
        "Procrastination steals time; decisive action reclaims your future.",
        "Stay seated and focused until your study goal for the hour is met.",
        "Mastering your attention is mastering your academic destiny.",
        "Quality study time outweighs quantity every single time.",
        "Respect your study timetable as you would an important appointment.",
        "Eliminate multitasking and give your full attention to one topic.",
        "A focused mind solves complex problems with effortless clarity.",
        "Take purposeful short breaks to keep your brain refreshed and sharp.",

        // Resilience, Overcoming Setbacks & Long-Term Goals (61-80)
        "Mistakes are proof that you are trying and learning.",
        "A setback is only a setup for a much stronger comeback.",
        "Your long-term goals are worth every temporary sacrifice today.",
        "Embrace difficult topics as opportunities to grow smarter.",
        "Tough times in your studies build unbreakable mental resilience.",
        "Keep your eyes on your graduation goal and keep moving forward.",
        "Failure is not the opposite of success; it is part of success.",
        "When studying gets tough, remember why you started this journey.",
        "Your dedication today builds the foundation of your tomorrow.",
        "Every expert was once a beginner who refused to quit.",
        "Obstacles in your syllabus are just stepping stones to mastery.",
        "Resilience is continuing your revision when the going gets hard.",
        "Your dreams of academic excellence demand your daily commitment.",
        "Do not let a difficult mock test discourage your grand vision.",
        "Push through the mental fatigue; greatness lies just beyond it.",
        "Every challenge overcome makes your intellect sharper and stronger.",
        "Great achievements require time, patience, and persistent effort.",
        "Stay resilient when concepts feel foreign; persistence unlocks understanding.",
        "Your ambition is greater than any academic obstacle in your path.",
        "Trust the process of learning and never lose faith in yourself.",

        // Additional Inspiring Quotes for Complete Coverage (81-105)
        "Knowledge is the only treasure that increases when shared and studied.",
        "Feed your curiosity and question how things work in your textbooks.",
        "Your intellect expands every time you tackle a challenging problem.",
        "Learning is a treasure that will follow its owner everywhere.",
        "Invest in your education today; it pays the best interest tomorrow.",
        "Read with curiosity, write with clarity, and revise with purpose.",
        "Education is your passport to a bright and independent future.",
        "Let your passion for learning outshine any fear of failure.",
        "Clear concepts in your mind lead to confident answers on paper.",
        "Study not to memorize, but to truly understand and apply.",
        "Your potential is limitless when backed by consistent effort.",
        "Dream big, study hard, and stay humble throughout your journey.",
        "Every chapter finished is a victory worth celebrating.",
        "Strive for continuous improvement rather than overnight perfection.",
        "Your positive study mindset attracts clarity, focus, and success.",
        "Keep pushing your intellectual boundaries every single week.",
        "Success is built on a foundation of daily studious habits.",
        "Let your dedication be your signature in every examination.",
        "Empower your mind with knowledge and your future with confidence.",
        "Today's focused revision is tomorrow's academic triumph.",
        "You are capable of achieving extraordinary results through hard work.",
        "Keep your spirits high, your books open, and your goals in sight."
    )

    /**
     * Determine the current time slot info based on local time.
     * Slots:
     * 0: Morning (6:00 AM - 10:59 AM) -> "Morning Motivation"
     * 1: Late Morning (11:00 AM - 3:59 PM) -> "Late Morning Focus"
     * 2: Afternoon (4:00 PM - 8:59 PM) -> "Afternoon Drive"
     * 3: Evening/Night (9:00 PM - 5:59 AM) -> "Evening Reflection"
     */
    fun computeCurrentSlot(calendar: Calendar = Calendar.getInstance()): Triple<Long, Int, String> {
        val year = calendar.get(Calendar.YEAR)
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        // Date key: yyyyDDD
        val dateKey = (year * 1000L) + dayOfYear

        val slotIndex: Int
        val slotTitle: String

        when (hour) {
            in 6..10 -> {
                slotIndex = 0
                slotTitle = "Morning Motivation"
            }
            in 11..15 -> {
                slotIndex = 1
                slotTitle = "Late Morning Focus"
            }
            in 16..20 -> {
                slotIndex = 2
                slotTitle = "Afternoon Drive"
            }
            else -> {
                slotIndex = 3
                slotTitle = "Evening Reflection"
            }
        }

        return Triple(dateKey, slotIndex, slotTitle)
    }

    /**
     * Get or compute the quote for the current time slot, ensuring persistence
     * and avoiding consecutive duplicate quotes.
     */
    fun refreshAndGetQuote(context: Context, calendar: Calendar = Calendar.getInstance()): QuoteSlotInfo {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastTimestamp = prefs.getLong(KEY_LAST_MOTIVATION_TIMESTAMP, 0L)
        val currentTime = System.currentTimeMillis()

        // 24 hours = 86,400,000 ms
        val isExpired = (currentTime - lastTimestamp) >= 86_400_000

        val quoteText: String
        val timestampToSave: Long

        if (!isExpired && prefs.contains(KEY_LAST_MOTIVATION_TEXT)) {
            quoteText = prefs.getString(KEY_LAST_MOTIVATION_TEXT, quotesList[0])!!
            timestampToSave = lastTimestamp
        } else {
            val lastQuote = prefs.getString(KEY_LAST_MOTIVATION_TEXT, "")
            var candidateIndex = (Math.random() * quotesList.size).toInt()
            
            var attempts = 0
            while (quotesList[candidateIndex] == lastQuote && attempts < 10 && quotesList.size > 1) {
                candidateIndex = (Math.random() * quotesList.size).toInt()
                attempts++
            }
            quoteText = quotesList[candidateIndex]
            timestampToSave = currentTime

            prefs.edit()
                .putString(KEY_LAST_MOTIVATION_TEXT, quoteText)
                .putLong(KEY_LAST_MOTIVATION_TIMESTAMP, timestampToSave)
                .apply()
        }

        _currentQuote.value = quoteText
        _currentSlotTitle.value = "Daily Motivation"

        return QuoteSlotInfo(
            quote = quoteText,
            slotTitle = "Daily Motivation",
            dateKey = timestampToSave
        )
    }

    /**
     * Initialize quote from storage when app starts or resumes.
     */
    fun checkAndUpdateOnResume(context: Context) {
        val info = refreshAndGetQuote(context)
        _currentQuote.value = info.quote
        _currentSlotTitle.value = info.slotTitle
    }
}
