package com.example.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.core.storage.StudentPreferences
import com.example.ui.screens.MainAppScreen
import com.example.ui.screens.NotificationSettingsScreen
import com.example.ui.screens.RoadmapScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.ProfileCompletionScreen
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.auth.PhoneLoginScreen
import com.example.ui.screens.auth.SignUpScreen
import com.example.ui.screens.auth.VerifyOtpScreen
import java.net.URLDecoder
import java.net.URLEncoder

object NexoraDestinations {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val SIGN_UP = "sign_up"
    const val PHONE_LOGIN = "phone_login"
    const val VERIFY_OTP = "verify_otp/{countryCode}/{phoneNumber}"
    fun verifyOtpRoute(countryCode: String, phoneNumber: String): String =
        "verify_otp/${URLEncoder.encode(countryCode, "UTF-8")}/$phoneNumber"

    const val SETUP = "student_identity_setup"
    const val MAIN = "main"
    const val SETTINGS = "settings"
    const val NOTIFICATION_SETTINGS = "notification_settings"
    const val ROADMAP = "roadmap"
}

/**
 * Main Navigation Host for NEXORA LEARN setting up the navigation graph from
 * Splash screen to Authentication, Student Identity Setup, Home Screen, and Settings Screen.
 */
@Composable
fun NexoraNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val observedProfile by StudentPreferences.currentProfile.collectAsState()

    NavHost(
        navController = navController,
        startDestination = NexoraDestinations.SPLASH,
        modifier = modifier
    ) {
        // 1. Splash Screen
        composable(
            route = NexoraDestinations.SPLASH,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            SplashScreen(
                onSplashFinished = {
                    val isSetupDone = StudentPreferences.hasCompletedSetup(context)
                    val isGuest = StudentPreferences.isGuestMode(context)
                    val hasName = StudentPreferences.hasSavedName(context)

                    val target = when {
                        isGuest -> NexoraDestinations.MAIN
                        isSetupDone || hasName -> NexoraDestinations.MAIN
                        else -> NexoraDestinations.LOGIN
                    }

                    navController.navigate(target) {
                        popUpTo(NexoraDestinations.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        // 3. Premium Login Screen
        composable(
            route = NexoraDestinations.LOGIN,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            LoginScreen(
                onLoginSuccess = {
                    val setupDone = StudentPreferences.hasCompletedSetup(context)
                    val target = if (setupDone) NexoraDestinations.MAIN else NexoraDestinations.SETUP
                    navController.navigate(target) {
                        popUpTo(NexoraDestinations.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToSignUp = {
                    navController.navigate(NexoraDestinations.SIGN_UP)
                },
                onNavigateToPhoneAuth = {
                    navController.navigate(NexoraDestinations.PHONE_LOGIN)
                },
                onExploreAsGuest = {
                    navController.navigate(NexoraDestinations.MAIN) {
                        popUpTo(NexoraDestinations.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        // 4. Sign Up Screen
        composable(
            route = NexoraDestinations.SIGN_UP,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            SignUpScreen(
                onNavigateToLogin = {
                    navController.navigate(NexoraDestinations.LOGIN) {
                        popUpTo(NexoraDestinations.SIGN_UP) { inclusive = true }
                    }
                },
                onNavigateToPhoneAuth = {
                    navController.navigate(NexoraDestinations.PHONE_LOGIN)
                },
                onSignUpSuccess = {
                    val setupDone = StudentPreferences.hasCompletedSetup(context)
                    val target = if (setupDone) NexoraDestinations.MAIN else NexoraDestinations.SETUP
                    navController.navigate(target) {
                        popUpTo(NexoraDestinations.SIGN_UP) { inclusive = true }
                    }
                }
            )
        }

        // 5. Phone Login Entry Screen
        composable(
            route = NexoraDestinations.PHONE_LOGIN,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            PhoneLoginScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onOtpSent = { countryCode, phoneNumber ->
                    navController.navigate(NexoraDestinations.verifyOtpRoute(countryCode, phoneNumber))
                }
            )
        }

        // 6. Verify Phone OTP Screen
        composable(
            route = NexoraDestinations.VERIFY_OTP,
            arguments = listOf(
                navArgument("countryCode") { type = NavType.StringType },
                navArgument("phoneNumber") { type = NavType.StringType }
            ),
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) { backStackEntry ->
            val rawCountry = backStackEntry.arguments?.getString("countryCode") ?: "+91"
            val countryCode = try { URLDecoder.decode(rawCountry, "UTF-8") } catch (_: Exception) { rawCountry }
            val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""

            VerifyOtpScreen(
                countryCode = countryCode,
                phoneNumber = phoneNumber,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onVerificationSuccess = {
                    val setupDone = StudentPreferences.hasCompletedSetup(context)
                    val target = if (setupDone) NexoraDestinations.MAIN else NexoraDestinations.SETUP
                    navController.navigate(target) {
                        popUpTo(NexoraDestinations.PHONE_LOGIN) { inclusive = true }
                    }
                }
            )
        }

        // 7. Student Setup Screen
        composable(
            route = NexoraDestinations.SETUP,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            ProfileCompletionScreen(
                onSetupComplete = {
                    navController.navigate(NexoraDestinations.MAIN) {
                        popUpTo(NexoraDestinations.SETUP) { inclusive = true }
                    }
                }
            )
        }

        // 8. Main App Screen (Home & Tabs)
        composable(
            route = NexoraDestinations.MAIN,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            val activeProfile = observedProfile ?: StudentPreferences.getStudentProfile(context)
            MainAppScreen(
                studentProfile = activeProfile,
                onOpenSettings = {
                    navController.navigate(NexoraDestinations.SETTINGS)
                },
                onSignOut = {
                    navController.navigate(NexoraDestinations.LOGIN) {
                        popUpTo(NexoraDestinations.MAIN) { inclusive = true }
                    }
                }
            )
        }

        // 9. Dedicated Settings Screen
        composable(
            route = NexoraDestinations.SETTINGS,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            val activeProfile = observedProfile ?: StudentPreferences.getStudentProfile(context)
            SettingsScreen(
                studentProfile = activeProfile,
                onBackClick = {
                    navController.popBackStack()
                },
                onSignOut = {
                    navController.navigate(NexoraDestinations.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToNotifications = {
                    navController.navigate(NexoraDestinations.NOTIFICATION_SETTINGS)
                }
            )
        }

        // 10. Notification Settings Screen
        composable(
            route = NexoraDestinations.NOTIFICATION_SETTINGS,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            NotificationSettingsScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // 11. Roadmap Screen
        composable(
            route = NexoraDestinations.ROADMAP,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            RoadmapScreen(
                onBackClick = { navController.popBackStack() },
                onSubjectClick = { /* Navigate to subject detail */ }
            )
        }
    }
}
