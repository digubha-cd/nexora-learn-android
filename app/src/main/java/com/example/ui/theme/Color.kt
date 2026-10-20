package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * NEXORA LEARN — THEME-AWARE DYNAMIC COLOR PALETTE
 * All text, background, surface, and border constants automatically
 * resolve to MaterialTheme.colorScheme for Light, Deep Navy, and Navy Night.
 */

// 1. Theme-Aware Text Colors
val NexoraTextPrimary: Color
    @Composable get() = MaterialTheme.colorScheme.onBackground

val NexoraTextSecondary: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

val NexoraTextMuted: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

val NexoraTextLight = Color(0xFFFFFFFF)
val NexoraTextLightMuted = Color(0xFFCBD5E1)

// 2. Theme-Aware Background and Surface Palette
val NexoraBackground: Color
    @Composable get() = MaterialTheme.colorScheme.background

val NexoraSurface: Color
    @Composable get() = MaterialTheme.colorScheme.surface

val NexoraSurfaceVariant: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceVariant

val NexoraSurfaceElevated: Color
    @Composable get() = MaterialTheme.colorScheme.surface

val NexoraBorder: Color
    @Composable get() = MaterialTheme.colorScheme.outline

val NexoraBorderGlow = Color(0x223B82F6)

// 3. Vibrant Brand Accents & Gradients
val NexoraElectricBlue = Color(0xFF2563EB)      // Blue 600
val NexoraCyan = Color(0xFF06B6D4)              // Cyan 500
val NexoraPurple = Color(0xFF8B5CF6)            // Violet 500
val NexoraPink = Color(0xFFEC4899)              // Pink 500
val NexoraViolet = Color(0xFF7C3AED)            // Violet 600
val NexoraDeepNavy = Color(0xFF0F172A)
val NexoraRoyalBlue = Color(0xFF3B82F6)         // Blue 500
val NexoraDeepViolet = Color(0xFF6D28D9)
val NexoraDeepTeal = Color(0xFF0E7490)
val NexoraBurgundy = Color(0xFFBE185D)
val NexoraBurntAmber = Color(0xFFD97706)
val NexoraIndigo = Color(0xFF4F46E5)
val NexoraEmerald = Color(0xFF059669)

val NexoraButtonGradient = Brush.horizontalGradient(
    colors = listOf(NexoraElectricBlue, NexoraCyan)
)

val NexoraLogoGradient = Brush.linearGradient(
    colors = listOf(NexoraCyan, NexoraElectricBlue, NexoraPurple)
)

val NexoraHeroGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF1E3A8A), Color(0xFF3B82F6), Color(0xFF06B6D4))
)

// 4. Status and UI Colors
val NexoraSuccess = Color(0xFF10B981)           // Emerald 500
val NexoraError = Color(0xFFEF4444)             // Red 500
val NexoraWarning = Color(0xFFF59E0B)           // Amber 500
val NexoraGold = Color(0xFFEAB308)              // Yellow 500
val NexoraMagenta = Color(0xFFDB2777)

// 5. Soft Tinted Surfaces
val NexoraSky = Color(0xFFE0F2FE)               // Sky 100
val NexoraLavender = Color(0xFFEDE9FE)          // Violet 100
val NexoraMint = Color(0xFFD1FAE5)              // Emerald 100
val NexoraRose = Color(0xFFFCE7F3)              // Pink 100
val NexoraAmber = Color(0xFFFEF3C7)             // Amber 100

// 6. Subject & Category Colors
val SubjectGujaratiColor = Color(0xFF0284C7)    // Sky Blue
val SubjectEnglishColor = Color(0xFF7C3AED)     // Purple
val SubjectSpCcColor = Color(0xFF059669)        // Emerald
val SubjectBaColor = Color(0xFFD97706)          // Amber
val SubjectStatsColor = Color(0xFFDB2777)       // Pink
val SubjectAccountsColor = Color(0xFF2563EB)    // Blue
val SubjectEconColor = Color(0xFF0E7490)        // Teal

fun getSubjectAccentColor(subjectId: String?, subjectName: String? = null): Color {
    val key = (subjectId ?: subjectName ?: "").lowercase().trim()
    return when {
        key.contains("account") || key == "153" -> SubjectAccountsColor
        key.contains("stat") || key == "135" -> SubjectStatsColor
        key.contains("econ") || key == "022" -> SubjectEconColor
        key.contains("ba") || key.contains("b.a") || key.contains("commerce") || key.contains("organization") || key == "154" -> SubjectBaColor
        key.contains("sp") || key.contains("cc") || key == "337" -> SubjectSpCcColor
        key.contains("english") || key == "013" -> SubjectEnglishColor
        key.contains("gujarati") || key == "001" -> SubjectGujaratiColor
        else -> NexoraCyan // Personal / Custom task
    }
}

val CategoryHome = NexoraElectricBlue
val CategoryPlanner = NexoraPurple
val CategoryExams = NexoraPink
val CategorySubjects = NexoraCyan
val CategoryProfile = NexoraEmerald
val CategoryTimer = NexoraCyan
val CategoryStats = NexoraPurple
