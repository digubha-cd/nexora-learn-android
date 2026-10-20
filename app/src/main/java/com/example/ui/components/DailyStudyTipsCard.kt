package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.model.DailyStudyTip
import com.example.core.model.Subject
import com.example.core.repository.DailyStudyTipsRepository
import com.example.core.repository.FavoriteTipsRepository
import com.example.core.repository.TipLanguage
import com.example.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Daily Study Tips card powered by Gemini AI, specially crafted for Class 12 Commerce
 * students in Gujarati (with bilingual toggle support).
 * 
 * Responsive, clean architecture:
 * - Prevents Gujarati text from wrapping into narrow vertical lines.
 * - Spans titles, subtitles, and content naturally across full available card width.
 * - Keeps every action button visible, clickable, and appropriately sized using FlowRow.
 * - Preserves Room database favorites, Android Intent sharing, clipboard copy, and refresh.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun DailyStudyTipsCard(
    modifier: Modifier = Modifier
) {
    val activeTip by DailyStudyTipsRepository.activeTip.collectAsStateWithLifecycle()
    val selectedSubjectId by DailyStudyTipsRepository.selectedSubjectId.collectAsStateWithLifecycle()
    val selectedLanguage by DailyStudyTipsRepository.selectedLanguage.collectAsStateWithLifecycle()
    val isLoading by DailyStudyTipsRepository.isLoading.collectAsStateWithLifecycle()
    val errorMessage by DailyStudyTipsRepository.errorMessage.collectAsStateWithLifecycle()

    val savedFavorites by FavoriteTipsRepository.favoritesList.collectAsStateWithLifecycle()
    var showSavedTipsDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current
    val isGujarati = selectedLanguage == TipLanguage.GUJARATI

    val spinTransition = rememberInfiniteTransition(label = "gemini_spin")
    val spinAngle by spinTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gemini_spin_angle"
    )

    NexoraCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_study_tips_card")
            .animateContentSize(),
        cornerRadius = 22.dp,
        contentPadding = 16.dp,
        containerColor = Color.White,
        borderColor = NexoraCyan.copy(alpha = 0.35f)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Tier 1: Brand Logo & Title with Header Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Sparkle Emblem + Title + GEMINI AI badge
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(NexoraCyan.copy(alpha = 0.2f), NexoraPurple.copy(alpha = 0.25f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✨",
                            fontSize = 18.sp,
                            modifier = Modifier.rotate(if (isLoading) spinAngle else 0f)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (isGujarati) "દૈનિક અભ્યાસ ટિપ્સ" else "DAILY STUDY TIPS",
                                color = NexoraTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = if (isGujarati) 0.sp else 0.5.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NexoraCyan.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "GEMINI AI",
                                    color = NexoraCyan,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Right: Header Action Buttons (Saved Tips & Refresh Action)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Saved Tips Button (Opens Room Review Dialog)
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (savedFavorites.isNotEmpty()) Color(0xFFFEE2E2) else NexoraSurfaceVariant)
                            .border(
                                width = 1.dp,
                                color = if (savedFavorites.isNotEmpty()) Color(0xFFFCA5A5) else NexoraBorder,
                                shape = CircleShape
                            )
                            .clickable { showSavedTipsDialog = true }
                            .testTag("open_saved_tips_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        BadgedBox(
                            badge = {
                                if (savedFavorites.isNotEmpty()) {
                                    Badge(
                                        containerColor = Color(0xFFEF4444),
                                        contentColor = Color.White
                                    ) {
                                        Text("${savedFavorites.size}", fontSize = 9.sp)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (savedFavorites.isNotEmpty()) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (isGujarati) "સાચવેલી ટિપ્સ રિવ્યૂ કરો" else "Review saved study tips",
                                tint = if (savedFavorites.isNotEmpty()) Color(0xFFEF4444) else NexoraTextSecondary,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    // Refresh Button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isLoading) NexoraSurfaceVariant else NexoraCyan.copy(alpha = 0.12f))
                            .border(
                                width = 1.dp,
                                color = if (isLoading) NexoraBorder else NexoraCyan.copy(alpha = 0.35f),
                                shape = CircleShape
                            )
                            .clickable(
                                enabled = !isLoading,
                                onClick = {
                                    val toastMsg = if (isGujarati) "જેમિની AI નવી ટિપ લાવી રહ્યું છે..." else "Fetching fresh study tip from Gemini..."
                                    Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                                    DailyStudyTipsRepository.refreshTip(selectedSubjectId, selectedLanguage)
                                }
                            )
                            .testTag("refresh_study_tip_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = if (isGujarati) "નવી ટિપ મેળવો (રિફ્રેશ)" else "Refresh Daily Tip",
                            tint = if (isLoading) NexoraTextMuted else NexoraCyan,
                            modifier = Modifier
                                .size(19.dp)
                                .rotate(if (isLoading) spinAngle else 0f)
                        )
                    }
                }
            }

            // Subtitle across full available width so Gujarati text flows naturally without vertical wrapping
            Text(
                text = if (isGujarati) "ધોરણ ૧૨ કોમર્સ ૯૦+ ગુણ માટે બોર્ડ વ્યૂહરચના" else "Subject-specific strategies for 90+ marks",
                color = NexoraTextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                modifier = Modifier.fillMaxWidth()
            )

            // Tier 2: Language Switcher & Saved Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Language Switcher Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(NexoraSurfaceVariant)
                        .border(1.dp, NexoraBorder, RoundedCornerShape(16.dp))
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isGujarati) NexoraCyan else Color.Transparent)
                            .clickable { DailyStudyTipsRepository.selectLanguage(TipLanguage.GUJARATI) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("lang_toggle_gujarati"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ગુજરાતી",
                            color = if (isGujarati) Color.White else NexoraTextSecondary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (!isGujarati) NexoraCyan else Color.Transparent)
                            .clickable { DailyStudyTipsRepository.selectLanguage(TipLanguage.ENGLISH) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("lang_toggle_english"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "English",
                            color = if (!isGujarati) Color.White else NexoraTextSecondary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (savedFavorites.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFEF2F2))
                            .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(12.dp))
                            .clickable { showSavedTipsDialog = true }
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Text("❤️", fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isGujarati) "${savedFavorites.size} સેવ કરેલ" else "${savedFavorites.size} Saved",
                            color = Color(0xFFDC2626),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Tier 3: Subject Filter Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FilterList,
                        contentDescription = null,
                        tint = NexoraCyan,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = if (isGujarati) "વિષય મુજબ ટિપ્સ ફિલ્ટર કરો" else "FILTER BY SUBJECT",
                        color = NexoraTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = if (isGujarati) 0.sp else 0.5.sp
                    )
                }

                if (selectedSubjectId != "all") {
                    Text(
                        text = if (isGujarati) "બધા વિષયો બતાવો (Reset)" else "Reset to All",
                        color = NexoraCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { DailyStudyTipsRepository.selectSubject("all") }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .testTag("reset_subject_filter_button")
                    )
                }
            }

            // Tier 4: Subject Filter Chip Group in Current Language
            val subjectsList = remember(isGujarati) {
                if (isGujarati) {
                    listOf(
                        Triple("all", "બધા વિષયો", "📚"),
                        Triple("accounts", "નામાનાં મૂળતત્ત્વો (Accountancy)", "💼"),
                        Triple("economics", "અર્થશાસ્ત્ર (Economics)", "📈"),
                        Triple("ba", "વાણિજ્ય વ્યવસ્થા (Business Studies)", "🏢"),
                        Triple("stat", "આંકડાશાસ્ત્ર (Statistics)", "📊"),
                        Triple("sp_cc", "એસ.પી. અને સી.સી.", "✉️"),
                        Triple("english", "અંગ્રેજી (English)", "🔤"),
                        Triple("gujarati", "ગુજરાતી (Gujarati)", "📖")
                    )
                } else {
                    listOf(
                        Triple("all", "All Subjects", "📚"),
                        Triple("accounts", "Accountancy", "💼"),
                        Triple("economics", "Economics", "📈"),
                        Triple("ba", "Business Studies", "🏢"),
                        Triple("stat", "Statistics", "📊"),
                        Triple("sp_cc", "SP & CC", "✉️"),
                        Triple("english", "English", "🔤"),
                        Triple("gujarati", "Gujarati", "📖")
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .testTag("subject_filter_chip_group"),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                subjectsList.forEach { (subId, label, emoji) ->
                    val isSelected = subId == selectedSubjectId
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (isSelected && subId != "all") {
                                DailyStudyTipsRepository.selectSubject("all")
                            } else {
                                DailyStudyTipsRepository.selectSubject(subId)
                            }
                        },
                        leadingIcon = {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = NexoraCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                            } else {
                                Text(
                                    text = emoji,
                                    fontSize = 12.sp
                                )
                            }
                        },
                        label = {
                            Text(
                                text = label,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NexoraCyan.copy(alpha = 0.18f),
                            selectedLabelColor = NexoraCyan,
                            selectedLeadingIconColor = NexoraCyan,
                            containerColor = NexoraSurfaceVariant,
                            labelColor = NexoraTextPrimary
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) NexoraCyan else NexoraBorder
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("tip_subject_chip_$subId")
                    )
                }
            }

            // Tier 5: Tip Content Card with Loading state or Animated Fade-in/Slide-in
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(NexoraSurfaceVariant)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            color = NexoraCyan,
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.5.dp
                        )
                        Text(
                            text = if (isGujarati) "જેમિની AI એક્સપર્ટ ટિપ તૈયાર થઈ રહી છે..." else "Consulting Gemini AI study coach...",
                            color = NexoraTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else if (activeTip != null) {
                AnimatedContent(
                    targetState = activeTip,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(350, easing = EaseOutCubic)) +
                         slideInVertically(animationSpec = tween(350, easing = EaseOutCubic)) { height -> height / 5 })
                            .togetherWith(
                                fadeOut(animationSpec = tween(180, easing = FastOutLinearInEasing)) +
                                slideOutVertically(animationSpec = tween(180, easing = FastOutLinearInEasing)) { height -> -height / 5 }
                            )
                    },
                    label = "study_tip_slide_fade"
                ) { currentTip ->
                    if (currentTip != null) {
                        val tip = currentTip
                        val isFavorited = savedFavorites.any { it.id == tip.id || it.headline == tip.headline }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(NexoraSurfaceVariant)
                                .border(1.dp, NexoraBorder, RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Row 1: Subject badge & High-yield badge on left, Favorite button on right
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f, fill = false),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(NexoraCyan.copy(alpha = 0.12f))
                                            .border(1.dp, NexoraCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = tip.subjectName,
                                            color = NexoraCyan,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(NexoraSurfaceElevated)
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (isGujarati) "💡 બોર્ડ ટિપ" else "💡 High-Yield",
                                            color = NexoraTextMuted,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                // Favorite Toggle Button at Card Top
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            val willBeFav = !isFavorited
                                            FavoriteTipsRepository.toggleFavorite(tip, isFavorited)
                                            val toastMsg = if (willBeFav) {
                                                if (isGujarati) "ટિપ ફેવરિટમાં સેવ થઈ ગઈ! ❤️ (રૂમ ડેટાબેઝ)" else "Tip saved to Favorites! ❤️ (Room DB)"
                                            } else {
                                                if (isGujarati) "ટિપ ફેવરિટમાંથી હટાવી દીધી" else "Tip removed from Favorites"
                                            }
                                            Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("favorite_study_tip_button")
                                ) {
                                    Icon(
                                        imageVector = if (isFavorited) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = if (isFavorited) {
                                            if (isGujarati) "ફેવરિટમાંથી હટાવો" else "Remove from Favorites"
                                        } else {
                                            if (isGujarati) "ફેવરિટમાં સેવ કરો" else "Save to Favorites"
                                        },
                                        tint = if (isFavorited) Color(0xFFEF4444) else NexoraTextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Headline: Prominent, fully readable, supports Gujarati unicode
                            Text(
                                text = tip.headline,
                                color = NexoraTextPrimary,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 22.sp,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Core Tip Explanation with proper line spacing
                            Text(
                                text = tip.tipContent,
                                color = NexoraTextSecondary,
                                fontSize = 13.5.sp,
                                lineHeight = 20.sp,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // 10-Minute Action Plan Box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(NexoraCyan.copy(alpha = 0.08f))
                                    .border(1.dp, NexoraCyan.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Text(
                                        text = "⚡",
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = if (isGujarati) "આજનો ૧૦-મિનિટ એક્શન પ્લાન" else "TODAY'S 10-MIN ACTION PLAN",
                                            color = NexoraCyan,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 0.6.sp
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = tip.actionStep,
                                            color = NexoraTextPrimary,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }

                            // Source indicator
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AutoAwesome,
                                    contentDescription = null,
                                    tint = NexoraTextMuted,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = tip.source,
                                    color = NexoraTextMuted,
                                    fontSize = 11.sp
                                )
                            }

                            // Responsive Action Buttons Bar with FlowRow: Every button fully visible & clickable
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // 1. Refresh Button
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(NexoraCyan.copy(alpha = 0.1f))
                                        .border(1.dp, NexoraCyan.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                        .clickable(enabled = !isLoading) {
                                            val toastMsg = if (isGujarati) "જેમિની AI નવી ટિપ લાવી રહ્યું છે..." else "Fetching fresh study tip from Gemini..."
                                            Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                                            DailyStudyTipsRepository.refreshTip(selectedSubjectId, selectedLanguage)
                                        }
                                        .padding(horizontal = 11.dp, vertical = 8.dp)
                                        .testTag("footer_refresh_study_tip_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Refresh,
                                        contentDescription = if (isGujarati) "નવી ટિપ મેળવો" else "Get New Tip",
                                        tint = if (isLoading) NexoraTextMuted else NexoraCyan,
                                        modifier = Modifier
                                            .size(15.dp)
                                            .rotate(if (isLoading) spinAngle else 0f)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (isGujarati) "નવી ટિપ" else "New Tip",
                                        color = if (isLoading) NexoraTextMuted else NexoraCyan,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // 2. Share Button with Intent.ACTION_SEND
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(NexoraCyan.copy(alpha = 0.1f))
                                        .border(1.dp, NexoraCyan.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                        .clickable {
                                            val sendIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(
                                                    Intent.EXTRA_SUBJECT,
                                                    if (isGujarati) "ધોરણ ૧૨ કોમર્સ અભ્યાસ ટિપ - ${tip.subjectName}" else "Class 12 Commerce Study Tip - ${tip.subjectName}"
                                                )
                                                putExtra(
                                                    Intent.EXTRA_TEXT,
                                                    """
                                                    📖 *NEXORA - GSEB Class 12 Commerce Study Tip*
                                                    📌 *${tip.subjectName}*: ${tip.headline}

                                                    💡 ${tip.tipContent}

                                                    ⚡ *${if (isGujarati) "૧૦-મિનિટ એક્શન પ્લાન" else "10-Min Action Plan"}*:
                                                    ${tip.actionStep}

                                                    ✨ Shared via NEXORA Learn
                                                    """.trimIndent()
                                                )
                                                type = "text/plain"
                                            }
                                            val shareIntent = Intent.createChooser(
                                                sendIntent,
                                                if (isGujarati) "ટિપ ક્લાસમેટ્સ સાથે શેર કરો" else "Share study tip with classmates"
                                            )
                                            context.startActivity(shareIntent)
                                        }
                                        .padding(horizontal = 11.dp, vertical = 8.dp)
                                        .testTag("share_study_tip_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Share,
                                        contentDescription = if (isGujarati) "ટિપ ક્લાસમેટ્સ સાથે શેર કરો" else "Share tip with classmates",
                                        tint = NexoraCyan,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (isGujarati) "શેર" else "Share",
                                        color = NexoraCyan,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // 3. Favorite Button in Footer (Room DB)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isFavorited) Color(0xFFFEE2E2) else NexoraSurfaceVariant)
                                        .border(
                                            width = 1.dp,
                                            color = if (isFavorited) Color(0xFFFCA5A5) else NexoraBorder,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            coroutineScope.launch {
                                                val willBeFav = !isFavorited
                                                FavoriteTipsRepository.toggleFavorite(tip, isFavorited)
                                                val toastMsg = if (willBeFav) {
                                                    if (isGujarati) "ટિપ ફેવરિટમાં સેવ થઈ ગઈ! ❤️" else "Tip saved to Favorites! ❤️"
                                                } else {
                                                    if (isGujarati) "ટિપ ફેવરિટમાંથી હટાવી દીધી" else "Tip removed from Favorites"
                                                }
                                                Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .padding(horizontal = 11.dp, vertical = 8.dp)
                                        .testTag("footer_favorite_study_tip_button")
                                ) {
                                    Icon(
                                        imageVector = if (isFavorited) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = if (isFavorited) {
                                            if (isGujarati) "ફેવરિટમાંથી હટાવો" else "Remove from Favorites"
                                        } else {
                                            if (isGujarati) "ફેવરિટમાં સેવ કરો" else "Save to Favorites"
                                        },
                                        tint = if (isFavorited) Color(0xFFEF4444) else NexoraTextSecondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (isFavorited) {
                                            if (isGujarati) "સાચવેલ" else "Saved"
                                        } else {
                                            if (isGujarati) "ફેવરિટ" else "Favorite"
                                        },
                                        color = if (isFavorited) Color(0xFFEF4444) else NexoraTextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // 4. Copy Action
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(NexoraSurfaceVariant)
                                        .border(1.dp, NexoraBorder, RoundedCornerShape(10.dp))
                                        .clickable {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                            val clip = ClipData.newPlainText(
                                                "NEXORA Study Tip",
                                                "${tip.headline}\n\n${tip.tipContent}\n\n${if (isGujarati) "એક્શન પ્લાન" else "Action"}: ${tip.actionStep}"
                                            )
                                            clipboard?.setPrimaryClip(clip)
                                            val toastMsg = if (isGujarati) "ટિપ ક્લિપબોર્ડ પર કોપી થઈ ગઈ!" else "Tip copied to clipboard!"
                                            Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 11.dp, vertical = 8.dp)
                                        .testTag("copy_study_tip_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ContentCopy,
                                        contentDescription = if (isGujarati) "ટિપ કોપી કરો" else "Copy Tip",
                                        tint = NexoraTextSecondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (isGujarati) "કોપી" else "Copy",
                                        color = NexoraTextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Error notice banner if any
            AnimatedVisibility(visible = !errorMessage.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(NexoraGold.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = NexoraGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = NexoraTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }

    // Saved tips review dialog to review all saved tips from Room database
    if (showSavedTipsDialog) {
        SavedTipsDialog(
            isGujarati = isGujarati,
            onDismissRequest = { showSavedTipsDialog = false }
        )
    }
}
