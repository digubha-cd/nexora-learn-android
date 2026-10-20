package com.example.core.repository

import android.content.Context
import android.net.Uri
import com.example.core.model.StudentProfile
import com.example.core.model.StudyBadge
import com.example.core.storage.StudentPreferences
import com.example.core.util.StudentIdGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

interface StudentProfileRepository {
    suspend fun saveProfile(profile: StudentProfile): Result<Unit>
    suspend fun uploadProfileImage(userId: String, uri: Uri): Result<String>
    suspend fun getProfile(userId: String): Result<StudentProfile?>
    suspend fun ensureStudentId(profile: StudentProfile): StudentProfile
    suspend fun getStudentByStudentId(studentId: String): Result<StudentProfile?>
    suspend fun searchStudents(query: String): Result<List<StudentProfile>>
    suspend fun syncStudentProgress(
        userId: String,
        completedTodos: Int,
        streakDays: Int,
        completedChapters: List<String>,
        chapterMarks: Map<String, Double>
    ): Result<Unit>
    suspend fun awardPointsIdempotent(
        userId: String,
        actionKey: String,
        pointsToAward: Int
    ): Result<Int>
    fun getGlobalLeaderboardFlow(limit: Int = 50): Flow<Result<List<StudentProfile>>>
    fun getUserBadgesFlow(userId: String): Flow<Result<List<StudyBadge>>>
    suspend fun syncBadgesToFirestore(userId: String, badges: List<StudyBadge>): Result<Unit>
    suspend fun registerUniqueStudentId(
        userId: String,
        studentName: String,
        requestedStudentId: String
    ): Result<String>
    suspend fun isStudentIdAvailable(
        candidateId: String,
        currentUserId: String
    ): Boolean
}

class LocalStudentProfileRepository(
    private val context: Context
) : StudentProfileRepository {

    override suspend fun saveProfile(profile: StudentProfile): Result<Unit> {
        StudentPreferences.updateStudentProfile(context, profile)
        if (com.example.core.supabase.SupabaseClientManager.isConfigured(context)) {
            try {
                com.example.core.supabase.SupabaseAuthRepository(context).syncProfileToCloud(profile)
            } catch (_: Exception) {}
        }
        return Result.success(Unit)
    }

    override suspend fun uploadProfileImage(userId: String, uri: Uri): Result<String> {
        val uriStr = uri.toString()
        StudentPreferences.savePhotoUrl(context, uriStr)
        return Result.success(uriStr)
    }

    override suspend fun getProfile(userId: String): Result<StudentProfile?> {
        val profile = StudentPreferences.getStudentProfile(context)
        return Result.success(profile)
    }

    override suspend fun ensureStudentId(profile: StudentProfile): StudentProfile {
        if (profile.studentId.isNotBlank() && StudentIdGenerator.isValidStudentId(profile.studentId)) {
            return profile
        }
        val newId = StudentIdGenerator.generateCandidateId()
        val updated = profile.copy(studentId = newId)
        StudentPreferences.updateStudentProfile(context, updated)
        return updated
    }

    override suspend fun getStudentByStudentId(studentId: String): Result<StudentProfile?> {
        val current = StudentPreferences.getStudentProfile(context)
        return if (current.studentId == studentId) {
            Result.success(current)
        } else {
            Result.success(null)
        }
    }

    override suspend fun searchStudents(query: String): Result<List<StudentProfile>> {
        val current = StudentPreferences.getStudentProfile(context)
        return if (current.studentName.contains(query, ignoreCase = true) || current.studentId.contains(query, ignoreCase = true)) {
            Result.success(listOf(current))
        } else {
            Result.success(emptyList())
        }
    }

    override suspend fun syncStudentProgress(
        userId: String,
        completedTodos: Int,
        streakDays: Int,
        completedChapters: List<String>,
        chapterMarks: Map<String, Double>
    ): Result<Unit> {
        val current = StudentPreferences.getStudentProfile(context)
        val updated = current.copy(
            completedTodoCount = completedTodos,
            studyStreakDays = streakDays
        )
        StudentPreferences.updateStudentProfile(context, updated)
        return Result.success(Unit)
    }

    override suspend fun awardPointsIdempotent(
        userId: String,
        actionKey: String,
        pointsToAward: Int
    ): Result<Int> = withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val prefs = context.getSharedPreferences("nexora_points_idempotency", Context.MODE_PRIVATE)
            if (prefs.getBoolean(actionKey, false)) {
                // Already awarded
                val current = StudentPreferences.getStudentProfile(context)
                return@withContext Result.success(current.competitionPoints)
            }

            val current = StudentPreferences.getStudentProfile(context)
            val newPoints = current.competitionPoints + pointsToAward
            val updated = current.copy(
                competitionPoints = newPoints,
                updatedAt = System.currentTimeMillis()
            )
            
            // Save locally
            StudentPreferences.updateStudentProfile(context, updated)
            
            // Mark as awarded
            prefs.edit().putBoolean(actionKey, true).apply()
            
            // Sync to cloud
            if (com.example.core.supabase.SupabaseClientManager.isConfigured(context)) {
                try {
                    val authRepo = com.example.core.supabase.SupabaseAuthRepository(context)
                    if (authRepo.isUserSignedIn()) {
                        authRepo.syncProfileToCloud(updated)
                    }
                } catch (e: Exception) {
                    android.util.Log.w("ProfileRepo", "Point sync failed but local saved: ${e.message}")
                }
            }
            
            Result.success(newPoints)
        } catch (e: Exception) {
            android.util.Log.e("ProfileRepo", "Error awarding points: ${e.message}")
            Result.failure(e)
        }
    }

    override fun getGlobalLeaderboardFlow(limit: Int): Flow<Result<List<StudentProfile>>> {
        val current = StudentPreferences.getStudentProfile(context)
        return flowOf(Result.success(listOf(current)))
    }

    override fun getUserBadgesFlow(userId: String): Flow<Result<List<StudyBadge>>> {
        val current = StudentPreferences.getStudentProfile(context)
        val badges = com.example.core.util.StudyBadgeProvider.calculateBadges(
            sessionHistory = emptyList(),
            streakDays = current.studyStreakDays,
            completedChaptersCount = current.completedChapterIds.size,
            totalCompletedTasks = current.completedTodoCount
        )
        return flowOf(Result.success(badges))
    }

    override suspend fun syncBadgesToFirestore(userId: String, badges: List<StudyBadge>): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun registerUniqueStudentId(
        userId: String,
        studentName: String,
        requestedStudentId: String
    ): Result<String> {
        val current = StudentPreferences.getStudentProfile(context)
        val updated = current.copy(
            studentName = studentName,
            studentId = requestedStudentId
        )
        StudentPreferences.updateStudentProfile(context, updated)
        return Result.success(requestedStudentId)
    }

    override suspend fun isStudentIdAvailable(
        candidateId: String,
        currentUserId: String
    ): Boolean {
        return true
    }
}

/**
 * Type alias for backward compatibility so existing code continues working with local persistence.
 */
typealias FirestoreStudentProfileRepository = LocalStudentProfileRepository
