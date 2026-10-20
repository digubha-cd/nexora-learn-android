package com.example.core.supabase

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Singleton manager for the official Supabase Kotlin client in NEXORA LEARN.
 *
 * Configures the Supabase Project URL and Publishable Key safely:
 * - Reads project URL and publishable key from BuildConfig (injected via .env or AI Studio Secrets)
 * - Supports fallback to user-provided publishable key saved in SharedPreferences
 * - Never puts secret keys or database passwords in source code
 */
object SupabaseClientManager {

    private const val TAG = "SupabaseClientManager"
    private const val PREFS_NAME = "nexora_supabase_prefs"
    private const val KEY_CUSTOM_PUBLISHABLE_KEY = "custom_publishable_key"

    // Default Supabase project URL specified in project brief
    const val DEFAULT_SUPABASE_URL = "https://mysnralzdpusuuquhxlf.supabase.co"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var _supabaseClient: SupabaseClient? = null
    val client: SupabaseClient?
        get() = _supabaseClient

    val auth: Auth?
        get() = _supabaseClient?.auth

    val postgrest: Postgrest
        get() = _supabaseClient?.postgrest ?: error("Supabase client is not initialized")

    private val _isConfiguredFlow = MutableStateFlow(false)
    val isConfiguredFlow: StateFlow<Boolean> = _isConfiguredFlow.asStateFlow()

    private val _currentUserFlow = MutableStateFlow<UserInfo?>(null)
    val currentUserFlow: StateFlow<UserInfo?> = _currentUserFlow.asStateFlow()

    private val _sessionStatusFlow = MutableStateFlow<SessionStatus?>(null)
    val sessionStatusFlow: StateFlow<SessionStatus?> = _sessionStatusFlow.asStateFlow()

    private var appContext: Context? = null

    /**
     * Initializes the Supabase client using safe configuration.
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext
        val url = getSupabaseUrl()
        val key = getPublishableKey(context)

        if (key.isBlank() || key == "YOUR_SUPABASE_PUBLISHABLE_KEY") {
            Log.w(TAG, "Supabase publishable key is not yet configured. Local study features remain 100% active.")
            _isConfiguredFlow.value = false
            return
        }

        try {
            _supabaseClient = createSupabaseClient(
                supabaseUrl = url,
                supabaseKey = key
            ) {
                install(Auth) {
                    scheme = "com.aistudio.nexoralearn.auth"
                    host = "callback"
                    defaultRedirectUrl = "com.aistudio.nexoralearn.auth://callback"
                }
                install(Postgrest)
            }

            _isConfiguredFlow.value = true
            Log.i(TAG, "Supabase Kotlin client initialized successfully for $url")

            // Monitor session state
            scope.launch {
                try {
                    _supabaseClient?.auth?.sessionStatus?.collect { status ->
                        _sessionStatusFlow.value = status
                        when (status) {
                            is SessionStatus.Authenticated -> {
                                _currentUserFlow.value = _supabaseClient?.auth?.currentUserOrNull()
                            }
                            is SessionStatus.NotAuthenticated -> {
                                _currentUserFlow.value = null
                            }
                            else -> {}
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error collecting Supabase session status: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Supabase client: ${e.message}", e)
            _isConfiguredFlow.value = false
        }
    }

    fun getSupabaseUrl(): String {
        return try {
            val configUrl = BuildConfig.SUPABASE_URL
            if (!configUrl.isNullOrBlank() && configUrl.startsWith("http")) configUrl else DEFAULT_SUPABASE_URL
        } catch (_: Exception) {
            DEFAULT_SUPABASE_URL
        }
    }

    fun getPublishableKey(context: Context): String {
        // 1. Check custom runtime key stored in secure local SharedPreferences
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val customKey = prefs.getString(KEY_CUSTOM_PUBLISHABLE_KEY, "")?.trim().orEmpty()
        if (customKey.isNotBlank()) {
            return customKey
        }

        // 2. Check BuildConfig injected via Secrets Gradle Plugin (.env) or fallback to default production key
        return try {
            val key = BuildConfig.SUPABASE_PUBLISHABLE_KEY
            if (!key.isNullOrBlank() && key != "YOUR_SUPABASE_PUBLISHABLE_KEY") {
                key.trim()
            } else {
                "sb_publishable_PB87rcmEcM9o0Zb8nbz3zw_yOyKh8Q6"
            }
        } catch (_: Exception) {
            "sb_publishable_PB87rcmEcM9o0Zb8nbz3zw_yOyKh8Q6"
        }
    }

    /**
     * Allows student to configure or update their Supabase publishable key from their dashboard.
     */
    fun saveCustomPublishableKey(context: Context, key: String) {
        val trimmed = key.trim()
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CUSTOM_PUBLISHABLE_KEY, trimmed).apply()
        initialize(context)
    }

    fun isConfigured(context: Context): Boolean {
        return getPublishableKey(context).isNotBlank()
    }

    fun getCurrentUserId(): String? {
        val auth = _supabaseClient?.auth ?: return null
        return auth.currentUserOrNull()?.id ?: auth.currentSessionOrNull()?.user?.id
    }

    /**
     * Helper to verify if a string follows the standard UUID format.
     * Prevents PostgreSQL syntax errors when passing non-UUID strings to UUID columns.
     */
    fun isValidUuid(id: String?): Boolean {
        val trimmed = id?.trim() ?: return false
        if (trimmed.isBlank()) return false
        // UUID format: 8-4-4-4-12 hex characters
        val regex = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
        return regex.matches(trimmed)
    }

    /**
     * Handles incoming deep links from OAuth provider redirects.
     */
    fun handleAuthDeeplink(intent: Intent) {
        val uri: Uri = intent.data ?: return
        val scheme = uri.scheme ?: return
        val client = _supabaseClient ?: return

        Log.i(TAG, "Processing auth deep link: $uri (scheme=$scheme, host=${uri.host})")

        if (scheme != "com.aistudio.nexoralearn.auth" && scheme != "com.aistudio.nexoralearn.qjvpk") {
            Log.d(TAG, "Ignoring non-auth scheme: $scheme")
            return
        }

        // 1. Process via Supabase official handleDeeplinks
        try {
            client.handleDeeplinks(intent) { session ->
                val authUser = session.user
                Log.i(TAG, "OAuth session successfully established via handleDeeplinks for user: ${authUser?.id}")
                _currentUserFlow.value = authUser
                if (authUser != null) {
                    scope.launch {
                        appContext?.let { ctx ->
                            SupabaseAuthRepository(ctx).syncGoogleUserProfile(authUser)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in client.handleDeeplinks: ${e.message}", e)
        }

        // 2. Fallback check for authorization code in query params or fragment
        scope.launch {
            try {
                val code = uri.getQueryParameter("code")
                if (!code.isNullOrBlank()) {
                    Log.i(TAG, "Exchanging auth code from deep link for session...")
                    client.auth.exchangeCodeForSession(code)
                    val authUser = client.auth.currentUserOrNull()
                    _currentUserFlow.value = authUser
                    Log.i(TAG, "Session established via code exchange: ${authUser?.id}")
                    if (authUser != null) {
                        appContext?.let { ctx ->
                            SupabaseAuthRepository(ctx).syncGoogleUserProfile(authUser)
                        }
                    }
                }
            } catch (fallbackEx: Exception) {
                Log.e(TAG, "Fallback deep link session exchange error: ${fallbackEx.message}")
            }
        }
    }
}
