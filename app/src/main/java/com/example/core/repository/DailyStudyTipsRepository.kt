package com.example.core.repository

import android.util.Log
import com.example.BuildConfig
import com.example.core.model.DailyStudyTip
import com.example.core.model.Subject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class TipLanguage(val code: String, val displayName: String, val nativeName: String) {
    GUJARATI("gu", "Gujarati", "ગુજરાતી"),
    ENGLISH("en", "English", "English")
}

/**
 * Repository for fetching and caching subject-specific Daily Study Tips
 * using the Gemini 3.5 Flash REST API, with special focus on Gujarati language
 * tailored for Class 12 Commerce (GSEB) students.
 */
object DailyStudyTipsRepository {

    private const val TAG = "DailyStudyTipsRepo"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val scope = CoroutineScope(Dispatchers.IO)

    fun getGujaratiSubjectName(subjectId: String): String {
        return when (subjectId) {
            "accounts" -> "નામાનાં મૂળતત્ત્વો (Accounts)"
            "stat" -> "આંકડાશાસ્ત્ર (Statistics)"
            "economics" -> "અર્થશાસ્ત્ર (Economics)"
            "ba" -> "વાણિજ્ય વ્યવસ્થા અને સંચાલન (B.A.)"
            "sp_cc" -> "એસ.પી. અને સી.સી. (SP & CC)"
            "english" -> "અંગ્રેજી (English)"
            "gujarati" -> "ગુજરાતી (પ્રથમ ભાષા)"
            "all" -> "બધા વિષયો (Class 12 Commerce)"
            else -> subjectId
        }
    }

