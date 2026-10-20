package com.example

import com.example.core.model.Subject
import com.example.core.repository.DailyStudyTipsRepository
import com.example.core.repository.TipLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for DailyStudyTipsRepository with Class 12 Commerce Gujarati & English tips.
 */
class DailyStudyTipsTest {

    @Test
    fun testDefaultTipIsLoadedInGujarati() {
        DailyStudyTipsRepository.selectLanguage(TipLanguage.GUJARATI)
        DailyStudyTipsRepository.selectSubject("all")
        val defaultTip = DailyStudyTipsRepository.activeTip.value
        assertNotNull("Default tip should be available on initialization", defaultTip)
        assertEquals("all", defaultTip?.subjectId)
        assertEquals("gu", defaultTip?.language)
        assertTrue("Headline must not be blank", defaultTip?.headline?.isNotBlank() == true)
        assertTrue("Headline must contain Gujarati or 90+", defaultTip?.headline?.contains("૯૦+") == true || defaultTip?.headline?.contains("90+") == true)
        assertTrue("Tip content must not be blank", defaultTip?.tipContent?.isNotBlank() == true)
        assertTrue("Action step must not be blank", defaultTip?.actionStep?.isNotBlank() == true)
    }

    @Test
    fun testAllSevenCommerceSubjectsHaveGujaratiTips() {
        DailyStudyTipsRepository.selectLanguage(TipLanguage.GUJARATI)
        val subjectIds = Subject.OFFICIAL_SUBJECTS.map { it.id }
        assertEquals(7, subjectIds.size)

        for (subjectId in subjectIds) {
            DailyStudyTipsRepository.selectSubject(subjectId)
            val tip = DailyStudyTipsRepository.activeTip.value
            assertNotNull("Tip for subject $subjectId should exist", tip)
            assertEquals("Selected subject should match tip subjectId", subjectId, tip?.subjectId)
            assertEquals("Language should be Gujarati", "gu", tip?.language)
            assertTrue("Headline for $subjectId must be non-empty", tip?.headline?.isNotBlank() == true)
            assertTrue("Tip content for $subjectId must be non-empty", tip?.tipContent?.isNotBlank() == true)
            assertTrue("Action step for $subjectId must be non-empty", tip?.actionStep?.isNotBlank() == true)
        }
    }

    @Test
    fun testLanguageToggleToEnglishAndBack() {
        // Switch to English
        DailyStudyTipsRepository.selectLanguage(TipLanguage.ENGLISH)
        DailyStudyTipsRepository.selectSubject("accounts")
        val englishTip = DailyStudyTipsRepository.activeTip.value
        assertEquals("en", englishTip?.language)
        assertTrue("English tip headline should contain Partnership or accounts", englishTip?.headline?.contains("Partnership") == true)

        // Switch back to Gujarati
        DailyStudyTipsRepository.selectLanguage(TipLanguage.GUJARATI)
        val gujaratiTip = DailyStudyTipsRepository.activeTip.value
        assertEquals("gu", gujaratiTip?.language)
        assertTrue("Gujarati tip headline should be in Gujarati", gujaratiTip?.headline?.contains("ભાગીદારી") == true)
    }

    @Test
    fun testSwitchingBackToAllSubjects() {
        DailyStudyTipsRepository.selectSubject("accounts")
        assertEquals("accounts", DailyStudyTipsRepository.selectedSubjectId.value)

        DailyStudyTipsRepository.selectSubject("all")
        assertEquals("all", DailyStudyTipsRepository.selectedSubjectId.value)
        val tip = DailyStudyTipsRepository.activeTip.value
        assertEquals("all", tip?.subjectId)
        assertTrue(tip?.headline?.isNotBlank() == true)
    }

