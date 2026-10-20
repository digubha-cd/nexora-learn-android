package com.example

import org.junit.Assert.*
import org.junit.Test
import com.example.core.util.StudentIdGenerator

/**
 * Unit test to verify project and student utilities.
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testStudentIdGenerator() {
    val candidate = StudentIdGenerator.generateCandidateId()
    assertTrue("Candidate ID should be valid", StudentIdGenerator.isValidStudentId(candidate))
    val normalized = StudentIdGenerator.normalizeStudentId(candidate)
    assertNotNull(normalized)
  }
}
