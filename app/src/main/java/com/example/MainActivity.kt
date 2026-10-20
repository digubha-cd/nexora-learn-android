package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.storage.StudentPreferences
import com.example.ui.navigation.NexoraNavHost
import com.example.ui.theme.NexoraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.core.theme.NexoraThemeManager.init(applicationContext)
        enableEdgeToEdge()
        intent?.let { com.example.core.supabase.SupabaseClientManager.handleAuthDeeplink(it) }
        setContent {
            val themeMode by com.example.core.theme.NexoraThemeManager.themeModeFlow.collectAsStateWithLifecycle(
                initialValue = com.example.core.theme.NexoraThemeManager.getThemeMode(applicationContext)
            )
            Crossfade(
                targetState = themeMode,
                animationSpec = tween(durationMillis = 600),
                label = "ThemeTransition"
            ) { targetTheme ->
                NexoraTheme(themeMode = targetTheme) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        NexoraNavHost()
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        com.example.core.supabase.SupabaseClientManager.handleAuthDeeplink(intent)
    }
}
