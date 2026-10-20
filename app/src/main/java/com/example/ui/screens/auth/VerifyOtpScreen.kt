package com.example.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.supabase.SupabaseAuthRepository
import com.example.ui.components.NexoraCard
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VerifyOtpScreen(
    countryCode: String,
    phoneNumber: String,
    onNavigateBack: () -> Unit,
    onVerificationSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authRepo = remember { SupabaseAuthRepository(context) }
    val keyboardController = LocalSoftwareKeyboardController.current

    var otpInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isResending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>("OTP sent via SMS. Please enter the 6-digit code.") }

    var resendCountdown by remember { mutableIntStateOf(60) }
    val canResend = resendCountdown <= 0

    val otpFocusRequester = remember { FocusRequester() }

    // Countdown Timer Loop
    LaunchedEffect(resendCountdown) {
        if (resendCountdown > 0) {
            delay(1000L)
            resendCountdown -= 1
        }
    }

    LaunchedEffect(Unit) {
        try {
            otpFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    fun handleVerify() {
        errorMessage = null
        val cleanOtp = otpInput.trim()
        if (cleanOtp.length != 6) {
            errorMessage = "Please enter the complete 6-digit verification code."
            return
        }

        isLoading = true
        keyboardController?.hide()

        scope.launch {
            try {
                val result = authRepo.verifyPhoneOtp(countryCode, phoneNumber, cleanOtp)
                if (result.isSuccess) {
                    onVerificationSuccess()
                } else {
                    errorMessage = result.exceptionOrNull()?.message ?: "Verification failed. Please check the code."
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to verify OTP."
            } finally {
                isLoading = false
            }
        }
    }

    fun handleResend() {
        if (!canResend || isResending) return
        errorMessage = null
        successMessage = null
        isResending = true

        scope.launch {
            try {
                val result = authRepo.resendPhoneOtp(countryCode, phoneNumber)
                if (result.isSuccess) {
                    resendCountdown = 60
                    successMessage = "New verification code sent via SMS."
                } else {
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to resend code."
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to resend OTP."
            } finally {
                isResending = false
            }
        }
    }

    val maskedPhone = remember(countryCode, phoneNumber) {
        val clean = phoneNumber.filter { it.isDigit() }
        if (clean.length > 4) {
            val prefix = clean.take(2)
            val suffix = clean.takeLast(2)
            val stars = "*".repeat(clean.length - 4)
            "$countryCode $prefix $stars $suffix"
        } else {
            "$countryCode $clean"
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("verify_otp_screen_root"),
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
            // Top Back Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .testTag("verify_otp_back_button")
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "Verify Code",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Header Emblem
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(NexoraPurple, NexoraCyan)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Verification Code",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Enter the 6-digit code sent to $maskedPhone",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // OTP Input Card
            NexoraCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp,
                contentPadding = 20.dp
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Enter 6-Digit Code",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Digit Display Boxes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until 6) {
                            val digit = otpInput.getOrNull(i)?.toString() ?: ""
                            val isFocused = otpInput.length == i || (i == 5 && otpInput.length == 6)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (digit.isNotEmpty()) NexoraCyan.copy(alpha = 0.12f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .border(
                                        width = if (isFocused) 2.dp else 1.dp,
                                        color = if (isFocused) NexoraCyan else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = digit,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    // Hidden/Focused Text Field catching keystrokes
                    OutlinedTextField(
                        value = otpInput,
                        onValueChange = { input ->
                            if (input.length <= 6) {
                                otpInput = input.filter { it.isDigit() }
                                errorMessage = null
                                if (otpInput.length == 6) {
                                    handleVerify()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(otpFocusRequester)
                            .testTag("otp_code_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { handleVerify() }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                }
            }

            // Success banner
            AnimatedVisibility(visible = successMessage != null && errorMessage == null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = NexoraSuccess.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NexoraSuccess.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = NexoraSuccess,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = successMessage.orEmpty(),
                            color = NexoraSuccess,
                            fontSize = 13.sp
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

            Spacer(modifier = Modifier.height(24.dp))

            // Verify Button
            Button(
                onClick = { handleVerify() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("verify_otp_submit_button"),
                enabled = !isLoading && otpInput.length == 6,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NexoraCyan,
                    contentColor = Color(0xFF070B19),
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
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
                        text = "Verifying Code...",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "Verify & Continue",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Resend OTP Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (canResend) {
                    TextButton(
                        onClick = { handleResend() },
                        enabled = !isResending,
                        modifier = Modifier.testTag("resend_otp_button")
                    ) {
                        if (isResending) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Resending...")
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Resend Code via SMS",
                                color = NexoraCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Resend code in ",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${resendCountdown}s",
                        color = NexoraCyan,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Change number link
            TextButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("change_phone_number_button")
            ) {
                Text(
                    text = "Incorrect number? Change Phone Number",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        }
    }
}
