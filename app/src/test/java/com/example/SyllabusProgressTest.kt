package com.example

import com.example.core.model.Subject
import com.example.core.repository.SyllabusRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SyllabusProgressTest {

    @Before
    fun setup() {
        SyllabusRepository.resetAllCompletions()
    }

    @Test
    fun testAllSevenSubjectsPresent() {
        val subjects = Subject.OFFICIAL_SUBJECTS
        assertEquals(7, subjects.size)

        val subjectIds = subjects.map { it.id }.toSet()
        assertTrue(subjectIds.contains("gujarati"))
        assertTrue(subjectIds.contains("english"))
        assertTrue(subjectIds.contains("sp_cc"))
        assertTrue(subjectIds.contains("ba"))
        assertTrue(subjectIds.contains("stat"))
        assertTrue(subjectIds.contains("accounts"))
        assertTrue(subjectIds.contains("economics"))
    }

    @Test
    fun testSubjectProgressCalculations() {
        assertEquals(0f, SyllabusRepository.getOverallProgress(), 0.001f)

        // Complete 1 Gujarati chapter
        val gujChapter = SyllabusRepository.getChapters("gujarati").first()
        SyllabusRepository.setChapterCompleted(gujChapter.id, true)

        val gujTotal = SyllabusRepository.getChapters("gujarati").size
        val gujProgress = SyllabusRepository.getSubjectProgress("gujarati")
        assertEquals(1f / gujTotal, gujProgress, 0.001f)

        assertTrue(SyllabusRepository.getOverallProgress() > 0f)
    }

    @Test
    fun testTotalChaptersCount() {
        val total = SyllabusRepository.getTotalChaptersCount()
        assertTrue(total > 0)
        assertEquals(94, total)
    }
}
