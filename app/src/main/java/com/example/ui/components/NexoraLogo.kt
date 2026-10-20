package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.NexoraCyan
import com.example.ui.theme.NexoraPurple

/**
 * Official NEXORA LEARN Logo Component.
 * Faithfully renders the emblem and typography inside a circular frame with subtle cyan and purple accents.
 */
@Composable
fun NexoraLogo(
    modifier: Modifier = Modifier,
    emblemSize: Dp = 140.dp,
    showWordmark: Boolean = true,
    showSubtitle: Boolean = false,
    subtitleText: String = "",
    darkContent: Boolean = false
) {
    Surface(
        modifier = modifier
            .size(emblemSize)
            .shadow(
                elevation = if (emblemSize >= 48.dp) 6.dp else 2.dp,
                shape = CircleShape,
                ambientColor = NexoraCyan.copy(alpha = 0.35f),
                spotColor = NexoraPurple.copy(alpha = 0.35f)
            )
            .testTag("nexora_logo_container"),
        shape = CircleShape,
        color = Color(0xFF010108),
        border = BorderStroke(
            width = if (emblemSize >= 48.dp) 1.5.dp else 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF00E5FF),
                    Color(0xFF0070F3),
                    Color(0xFF8B5CF6)
                )
            )
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_nexora_logo),
                contentDescription = "Nexora Learn Logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize(0.92f)
                    .clip(CircleShape)
            )
        }
    }
}