    // Curated high-yield tips in Gujarati for GSEB Class 12 Commerce
    val CURATED_TIPS_GUJARATI = mapOf(
        "all" to DailyStudyTip(
            subjectId = "all",
            subjectName = "ધોરણ ૧૨ કોમર્સ (GSEB)",
            headline = "૯૦+ ગુણ લાવવાની બ્લુપ્રિન્ટ: દૈનિક રિવિઝન ફોર્મ્યુલા",
            tipContent = "રોજ સાંજે અભ્યાસના ૨ સ્લોટ બનાવો: ૧ પ્રેક્ટિકલ વિષય (નામાનાં મૂળતત્ત્વો/આંકડાશાસ્ત્ર) ગણતરી માટે, અને ૧ થિયરી વિષય (B.A./અર્થશાસ્ત્ર/SP) મુદ્દાસર યાદ રાખવા માટે. મુખ્ય વ્યાખ્યાઓ પોતાની નોટબુકમાં ટૂંકમાં લખો.",
            actionStep = "એકાઉન્ટ અથવા સ્ટેટમાંથી ૩ મુખ્ય સૂત્રો મોઢે નોટબુકમાં લખીને ચકાસો.",
            language = "gu",
            source = "બોર્ડ એક્સપર્ટ ટિપ"
        ),
        "accounts" to DailyStudyTip(
            subjectId = "accounts",
            subjectName = "નામાનાં મૂળતત્ત્વો",
            headline = "ભાગીદારી પુનઃમૂલ્યાંકન ખાતું અને વર્કિંગ નોટ્સના સ્ટેપ માર્ક્સ",
            tipContent = "ગુજરાત બોર્ડ પેપર ચેકિંગમાં પુનઃમૂલ્યાંકન ખાતું, પાઘડીની ગણતરી અને ભાગીદારોના મૂડી ખાતાની વર્કિંગ નોટ્સ (Working Notes) ના ૩૦% થી ૪૦% ગુણ હોય છે. ઉધાર-જમા ખાના સુઘડ દોરો અને જરૂરી બાબત જે (બા.જે.) ચોક્કસ લખો.",
            actionStep = "ભાગીદારી પ્રવેશ/નિવૃત્તિમાંથી મિલકતો-દેવાના પુનઃમૂલ્યાંકનનો ૧ સંપૂર્ણ દાખલો જાતે ગણો.",
            language = "gu",
            source = "બોર્ડ એક્સપર્ટ ટિપ"
        ),
        "stat" to DailyStudyTip(
            subjectId = "stat",
            subjectName = "આંકડાશાસ્ત્ર",
            headline = "સૂચકઆંક અને કાર્લ પિયર્સન સૂત્ર પસંદગીની ચાવી",
            tipContent = "ગણતરી શરૂ કરતા પહેલા X અને Y ના મધ્યક (Mean) પૂર્ણાંક છે કે અપૂર્ણાંક તે ચકાસો. જો મધ્યક અપૂર્ણાંક હોય, તો પોઈન્ટ્સની ભૂલ ટાળવા UV ટૂંકી રીતનો જ ઉપયોગ કરો. ક્રમાંક સહસંબંધમાં ગાંઠ (Tie) પડે ત્યારે CF સુધારો ભૂલશો નહીં.",
            actionStep = "ટેક્સ્ટબુકના સ્વાધ્યાયમાંથી UV રીતનો ૧ દાખલો કેલ્ક્યુલેટર સાથે ઝડપથી ગણો.",
            language = "gu",
            source = "બોર્ડ એક્સપર્ટ ટિપ"
        ),
        "economics" to DailyStudyTip(
            subjectId = "economics",
            subjectName = "અર્થશાસ્ત્ર",
            headline = "અર્થશાસ્ત્રમાં આલેખ અને ધરીનું નામકરણ અનિવાર્ય",
            tipContent = "માંગ અને પુરવઠાના પ્રશ્નોમાં Y-ધરી પર 'કિંમત' અને X-ધરી પર 'જથ્થો' તેમજ ઉદ્ગમબિંદુ (O) દર્શાવવું ફરજિયાત છે. ૫ ગુણના પ્રશ્નોમાં ભારતીય અર્થતંત્રના વર્તમાન ઉદાહરણો ઉમેરવાથી વધુ માર્ક્સ મળે છે.",
            actionStep = "માંગની મૂલ્યસાપેક્ષતાના પાંચેય પ્રકારની આકૃતિઓ કાચી નોટમાં નામકરણ સાથે દોરો.",
            language = "gu",
            source = "બોર્ડ એક્સપર્ટ ટિપ"
        ),
        "ba" to DailyStudyTip(
            subjectId = "ba",
            subjectName = "વાણિજ્ય વ્યવસ્થા અને સંચાલન",
            headline = "૫ ગુણના જવાબો મુદ્દાસર અને ફ્લોચાર્ટ સાથે લખો",
            tipContent = "વાણિજ્ય વ્યવસ્થામાં મોટા ફકરા લખવાનું ટાળો. બોર્ડ પદ્ધતિ મુજબ: અર્થ -> અધિકૃત વ્યાખ્યા -> ડાયાગ્રામ/ફ્લોચાર્ટ -> મુખ્ય મુદ્દાઓની પેટા-મથાળા (Sub-headings) સાથે સમજૂતી લખો.",
            actionStep = "હેનરી ફેયોલના સંચાલનના ૧૪ સિદ્ધાંતોમાંથી પ્રથમ ૭ સિદ્ધાંતો ક્રમબદ્ધ યાદ કરી લખો.",
            language = "gu",
            source = "બોર્ડ એક્સપર્ટ ટિપ"
        ),
        "sp_cc" to DailyStudyTip(
            subjectId = "sp_cc",
            subjectName = "એસ.પી. અને સી.સી.",
            headline = "પત્ર વ્યવહારનું સચોટ માળખું (Layout) ૪૦% ગુણ અપાવે છે",
            tipContent = "વાણિજ્યિક પત્રવ્યવહારમાં ૭ મુખ્ય અંગો: શીર્ષક, તારીખ (બ્રિટિશ/અમેરિકન પદ્ધતિ), આંતરિક સરનામું, સંબોધન, વિષય/સંદર્ભ, મુખ્ય ફકરા અને સમાપન/સહીનું યોગ્ય સ્થાન ચકાસો.",
            actionStep = "બેંક ફરિયાદ પત્ર અથવા ડીમેટ ખાતા ખોલાવવા અંગેના પત્રનું માત્ર માળખું (Layout) દોરો.",
            language = "gu",
            source = "બોર્ડ એક્સપર્ટ ટિપ"
        ),
        "english" to DailyStudyTip(
            subjectId = "english",
            subjectName = "અંગ્રેજી (English)",
            headline = "અહેવાલ લેખન (Report Writing) અને પેસિવ વોઈસ નિયમ",
            tipContent = "અંગ્રેજી જનરલ પ્રવાહમાં શાળા અથવા જાહેર કાર્યક્રમના અહેવાલ (Report Writing) હંમેશા ભૂતકાળ અને Passive Voice માં લખો (દા.ત., 'A seminar was organized...'). સારાંશ લેખનમાં ૧/૩ ભાગ અને યોગ્ય શીર્ષક આપો.",
            actionStep = "Tree Plantation Day અથવા Independence Day ના રિપોર્ટની શરૂઆતના ૩ વાક્યો અંગ્રેજીમાં લખો.",
            language = "gu",
            source = "બોર્ડ એક્સપર્ટ ટિપ"
        ),
        "gujarati" to DailyStudyTip(
            subjectId = "gujarati",
            subjectName = "ગુજરાતી (પ્રથમ ભાષા)",
            headline = "વ્યાકરણ વિભાગમાં રોકડા માર્ક્સ અને વિચાર-વિસ્તાર શણગાર",
            tipContent = "જોડણી, સંધિ, સમાસ, અલંકાર અને છંદ પૂરા માર્ક્સ અપાવે છે - રોજ ૧૦ પ્રશ્નો સોલ્વ કરો. નિબંધ કે વિચાર વિસ્તારમાં વિષયને અનુરૂપ કાવ્યપંક્તિ કે સુવિચારથી શરૂઆત કરવાથી પરીક્ષક પર ઉત્તમ પ્રભાવ પડે છે.",
            actionStep = "પાઠ્યપુસ્તકના ૫ શબ્દોના સમાસ (કર્મધારય, તત્પુરુષ, દ્વન્દ્વ) ઓળખીને નોટબુકમાં લખો.",
            language = "gu",
            source = "બોર્ડ એક્સપર્ટ ટિપ"
        )
    )

