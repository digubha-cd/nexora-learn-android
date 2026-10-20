package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*

/**
 * Unique NEXORA AI Avatar displaying a futuristic glowing AI orb symbol.
 */
@Composable
fun NexoraAiAvatar(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    showGlow: Boolean = true
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.toPx() / 2f
            if (showGlow) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            NexoraCyan.copy(alpha = 0.3f),
                            NexoraPurple.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    ),
                    radius = radius
                )
            }
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(NexoraCyan, NexoraPurple, NexoraCyan)
                ),
                radius = radius * 0.92f
            )
            drawCircle(
                color = Color.White,
                radius = radius * 0.8f
            )
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(NexoraCyan, NexoraPurple)
                ),
                radius = radius * 0.74f
            )
        }
        Icon(
            imageVector = Icons.Outlined.AutoAwesome,
            contentDescription = "NEXORA AI Avatar",
            tint = Color.White,
            modifier = Modifier.size(size * 0.46f)
        )
    }
}
