package com.example.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PhoneAndroid
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
import com.example.core.supabase.SupabaseClientManager
import com.example.ui.components.NexoraCard
import com.example.ui.components.NexoraLogo
import com.example.ui.theme.*
import kotlinx.coroutines.launch

data class CountryCodeItem(
    val code: String,
    val country: String,
    val flag: String,
    val minLength: Int = 10
)

val COMMON_COUNTRY_CODES = listOf(
    CountryCodeItem("+91", "India", "🇮🇳", 10),
    CountryCodeItem("+1", "United States / Canada", "🇺🇸", 10),
    CountryCodeItem("+44", "United Kingdom", "🇬🇧", 10),
    CountryCodeItem("+971", "United Arab Emirates", "🇦🇪", 9),
    CountryCodeItem("+61", "Australia", "🇦🇺", 9),
    CountryCodeItem("+65", "Singapore", "🇸🇬", 8),
    CountryCodeItem("+966", "Saudi Arabia", "🇸🇦", 9)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneLoginScreen(
    onNavigateBack: () -> Unit,
    onOtpSent: (countryCode: String, phoneNumber: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authRepo = remember { SupabaseAuthRepository(context) }
    val keyboardController = LocalSoftwareKeyboardController.current

    var selectedCountry by remember { mutableStateOf(COMMON_COUNTRY_CODES.first()) }
    var showCountryPicker by remember { mutableStateOf(false) }
    var phoneInput by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isPhoneProviderUnconfigured by remember { mutableStateOf(false) }

    val phoneFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try {
            phoneFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    fun handleSendOtp() {
        errorMessage = null
        isPhoneProviderUnconfigured = false
        val clean = phoneInput.filter { it.isDigit() }
        if (clean.length < 8) {
            errorMessage = "Please enter a valid phone number (at least ${selectedCountry.minLength} digits for ${selectedCountry.country})."
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
                val result = authRepo.sendPhoneOtp(selectedCountry.code, clean)
                if (result.isSuccess) {
                    onOtpSent(selectedCountry.code, clean)
                } else {
                    val ex = result.exceptionOrNull()
                    val msg = ex?.message ?: "Failed to send verification code."
                    errorMessage = msg
                    if (msg.contains("Dashboard", ignoreCase = true) ||
                        msg.contains("provider", ignoreCase = true) ||
                        msg.contains("gateway", ignoreCase = true) ||
                        msg.contains("disabled", ignoreCase = true)
                    ) {
                        isPhoneProviderUnconfigured = true
                    }
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Network error. Please try again."
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("phone_login_screen_root"),
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
            // Top Navigation Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .testTag("phone_login_back_button")
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
                    text = "Phone Sign-In",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Header Emblem & Title
            NexoraLogo(
                emblemSize = 72.dp
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Enter Your Mobile Number",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "We will send an official 6-digit SMS verification code to log you securely into NEXORA LEARN.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Phone Input Card
            NexoraCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp,
                contentPadding = 20.dp
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Mobile Phone Number",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Country Code Button
                        Surface(
                            onClick = { showCountryPicker = true },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .height(56.dp)
                                .testTag("country_code_selector")
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 14.dp)
                                    .fillMaxHeight(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${selectedCountry.flag} ${selectedCountry.code}",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.Outlined.ArrowDropDown,
                                    contentDescription = "Select Country",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Phone Number Input
                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = {
                                if (it.length <= 15) {
                                    phoneInput = it.filter { ch -> ch.isDigit() }
                                    errorMessage = null
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .focusRequester(phoneFocusRequester)
                                .testTag("phone_number_input"),
                            placeholder = {
                                Text(
                                    text = "98765 43210",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    fontSize = 15.sp
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { handleSendOtp() }
                            ),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NexoraCyan,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                    }

                    // Explanatory badge
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = NexoraCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Standard operator SMS charges may apply.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
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
                    shape = RoundedCornerShape(16.dp),
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

            // Supabase Unconfigured Notice Card
            AnimatedVisibility(visible = isPhoneProviderUnconfigured) {
                NexoraCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    cornerRadius = 16.dp,
                    borderColor = NexoraGold.copy(alpha = 0.5f),
                    containerColor = NexoraGold.copy(alpha = 0.08f)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.WarningAmber,
                                contentDescription = null,
                                tint = NexoraGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Supabase Phone Provider Setup Required",
                                color = NexoraGold,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "To enable real Phone OTP in production, configure an SMS Provider (Twilio, MessageBird, or Vonage) under:\nSupabase Dashboard > Authentication > Providers > Phone.",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Send OTP Button
            Button(
                onClick = { handleSendOtp() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("send_phone_otp_button"),
                enabled = !isLoading && phoneInput.length >= 8,
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
                        text = "Sending OTP...",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "Send Verification Code",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Footer info
            Text(
                text = "Protected by Supabase Zero-Knowledge Authentication",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            )
        }
    }

    // Country Picker Dialog / BottomSheet
    if (showCountryPicker) {
        AlertDialog(
            onDismissRequest = { showCountryPicker = false },
            title = {
                Text(
                    text = "Select Country Code",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    COMMON_COUNTRY_CODES.forEach { item ->
                        val isSelected = selectedCountry.code == item.code
                        Surface(
                            onClick = {
                                selectedCountry = item
                                showCountryPicker = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) NexoraCyan.copy(alpha = 0.15f) else Color.Transparent,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, NexoraCyan) else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = item.flag, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = item.country,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = item.code,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) NexoraCyan else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCountryPicker = false }) {
                    Text("Close")
                }
            }
        )
    }
}
