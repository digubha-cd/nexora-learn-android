package com.example.core.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.storage.StudentPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Reactive StateFlow-based Theme Manager for NEXORA LEARN.
 * Ensures all app-wide observers react immediately to changes in settings without requiring recomposition from scratch.
 */
object NexoraThemeManager {
    private const val PREFS_NAME = "nexora_theme_prefs"
    private const val KEY_THEME_MODE = "key_theme_mode"

    private val _themeModeFlow = MutableStateFlow("LIGHT")
    val themeModeFlow: StateFlow<String> = _themeModeFlow.asStateFlow()

    fun init(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val mode = prefs.getString(KEY_THEME_MODE, null)
            ?: run {
                val studentPrefMode = StudentPreferences.getThemeMode(context)
                prefs.edit().putString(KEY_THEME_MODE, studentPrefMode).apply()
                studentPrefMode
            }
        _themeModeFlow.value = mode
    }

    fun getThemeMode(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val mode = prefs.getString(KEY_THEME_MODE, null) ?: "LIGHT"
        if (_themeModeFlow.value != mode) {
            _themeModeFlow.value = mode
        }
        return mode
    }

    fun setThemeMode(context: Context, mode: String) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
        _themeModeFlow.value = mode
        // Keep StudentPreferences in sync
        StudentPreferences.setThemeMode(context, mode)
    }

    @Composable
    fun currentThemeMode(): String {
        val context = androidx.compose.ui.platform.LocalContext.current
        val mode by themeModeFlow.collectAsStateWithLifecycle(initialValue = getThemeMode(context))
        return mode
    }
}