    // Curated high-yield tips in English for Class 12 Commerce
    val CURATED_TIPS_ENGLISH = mapOf(
        "all" to DailyStudyTip(
            subjectId = "all",
            subjectName = "Class 12 Commerce",
            headline = "The 90+ Blueprint: Daily Revision Rhythm",
            tipContent = "Divide your evening into two slots: 1 numerical subject (Accounts/Stats) for problem solving, and 1 theory subject (B.A./Eco/SP) for pointwise memorization. Always summarize key definitions in your own notes.",
            actionStep = "Review 3 key formulas from Accounts or Stats and write them down without looking.",
            language = "en",
            source = "Curated Board Tip"
        ),
        "accounts" to DailyStudyTip(
            subjectId = "accounts",
            subjectName = "Elements of Accounts",
            headline = "Partnership Accounts & Working Notes Step-Marks",
            tipContent = "In Gujarat Board valuation, working notes for Revaluation, Goodwill adjustment, and Partner's Capital Account carry up to 40% of question marks. Always draw neat debit/credit columns and brief narrations.",
            actionStep = "Solve 1 complete question on Revaluation of Assets & Liabilities from Partnership Accounts.",
            language = "en",
            source = "Curated Board Tip"
        ),
        "stat" to DailyStudyTip(
            subjectId = "stat",
            subjectName = "Statistics",
            headline = "Index Numbers & Karl Pearson Formula Selection",
            tipContent = "Before jumping into calculations, inspect the means of X and Y. If means are integers, use Σ(x - x̄)(y - ȳ). If fractional, use the UV shortcut method to prevent messy decimals and calculation errors.",
            actionStep = "Compute the correlation coefficient 'r' for one textbook illustration using the UV shortcut method.",
            language = "en",
            source = "Curated Board Tip"
        ),
        "economics" to DailyStudyTip(
            subjectId = "economics",
            subjectName = "Economics",
            headline = "Label Every Axis in Demand & Supply Curves",
            tipContent = "GSEB examiners strictly look for proper axis labelling: 'Price' on Y-axis and 'Quantity Demanded' on X-axis, with origin marked (O). Pair every 5-mark answer with a real-world Indian economy example.",
            actionStep = "Draw and label the Elasticity of Demand curve with all 5 degrees on a blank sheet of paper.",
            language = "en",
            source = "Curated Board Tip"
        ),
        "ba" to DailyStudyTip(
            subjectId = "ba",
            subjectName = "B.A. (Business Administration)",
            headline = "Structure 5-Mark Answers Point-by-Point",
            tipContent = "Avoid long unformatted paragraphs in Business Administration. Use the standard Board format: Meaning -> Official Definition -> Flowchart/Diagram -> Explanation of 5 points with subheadings -> Practical Example.",
            actionStep = "Memorize Fayol's 14 Principles using the acronym 'DAD U R SO CUTE' and write down the first 7.",
            language = "en",
            source = "Curated Board Tip"
        ),
        "sp_cc" to DailyStudyTip(
            subjectId = "sp_cc",
            subjectName = "SP & CC",
            headline = "Letter Writing Layout is 40% of Your Marks",
            tipContent = "In Commercial Correspondence, verify the 7 standard layout elements: Title/Header, Date, Inside Address, Salutation, Subject/Reference, Body paragraphs, and Complimentary Close with Signature.",
            actionStep = "Draft the skeleton layout of a bank complaint letter or share transfer letter without the body text.",
            language = "en",
            source = "Curated Board Tip"
        ),
        "english" to DailyStudyTip(
            subjectId = "english",
            subjectName = "English",
            headline = "Formal Report Writing & Active Vocabulary",
            tipContent = "Use passive voice and past tense when writing school or newspaper event reports (e.g., 'A blood donation camp was organized...'). For comprehension passages, answer in full grammatical sentences.",
            actionStep = "Write a 50-word opening paragraph for a report on 'Annual Sports Day Celebration' in formal tone.",
            language = "en",
            source = "Curated Board Tip"
        ),
        "gujarati" to DailyStudyTip(
            subjectId = "gujarati",
            subjectName = "Gujarati",
            headline = "Vyakaran & Nibandh Sub-Heading Strategy",
            tipContent = "Grammar (Jodani, Sandhi, Samas, Alankar, Chhand) is 100% scoring—practice 10 questions daily. In Essays, begin with a relevant couplet (Doha/Kavita pankti) to immediately impress the evaluator.",
            actionStep = "Practice identifying Samas (Karmadharay, Tatpurush, Dvigyu) for 5 textbook words.",
            language = "en",
            source = "Curated Board Tip"
        )
    )

    private val _selectedLanguage = MutableStateFlow(TipLanguage.GUJARATI)
    val selectedLanguage: StateFlow<TipLanguage> = _selectedLanguage.asStateFlow()

    private val _selectedSubjectId = MutableStateFlow("all")
    val selectedSubjectId: StateFlow<String> = _selectedSubjectId.asStateFlow()

    private val _activeTip = MutableStateFlow<DailyStudyTip?>(CURATED_TIPS_GUJARATI["all"])
    val activeTip: StateFlow<DailyStudyTip?> = _activeTip.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Cache key format: "languageCode:subjectId"
    private val tipsCache = mutableMapOf<String, DailyStudyTip>()

    init {
        // Pre-populate cache with curated defaults for both languages
        CURATED_TIPS_GUJARATI.forEach { (subId, tip) ->
            tipsCache["gu:$subId"] = tip
        }
        CURATED_TIPS_ENGLISH.forEach { (subId, tip) ->
            tipsCache["en:$subId"] = tip
        }
    }

    fun selectLanguage(language: TipLanguage) {
        _selectedLanguage.value = language
        updateActiveTipForCurrentSelection()
    }

    fun selectSubject(subjectId: String) {
        _selectedSubjectId.value = subjectId
        updateActiveTipForCurrentSelection()
    }

    private fun updateActiveTipForCurrentSelection() {
        val lang = _selectedLanguage.value
        val subId = _selectedSubjectId.value
        val cacheKey = "${lang.code}:$subId"

        val cached = tipsCache[cacheKey]
            ?: if (lang == TipLanguage.GUJARATI) {
                CURATED_TIPS_GUJARATI[subId] ?: CURATED_TIPS_GUJARATI["all"]
            } else {
                CURATED_TIPS_ENGLISH[subId] ?: CURATED_TIPS_ENGLISH["all"]
            }

        _activeTip.value = cached
    }

    private var fallbackRotationIndex = 0