    @Test
    fun testManualRefreshFunctionality() {
        DailyStudyTipsRepository.selectLanguage(TipLanguage.GUJARATI)
        DailyStudyTipsRepository.selectSubject("accounts")
        val initialTip = DailyStudyTipsRepository.activeTip.value
        assertNotNull("Initial tip should not be null", initialTip)

        // Trigger manual refresh
        DailyStudyTipsRepository.refreshTip("accounts", TipLanguage.GUJARATI)
        
        // Give background dispatcher moment to execute
        Thread.sleep(150)
        val refreshedTip = DailyStudyTipsRepository.activeTip.value
        assertNotNull("Refreshed tip should exist", refreshedTip)
        assertEquals("accounts", refreshedTip?.subjectId)
        assertEquals("gu", refreshedTip?.language)
    }

    @Test
    fun testFavoriteTipEntityConversion() {
        val originalTip = com.example.core.model.DailyStudyTip(
            id = "test-tip-1",
            subjectId = "accounts",
            subjectName = "નામાનાં મૂળતત્ત્વો",
            headline = "ભાગીદારી પુનર્ગઠન શોર્ટકટ",
            tipContent = "નફા-નુકસાન પ્રમાણ ફેરફાર ગણતરી.",
            actionStep = "સ્વાધ્યાય દાખલો ૫ ગણો.",
            language = "gu",
            source = "Gemini AI"
        )

        val entity = com.example.core.database.entity.FavoriteTipEntity.fromDailyStudyTip(originalTip)
        assertEquals(originalTip.id, entity.id)
        assertEquals(originalTip.subjectId, entity.subjectId)
        assertEquals(originalTip.headline, entity.headline)
        assertEquals(originalTip.tipContent, entity.tipContent)
        assertEquals(originalTip.actionStep, entity.actionStep)
        assertEquals(originalTip.language, entity.language)

        val reconstructedTip = entity.toDailyStudyTip()
        assertEquals(originalTip.id, reconstructedTip.id)
        assertEquals(originalTip.headline, reconstructedTip.headline)
        assertEquals(originalTip.actionStep, reconstructedTip.actionStep)
    }

    @Test
    fun testFavoriteTipsRepositoryWithFakeDao() = kotlinx.coroutines.runBlocking {
        val fakeDao = object : com.example.core.database.dao.FavoriteTipDao {
            val list = mutableListOf<com.example.core.database.entity.FavoriteTipEntity>()
            val stateFlow = kotlinx.coroutines.flow.MutableStateFlow<List<com.example.core.database.entity.FavoriteTipEntity>>(emptyList())

            override fun getAllFavoriteTips(): kotlinx.coroutines.flow.Flow<List<com.example.core.database.entity.FavoriteTipEntity>> = stateFlow
            override fun isTipFavorite(id: String, headline: String): kotlinx.coroutines.flow.Flow<Boolean> =
                kotlinx.coroutines.flow.flowOf(list.any { it.id == id || it.headline == headline })
            override fun isFavoriteById(id: String): kotlinx.coroutines.flow.Flow<Boolean> =
                kotlinx.coroutines.flow.flowOf(list.any { it.id == id })
            override suspend fun insertFavoriteTip(tip: com.example.core.database.entity.FavoriteTipEntity) {
                list.removeAll { it.id == tip.id }
                list.add(tip)
                stateFlow.value = list.toList()
            }
            override suspend fun deleteFavorite(id: String, headline: String) {
                list.removeAll { it.id == id || it.headline == headline }
                stateFlow.value = list.toList()
            }
            override suspend fun deleteFavoriteById(id: String) {
                list.removeAll { it.id == id }
                stateFlow.value = list.toList()
            }
            override suspend fun clearAll() {
                list.clear()
                stateFlow.value = emptyList()
            }
        }

        com.example.core.repository.FavoriteTipsRepository.initializeWithDao(fakeDao)

        val sampleTip = com.example.core.model.DailyStudyTip(
            id = "fav-101",
            subjectId = "stat",
            subjectName = "આંકડાશાસ્ત્ર",
            headline = "સૂચક આંક સૂત્રોની સરખામણી",
            tipContent = "લાસ્પેયર અને પાસેના સૂત્રોની સરખામણી.",
            actionStep = "ફિશરના આદર્શ સૂચકઆંકનો ૧ દાખલો ગણો."
        )

        // Save favorite
        val added = com.example.core.repository.FavoriteTipsRepository.toggleFavorite(sampleTip, currentlyFavorite = false)
        assertTrue("Should return true when newly favorited", added)
        assertEquals(1, fakeDao.list.size)
        assertEquals("fav-101", fakeDao.list[0].id)

        // Remove favorite
        val removed = com.example.core.repository.FavoriteTipsRepository.toggleFavorite(sampleTip, currentlyFavorite = true)
        org.junit.Assert.assertFalse("Should return false when unfavorited", removed)
        assertEquals(0, fakeDao.list.size)
    }

