package com.example.ui.screens

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.model.StudentProfile
import com.example.core.repository.LocalStudentProfileRepository
import com.example.core.storage.StudentPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _uploadResult = MutableStateFlow<Result<String>?>(null)
    val uploadResult: StateFlow<Result<String>?> = _uploadResult.asStateFlow()

    /**
     * Updates the user's profile image locally in StudentPreferences.
     */
    fun uploadProfilePicture(
        context: Context,
        uri: Uri,
        currentProfile: StudentProfile,
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            _isUploading.value = true
            _uploadResult.value = null

            val repo = LocalStudentProfileRepository(context)
            val finalPhotoUrl = uri.toString()

            repo.uploadProfileImage(currentProfile.userId, uri)
            val updatedProfile = StudentPreferences.savePhotoUrl(context, finalPhotoUrl)
            repo.saveProfile(updatedProfile)

            _uploadResult.value = Result.success(finalPhotoUrl)
            _isUploading.value = false

            onComplete(false, "Profile picture updated successfully!")
        }
    }

    /**
     * Updates the student's name in both local storage and cloud.
     */
    fun updateStudentName(
        context: Context,
        newName: String,
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            try {
                val trimmed = newName.trim()
                if (trimmed.length < 2) {
                    onComplete(true, "Name must be at least 2 characters")
                    return@launch
                }

                val repo = LocalStudentProfileRepository(context)
                val updatedProfile = StudentPreferences.saveStudentName(context, trimmed)
                
                // Sync to cloud
                val syncResult = repo.saveProfile(updatedProfile)
                
                if (syncResult.isSuccess) {
                    onComplete(false, "Name updated successfully!")
                } else {
                    onComplete(false, "Name saved locally, but cloud sync failed.")
                }
            } catch (e: Exception) {
                onComplete(true, "Error: ${e.message}")
            }
        }
    }

    private val _isLoadingProfile = MutableStateFlow(false)
    val isLoadingProfile: StateFlow<Boolean> = _isLoadingProfile.asStateFlow()

    /**
     * Refreshes the student profile from Supabase cloud.
     */
    fun refreshProfile(context: Context) {
        viewModelScope.launch {
            _isLoadingProfile.value = true
            try {
                val authRepo = com.example.core.supabase.SupabaseAuthRepository(context)
                if (authRepo.isUserSignedIn()) {
                    authRepo.pullProfileFromCloud()
                }
            } catch (_: Exception) {
            } finally {
                _isLoadingProfile.value = false
            }
        }
    }

    /**
     * Alias method for uploading selected image URI.
     */
    fun uploadSelectedImage(
        context: Context,
        uri: Uri,
        currentProfile: StudentProfile,
        onResult: ((Result<String>) -> Unit)? = null
    ) {
        uploadProfilePicture(context, uri, currentProfile) { _, message ->
            val result = Result.success(StudentPreferences.getStudentProfile(context).photoUrl ?: "")
            onResult?.invoke(result)
        }
    }

    /**
     * Function to upload image URI.
     */
    fun uploadImageUri(
        context: Context,
        uri: Uri,
        currentProfile: StudentProfile,
        onComplete: ((Result<String>) -> Unit)? = null
    ) {
        uploadSelectedImage(context, uri, currentProfile, onComplete)
    }
}