    // Multiple curated fallback tips per subject for rotating when offline or refreshing without an API key
    val CURATED_POOL_GUJARATI: Map<String, List<DailyStudyTip>> = mapOf(
        "all" to listOf(
            CURATED_TIPS_GUJARATI["all"]!!,
            DailyStudyTip(
                subjectId = "all",
                subjectName = "ધોરણ ૧૨ કોમર્સ (GSEB)",
                headline = "બોર્ડ ઉત્તરવહી પ્રેઝન્ટેશન: અંડરલાઈન અને બ્લોક ડાયાગ્રામ પદ્ધતિ",
                tipContent = "પરીક્ષક સૌથી પહેલા તમારી રજૂઆત જુએ છે. તમામ હેડિંગ્સ નીચે પેન્સિલથી અંડરલાઈન કરો અને મહત્વના સૂત્રોને ચોરસ બોક્સમાં દર્શાવો. ઉત્તરવહીના દરેક પેજ પર માર્જિન છોડીને સાફ-સુથરું લખાણ રાખો.",
                actionStep = "કોઈપણ એક જૂના પ્રશ્નપત્રના ૧ સેક્શનનું મોડલ પેપર પ્રેઝન્ટેશન ૧૦ મિનિટમાં પ્લાન કરો.",
                language = "gu",
                source = "બોર્ડ એક્સપર્ટ ટિપ"
            ),
            DailyStudyTip(
                subjectId = "all",
                subjectName = "ધોરણ ૧૨ કોમર્સ (GSEB)",
                headline = "પોમોડોરો અભ્યાસ તકનીક: ૨૫ મિનિટ ફોકસ અને ૫ મિનિટ બ્રેક",
                tipContent = "સળંગ ૩ કલાક વાંચવા કરતાં ૨૫ મિનિટ સંપૂર્ણ એકાગ્રતાથી વાંચીને ૫ મિનિટ આંખોને આરામ આપો. ૪ સત્રો પછી ૨૦ મિનિટનો મોટો બ્રેક લો. આનાથી વાંચેલું ૮૦% વધુ સમય યાદ રહેશે.",
                actionStep = "આજ માટે ૨૫ મિનિટનો ૧ પોમોડોરો ટાઈમર સેટ કરી અઘરો ટોપિક વાંચવાનું શરૂ કરો.",
                language = "gu",
                source = "બોર્ડ એક્સપર્ટ ટિપ"
            )
        ),
        "accounts" to listOf(
            CURATED_TIPS_GUJARATI["accounts"]!!,
            DailyStudyTip(
                subjectId = "accounts",
                subjectName = "નામાનાં મૂળતત્ત્વો",
                headline = "વાર્ષિક હિસાબોમાં ગુપ્ત હવાલા (Hidden Adjustments) ની સાવચેતી",
                tipContent = "કાચા સરવૈયામાં તારીખ સાથે આપેલ લોન પર વ્યાજ, મકાન કે પેટન્ટ પર ઘસારો અને વીમા પ્રીમિયમ જેવા ગુપ્ત હવાલા બોર્ડમાં અચૂક પૂછાય છે. દાખલો શરૂ કરતા પહેલા આવા તમામ ગુપ્ત હવાલા ટીક કરી લેવા.",
                actionStep = "ભાગીદારી પેઢીના વાર્ષિક હિસાબમાંથી ૨ ગુપ્ત હવાલા શોધી તેમની બંને અસર લખો.",
                language = "gu",
                source = "બોર્ડ એક્સપર્ટ ટિપ"
            ),
            DailyStudyTip(
                subjectId = "accounts",
                subjectName = "નામાનાં મૂળતત્ત્વો",
                headline = "કંપનીના હિસાબોમાં શેર જપ્તી અને ડિબેન્ચર પરત આમનોંધ",
                tipContent = "શેર જપ્તી (Share Forfeiture) માં જપ્ત થયેલી રકમ હંમેશા શેર જપ્તી ખાતે જમા થાય છે. પ્રીમિયમ મળેલ ન હોય તો જ જામીનગીરી પ્રીમિયમ ઉધાર થાય. આ નિયમ યાદ રાખવાથી ૮ ગુણનો દાખલો સરળ બને છે.",
                actionStep = "શેર જપ્તી અને ફરી બહાર પાડવાની ૩ મૂળભૂત આમનોંધ ફટાફટ લખી લો.",
                language = "gu",
                source = "બોર્ડ એક્સપર્ટ ટિપ"
            )
        ),
        "stat" to listOf(
            CURATED_TIPS_GUJARATI["stat"]!!,
            DailyStudyTip(
                subjectId = "stat",
                subjectName = "આંકડાશાસ્ત્ર",
                headline = "નિયતસંબંધ રેખા અને નિશ્ચાયકતાનો આંક (R²) નું મહત્વ",
                tipContent = "નિયતસંબંધમાં y ની x પરની રેખાનું સમીકરણ ŷ = a + bx લખ્યા પછી આપેલ x ની કિંમત માટે અનુમાન મૂકવાનું ભૂલશો નહીં. ત્રુટી e = y - ŷ ની ગણતરી બોર્ડમાં અવારનવાર ૧ માર્કમાં પૂછાય છે.",
                actionStep = "ટેક્સ્ટબુકમાંથી નિયતસંબંધ રેખા પરથી અનુમાન મેળવતો ૧ દાખલો કેલ્ક્યુલેટરથી ઉકેલો.",
                language = "gu",
                source = "બોર્ડ એક્સપર્ટ ટિપ"
            ),
            DailyStudyTip(
                subjectId = "stat",
                subjectName = "આંકડાશાસ્ત્ર",
                headline = "સામયિક શ્રેણીમાં ચલિત સરેરાશ પદ્ધતિના રોકડા ૫ માર્ક્સ",
                tipContent = "૩-વર્ષીય, ૪-વર્ષીય કે ૫-વર્ષીય ચલિત સરેરાશમાં સરવાળો સાચી લાઈનમાં (ખાસ કરીને ૪-માસિકમાં બે લાઈનની વચ્ચે) લખવાની કાળજી રાખો. આ રીતમાં પૂર્ણ માર્ક્સ લાવવા સૌથી સહેલા છે.",
                actionStep = "૪-માસિક ચલિત સરેરાશનું કોષ્ટક બનાવી પ્રથમ ૪ સરવાળા જાતે ગણો.",
                language = "gu",
                source = "બોર્ડ એક્સપર્ટ ટિપ"
            )
        ),
        "economics" to listOf(
            CURATED_TIPS_GUJARATI["economics"]!!,
            DailyStudyTip(
                subjectId = "economics",
                subjectName = "અર્થશાસ્ત્ર",
                headline = "નાણાકીય નીતિના પરિમાણાત્મક અને ગુણાત્મક સાધનો",
                tipContent = "RBI ના પરિમાણાત્મક સાધનો (બેંક રેટ, રેપો રેટ, રિવર્સ રેપો રેટ, CRR, SLR) ના આંકડા અને અર્થ પાક્કા કરો. મુદ્દાસર યાદ રાખવા માટે 'BR-RR-CRR-SLR' શોર્ટ કોડ વાપરો.",
                actionStep = "રેપો રેટ અને રિવર્સ રેપો રેટ વચ્ચેનો તફાવત ૩ મુદ્દામાં લખો.",
                language = "gu",
                source = "બોર્ડ એક્સપર્ટ ટિપ"
            ),
            DailyStudyTip(
                subjectId = "economics",
                subjectName = "અર્થશાસ્ત્ર",
                headline = "ગરીબી અને બેરોજગારીના પ્રકારો આકૃતિ સાથે સમજાવો",
                tipContent = "પ્રચ્છન્ન બેરોજગારી, ચક્રીય બેરોજગારી અને ઘર્ષણજન્ય બેરોજગારીના ઉદાહરણો ભારતીય કૃષિ અને ઉદ્યોગ સંદર્ભે આપો. આંકડાકીય માહિતી દર્શાવવા નાનો ચાર્ટ દોરો.",
                actionStep = "બેરોજગારીના ૫ મુખ્ય પ્રકારોના નામ અને ૧-૧ ઉદાહરણ ઝડપથી નોટ કરો.",
                language = "gu",
                source = "બોર્ડ એક્સપર્ટ ટિપ"
            )
        ),
        "ba" to listOf(
            CURATED_TIPS_GUJARATI["ba"]!!,
            DailyStudyTip(
                subjectId = "ba",
                subjectName = "વાણિજ્ય વ્યવસ્થા અને સંચાલન",
                headline = "આયોજન અને અંકુશ: એક સિક્કાની બે બાજુઓ",
                tipContent = "આયોજન ભવિષ્ય માટે છે અને અંકુશ ભૂતકાળનું મૂલ્યાંકન કરે છે. અંકુશ વિના આયોજન પાંગળું છે. આ વિધાન સમજાવવા પહેલા આયોજનનો અર્થ, અંકુશનો અર્થ અને બંને વચ્ચેનો સંબંધ ફ્લોચાર્ટમાં દર્શાવો.",
                actionStep = "આયોજન અને અંકુશ વચ્ચેના સંબંધની ૨ મિનિટની સમરી નોટબુકમાં લખો.",
                language = "gu",
                source = "બોર્ડ એક્સપર્ટ ટિપ"
            ),
            DailyStudyTip(
                subjectId = "ba",
                subjectName = "વાણિજ્ય વ્યવસ્થા અને સંચાલન",
                headline = "નાણાકીય બજાર: શેરબજાર અને સેબી (SEBI) ના કાર્યો",
                tipContent = "ડિપોઝિટરી (NSDL, CDSL) અને ડીમેટ ખાતાની કામગીરી ૫ ગુણમાં પૂછાઈ શકે છે. સેબીના રક્ષણાત્મક અને નિયમનકારી કાર્યોને અલગ-અલગ મથાળા હેઠળ રજૂ કરો.",
                actionStep = "સેબીના ૩ રક્ષણાત્મક કાર્યો મુદ્દાસર યાદ કરી લખો.",
                language = "gu",
                source = "બોર્ડ એક્સપર્ટ ટિપ"
            )
        ),
        "sp_cc" to listOf(
            CURATED_TIPS_GUJARATI["sp_cc"]!!,
            DailyStudyTip(
                subjectId = "sp_cc",
                subjectName = "એસ.પી. અને સી.સી.",
                headline = "ડિબેન્ચર ટ્રસ્ટ ડીડ અને કંપની સેક્રેટરીની ફરજો",
                tipContent = "ડિબેન્ચર બહાર પાડતી વખતે ટ્રસ્ટીઓની નિમણૂક અને ટ્રસ્ટ ડીડની જોગવાઈઓ કાયદાકીય છે. સેક્રેટરીએ કંપની ધારા ૨૦૧૩ ની કલમો મુજબ કઈ નોટિસ મોકલવી તેની વિગત ક્રમબદ્ધ આપો.",
                actionStep = "ડિબેન્ચર હોલ્ડર્સની સભા બોલાવવાની નોટિસનું ફોર્મેટ તૈયાર કરો.",
                language = "gu",
                source = "બોર્ડ એક્સપર્ટ ટિપ"
            )
        ),
        "english" to listOf(
            CURATED_TIPS_GUJARATI["english"]!!,
            DailyStudyTip(
                subjectId = "english",
                subjectName = "અંગ્રેજી (English)",
                headline = "ઇમેઇલ રાઇટિંગ (Email Writing) નું સાચું ફોર્મેટ",
                tipContent = "To, CC, BCC, Subject અને Salutation નું યોગ્ય સ્થાન રાખો. Formal ઈમેઇલમાં ટૂંકા શબ્દો (U, ASAP) નો ઉપયોગ ન કરવો. અંતમાં 'Warm regards' અને સહી અવશ્ય મૂકો.",
                actionStep = "પુસ્તકો મંગાવવા માટે પબ્લિશરને ૫ વાક્યોનો ઔપચારિક ઈમેઈલ ડ્રાફ્ટ કરો.",
                language = "gu",
                source = "બોર્ડ એક્સપર્ટ ટિપ"
            )
        ),
        "gujarati" to listOf(
            CURATED_TIPS_GUJARATI["gujarati"]!!,
            DailyStudyTip(
                subjectId = "gujarati",
                subjectName = "ગુજરાતી (પ્રથમ ભાષા)",
                headline = "ગદ્ય-સમીક્ષા અને સંક્ષેપીકરણમાં ૧/૩ ભાગનો નિયમ",
                tipContent = "આપેલ ફકરાના કુલ શબ્દો ગણીને તેના આશરે ત્રીજા ભાગમાં સંક્ષેપ કરો. મૂળ ફકરાના ઉદાહરણો કે અલંકારો કાઢી નાખો અને પોતાના શબ્દોમાં કેન્દ્રીય વિચાર લખી યોગ્ય શીર્ષક આપો.",
                actionStep = "કોઈપણ એક પાઠના ફકરાનું ૧/૩ ભાગમાં સંક્ષેપીકરણ કરી યોગ્ય શીર્ષક આપો.",
                language = "gu",
                source = "બોર્ડ એક્સપર્ટ ટિપ"
            )
        )
    )

