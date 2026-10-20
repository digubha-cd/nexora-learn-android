package com.example.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import com.example.core.supabase.SupabaseAuthRepository
import com.example.core.supabase.SupabaseClientManager
import com.example.ui.components.OfficialLogo
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit = {},
    onNavigateToPhoneAuth: () -> Unit = {},
    onExploreAsGuest: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authRepo = remember { SupabaseAuthRepository(context) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var forgotPasswordEmail by remember { mutableStateOf("") }
    var forgotPasswordStatus by remember { mutableStateOf<String?>(null) }
    var isResettingPassword by remember { mutableStateOf(false) }

    val sessionStatus by SupabaseClientManager.sessionStatusFlow.collectAsState()
    val currentUser by SupabaseClientManager.currentUserFlow.collectAsState()

    // Automatically navigate when session becomes authenticated (handles Google Sign-In redirect)
    LaunchedEffect(sessionStatus, currentUser) {
        if (sessionStatus is io.github.jan.supabase.auth.status.SessionStatus.Authenticated || currentUser != null) {
            onLoginSuccess()
        }
    }

    fun handleSignIn() {
        errorMessage = null
        infoMessage = null

        val trimmedEmail = emailInput.trim()
        val password = passwordInput

        if (trimmedEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            errorMessage = "Please enter a valid email address."
            return
        }

        if (password.isBlank()) {
            errorMessage = "Please enter your password."
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
                val result = authRepo.signIn(trimmedEmail, password)
                if (result.isSuccess) {
                    onLoginSuccess()
                } else {
                    errorMessage = result.exceptionOrNull()?.message ?: "Sign-in failed. Please check credentials."
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Sign-in error occurred."
            } finally {
                isLoading = false
            }
        }
    }

    fun handleGoogleSignIn() {
        errorMessage = null
        infoMessage = null

        if (!SupabaseClientManager.isConfigured(context)) {
            errorMessage = "Supabase Publishable Key not configured."
            return
        }

        isGoogleLoading = true
        scope.launch {
            try {
                val result = authRepo.signInWithGoogle()
                if (result.isSuccess) {
                    val isAlreadyAuth = SupabaseClientManager.auth?.currentUserOrNull() != null
                    if (isAlreadyAuth) {
                        onLoginSuccess()
                    } else {
                        infoMessage = "Opening Google Sign-In in browser... Complete sign-in to return."
                    }
                } else {
                    errorMessage = result.exceptionOrNull()?.message ?: "Google Sign-In failed to start."
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Google Sign-In error."
            } finally {
                isGoogleLoading = false
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF050811))
            .testTag("login_screen_root")
    ) {
        // Ambient cosmic glow: Cyan glow at top-center, subtle violet glow at right
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x2800E5FF),
                        Color(0x1006B6D4),
                        Color(0x00000000)
                    ),
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.10f),
                    radius = size.width * 0.60f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x208B5CF6),
                        Color(0x0C7C3AED),
                        Color(0x00000000)
                    ),
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.88f, size.height * 0.40f),
                    radius = size.width * 0.50f
                )
            )
        }

        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.systemBars
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 440.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))

                        // Centered Official Logo
                        OfficialLogo(
                            size = 110.dp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Target 90+ Badge
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = Color(0x1F00E5FF),
                            border = BorderStroke(1.dp, Color(0x4D00E5FF))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(NexoraCyan)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "CLASS 12 COMMERCE · TARGET 90+",
                                    color = NexoraCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Welcome Back, Scholar",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "Sign in to access your Gujarat Board study roadmap",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Premium Material 3 Login Form Card
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            color = Color(0xFF0C1324),
                            border = BorderStroke(
                                1.dp,
                                Brush.linearGradient(
                                    listOf(
                                        Color(0x5500E5FF),
                                        Color(0x221E293B),
                                        Color(0x448B5CF6)
                                    )
                                )
                            ),
                            shadowElevation = 8.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(22.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Email Field
                                OutlinedTextField(
                                    value = emailInput,
                                    onValueChange = {
                                        emailInput = it
                                        errorMessage = null
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("login_email_input"),
                                    label = { Text("Email Address") },
                                    placeholder = { Text("student@example.com") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Outlined.Email,
                                            contentDescription = null,
                                            tint = NexoraCyan
                                        )
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
                                        unfocusedBorderColor = Color(0xFF1E293B),
                                        focusedLabelColor = NexoraCyan,
                                        unfocusedLabelColor = Color(0xFF94A3B8),
                                        focusedContainerColor = Color(0xFF080D1A),
                                        unfocusedContainerColor = Color(0xFF080D1A),
                                        focusedTextColor = Color(0xFFF8FAFC),
                                        unfocusedTextColor = Color(0xFFF1F5F9),
                                        cursorColor = NexoraCyan
                                    )
                                )

                                // Password Field
                                OutlinedTextField(
                                    value = passwordInput,
                                    onValueChange = {
                                        passwordInput = it
                                        errorMessage = null
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("login_password_input"),
                                    label = { Text("Password") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Outlined.Lock,
                                            contentDescription = null,
                                            tint = NexoraPurple
                                        )
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { showPassword = !showPassword }) {
                                            Icon(
                                                imageVector = if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                                contentDescription = "Toggle password visibility",
                                                tint = Color(0xFF94A3B8)
                                            )
                                        }
                                    },
                                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = { handleSignIn() }
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = NexoraCyan,
                                        unfocusedBorderColor = Color(0xFF1E293B),
                                        focusedLabelColor = NexoraCyan,
                                        unfocusedLabelColor = Color(0xFF94A3B8),
                                        focusedContainerColor = Color(0xFF080D1A),
                                        unfocusedContainerColor = Color(0xFF080D1A),
                                        focusedTextColor = Color(0xFFF8FAFC),
                                        unfocusedTextColor = Color(0xFFF1F5F9),
                                        cursorColor = NexoraCyan
                                    )
                                )

                                // Forgot Password Link
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        text = "Forgot Password?",
                                        color = NexoraCyan,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier
                                            .clickable {
                                                forgotPasswordEmail = emailInput
                                                forgotPasswordStatus = null
                                                showForgotPasswordDialog = true
                                            }
                                            .testTag("forgot_password_button")
                                    )
                                }

                                // Primary Sign In Button
                                Button(
                                    onClick = { handleSignIn() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("login_submit_button"),
                                    enabled = !isLoading && !isGoogleLoading,
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Transparent,
                                        disabledContainerColor = Color(0xFF1E293B)
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .then(
                                                if (!isLoading && !isGoogleLoading) {
                                                    Modifier.background(
                                                        Brush.horizontalGradient(
                                                            listOf(
                                                                Color(0xFF00E5FF),
                                                                Color(0xFF0070F3),
                                                                Color(0xFF8B5CF6)
                                                            )
                                                        )
                                                    )
                                                } else {
                                                    Modifier.background(Color(0xFF1E293B))
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isLoading) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(20.dp),
                                                    strokeWidth = 2.5.dp,
                                                    color = Color.White
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = "Signing In...",
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = "Sign In",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF050814)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Info or Error Banners
                        AnimatedVisibility(visible = infoMessage != null && errorMessage == null) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0x2600E5FF),
                                border = BorderStroke(1.dp, NexoraCyan.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = NexoraCyan
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = infoMessage.orEmpty(),
                                        color = Color(0xFFE2E8F0),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(visible = errorMessage != null) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0x33EF4444),
                                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.WarningAmber,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = errorMessage.orEmpty(),
                                        color = Color(0xFFFCA5A5),
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Divider: OR CONTINUE WITH
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "OR CONTINUE WITH",
                                color = Color(0xFF64748B),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(horizontal = 14.dp)
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = Color(0xFF1E293B)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Alternative Auth Buttons Column
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Continue with Google Button
                            OutlinedButton(
                                onClick = { handleGoogleSignIn() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("google_signin_button"),
                                enabled = !isLoading && !isGoogleLoading,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color(0xFF0C1324),
                                    contentColor = Color.White
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (isGoogleLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = NexoraCyan
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Connecting to Google...", color = Color.White)
                                    } else {
                                        Text(
                                            text = "G",
                                            color = Color(0xFF4285F4),
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Continue with Google",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            // Mobile OTP Button with "Coming Soon" badge
                            OutlinedButton(
                                onClick = { /* Disabled */ },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 50.dp)
                                    .testTag("phone_signin_button"),
                                enabled = false,
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                border = BorderStroke(1.dp, Color(0xFF1E293B).copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    disabledContainerColor = Color(0xFF080D1A).copy(alpha = 0.6f),
                                    disabledContentColor = Color(0xFF64748B)
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
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(19.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Mobile OTP",
                                            color = Color(0xFF64748B),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = NexoraGold.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, NexoraGold.copy(alpha = 0.4f))
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
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Footer Links: Create Account
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Don't have an account? ",
                                color = Color(0xFF94A3B8),
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Create Account",
                                color = NexoraCyan,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { onNavigateToSignUp() }
                                    .testTag("create_account_link")
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Guest Mode Option
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = Color(0xFF0C1324),
                            border = BorderStroke(1.dp, Color(0xFF1E293B)),
                            modifier = Modifier
                                .clickable {
                                    StudentPreferences.setGuestMode(context, true)
                                    onExploreAsGuest()
                                }
                                .testTag("guest_mode_button")
                        ) {
                            Text(
                                text = "Continue as Guest (Offline)",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isResettingPassword) {
                    showForgotPasswordDialog = false
                    forgotPasswordStatus = null
                }
            },
            containerColor = Color(0xFF0C1324),
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "Reset Password",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter your registered email address and we'll send you a password reset link.",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    OutlinedTextField(
                        value = forgotPasswordEmail,
                        onValueChange = { forgotPasswordEmail = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Email Address") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NexoraCyan,
                            unfocusedBorderColor = Color(0xFF1E293B),
                            focusedLabelColor = NexoraCyan,
                            unfocusedLabelColor = Color(0xFF94A3B8),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = NexoraCyan
                        )
                    )

                    if (forgotPasswordStatus != null) {
                        Text(
                            text = forgotPasswordStatus.orEmpty(),
                            color = if (forgotPasswordStatus?.contains("sent", ignoreCase = true) == true) NexoraSuccess else NexoraError,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = forgotPasswordEmail.trim()
                        if (trimmed.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()) {
                            forgotPasswordStatus = "Please enter a valid email address."
                            return@Button
                        }
                        isResettingPassword = true
                        forgotPasswordStatus = null
                        scope.launch {
                            try {
                                val result = authRepo.resetPassword(trimmed)
                                if (result.isSuccess) {
                                    forgotPasswordStatus = "Reset link sent! Please check your email inbox."
                                } else {
                                    forgotPasswordStatus = result.exceptionOrNull()?.message ?: "Failed to send reset link."
                                }
                            } catch (e: Exception) {
                                forgotPasswordStatus = e.message ?: "Failed to reset password."
                            } finally {
                                isResettingPassword = false
                            }
                        }
                    },
                    enabled = !isResettingPassword,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NexoraCyan,
                        contentColor = Color(0xFF050814)
                    )
                ) {
                    if (isResettingPassword) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF050814))
                    } else {
                        Text("Send Link", color = Color(0xFF050814), fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showForgotPasswordDialog = false
                        forgotPasswordStatus = null
                    },
                    enabled = !isResettingPassword
                ) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }
}