    @Test
    fun testFilterTipsBySpecificSubjectsAccountancyEconomicsBusinessStudies() {
        // 1. Accountancy Toggle (Gujarati)
        DailyStudyTipsRepository.selectLanguage(TipLanguage.GUJARATI)
        DailyStudyTipsRepository.selectSubject("accounts")
        assertEquals("accounts", DailyStudyTipsRepository.selectedSubjectId.value)
        val accountsTipGu = DailyStudyTipsRepository.activeTip.value
        assertNotNull(accountsTipGu)
        assertEquals("accounts", accountsTipGu?.subjectId)
        assertTrue(accountsTipGu?.headline?.contains("પુનઃમૂલ્યાંકન") == true || accountsTipGu?.headline?.contains("ભાગીદારી") == true)

        // Accountancy Toggle (English)
        DailyStudyTipsRepository.selectLanguage(TipLanguage.ENGLISH)
        val accountsTipEn = DailyStudyTipsRepository.activeTip.value
        assertNotNull(accountsTipEn)
        assertEquals("accounts", accountsTipEn?.subjectId)
        assertTrue(accountsTipEn?.headline?.contains("Partnership") == true || accountsTipEn?.headline?.contains("Accounts") == true)

        // 2. Economics Toggle (English)
        DailyStudyTipsRepository.selectSubject("economics")
        assertEquals("economics", DailyStudyTipsRepository.selectedSubjectId.value)
        val ecoTipEn = DailyStudyTipsRepository.activeTip.value
        assertNotNull(ecoTipEn)
        assertEquals("economics", ecoTipEn?.subjectId)
        assertTrue(ecoTipEn?.headline?.contains("Demand") == true || ecoTipEn?.tipContent?.contains("Economics") == true || ecoTipEn?.headline?.contains("Axis") == true)

        // Economics Toggle (Gujarati)
        DailyStudyTipsRepository.selectLanguage(TipLanguage.GUJARATI)
        val ecoTipGu = DailyStudyTipsRepository.activeTip.value
        assertNotNull(ecoTipGu)
        assertEquals("economics", ecoTipGu?.subjectId)
        assertTrue(ecoTipGu?.headline?.contains("અર્થશાસ્ત્ર") == true || ecoTipGu?.headline?.contains("આલેખ") == true)

        // 3. Business Studies (B.A.) Toggle (Gujarati)
        DailyStudyTipsRepository.selectSubject("ba")
        assertEquals("ba", DailyStudyTipsRepository.selectedSubjectId.value)
        val baTipGu = DailyStudyTipsRepository.activeTip.value
        assertNotNull(baTipGu)
        assertEquals("ba", baTipGu?.subjectId)
        assertTrue(baTipGu?.headline?.contains("જવાબો") == true || baTipGu?.tipContent?.contains("વાણિજ્ય") == true)

        // Business Studies (B.A.) Toggle (English)
        DailyStudyTipsRepository.selectLanguage(TipLanguage.ENGLISH)
        val baTipEn = DailyStudyTipsRepository.activeTip.value
        assertNotNull(baTipEn)
        assertEquals("ba", baTipEn?.subjectId)
        assertTrue(baTipEn?.headline?.contains("Point-by-Point") == true || baTipEn?.tipContent?.contains("Business") == true)

        // 4. Reset back to All Subjects
        DailyStudyTipsRepository.selectSubject("all")
        assertEquals("all", DailyStudyTipsRepository.selectedSubjectId.value)
        val allTip = DailyStudyTipsRepository.activeTip.value
        assertNotNull(allTip)
        assertEquals("all", allTip?.subjectId)
    }
}