    val CURATED_POOL_ENGLISH: Map<String, List<DailyStudyTip>> = mapOf(
        "all" to listOf(
            CURATED_TIPS_ENGLISH["all"]!!,
            DailyStudyTip(
                subjectId = "all",
                subjectName = "Class 12 Commerce",
                headline = "Board Presentation: Underlines and Flowcharts",
                tipContent = "Valuators appreciate structured papers. Underline key headings with a pencil and enclose final numerical answers in neat rectangles. Always start every 5-mark answer on a fresh page.",
                actionStep = "Plan a mock answer layout for one 5-mark question in 10 minutes.",
                language = "en",
                source = "Curated Board Tip"
            )
        ),
        "accounts" to listOf(
            CURATED_TIPS_ENGLISH["accounts"]!!,
            DailyStudyTip(
                subjectId = "accounts",
                subjectName = "Elements of Accounts",
                headline = "Watch out for Hidden Adjustments in Final Accounts",
                tipContent = "Trial balance dates for loans, depreciation rates on leasehold property, and prepaid insurance are hidden adjustments. Spot and mark them before balancing ledgers.",
                actionStep = "Identify 2 hidden adjustments from a Partnership Final Accounts problem and write both entries.",
                language = "en",
                source = "Curated Board Tip"
            )
        ),
        "stat" to listOf(
            CURATED_TIPS_ENGLISH["stat"]!!,
            DailyStudyTip(
                subjectId = "stat",
                subjectName = "Statistics",
                headline = "Moving Average Method: Easy Full Marks",
                tipContent = "In 4-month or 4-quarterly moving averages, center the totals between lines carefully. Double-check calculator inputs to ensure cumulative accuracy.",
                actionStep = "Compute the first 3 lines of a 4-year moving average table right now.",
                language = "en",
                source = "Curated Board Tip"
            )
        ),
        "economics" to listOf(
            CURATED_TIPS_ENGLISH["economics"]!!,
            DailyStudyTip(
                subjectId = "economics",
                subjectName = "Economics",
                headline = "Monetary Policy Instruments: Quantitative vs Qualitative",
                tipContent = "Memorize the key tools of RBI: Bank Rate, Repo Rate, Reverse Repo Rate, CRR, and SLR. Provide current real-world Indian banking examples in your answers.",
                actionStep = "Summarize the difference between Repo Rate and Reverse Repo Rate in 3 concise bullet points.",
                language = "en",
                source = "Curated Board Tip"
            )
        ),
        "ba" to listOf(
            CURATED_TIPS_ENGLISH["ba"]!!,
            DailyStudyTip(
                subjectId = "ba",
                subjectName = "Business Administration",
                headline = "Planning & Controlling: Two Sides of a Coin",
                tipContent = "Planning without controlling is blind, and controlling without planning is meaningless. Use a flowchart connecting Planning -> Organizing -> Staffing -> Directing -> Controlling.",
                actionStep = "Draw a clean diagram showing the linkage between planning and controlling.",
                language = "en",
                source = "Curated Board Tip"
            )
        ),
        "sp_cc" to listOf(
            CURATED_TIPS_ENGLISH["sp_cc"]!!,
            DailyStudyTip(
                subjectId = "sp_cc",
                subjectName = "SP & CC",
                headline = "Debenture Trust Deed & Statutory Duties",
                tipContent = "Highlight the appointment of trustees and legal clauses of the Trust Deed under Companies Act 2013 to maximize score in Section E questions.",
                actionStep = "Draft the layout of a notice for a Debenture Holders Meeting.",
                language = "en",
                source = "Curated Board Tip"
            )
        ),
        "english" to listOf(
            CURATED_TIPS_ENGLISH["english"]!!,
            DailyStudyTip(
                subjectId = "english",
                subjectName = "English",
                headline = "Formal Email Writing Structure",
                tipContent = "Ensure clear To, Subject, Formal Salutation, concise body paragraphs, and professional sign-off (Warm regards). Avoid abbreviations in formal correspondence.",
                actionStep = "Draft a 5-sentence formal email ordering books from a publisher.",
                language = "en",
                source = "Curated Board Tip"
            )
        ),
        "gujarati" to listOf(
            CURATED_TIPS_ENGLISH["gujarati"]!!,
            DailyStudyTip(
                subjectId = "gujarati",
                subjectName = "Gujarati First Language",
                headline = "Précis Writing 1/3rd Rule",
                tipContent = "Count the words of the given passage and write the summary in approximately one-third of the length. Capture the central thought and give a suitable title.",
                actionStep = "Practice summarizing any one textbook paragraph into 1/3rd length with a title.",
                language = "en",
                source = "Curated Board Tip"
            )
        )
    )

