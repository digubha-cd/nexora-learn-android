package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.animation.bounceClick
import com.example.ui.theme.*

/**
 * Enhanced banner card for NEXORA AI (powered by Google Gemini) on the Home Screen,
 * featuring Quick Actions: Solve a Question, Explain a Topic, Exam Preparation, and Ask Anything.
 */
@Composable
fun AskNexoraAiCard(
    title: String = "NEXORA AI",
    subtitle: String = "Your Smart Study & Problem-Solving Assistant",
    subjectContext: String? = null,
    chapterContext: String? = null,
    onOpenAiChat: (String?, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    NexoraCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ask_nexora_ai_card"),
        containerColor = Color.White,
        borderColor = NexoraCyan.copy(alpha = 0.35f),
        cornerRadius = 24.dp,
        contentPadding = 18.dp,
        elevation = 2.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Header & Main Click Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick { onOpenAiChat(subjectContext, chapterContext) },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(NexoraCyan.copy(alpha = 0.2f), NexoraPurple.copy(alpha = 0.25f))
                                )
                            )
                            .border(1.dp, NexoraCyan.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = "NEXORA AI",
                            tint = NexoraCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = title,
                                color = NexoraTextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NexoraCyan.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "GEMINI 2.5",
                                    color = NexoraCyan,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            color = NexoraTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .background(NexoraCyan.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                        .border(1.dp, NexoraCyan.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open AI Chat",
                        tint = NexoraCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(NexoraBorder.copy(alpha = 0.5f)))
            Spacer(modifier = Modifier.height(14.dp))

            // Quick Actions Header
            Text(
                text = "Quick AI Actions",
                color = NexoraTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Action Chips / Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AiQuickActionChip(
                    icon = Icons.Outlined.HelpOutline,
                    label = "Solve Q",
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenAiChat(subjectContext, chapterContext) }
                )
                AiQuickActionChip(
                    icon = Icons.Outlined.MenuBook,
                    label = "Explain",
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenAiChat(subjectContext, chapterContext) }
                )
                AiQuickActionChip(
                    icon = Icons.Outlined.School,
                    label = "Exam Prep",
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenAiChat(subjectContext, chapterContext) }
                )
                AiQuickActionChip(
                    icon = Icons.Outlined.Chat,
                    label = "Ask Anything",
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenAiChat(subjectContext, chapterContext) }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Ask NEXORA AI Primary Button
            Button(
                onClick = { onOpenAiChat(subjectContext, chapterContext) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .height(44.dp)
                    .testTag("ask_nexora_ai_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NexoraCyan
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Ask NEXORA AI",
                    color = Color.White,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AiQuickActionChip(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .bounceClick { onClick() }
            .background(NexoraSurfaceVariant, RoundedCornerShape(12.dp))
            .border(1.dp, NexoraBorder, RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = NexoraCyan,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                color = NexoraTextPrimary,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}
