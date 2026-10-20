package com.example.ui.screens.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.storage.StudentPreferences
import com.example.ui.components.NexoraCard
import com.example.ui.components.OfficialLogo
import com.example.ui.theme.*
import kotlinx.coroutines.launch

data class OnboardingStep(
    val stepNumber: Int,
    val badgeText: String,
    val title: String,
    val subtitle: String,
    val primaryIcon: ImageVector,
    val heroGradient: List<Color>,
    val highlights: List<Pair<ImageVector, String>>
)

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val steps = remember {
        listOf(
            OnboardingStep(
                stepNumber = 1,
                badgeText = "CLASS 12 COMMERCE",
                title = "Achieve 90+ Marks in Gujarat Board",
                subtitle = "Your comprehensive, structured study companion engineered exclusively for Commerce students to master every subject with confidence.",
                primaryIcon = Icons.Rounded.EmojiEvents,
                heroGradient = listOf(Color(0xFF2563EB), Color(0xFF06B6D4)),
                highlights = listOf(
                    Icons.AutoMirrored.Filled.MenuBook to "Complete Accounts, Stats, Eco, OCM & Languages",
                    Icons.Rounded.School to "Curated syllabus aligned with Board patterns",
                    Icons.Rounded.CheckCircle to "Systematic chapter tracking & daily targets"
                )
            ),
            OnboardingStep(
                stepNumber = 2,
                badgeText = "DUAL-PHASE ROADMAP",
                title = "Synchronized Exam Roadmaps",
                subtitle = "Two distinct, targeted phases to guide your entire academic year without confusion or last-minute panic.",
                primaryIcon = Icons.Rounded.CalendarMonth,
                heroGradient = listOf(Color(0xFF8B5CF6), Color(0xFF06B6D4)),
                highlights = listOf(
                    Icons.Rounded.CheckCircle to "Phase 1: School Exam (Oct 2026 • 66 Chapters)",
                    Icons.Rounded.CheckCircle to "Phase 2: Board Exam (Feb 2027 • 94 Chapters)",
                    Icons.Rounded.Schedule to "Live IST countdowns and smart milestone alerts"
                )
            ),
            OnboardingStep(
                stepNumber = 3,
                badgeText = "DEEP FOCUS & AI MENTOR",
                title = "Study Timer & Instant Doubt Solver",
                subtitle = "Boost retention with Pomodoro focus sessions, automated reminders, and 24/7 AI-powered chapter explanations.",
                primaryIcon = Icons.Rounded.Timer,
                heroGradient = listOf(Color(0xFF4F46E5), Color(0xFF7C3AED)),
                highlights = listOf(
                    Icons.Rounded.Timer to "Persistent focus timer with auto-logging",
                    Icons.Rounded.AutoAwesome to "AI Doubt Solver tailored for Commerce concepts",
                    Icons.Rounded.Schedule to "Automated morning & evening study reminders"
                )
            ),
            OnboardingStep(
                stepNumber = 4,
                badgeText = "STREAKS & ACHIEVEMENTS",
                title = "Track Habits & Celebrate Streaks",
                subtitle = "Earn competition points for every completed chapter and session. Climb the monthly leaderboard and build unstoppable discipline.",
                primaryIcon = Icons.Rounded.LocalFireDepartment,
                heroGradient = listOf(Color(0xFFD97706), Color(0xFFEC4899)),
                highlights = listOf(
                    Icons.Rounded.LocalFireDepartment to "Daily study streaks with freeze protection",
                    Icons.Rounded.MilitaryTech to "Unlock exclusive mastery & consistency badges",
                    Icons.Rounded.CheckCircle to "Full cloud backup & sync via Supabase"
                )
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { steps.size })
    val isLastPage = pagerState.currentPage == steps.size - 1

    fun finishOnboarding() {
        StudentPreferences.setOnboardingCompleted(context, true)
        onFinished()
    }

    // Intercept back button to navigate backwards in the pager
    BackHandler(enabled = pagerState.currentPage > 0) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(pagerState.currentPage - 1)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("onboarding_screen_root"),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.systemBars
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF070B19),
                            Color(0xFF0F172A),
                            Color(0xFF020617)
                        )
                    )
                )
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Action Bar with Logo & Skip Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Small logo emblem
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OfficialLogo(
                            size = 40.dp
                        )
                    }

                    // Skip button (visible on non-last pages)
                    if (!isLastPage) {
                        TextButton(
                            onClick = { finishOnboarding() },
                            modifier = Modifier
                                .testTag("onboarding_skip_button")
                                .minimumInteractiveComponentSize(),
                            colors = ButtonDefaults.textButtonColors(contentColor = NexoraCyan)
                        ) {
                            Text(
                                text = "Skip",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        // Empty placeholder to keep alignment
                        Spacer(modifier = Modifier.width(48.dp))
                    }
                }

                // Pager Content
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { pageIndex ->
                    val step = steps[pageIndex]
                    OnboardingPageContent(step = step)
                }

                // Bottom Navigation & Controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Animated Page Indicator Dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(bottom = 20.dp)
                            .testTag("onboarding_indicators")
                    ) {
                        repeat(steps.size) { index ->
                            val isSelected = pagerState.currentPage == index
                            val width by animateDpAsState(
                                targetValue = if (isSelected) 28.dp else 8.dp,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                label = "dot_width"
                            )
                            Box(
                                modifier = Modifier
                                    .height(8.dp)
                                    .width(width)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) NexoraCyan else Color.White.copy(alpha = 0.25f)
                                    )
                            )
                        }
                    }

                    // Primary Action Button (Next or Get Started)
                    if (!isLastPage) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Back button if on subsequent page
                            if (pagerState.currentPage > 0) {
                                OutlinedButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(0.4f)
                                        .height(52.dp)
                                        .testTag("onboarding_back_button"),
                                    shape = RoundedCornerShape(16.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = "Back",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Back", fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                            }

                            // Next Button
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                },
                                modifier = Modifier
                                    .weight(if (pagerState.currentPage > 0) 0.6f else 1f)
                                    .height(52.dp)
                                    .testTag("onboarding_next_button"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NexoraCyan,
                                    contentColor = Color(0xFF070B19)
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "Next",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        // Final 'Get Started' Button with gradient glow
                        Button(
                            onClick = { finishOnboarding() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("onboarding_get_started_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent
                            ),
                            contentPadding = PaddingValues()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color(0xFF06B6D4),
                                                Color(0xFF2563EB),
                                                Color(0xFF8B5CF6)
                                            )
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RocketLaunch,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Get Started",
                                        color = Color.White,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(
    step: OnboardingStep,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Hero Icon Illustration Card with Ambient Radial Gradient
        Box(
            modifier = Modifier
                .size(140.dp)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                step.heroGradient.first().copy(alpha = 0.35f),
                                Color.Transparent
                            ),
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = size.width * 0.75f
                        )
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            colors = step.heroGradient
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        color = Color.White.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(28.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = step.primaryIcon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Badge pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(30.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(30.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = step.badgeText,
                color = NexoraCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Title
        Text(
            text = step.title,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            lineHeight = 31.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Subtitle
        Text(
            text = step.subtitle,
            color = Color(0xFF94A3B8),
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            lineHeight = 21.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Highlight Cards List
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            step.highlights.forEach { (icon, text) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1E293B).copy(alpha = 0.6f))
                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(NexoraCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = NexoraCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = text,
                        color = Color(0xFFE2E8F0),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