    fun refreshTip(
        subjectId: String = _selectedSubjectId.value,
        language: TipLanguage = _selectedLanguage.value
    ) {
        scope.launch {
            fetchTipFromGemini(subjectId, language)
        }
    }

    private suspend fun fetchTipFromGemini(
        subjectId: String,
        language: TipLanguage
    ) = withContext(Dispatchers.IO) {
        _isLoading.value = true
        _errorMessage.value = null

        val subject = Subject.OFFICIAL_SUBJECTS.find { it.id == subjectId }
        val englishSubjectName = if (subjectId == "all") "Gujarat Board Class 12 Commerce General Preparation" else subject?.name ?: "Class 12 Commerce"
        val gujaratiSubjectName = getGujaratiSubjectName(subjectId)

        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }

        val isKeyValid = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"

        if (!isKeyValid) {
            Log.d(TAG, "Gemini API key is placeholder or empty. Rotating curated Board expert tip.")
            fallbackRotationIndex++
            val pool = if (language == TipLanguage.GUJARATI) {
                CURATED_POOL_GUJARATI[subjectId] ?: listOf(CURATED_TIPS_GUJARATI[subjectId] ?: CURATED_TIPS_GUJARATI["all"]!!)
            } else {
                CURATED_POOL_ENGLISH[subjectId] ?: listOf(CURATED_TIPS_ENGLISH[subjectId] ?: CURATED_TIPS_ENGLISH["all"]!!)
            }
            val fallback = pool[fallbackRotationIndex % pool.size]
            val liveNoticeTip = fallback.copy(
                source = if (language == TipLanguage.GUJARATI) "ગુજરાત બોર્ડ એક્સપર્ટ ટિપ (લાઈવ AI માટે કી સેટ કરો)" else "Curated Board Tip (Add API Key for live AI)",
                timestampEpochMillis = System.currentTimeMillis()
            )
            val cacheKey = "${language.code}:$subjectId"
            tipsCache[cacheKey] = liveNoticeTip
            _activeTip.value = liveNoticeTip
            _isLoading.value = false
            return@withContext
        }

