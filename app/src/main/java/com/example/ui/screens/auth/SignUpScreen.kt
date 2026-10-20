package com.example.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.storage.StudentPreferences
import com.example.core.supabase.SignUpOutcome
import com.example.core.supabase.SupabaseAuthRepository
import com.example.core.supabase.SupabaseClientManager
import com.example.ui.components.NexoraCard
import com.example.ui.components.NexoraLogo
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SignUpScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToPhoneAuth: () -> Unit,
    onSignUpSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authRepo = remember { SupabaseAuthRepository(context) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var nameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }

    var selectedMedium by remember { mutableStateOf("Gujarati Medium") }
    var selectedTargetMarks by remember { mutableStateOf("90+ Marks") }

    val mediumOptions = remember { listOf("Gujarati Medium", "English Medium") }
    val targetMarksOptions = remember { listOf("90+ Marks", "95+ Marks", "Pass with Distinction") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var verificationRequiredMessage by remember { mutableStateOf<String?>(null) }

    fun handleSignUp() {
        errorMessage = null
        verificationRequiredMessage = null

        val trimmedName = nameInput.trim()
        val trimmedEmail = emailInput.trim()
        val password = passwordInput

        if (trimmedName.length < 2) {
            errorMessage = "Please enter your full name (at least 2 characters)."
            return
        }

        if (trimmedEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            errorMessage = "Please enter a valid email address."
            return
        }

        if (password.length < 6) {
            errorMessage = "Password must be at least 6 characters long."
            return
        }

        if (password != confirmPasswordInput) {
            errorMessage = "Passwords do not match. Please re-enter your password."
            return
        }

        if (!SupabaseClientManager.isConfigured(context)) {
            errorMessage = "Supabase Publishable Key not configured."
            return
        }

        isLoading = true
        keyboardController?.hide()

        scope.launch {
            try {
                val result = authRepo.signUp(
                    email = trimmedEmail,
                    password = password,
                    studentName = trimmedName,
                    targetMarks = selectedTargetMarks,
                    medium = selectedMedium
                )

                if (result.isSuccess) {
                    when (val outcome = result.getOrThrow()) {
                        is SignUpOutcome.LoggedIn -> {
                            onSignUpSuccess()
                        }
                        is SignUpOutcome.VerificationRequired -> {
                            verificationRequiredMessage = outcome.message
                        }
                    }
                } else {
                    errorMessage = result.exceptionOrNull()?.message ?: "Sign-up failed. Please try again."
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Sign-up error occurred."
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("signup_screen_root"),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.systemBars
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateToLogin,
                    modifier = Modifier
                        .testTag("signup_back_to_login_button")
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back to Login",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "Create Account",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Branding Emblem
            NexoraLogo(
                emblemSize = 68.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Join NEXORA LEARN",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Create your student account to sync syllabus progress, study sessions, and climb the leaderboard.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Form Fields Card
            NexoraCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp,
                contentPadding = 20.dp
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Full Name
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = {
                            nameInput = it
                            errorMessage = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("signup_name_input"),
                        label = { Text("Full Student Name") },
                        placeholder = { Text("e.g. Diya Patel") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Person, contentDescription = null, tint = NexoraCyan)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NexoraCyan,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    )

                    // Email Address
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = {
                            emailInput = it
                            errorMessage = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("signup_email_input"),
                        label = { Text("Email Address") },
                        placeholder = { Text("student@example.com") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Email, contentDescription = null, tint = NexoraCyan)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NexoraCyan,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    )

                    // Password
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = {
                            passwordInput = it
                            errorMessage = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("signup_password_input"),
                        label = { Text("Password (min 6 characters)") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Lock, contentDescription = null, tint = NexoraCyan)
                        },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NexoraCyan,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    )

                    // Confirm Password
                    OutlinedTextField(
                        value = confirmPasswordInput,
                        onValueChange = {
                            confirmPasswordInput = it
                            errorMessage = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("signup_confirm_password_input"),
                        label = { Text("Confirm Password") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Lock, contentDescription = null, tint = NexoraCyan)
                        },
                        trailingIcon = {
                            IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                                Icon(
                                    imageVector = if (showConfirmPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = "Toggle confirm password visibility"
                                )
                            }
                        },
                        visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { handleSignUp() }
                        ),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NexoraCyan,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    )

                    // Medium Selection
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Medium of Instruction",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            mediumOptions.forEach { medium ->
                                val isSelected = selectedMedium == medium
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedMedium = medium },
                                    label = { Text(medium, fontSize = 12.sp) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NexoraCyan.copy(alpha = 0.2f),
                                        selectedLabelColor = NexoraCyan
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = if (isSelected) NexoraCyan else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    )
                                )
                            }
                        }
                    }

                    // Target Marks Selection
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Target Score",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            targetMarksOptions.forEach { target ->
                                val isSelected = selectedTargetMarks == target
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedTargetMarks = target },
                                    label = { Text(target, fontSize = 12.sp) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NexoraPurple.copy(alpha = 0.2f),
                                        selectedLabelColor = NexoraPurple
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = if (isSelected) NexoraPurple else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    )
                                )
                            }
                        }
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
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
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

            // Verification Required Alert Dialog
            if (verificationRequiredMessage != null) {
                AlertDialog(
                    onDismissRequest = {
                        verificationRequiredMessage = null
                        onNavigateToLogin()
                    },
                    title = {
                        Text(
                            text = "Verification Email Sent",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(
                            text = verificationRequiredMessage.orEmpty(),
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                verificationRequiredMessage = null
                                onNavigateToLogin()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NexoraCyan)
                        ) {
                            Text("Go to Sign In", color = Color(0xFF070B19), fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Create Account Button
            Button(
                onClick = { handleSignUp() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("signup_submit_button"),
                enabled = !isLoading,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NexoraCyan,
                    contentColor = Color(0xFF070B19)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp,
                        color = Color(0xFF070B19)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Creating Account...",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "Create Account",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Alternative: Phone Sign-In (Coming Soon)
            OutlinedButton(
                onClick = { /* Disabled */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .testTag("signup_with_phone_button"),
                enabled = false,
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PhoneAndroid,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Mobile OTP",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NexoraGold.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NexoraGold.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "COMING SOON",
                            color = NexoraGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Footer Link: Already have an account? Sign In
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account? ",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
                Text(
                    text = "Sign In",
                    color = NexoraCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onNavigateToLogin() }
                        .testTag("signup_to_login_link")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
