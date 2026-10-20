package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.storage.StudentPreferences
import com.example.core.supabase.SupabaseAuthRepository
import com.example.core.supabase.SupabaseClientManager
import com.example.core.supabase.model.SupabaseStudentProfile
import com.example.core.util.StudentIdGenerator
import com.example.ui.animation.bounceClick
import com.example.ui.components.NexoraCard
import com.example.ui.components.NexoraLogo
import com.example.ui.theme.*
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

@Composable
fun ProfileCompletionScreen(
    onSetupComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    val initialProfile = remember { StudentPreferences.getStudentProfile(context) }

    var studentName by remember {
        mutableStateOf(StudentPreferences.getSavedStudentName(context).ifBlank { "" })
    }
    var candidateId by remember {
        mutableStateOf(
            if (initialProfile.studentId.isNotBlank()) {
                initialProfile.studentId
            } else {
                StudentIdGenerator.generateCandidateId()
            }
        )
    }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Fetch existing profile on launch to avoid generating new ID for returning users
    LaunchedEffect(Unit) {
        val authUserId = SupabaseClientManager.client?.auth?.currentUserOrNull()?.id
        if (authUserId != null && SupabaseClientManager.isValidUuid(authUserId)) {
            try {
                val client = SupabaseClientManager.client
                if (client != null) {
                    val remoteList = client.from("student_profiles")
                        .select {
                            filter {
                                eq("id", authUserId)
                            }
                        }.decodeList<SupabaseStudentProfile>()

                    if (remoteList.isNotEmpty()) {
                        val existing = remoteList.first()
                        val existingStudentId = existing.studentId.orEmpty()
                        val existingStudentName = existing.studentName.orEmpty()
                        if (existingStudentId.isNotBlank()) {
                            candidateId = existingStudentId
                        }
                        if (studentName.isBlank() || studentName == "Student" || studentName == "Class 12 Student") {
                            if (existingStudentName.isNotBlank()) {
                                studentName = existingStudentName
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileSetup", "Failed to fetch existing profile: ${e.message}")
            }
        }
    }

    fun handleSaveProfile() {
        errorMessage = null
        val trimmedName = studentName.trim()
        val trimmedId = candidateId.trim().uppercase()

        if (trimmedName.length < 2) {
            errorMessage = "Please enter your full name (at least 2 characters)."
            return
        }

        if (trimmedId.isBlank()) {
            errorMessage = "Please enter or confirm your Student ID."
            return
        }

        isSaving = true
        keyboardController?.hide()

        coroutineScope.launch {
            try {
                val authRepo = SupabaseAuthRepository(context)
                val authUserId = SupabaseClientManager.client?.auth?.currentUserOrNull()?.id

                if (authUserId == null || !SupabaseClientManager.isValidUuid(authUserId)) {
                    errorMessage = "Your session has expired. Please sign in again."
                    isSaving = false
                    return@launch
                }

                val result = authRepo.registerUniqueStudentId(
                    userId = authUserId,
                    studentName = trimmedName,
                    requestedStudentId = trimmedId
                )

                if (result.isSuccess) {
                    val finalId = result.getOrThrow()
                    val updatedProfile = initialProfile.copy(
                        studentName = trimmedName,
                        studentId = finalId,
                        userId = authUserId
                    )
                    StudentPreferences.setGuestMode(context, false)
                    StudentPreferences.updateStudentProfile(context, updatedProfile)
                    StudentPreferences.completeIdentitySetup(context, trimmedName, finalId)
                    onSetupComplete()
                } else {
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to save student profile."
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
            } finally {
                isSaving = false
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("profile_completion_screen_root"),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.systemBars
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Branding
            NexoraLogo(emblemSize = 80.dp)

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Finalize Your Profile",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "Set up your student name and unique institutional Student ID",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Profile Setup Form Card
            NexoraCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp,
                contentPadding = 22.dp
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    // Student Name
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "STUDENT FULL NAME",
                            color = NexoraCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        OutlinedTextField(
                            value = studentName,
                            onValueChange = {
                                studentName = it
                                errorMessage = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("setup_name_input"),
                            placeholder = { Text("Enter your full name (e.g. Diya Patel)") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Person, contentDescription = null, tint = NexoraCyan)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NexoraCyan,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                        )
                        Text(
                            text = "Used on official leaderboard rankings and study group cards",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }

                    // Student ID
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "INSTITUTIONAL STUDENT ID",
                            color = NexoraCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        OutlinedTextField(
                            value = candidateId,
                            onValueChange = {
                                candidateId = it.uppercase()
                                errorMessage = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("setup_student_id_input"),
                            placeholder = { Text("NX-2026-XXXX") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Badge, contentDescription = null, tint = NexoraCyan)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { handleSaveProfile() }),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NexoraCyan,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                        )
                        Text(
                            text = "Unique identifier format: NX-YYYY-XXXX (Pre-generated or institution assigned)",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Error banner
            AnimatedVisibility(visible = errorMessage != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WarningAmber,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = errorMessage.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Save / Complete Button
            Button(
                onClick = { handleSaveProfile() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("complete_setup_button"),
                enabled = !isSaving,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NexoraCyan,
                    contentColor = Color(0xFF070B19)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp,
                        color = Color(0xFF070B19)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Saving Profile...", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text("Complete Profile & Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
