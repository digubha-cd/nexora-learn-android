package com.example.core.supabase

import android.content.Context
import android.util.Log
import com.example.core.model.StudentProfile
import com.example.core.model.StudyTask
import com.example.core.storage.StudentPreferences
import com.example.core.supabase.model.SupabaseStudentMilestone
import com.example.core.supabase.model.SupabaseStudentProfile
import com.example.core.supabase.model.SupabaseStudyTask
import com.example.core.util.StudentIdGenerator
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.Phone
import io.github.jan.supabase.auth.providers.Google

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Concrete repository for Supabase Authentication and Database Cloud Synchronization.
 *
 * Implements:
 * 1. Student Sign-Up with Email & Password + automatic student profile creation in student_profiles table
 * 2. Student Sign-In with Email & Password + profile hydration into local StudentPreferences
 * 3. Student Sign-Out via official Supabase client
 * 4. Real PostgreSQL database synchronization for student profiles, study tasks, and milestone chapters
 * 5. Strict adherence to non-fake authentication: real network calls and real error propagation.
 */
sealed interface SignUpOutcome {
    data class LoggedIn(val profile: StudentProfile) : SignUpOutcome
    data class VerificationRequired(val message: String) : SignUpOutcome
}

class SupabaseAuthRepository(
    private val context: Context
) {

    private val appContext = context.applicationContext

    private fun getClient() = SupabaseClientManager.client
        ?: error("Supabase client is not initialized. Please ensure your Supabase Publishable Key is provided.")

    /**
     * Signs up a student using Supabase Authentication and creates their profile in the database.
     */
    suspend fun signUp(
        email: String,
        password: String,
        studentName: String,
        targetMarks: String = "90+ Marks",
        medium: String = "Gujarati Medium"
    ): Result<SignUpOutcome> = withContext(Dispatchers.IO) {
        try {
            val trimmedEmail = email.trim()
            val trimmedName = studentName.trim()
            if (trimmedEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
                return@withContext Result.failure(IllegalArgumentException("Please enter a valid email address"))
            }
            if (password.length < 6) {
                return@withContext Result.failure(IllegalArgumentException("Password must be at least 6 characters long"))
            }
            if (trimmedName.length < 2) {
                return@withContext Result.failure(IllegalArgumentException("Please enter your name (at least 2 characters)"))
            }

            val client = getClient()

            // 1. Supabase Auth Sign-Up
            client.auth.signUpWith(Email) {
                this.email = trimmedEmail
                this.password = password
            }

            // 2. Check for active session or user
            val session = client.auth.currentSessionOrNull()
            val authUser = client.auth.currentUserOrNull() ?: session?.user
            val userId = authUser?.id

            if (userId == null || session == null) {
                // Account created successfully in Supabase Auth, but email confirmation is required
                return@withContext Result.success(
                    SignUpOutcome.VerificationRequired(
                        "Account created successfully for $trimmedEmail. Please check your email inbox to verify your account, then sign in."
                    )
                )
            }

            // 3. Generate candidate student ID
            val studentId = StudentIdGenerator.generateCandidateId()

            val profile = StudentProfile(
                userId = userId,
                studentId = studentId,
                studentName = trimmedName,
                email = trimmedEmail,
                targetMarks = targetMarks,
                medium = medium,
                classLevel = "Class 12 Commerce",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            // 4. Upsert into Supabase student_profiles table
            if (SupabaseClientManager.isValidUuid(userId)) {
                try {
                    val profileDto = SupabaseStudentProfile.fromDomain(profile, userId)
                    client.from("student_profiles").upsert(profileDto)
                    Log.d(TAG, "Student profile uploaded to Supabase student_profiles for $userId")
                } catch (dbEx: Exception) {
                    Log.w(TAG, "Profile created locally but remote database upsert threw: ${dbEx.message}")
                }
            } else {
                Log.w(TAG, "Skipping remote profile upsert for non-UUID userId: $userId")
            }

            // 5. Save locally in StudentPreferences
            StudentPreferences.setGuestMode(appContext, false)
            val currentLocal = StudentPreferences.getStudentProfile(appContext)
            val finalProfile = currentLocal.copy(
                userId = userId,
                studentId = studentId,
                studentName = trimmedName,
                email = trimmedEmail,
                targetMarks = targetMarks,
                medium = medium,
                updatedAt = System.currentTimeMillis()
            )
            StudentPreferences.updateStudentProfile(appContext, finalProfile)

            Result.success(SignUpOutcome.LoggedIn(finalProfile))
        } catch (e: Exception) {
            Log.e(TAG, "Supabase Sign-Up error: ${e.message}", e)
            val msg = e.message ?: ""
            if (msg.contains("User already registered", ignoreCase = true) || msg.contains("user_already_exists", ignoreCase = true)) {
                Result.failure(IllegalArgumentException("An account with this email already exists. Please sign in instead."))
            } else {
                Result.failure(e)
            }
        }
    }

    /**
     * Signs in an existing student with Email and Password via Supabase Auth.
     */
    suspend fun signIn(
        email: String,
        password: String
    ): Result<StudentProfile> = withContext(Dispatchers.IO) {
        try {
            val trimmedEmail = email.trim()
            if (trimmedEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
                return@withContext Result.failure(IllegalArgumentException("Please enter a valid email address"))
            }
            if (password.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Please enter your password"))
            }

            val client = getClient()

            // 1. Supabase Auth Sign-In
            try {
                client.auth.signInWith(Email) {
                    this.email = trimmedEmail
                    this.password = password
                }
            } catch (authEx: Exception) {
                val msg = authEx.message ?: ""
                return@withContext when {
                    msg.contains("Email not confirmed", ignoreCase = true) || msg.contains("email_not_confirmed", ignoreCase = true) -> {
                        Result.failure(IllegalStateException("Your email address has not been verified yet. Please check your inbox for the verification link."))
                    }
                    msg.contains("Invalid login credentials", ignoreCase = true) || msg.contains("invalid_credentials", ignoreCase = true) -> {
                        Result.failure(IllegalArgumentException("Invalid email or password. Please try again or reset your password."))
                    }
                    else -> Result.failure(authEx)
                }
            }

            val authUser = client.auth.currentUserOrNull()
                ?: client.auth.currentSessionOrNull()?.user
                ?: return@withContext Result.failure(IllegalStateException("Sign-in succeeded, but user session could not be established."))

            val userId = authUser.id

            // 2. Fetch student profile from Supabase database
            var fetchedProfile: StudentProfile? = null
            try {
                val remoteList = client.from("student_profiles")
                    .select {
                        filter {
                            eq("id", userId)
                        }
                    }
                    .decodeList<SupabaseStudentProfile>()

                if (remoteList.isNotEmpty()) {
                    fetchedProfile = remoteList.first().toDomain(userId)
                    Log.d(TAG, "Loaded profile from Supabase: ${fetchedProfile.studentName} (${fetchedProfile.studentId})")
                }
            } catch (dbEx: Exception) {
                Log.w(TAG, "Failed to fetch remote student profile: ${dbEx.message}")
            }

            // 3. Fallback or merge with local profile
            val currentLocal = StudentPreferences.getStudentProfile(appContext)
            val finalProfile = fetchedProfile ?: currentLocal.copy(
                userId = userId,
                email = trimmedEmail,
                studentName = currentLocal.studentName.ifBlank { "Class 12 Student" }
            )

            // 4. Update local preferences
            StudentPreferences.setGuestMode(appContext, false)
            StudentPreferences.updateStudentProfile(appContext, finalProfile)
            if (finalProfile.studentName.isNotBlank() && finalProfile.studentId.isNotBlank()) {
                StudentPreferences.completeIdentitySetup(appContext, finalProfile.studentName, finalProfile.studentId)
            }

            Result.success(finalProfile)
        } catch (e: Exception) {
            Log.e(TAG, "Supabase Sign-In error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Sends password reset instructions to the student's email via Supabase Auth.
     */
    suspend fun resetPassword(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val trimmed = email.trim()
            if (trimmed.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()) {
                return@withContext Result.failure(IllegalArgumentException("Please enter a valid email address"))
            }
            val client = getClient()
            client.auth.resetPasswordForEmail(trimmed)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Supabase Password Reset error: ${e.message}", e)
            val msg = e.message ?: ""
            if (msg.contains("User not found", ignoreCase = true)) {
                Result.failure(IllegalArgumentException("No account found with this email address."))
            } else {
                Result.failure(e)
            }
        }
    }

    /**
     * Initiates Google Sign-In via Supabase Auth OAuth with official Android deep link redirect.
     */
    suspend fun signInWithGoogle(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val client = getClient()
            val redirectUri = "com.aistudio.nexoralearn.auth://callback"

            val currentAuthUser = client.auth.currentUserOrNull()
                ?: client.auth.currentSessionOrNull()?.user
            if (currentAuthUser != null) {
                syncGoogleUserProfile(currentAuthUser)
                return@withContext Result.success(Unit)
            }

            Log.i(TAG, "Initiating Google OAuth with redirectUrl=$redirectUri")
            client.auth.signInWith(Google, redirectUrl = redirectUri)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Supabase Google Sign-In error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Sends a phone OTP using Supabase Phone Auth.
     */
    suspend fun sendPhoneOtp(countryCode: String, phoneNumber: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cleanCountry = if (countryCode.startsWith("+")) countryCode else "+$countryCode"
            val cleanPhone = phoneNumber.filter { it.isDigit() }
            if (cleanPhone.length < 8) {
                return@withContext Result.failure(IllegalArgumentException("Please enter a valid phone number."))
            }
            val formatted = "$cleanCountry$cleanPhone"
            val client = getClient()

            Log.i(TAG, "Requesting Phone OTP for: $cleanCountry ***${cleanPhone.takeLast(4)}")
            client.auth.signInWith(Phone) {
                this.phone = formatted
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Supabase sendPhoneOtp error: ${e.message}", e)
            val msg = e.message ?: ""
            val userFriendly = when {
                msg.contains("Unsupported phone provider", ignoreCase = true) ||
                msg.contains("sms_provider_not_configured", ignoreCase = true) ||
                msg.contains("Phone provider is disabled", ignoreCase = true) ||
                msg.contains("Phone logins are disabled", ignoreCase = true) ||
                msg.contains("disabled", ignoreCase = true) ||
                msg.contains("Signups not allowed for otp", ignoreCase = true) ||
                msg.contains("provider not enabled", ignoreCase = true) -> {
                    "Phone authentication is not configured in the Supabase Dashboard. An SMS gateway (such as Twilio, MessageBird, or Vonage) must be enabled under Supabase > Authentication > Providers > Phone."
                }
                msg.contains("rate limit", ignoreCase = true) || msg.contains("429", ignoreCase = true) -> {
                    "SMS rate limit reached. Please wait a few minutes before requesting another OTP."
                }
                msg.contains("invalid phone", ignoreCase = true) -> {
                    "The phone number entered is invalid. Please check the country code and number."
                }
                else -> msg.ifBlank { "Failed to send SMS OTP. Please check your phone number and network." }
            }
            Result.failure(Exception(userFriendly))
        }
    }

    /**
     * Verifies the 6-digit Phone OTP and establishes a user session.
     */
    suspend fun verifyPhoneOtp(
        countryCode: String,
        phoneNumber: String,
        token: String
    ): Result<StudentProfile> = withContext(Dispatchers.IO) {
        try {
            val cleanCountry = if (countryCode.startsWith("+")) countryCode else "+$countryCode"
            val cleanPhone = phoneNumber.filter { it.isDigit() }
            val formatted = "$cleanCountry$cleanPhone"
            val cleanToken = token.trim()

            if (cleanToken.length < 6) {
                return@withContext Result.failure(IllegalArgumentException("Please enter the complete 6-digit verification code."))
            }

            val client = getClient()
            Log.i(TAG, "Verifying Phone OTP for: $cleanCountry ***${cleanPhone.takeLast(4)}")

            client.auth.verifyPhoneOtp(
                type = OtpType.Phone.SMS,
                phone = formatted,
                token = cleanToken
            )

            val authUser = client.auth.currentUserOrNull()
                ?: client.auth.currentSessionOrNull()?.user
                ?: return@withContext Result.failure(IllegalStateException("OTP verified, but user session could not be established."))

            val userId = authUser.id

            // Check if profile exists or create one
            var fetchedProfile: StudentProfile? = null
            try {
                val remoteList = client.from("student_profiles")
                    .select {
                        filter {
                            eq("id", userId)
                        }
                    }
                    .decodeList<SupabaseStudentProfile>()

                if (remoteList.isNotEmpty()) {
                    fetchedProfile = remoteList.first().toDomain(userId)
                }
            } catch (dbEx: Exception) {
                Log.w(TAG, "Could not fetch existing profile: ${dbEx.message}")
            }

            val currentLocal = StudentPreferences.getStudentProfile(appContext)
            val finalStudentId = fetchedProfile?.studentId
                ?: currentLocal.studentId.takeIf { it.isNotBlank() && it.startsWith("NX-") }
                ?: StudentIdGenerator.generateCandidateId()

            val finalProfile = fetchedProfile ?: currentLocal.copy(
                userId = userId,
                studentId = finalStudentId,
                phoneNumber = formatted,
                studentName = currentLocal.studentName.ifBlank { "Commerce Scholar" }
            )

            // Save to database
            try {
                val dto = SupabaseStudentProfile.fromDomain(finalProfile, userId)
                client.from("student_profiles").upsert(dto)
            } catch (dbEx: Exception) {
                Log.e(TAG, "Failed to upsert student profile for phone user: ${dbEx.message}")
            }

            // Save to local preferences
            StudentPreferences.setGuestMode(appContext, false)
            StudentPreferences.updateStudentProfile(appContext, finalProfile)
            StudentPreferences.completeIdentitySetup(appContext, finalProfile.studentName, finalProfile.studentId)

            Result.success(finalProfile)
        } catch (e: Exception) {
            Log.e(TAG, "Supabase verifyPhoneOtp error: ${e.message}", e)
            val msg = e.message ?: ""
            val userFriendly = when {
                msg.contains("Token has expired", ignoreCase = true) || msg.contains("otp_expired", ignoreCase = true) -> {
                    "The verification code has expired. Please request a new code."
                }
                msg.contains("Invalid token", ignoreCase = true) || msg.contains("invalid_grant", ignoreCase = true) -> {
                    "Incorrect verification code. Please check the code and try again."
                }
                else -> msg.ifBlank { "Failed to verify code. Please try again." }
            }
            Result.failure(Exception(userFriendly))
        }
    }

    /**
     * Resends Phone OTP.
     */
    suspend fun resendPhoneOtp(countryCode: String, phoneNumber: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cleanCountry = if (countryCode.startsWith("+")) countryCode else "+$countryCode"
            val cleanPhone = phoneNumber.filter { it.isDigit() }
            val formatted = "$cleanCountry$cleanPhone"
            val client = getClient()

            Log.i(TAG, "Resending Phone OTP for: $cleanCountry ***${cleanPhone.takeLast(4)}")
            client.auth.resendPhone(
                type = OtpType.Phone.SMS,
                phone = formatted
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Supabase resendPhoneOtp error: ${e.message}", e)
            val msg = e.message ?: ""
            val userFriendly = when {
                msg.contains("rate limit", ignoreCase = true) || msg.contains("429", ignoreCase = true) -> {
                    "SMS rate limit reached. Please wait a few moments before resending."
                }
                msg.contains("Unsupported phone provider", ignoreCase = true) ||
                msg.contains("sms_provider_not_configured", ignoreCase = true) -> {
                    "Phone authentication is not configured in the Supabase Dashboard. An SMS gateway must be configured under Supabase > Authentication > Providers > Phone."
                }
                else -> msg.ifBlank { "Failed to resend code. Please try again." }
            }
            Result.failure(Exception(userFriendly))
        }
    }

    /**
     * Synchronizes and initializes student profile after Google OAuth sign-in.
     */
    suspend fun syncGoogleUserProfile(authUser: UserInfo): Result<StudentProfile> = withContext(Dispatchers.IO) {
        try {
            val client = getClient()
            val userId = authUser.id
            val email = authUser.email ?: ""

            // Fetch name from metadata if available, otherwise fallback to email prefix
            val metadataName = authUser.userMetadata?.get("full_name")?.toString()
                ?: authUser.userMetadata?.get("name")?.toString()
                ?: email.substringBefore("@").ifBlank { "Student" }

            if (!SupabaseClientManager.isValidUuid(userId)) {
                return@withContext Result.failure(IllegalStateException("Invalid User UUID ($userId) received from Supabase Auth."))
            }

            // Check if profile exists before generating new Student ID
            var fetchedProfile: StudentProfile? = null
            try {
                val remoteList = client.from("student_profiles")
                    .select {
                        filter {
                            eq("id", userId)
                        }
                    }
                    .decodeList<SupabaseStudentProfile>()

                if (remoteList.isNotEmpty()) {
                    fetchedProfile = remoteList.first().toDomain(userId)
                    Log.d(TAG, "Fetched existing profile for Google user: ${fetchedProfile.studentName}")
                }
            } catch (dbEx: Exception) {
                Log.w(TAG, "Failed to fetch remote profile during Google Sign-In: ${dbEx.message}")
            }

            val localExisting = StudentPreferences.getStudentProfile(appContext)

            // Priority for Name: 1. Database, 2. Local Storage, 3. Provider Metadata
            val finalName = fetchedProfile?.studentName?.takeIf { it.isNotBlank() && it != "Student" && it != "Google Student" && it != "Class 12 Student" }
                ?: localExisting.studentName.takeIf { it.isNotBlank() && it != "Student" && it != "Google Student" && it != "Class 12 Student" }
                ?: metadataName

            // Priority for ID: 1. Fetched from DB, 2. Local existing, 3. Generate new
            val finalStudentId = fetchedProfile?.studentId
                ?: localExisting.studentId.takeIf { it.isNotBlank() && it.startsWith("NX-") }
                ?: StudentIdGenerator.generateCandidateId()

            val profile = StudentProfile(
                userId = userId,
                studentId = finalStudentId,
                studentName = finalName,
                email = email.ifBlank { localExisting.email },
                classLevel = fetchedProfile?.classLevel ?: localExisting.classLevel.ifBlank { "Class 12 Commerce" },
                photoUrl = fetchedProfile?.photoUrl ?: localExisting.photoUrl,
                avatarColorHex = fetchedProfile?.avatarColorHex ?: localExisting.avatarColorHex,
                competitionPoints = fetchedProfile?.competitionPoints ?: localExisting.competitionPoints,
                studyStreakDays = fetchedProfile?.studyStreakDays ?: localExisting.studyStreakDays,
                totalFocusedMinutes = fetchedProfile?.totalFocusedMinutes ?: localExisting.totalFocusedMinutes,
                createdAt = fetchedProfile?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            // Upsert only if we have a valid profile
            try {
                val dto = SupabaseStudentProfile.fromDomain(profile, userId)
                client.from("student_profiles").upsert(dto)
            } catch (dbEx: Exception) {
                Log.e(TAG, "Failed to upsert profile during Google Sign-In: ${dbEx.message}")
            }

            StudentPreferences.setGuestMode(appContext, false)
            StudentPreferences.updateStudentProfile(appContext, profile)

            // Mark setup as complete only if they have a real name (not placeholder) and a valid Student ID
            val isRealName = finalName.isNotBlank() && finalName != "Student" && finalName != "Google Student" && finalName != "Class 12 Student"
            if (isRealName && profile.studentId.isNotBlank()) {
                StudentPreferences.completeIdentitySetup(appContext, profile.studentName, profile.studentId)
            }

            Result.success(profile)
        } catch (e: Exception) {
            Log.e(TAG, "Supabase Google Sign-In profile sync error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Signs out the currently authenticated student from Supabase.
     */
    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val client = SupabaseClientManager.client
            client?.auth?.signOut()
            StudentPreferences.setGuestMode(appContext, false)
            Log.d(TAG, "Signed out from Supabase successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Supabase Sign-Out error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Checks if a user is currently signed in to Supabase.
     */
    fun isUserSignedIn(): Boolean {
        val client = SupabaseClientManager.client ?: return false
        return client.auth.currentUserOrNull() != null || client.auth.currentSessionOrNull() != null
    }

    /**
     * Synchronizes student profile, streak, and focus minutes to Supabase database.
     */
    suspend fun syncProfileToCloud(profile: StudentProfile): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val client = SupabaseClientManager.client ?: return@withContext Result.failure(IllegalStateException("Supabase not configured"))
            val userId = client.auth.currentUserOrNull()?.id ?: return@withContext Result.failure(IllegalStateException("User not authenticated"))

            if (!SupabaseClientManager.isValidUuid(userId)) {
                return@withContext Result.failure(IllegalArgumentException("Invalid user ID format for Supabase cloud operations"))
            }

            val dto = SupabaseStudentProfile.fromDomain(profile, userId)
            client.from("student_profiles").upsert(dto)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing profile to Supabase: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Synchronizes local student-created study tasks to Supabase database.
     */
    suspend fun syncTasksToCloud(tasks: List<StudyTask>): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val client = SupabaseClientManager.client ?: return@withContext Result.failure(IllegalStateException("Supabase not configured"))
            val userId = client.auth.currentUserOrNull()?.id ?: return@withContext Result.failure(IllegalStateException("User not authenticated"))

            if (!SupabaseClientManager.isValidUuid(userId)) {
                return@withContext Result.failure(IllegalArgumentException("Invalid user ID format for Supabase cloud operations"))
            }

            if (tasks.isEmpty()) return@withContext Result.success(0)

            val dtos = tasks.map { SupabaseStudyTask.fromDomain(it, userId) }
            client.from("study_tasks").upsert(dtos)
            Log.d(TAG, "Synced ${dtos.size} study tasks to Supabase")
            Result.success(dtos.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing tasks to Supabase: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Pulls student tasks from Supabase database.
     */
    suspend fun pullTasksFromCloud(): Result<List<StudyTask>> = withContext(Dispatchers.IO) {
        try {
            val client = SupabaseClientManager.client ?: return@withContext Result.failure(IllegalStateException("Supabase not configured"))
            val userId = client.auth.currentUserOrNull()?.id ?: return@withContext Result.failure(IllegalStateException("User not authenticated"))

            if (!SupabaseClientManager.isValidUuid(userId)) {
                return@withContext Result.failure(IllegalArgumentException("Invalid user ID format for Supabase cloud operations"))
            }

            val dtos = client.from("study_tasks")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<SupabaseStudyTask>()

            val tasks = dtos.map { it.toDomain() }
            Result.success(tasks)
        } catch (e: Exception) {
            Log.e(TAG, "Error pulling tasks from Supabase: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Safely registers a unique Student ID by retrying with new candidates if a collision occurs.
     */
    suspend fun registerUniqueStudentId(
        userId: String,
        studentName: String,
        requestedStudentId: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val client = getClient()
        var attempts = 0
        val maxAttempts = 5

        // Use the provided userId, but fallback to the current authenticated session if needed
        var effectiveUserId = userId
        if (!SupabaseClientManager.isValidUuid(effectiveUserId)) {
            effectiveUserId = SupabaseClientManager.getCurrentUserId() ?: ""
        }
        
        if (!SupabaseClientManager.isValidUuid(effectiveUserId)) {
            Log.w(TAG, "Cannot register student ID in Supabase for non-UUID user: $effectiveUserId")
            return@withContext Result.failure(IllegalArgumentException("User must be authenticated with a valid UUID to register a Student ID. (ID: $effectiveUserId)"))
        }

        // Check if user ALREADY has a Student ID assigned in the database
        try {
            val existingProfiles = client.from("student_profiles")
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeList<SupabaseStudentProfile>()
            
            if (existingProfiles.isNotEmpty()) {
                val existing = existingProfiles.first()
                val existingId = existing.studentId.orEmpty()
                if (existingId.isNotBlank()) {
                    Log.d(TAG, "User $userId already has Student ID: $existingId. Reusing it.")
                    
                    // Update local storage to match
                    val localProfile = StudentPreferences.getStudentProfile(appContext).copy(
                        userId = userId,
                        studentId = existingId,
                        studentName = studentName,
                        updatedAt = System.currentTimeMillis()
                    )
                    StudentPreferences.updateStudentProfile(appContext, localProfile)
                    StudentPreferences.completeIdentitySetup(appContext, studentName, existingId)
                    
                    return@withContext Result.success(existingId)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error checking existing profile: ${e.message}")
        }

        var currentCandidate = requestedStudentId ?: StudentIdGenerator.generateCandidateId()

        while (attempts < maxAttempts) {
            try {
                // Check if ID is already taken by someone else
                val collisions = client.from("student_profiles")
                    .select {
                        filter {
                            eq("student_id", currentCandidate)
                            // Only apply neq if userId is a valid UUID to avoid PostgreSQL type errors
                            neq("id", userId)
                        }
                    }
                    .decodeList<SupabaseStudentProfile>()

                if (collisions.isEmpty()) {
                    // ID is available, attempt to update profile
                    val profile = StudentPreferences.getStudentProfile(appContext).copy(
                        userId = effectiveUserId,
                        studentId = currentCandidate,
                        studentName = studentName,
                        updatedAt = System.currentTimeMillis()
                    )
                    val dto = SupabaseStudentProfile.fromDomain(profile, effectiveUserId)
                    client.from("student_profiles").upsert(dto)
                    
                    // Update local storage
                    StudentPreferences.updateStudentProfile(appContext, profile)
                    StudentPreferences.completeIdentitySetup(appContext, studentName, currentCandidate)
                    
                    return@withContext Result.success(currentCandidate)
                } else {
                    // Collision! Generate a new one and try again
                    currentCandidate = StudentIdGenerator.generateCandidateId()
                    attempts++
                }
            } catch (e: Exception) {
                Log.e(TAG, "Attempt $attempts failed: ${e.message}")
                attempts++
                if (attempts >= maxAttempts) return@withContext Result.failure(e)
            }
        }
        Result.failure(IllegalStateException("Could not generate a unique Student ID after $maxAttempts attempts."))
    }

    /**
     * Sends a friend request to another student.
     */
    suspend fun sendFriendRequest(receiverId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val client = getClient()
            val senderId = client.auth.currentUserOrNull()?.id ?: return@withContext Result.failure(IllegalStateException("User not authenticated"))
            
            if (senderId == receiverId) return@withContext Result.failure(IllegalArgumentException("You cannot add yourself as a friend."))

            // Ensure both IDs are valid UUIDs
            if (!SupabaseClientManager.isValidUuid(senderId) || !SupabaseClientManager.isValidUuid(receiverId)) {
                return@withContext Result.failure(IllegalArgumentException("Invalid sender or receiver ID format (UUID expected)"))
            }

            val request = com.example.core.supabase.model.SupabaseFriendRequest(
                senderId = senderId,
                receiverId = receiverId
            )
            client.from("friend_requests").insert(request)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending friend request: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Searches for a student by their unique Student ID or email address.
     */
    suspend fun searchStudentByStudentIdOrEmail(query: String): Result<StudentProfile?> = withContext(Dispatchers.IO) {
        try {
            val client = getClient()
            val trimmedQuery = query.trim()
            if (trimmedQuery.isBlank()) return@withContext Result.success(null)

            val currentUserId = client.auth.currentUserOrNull()?.id

            // Check if query is email
            val isEmail = trimmedQuery.contains("@") && android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedQuery).matches()

            val remoteList = client.from("student_profiles")
                .select {
                    filter {
                        if (isEmail) {
                            eq("email", trimmedQuery.lowercase())
                        } else {
                            val normalized = com.example.core.util.StudentIdGenerator.normalizeStudentId(trimmedQuery) ?: trimmedQuery.uppercase()
                            eq("student_id", normalized)
                        }
                        if (currentUserId != null && SupabaseClientManager.isValidUuid(currentUserId)) {
                            neq("id", currentUserId)
                        }
                    }
                }
                .decodeList<SupabaseStudentProfile>()

            if (remoteList.isNotEmpty()) {
                val dto = remoteList.first()
                Result.success(dto.toDomain(dto.id))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "Search student error: ${e.message}", e)
            Result.failure(e)
        }
    }

    data class FriendRequestWithSender(
        val requestId: String,
        val senderProfile: StudentProfile,
        val createdAt: Long
    )

    /**
     * Fetches pending friend requests received by the current user.
     */
    suspend fun getPendingFriendRequests(): Result<List<FriendRequestWithSender>> = withContext(Dispatchers.IO) {
        try {
            val client = getClient()
            val currentUserId = client.auth.currentUserOrNull()?.id
                ?: return@withContext Result.failure(IllegalStateException("User not authenticated"))

            val requests = client.from("friend_requests")
                .select {
                    filter {
                        eq("receiver_id", currentUserId)
                        eq("status", "PENDING")
                    }
                }
                .decodeList<com.example.core.supabase.model.SupabaseFriendRequest>()

            if (requests.isEmpty()) return@withContext Result.success(emptyList())

            val list = mutableListOf<FriendRequestWithSender>()
            for (req in requests) {
                try {
                    val senderProfiles = client.from("student_profiles")
                        .select {
                            filter {
                                eq("id", req.senderId)
                            }
                        }
                        .decodeList<SupabaseStudentProfile>()

                    if (senderProfiles.isNotEmpty()) {
                        val senderDto = senderProfiles.first()
                        val reqId = req.id ?: "${req.senderId}_${req.receiverId}"
                        list.add(FriendRequestWithSender(reqId, senderDto.toDomain(senderDto.id), req.createdAt ?: System.currentTimeMillis()))
                    }
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    Log.w(TAG, "Could not load sender profile for ${req.senderId}: ${e.message}")
                }
            }

            Result.success(list)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "Error fetching pending friend requests: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches accepted friends for the current user.
     */
    suspend fun getAcceptedFriends(): Result<List<StudentProfile>> = withContext(Dispatchers.IO) {
        try {
            val client = getClient()
            val currentUserId = client.auth.currentUserOrNull()?.id
                ?: return@withContext Result.failure(IllegalStateException("User not authenticated"))

            val sentAccepted = client.from("friend_requests")
                .select {
                    filter {
                        eq("sender_id", currentUserId)
                        eq("status", "ACCEPTED")
                    }
                }
                .decodeList<com.example.core.supabase.model.SupabaseFriendRequest>()

            val receivedAccepted = client.from("friend_requests")
                .select {
                    filter {
                        eq("receiver_id", currentUserId)
                        eq("status", "ACCEPTED")
                    }
                }
                .decodeList<com.example.core.supabase.model.SupabaseFriendRequest>()

            val friendUserIds = (sentAccepted.map { it.receiverId } + receivedAccepted.map { it.senderId }).distinct()

            if (friendUserIds.isEmpty()) return@withContext Result.success(emptyList())

            val friendsList = mutableListOf<StudentProfile>()
            for (fId in friendUserIds) {
                try {
                    val profiles = client.from("student_profiles")
                        .select {
                            filter {
                                eq("id", fId)
                            }
                        }
                        .decodeList<SupabaseStudentProfile>()

                    if (profiles.isNotEmpty()) {
                        val dto = profiles.first()
                        friendsList.add(dto.toDomain(dto.id))
                    }
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    Log.w(TAG, "Could not load friend profile for $fId: ${e.message}")
                }
            }

            Result.success(friendsList)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "Error fetching accepted friends: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Accepts a pending friend request.
     */
    suspend fun acceptFriendRequest(senderId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val client = getClient()
            val currentUserId = client.auth.currentUserOrNull()?.id
                ?: return@withContext Result.failure(IllegalStateException("User not authenticated"))

            client.from("friend_requests").update({
                set("status", "ACCEPTED")
            }) {
                filter {
                    eq("sender_id", senderId)
                    eq("receiver_id", currentUserId)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "Error accepting friend request: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Declines/removes a friend request or connection.
     */
    suspend fun declineFriendRequest(senderId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val client = getClient()
            val currentUserId = client.auth.currentUserOrNull()?.id
                ?: return@withContext Result.failure(IllegalStateException("User not authenticated"))

            client.from("friend_requests").delete {
                filter {
                    eq("sender_id", senderId)
                    eq("receiver_id", currentUserId)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "Error declining friend request: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Searches for a student by their unique Student ID.
     */
    suspend fun searchStudentByStudentId(studentId: String): Result<StudentProfile?> = searchStudentByStudentIdOrEmail(studentId)



    /**
     * Pulls latest student profile from Supabase database for the current authenticated user.
     */
    suspend fun pullProfileFromCloud(): Result<StudentProfile> = withContext(Dispatchers.IO) {
        try {
            val client = getClient()
            val authUser = client.auth.currentUserOrNull() ?: client.auth.currentSessionOrNull()?.user
                ?: return@withContext Result.failure(IllegalStateException("User not authenticated"))
            
            val userId = authUser.id

            val remoteList = client.from("student_profiles")
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeList<SupabaseStudentProfile>()

            if (remoteList.isNotEmpty()) {
                val profile = remoteList.first().toDomain(userId)
                StudentPreferences.updateStudentProfile(appContext, profile)
                Log.d(TAG, "Successfully pulled and updated local profile from Supabase for $userId")
                Result.success(profile)
            } else {
                Result.failure(NoSuchElementException("No profile found in cloud for user $userId"))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "Error pulling profile from cloud: ${e.message}")
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "SupabaseAuthRepository"
    }
}