        try {
            val url = "$API_BASE_URL/$MODEL_NAME:generateContent?key=$apiKey"
            val randomSeed = System.currentTimeMillis() % 100000

            val prompt = if (language == TipLanguage.GUJARATI) {
                """
                You are an elite Gujarat Secondary and Higher Secondary Education Board (GSEB) Class 12 Commerce exam expert tutor helping students score 90+ marks.
                Provide ONE FRESH, HIGH-YIELD, SPECIFIC daily study tip in the GUJARATI LANGUAGE (ગુજરાતી ભાષામાં) for Class 12 Commerce students for the subject: '$englishSubjectName' ($gujaratiSubjectName).
                Target syllabus: Gujarat Board Class 12 Commerce (નામાનાં મૂળતત્ત્વો, આંકડાશાસ્ત્ર, અર્થશાસ્ત્ર, વાણિજ્ય વ્યવસ્થા, એસ.પી., વગેરે).
                Freshness Request Seed: $randomSeed. Provide a novel tip covering a specific chapter formula, paper-presentation method, or common pitfall to avoid.

                CRITICAL REQUIREMENT:
                Every single field ('headline', 'tipContent', 'actionStep') MUST be written in natural, fluent GUJARATI script (ગુજરાતી લિપિમાં), using official GSEB Gujarati commerce terminology.

                Respond ONLY with a valid JSON object with the following fields:
                {
                  "headline": "A catchy 4 to 8 word actionable title in Gujarati (દા.ત., ભાગીદારી ખાતામાં વર્કિંગ નોટ્સના નિયમો)",
                  "tipContent": "2 to 3 sentences in Gujarati explaining a high-yield concept, paper presentation strategy, or common board exam mistake to avoid.",
                  "actionStep": "One concrete 10-minute exercise in Gujarati that the student can do right now to practice this."
                }
                Do not include markdown code blocks (e.g. ```json).
                """.trimIndent()
            } else {
                """
                You are an elite Gujarat Board (GSEB) Class 12 Commerce exam expert tutor helping students score 90+ marks.
                Provide ONE high-yield, specific daily study tip in English for the subject: '$englishSubjectName'.
                Target syllabus: Class 12 Commerce (GSEB/CBSE standards).

                Respond ONLY with a valid JSON object with the following fields:
                {
                  "headline": "A catchy 4 to 7 word actionable title",
                  "tipContent": "2 to 3 sentences explaining a high-yield concept, paper presentation strategy, or common student mistake to avoid.",
                  "actionStep": "One concrete 10-minute exercise or task the student can do right now to practice this."
                }
                Do not include markdown code blocks (e.g. ```json).
                """.trimIndent()
            }

            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            val partObj = JSONObject().apply {
                                put("text", prompt)
                            }
                            put(partObj)
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)

