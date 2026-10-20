package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.StudentProfile
import com.example.core.repository.LocalStudentProfileRepository
import com.example.core.storage.StudentPreferences
import com.example.ui.animation.bounceClick
import com.example.ui.components.NexoraCard
import com.example.ui.components.NexoraPrimaryButton
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun EditProfileScreen(
    studentProfile: StudentProfile,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var nameInput by remember { mutableStateOf(studentProfile.studentName) }
    var statusInput by remember { mutableStateOf(studentProfile.statusText) }
    var leaderboardOptIn by remember { mutableStateOf(studentProfile.leaderboardOptIn) }
    var isSaving by remember { mutableStateOf(false) }

    BackHandler {
        onBackClick()
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("edit_profile_screen"),
        containerColor = NexoraBackground,
        contentWindowInsets = WindowInsets.systemBars
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(44.dp)
                    .background(NexoraSurfaceVariant, CircleShape)
                    .bounceClick { onBackClick() }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = NexoraTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Edit Profile & Privacy",
                    color = NexoraTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Manage your public student identity",
                    color = NexoraTextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                NexoraCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24.dp,
                    contentPadding = 24.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                                tint = NexoraCyan,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Personal Information",
                                color = NexoraTextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_name_input"),
                            label = { Text("Display Name") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NexoraCyan,
                                unfocusedBorderColor = NexoraBorder,
                                focusedContainerColor = NexoraBackground,
                                unfocusedContainerColor = NexoraBackground
                            )
                        )

                        OutlinedTextField(
                            value = statusInput,
                            onValueChange = { statusInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Status / Goal Text") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NexoraCyan,
                                unfocusedBorderColor = NexoraBorder,
                                focusedContainerColor = NexoraBackground,
                                unfocusedContainerColor = NexoraBackground
                            )
                        )
                    }
                }
            }

            item {
                NexoraCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24.dp,
                    contentPadding = 24.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Visibility,
                                contentDescription = null,
                                tint = NexoraCyan,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Privacy & Visibility Settings",
                                color = NexoraTextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Show on Global Leaderboard",
                                    color = NexoraTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Allow other students to see your rank and study score.",
                                    color = NexoraTextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = leaderboardOptIn,
                                onCheckedChange = { leaderboardOptIn = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NexoraCyan,
                                    checkedTrackColor = NexoraCyan.copy(alpha = 0.4f)
                                )
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                NexoraPrimaryButton(
                    text = if (isSaving) "Saving to Supabase..." else "Save Changes",
                    onClick = {
                        val trimmedName = nameInput.trim()
                        if (trimmedName.isEmpty()) {
                            Toast.makeText(context, "Please enter a valid display name", Toast.LENGTH_SHORT).show()
                            return@NexoraPrimaryButton
                        }
                        isSaving = true
                        scope.launch {
                            try {
                                val updated = studentProfile.copy(
                                    studentName = trimmedName,
                                    statusText = statusInput.trim(),
                                    leaderboardOptIn = leaderboardOptIn,
                                    updatedAt = System.currentTimeMillis()
                                )
                                val repo = LocalStudentProfileRepository(context)
                                repo.saveProfile(updated)
                                Toast.makeText(context, "Profile updated & synced to Supabase!", Toast.LENGTH_SHORT).show()
                                onBackClick()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_profile_button"),
                    enabled = !isSaving
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
    }
}
