package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.repository.MotivationalQuotesRepository
import com.example.ui.theme.*

/**
 * Daily Motivation Card for the Home screen:
 * Automatically changes motivational quotes 4 times a day (Morning, Late Morning,
 * Afternoon, Evening) with offline support, local time scheduling, and a smooth
 * fade transition.
 */
@Composable
fun DailyMotivationCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentQuote by MotivationalQuotesRepository.currentQuote.collectAsStateWithLifecycle()
    val slotTitle by MotivationalQuotesRepository.currentSlotTitle.collectAsStateWithLifecycle()

    // Ensure quote is initialized/updated when entering composition or resuming
    LaunchedEffect(Unit) {
        MotivationalQuotesRepository.checkAndUpdateOnResume(context)
    }

    NexoraCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_motivation_card"),
        cornerRadius = 20.dp,
        contentPadding = 18.dp,
        containerColor = Color.White,
        borderColor = NexoraCyan.copy(alpha = 0.35f),
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Icon + "DAILY MOTIVATION" + Time Slot Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(NexoraCyan.copy(alpha = 0.2f), NexoraPurple.copy(alpha = 0.25f))
                                )
                            )
                            .border(1.dp, NexoraCyan.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lightbulb,
                            contentDescription = "Daily Motivation",
                            tint = NexoraCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "DAILY MOTIVATION",
                            color = NexoraTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.testTag("daily_motivation_title")
                        )
                        Text(
                            text = slotTitle,
                            color = NexoraTextSecondary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.testTag("daily_motivation_slot")
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NexoraCyan.copy(alpha = 0.12f))
                        .border(1.dp, NexoraCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = NexoraCyan,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "NEXORA",
                            color = NexoraCyan,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Quote Content with Smooth Fade Transition
            AnimatedContent(
                targetState = currentQuote,
                transitionSpec = {
                    fadeIn(animationSpec = tween(500, easing = FastOutSlowInEasing)) togetherWith
                            fadeOut(animationSpec = tween(300, easing = FastOutSlowInEasing))
                },
                label = "QuoteFade"
            ) { quote ->
                Text(
                    text = "\"$quote\"",
                    color = NexoraTextPrimary,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 21.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("daily_motivation_quote_text")
                )
            }
        }
    }
}
