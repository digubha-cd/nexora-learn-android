package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.constants.AppConstants
import com.example.core.model.StudentProfile
import com.example.core.model.Subject
import com.example.core.repository.FirestoreStudentProfileRepository
import com.example.core.repository.StudyPlannerRepository
import com.example.core.repository.SyllabusRepository
import com.example.core.storage.StudentPreferences
import com.example.ui.animation.bounceClick
import com.example.ui.animation.staggeredEntrance
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ProfileScreen(
    studentProfile: StudentProfile,
    isGuest: Boolean = false,
    onOpenLogin: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenEditProfile: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showEditNameDialog by remember { mutableStateOf(false) }
    var editedNameText by remember { mutableStateOf(studentProfile.studentName) }

    val isUploadingPhoto by viewModel.isUploading.collectAsStateWithLifecycle()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.uploadProfilePicture(context, uri, studentProfile) { _, toastMessage ->
                Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val completedChapterIds by SyllabusRepository.completedChapterIds.collectAsStateWithLifecycle()
    val totalChapters = SyllabusRepository.getTotalChaptersCount()
    val totalCompleted = SyllabusRepository.getTotalCompletedChaptersCount()
    val overallProgress = SyllabusRepository.getOverallProgress()
    val overallPercentInt = (overallProgress * 100).toInt()
    val totalEarnedPoints = SyllabusRepository.getTotalEarnedPoints()
    val streak = StudyPlannerRepository.getStudyStreak()

    // Edit Name Dialog
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = {
                Text(text = "Edit Name", color = NexoraTextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                OutlinedTextField(
                    value = editedNameText,
                    onValueChange = { editedNameText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter your name") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
            },
            confirmButton = {
                NexoraPrimaryButton(
                    text = "Save",
                    onClick = {
                        val trimmed = editedNameText.trim()
                        if (trimmed.isNotEmpty()) {
                            viewModel.updateStudentName(context, trimmed) { isError, message ->
                                if (!isError) {
                                    showEditNameDialog = false
                                }
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel", color = NexoraTextSecondary)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .testTag("profile_screen")
    ) {
        // 1. Premium Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NexoraLogo(emblemSize = 32.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Student Profile",
                        color = NexoraTextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Identity & Progress",
                        color = NexoraTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(44.dp)
                    .background(NexoraSurfaceVariant, CircleShape)
                    .bounceClick { onOpenSettings() }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = NexoraTextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Profile Completion logic moved out of LazyListScope
        val fields = listOf(
            studentProfile.studentName.isNotBlank() && studentProfile.studentName != "Student" && studentProfile.studentName != "Class 12 Student" && studentProfile.studentName != "Google Student",
            !studentProfile.email.isNullOrBlank(),
            studentProfile.medium.isNotBlank(),
            studentProfile.classLevel.isNotBlank(),
            studentProfile.targetMarks.isNotBlank()
        )
        val completion = (fields.count { it }.toFloat() / fields.size)
        val isProfileIncomplete = completion < 1f
        val isLoadingProfile by viewModel.isLoadingProfile.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            if (studentProfile.studentId.isBlank() || studentProfile.studentId == "STU-GUEST") {
                viewModel.refreshProfile(context)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Profile Card
            item {
                NexoraCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggeredEntrance(index = 0),
                    containerColor = Color.White,
                    borderColor = NexoraCyan.copy(alpha = 0.2f),
                    cornerRadius = 32.dp,
                    contentPadding = 24.dp,
                    elevation = 0.dp
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        // Avatar Display with Camera Edit Overlay
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clickable(enabled = !isUploadingPhoto) {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            NexoraAvatar(
                                photoUrl = studentProfile.photoUrl,
                                name = studentProfile.studentName,
                                avatarColorHex = studentProfile.avatarColorHex,
                                size = 84.dp
                            )

                            if (isUploadingPhoto) {
                                Box(
                                    modifier = Modifier
                                        .size(84.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.4f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(28.dp)
                                    .background(NexoraCyan, CircleShape)
                                    .border(2.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Change Profile Picture",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = studentProfile.studentName.ifBlank { "Student" },
                                color = NexoraTextPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = NexoraCyan,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { showEditNameDialog = true }
                            )
                        }
                        
                        Text(
                            text = "Class 12 Commerce Student",
                            color = NexoraTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Stats Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatMetricCard(
                        label = "Study Points",
                        value = "${studentProfile.competitionPoints}",
                        icon = Icons.Default.Stars,
                        color = NexoraGold,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricCard(
                        label = "Day Streak",
                        value = "${streak.currentStreak} Days",
                        icon = Icons.Default.Whatshot,
                        color = NexoraPink,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricCard(
                        label = "Longest Streak",
                        value = "${streak.bestStreak} Days",
                        icon = Icons.Default.EmojiEvents,
                        color = NexoraCyan,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (isProfileIncomplete) {
                item {
                    NexoraCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick { onOpenEditProfile() }
                            .staggeredEntrance(index = 1),
                        containerColor = NexoraPink.copy(alpha = 0.05f),
                        cornerRadius = 24.dp,
                        contentPadding = 20.dp,
                        borderColor = NexoraPink.copy(alpha = 0.2f),
                        elevation = 0.dp
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("✍️", fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Complete Your Profile",
                                        color = NexoraTextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "${(completion * 100).toInt()}%",
                                    color = NexoraPink,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { completion },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = NexoraPink,
                                trackColor = NexoraPink.copy(alpha = 0.1f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Set your real name and board details to be recognized on the leaderboard.",
                                color = NexoraTextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Student ID Card (Requirement 5)
            item {
                val clipboardManager = LocalClipboardManager.current
                val hasRealId = studentProfile.studentId.isNotBlank() && studentProfile.studentId.startsWith("NX-")
                val displayId = if (hasRealId) studentProfile.studentId else if (isGuest) "GUEST MODE" else "Loading..."
                
                NexoraCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggeredEntrance(index = 2),
                    containerColor = Color.White,
                    borderColor = if (hasRealId) NexoraCyan.copy(alpha = 0.2f) else NexoraBorder,
                    cornerRadius = 24.dp,
                    contentPadding = 16.dp,
                    elevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "PERMANENT STUDENT ID",
                                color = NexoraTextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (isLoadingProfile && !hasRealId) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = NexoraCyan)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "Verifying Identity...", color = NexoraTextMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                }
                            } else {
                                Text(
                                    text = displayId,
                                    color = if (hasRealId) NexoraTextPrimary else NexoraTextMuted,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                        
                        if (hasRealId) {
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(displayId))
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(NexoraSurfaceVariant, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = NexoraCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else if (!isGuest && !isLoadingProfile) {
                            TextButton(onClick = { viewModel.refreshProfile(context) }) {
                                Text("Retry", color = NexoraCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Achievements (RESTORED)
            item {
                StudyBadgesCard(userId = studentProfile.userId)
            }

            // Study Activity & Insights
            item {
                WeeklyInsightsCard()
            }

            // Progress Summary
            item {
                NexoraCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = Color.White,
                    borderColor = NexoraCyan.copy(alpha = 0.2f),
                    cornerRadius = 28.dp,
                    contentPadding = 20.dp,
                    elevation = 0.dp
                ) {
                    Column {
                        Text(text = "Overall Course Completion", color = NexoraTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(60.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = { overallProgress },
                                    modifier = Modifier.fillMaxSize(),
                                    color = NexoraCyan,
                                    trackColor = NexoraSurfaceVariant,
                                    strokeWidth = 6.dp,
                                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                                Text(
                                    text = "$overallPercentInt%",
                                    color = NexoraTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(20.dp))
                            
                            Column {
                                Text(text = "$totalCompleted / $totalChapters Chapters", color = NexoraTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                                Text(text = "Total Syllabus Progress", color = NexoraTextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Subject Breakdown
            item {
                Text(text = "Subject Performance", color = NexoraTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }

            itemsIndexed(Subject.OFFICIAL_SUBJECTS) { index, subject ->
                val style = getSubjectStyle(subject.id)
                val chapters = SyllabusRepository.getChapters(subject.id)
                val completed = chapters.count { completedChapterIds.contains(it.id) }
                val total = chapters.size
                val progress = if (total > 0) completed.toFloat() / total else 0f

                ProfileSubjectItem(
                    name = subject.name,
                    progress = progress,
                    info = "$completed/$total",
                    color = style.accentColor,
                    pastelBg = style.pastelBg
                )
            }

            // Edit Profile Button
            item {
                Spacer(modifier = Modifier.height(12.dp))
                NexoraSecondaryButton(
                    text = "Edit Profile & Privacy Settings",
                    onClick = onOpenEditProfile,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Account Actions
            item {
                Spacer(modifier = Modifier.height(8.dp))
                if (isGuest) {
                    NexoraPrimaryButton(
                        text = "Sign in to Backup Data",
                        onClick = onOpenLogin,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    NexoraSecondaryButton(
                        text = "Sign Out",
                        onClick = onSignOut,
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = NexoraRose,
                        contentColor = NexoraError
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                NexoraCreatorCredit()
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun StatMetricCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    NexoraCard(
        modifier = modifier,
        containerColor = Color.White,
        borderColor = NexoraCyan.copy(alpha = 0.2f),
        cornerRadius = 24.dp,
        contentPadding = 12.dp,
        elevation = 0.dp
    ) {
        Column {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, color = NexoraTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Text(text = label, color = NexoraTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ProfileSubjectItem(
    name: String,
    progress: Float,
    info: String,
    color: Color,
    pastelBg: Color
) {
    NexoraCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = Color.White,
        borderColor = NexoraCyan.copy(alpha = 0.2f),
        cornerRadius = 20.dp,
        contentPadding = 12.dp,
        elevation = 0.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(pastelBg.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = name, color = NexoraTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(text = info, color = NexoraTextSecondary, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = color,
                    trackColor = NexoraSurfaceVariant
                )
            }
        }
    }
}