                val generationConfig = JSONObject().apply {
                    put("temperature", 0.7)
                    put("responseMimeType", "application/json")
                }
                put("generationConfig", generationConfig)
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody.isNullOrBlank()) {
                Log.w(TAG, "Gemini API call failed with code: ${response.code}")
                val fallback = if (language == TipLanguage.GUJARATI) {
                    CURATED_TIPS_GUJARATI[subjectId] ?: CURATED_TIPS_GUJARATI["all"]!!
                } else {
                    CURATED_TIPS_ENGLISH[subjectId] ?: CURATED_TIPS_ENGLISH["all"]!!
                }
                _activeTip.value = fallback
                _errorMessage.value = if (language == TipLanguage.GUJARATI) {
                    "જેમિની AI સેવા હાલ અનુપલબ્ધ છે (${response.code}). બોર્ડ એક્સપર્ટ ટિપ દર્શાવી રહ્યા છીએ."
                } else {
                    "Gemini service temporarily unavailable (${response.code}). Showing Board tip."
                }
                _isLoading.value = false
                return@withContext
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text")

            if (text.isNullOrBlank()) {
                val fallback = if (language == TipLanguage.GUJARATI) {
                    CURATED_TIPS_GUJARATI[subjectId] ?: CURATED_TIPS_GUJARATI["all"]!!
                } else {
                    CURATED_TIPS_ENGLISH[subjectId] ?: CURATED_TIPS_ENGLISH["all"]!!
                }
                _activeTip.value = fallback
                _isLoading.value = false
                return@withContext
            }

            // Clean any potential markdown wrapping
            val cleanedJson = text.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val parsedTipJson = JSONObject(cleanedJson)
            val defaultHeadline = if (language == TipLanguage.GUJARATI) "સ્માર્ટ પરીક્ષા વ્યૂહરચના" else "Smart Study Strategy"
            val defaultContent = if (language == TipLanguage.GUJARATI) "મુખ્ય પ્રકરણોનું પુનરાવર્તન કરો અને નિયમિત દાખલાઓની પ્રેક્ટિસ કરો." else "Review key chapters and practice illustrations regularly."
            val defaultAction = if (language == TipLanguage.GUJARATI) "બોર્ડ પરીક્ષાનો ૧ જૂનો પ્રશ્ન સોલ્વ કરવા માટે ૧૦ મિનિટ ફાળવો." else "Spend 10 minutes solving 1 previous board exam question."

            val headline = parsedTipJson.optString("headline", defaultHeadline)
            val tipContent = parsedTipJson.optString("tipContent", defaultContent)
            val actionStep = parsedTipJson.optString("actionStep", defaultAction)

            val displaySubName = if (language == TipLanguage.GUJARATI) gujaratiSubjectName else englishSubjectName

            val newTip = DailyStudyTip(
                subjectId = subjectId,
                subjectName = displaySubName,
                headline = headline,
                tipContent = tipContent,
                actionStep = actionStep,
                language = language.code,
                source = if (language == TipLanguage.GUJARATI) "Gemini 3.5 Flash (ગુજરાતી)" else "Gemini 3.5 Flash",
                timestampEpochMillis = System.currentTimeMillis()
            )

            val cacheKey = "${language.code}:$subjectId"
            tipsCache[cacheKey] = newTip
            _activeTip.value = newTip
            _errorMessage.value = null
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Gemini tip: ${e.message}", e)
            fallbackRotationIndex++
            val pool = if (language == TipLanguage.GUJARATI) {
                CURATED_POOL_GUJARATI[subjectId] ?: listOf(CURATED_TIPS_GUJARATI[subjectId] ?: CURATED_TIPS_GUJARATI["all"]!!)
            } else {
                CURATED_POOL_ENGLISH[subjectId] ?: listOf(CURATED_TIPS_ENGLISH[subjectId] ?: CURATED_TIPS_ENGLISH["all"]!!)
            }
            val fallback = pool[fallbackRotationIndex % pool.size]
            _activeTip.value = fallback
            tipsCache["${language.code}:$subjectId"] = fallback
            _errorMessage.value = if (language == TipLanguage.GUJARATI) {
                "જેમિની AI સાથે કનેક્ટ કરવામાં સમસ્યા. બોર્ડ એક્સપર્ટ ટિપ દર્શાવી રહ્યા છીએ."
            } else {
                "Unable to connect to Gemini. Showing curated Board tip."
            }
        }
    }

    suspend fun fetchAiChatResponse(userMessage: String): String = withContext(Dispatchers.IO) {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (_: Exception) { "" }
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "I'm your Nexora AI study coach. Set your API key in AI Studio to enable live AI chat!"
        }

        try {
            val url = "$API_BASE_URL/$MODEL_NAME:generateContent?key=$apiKey"
            val prompt = """
                You are 'Nexora AI', an elite study coach for GSEB Class 12 Commerce students in Gujarat.
                Your mission is to help them score 90+ marks. 
                Answer the student's question concisely, encouragingly, and with specific academic advice if possible.
                Use a futuristic, professional, and friendly tone.
                
                Student says: "$userMessage"
                
                Respond in the language the student used (Gujarati or English).
                Keep the response under 100 words.
            """.trimIndent()

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().put(JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().apply { put("text", prompt) }))
                }))
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(requestBody).build()
            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext "I'm processing your request. Please try again."
            
            val json = JSONObject(responseBody)
            val candidates = json.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val candidate = candidates.getJSONObject(0)
                val content = candidate.optJSONObject("content")
                if (content != null) {
                    val parts = content.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text")
                    }
                }
            }
            "I'm here to support your study goals. How can I help you today?"
        } catch (e: Exception) {
            "I'm focusing on your study goals. How can I help you with your Class 12 Commerce prep today?"
        }
    }
}
