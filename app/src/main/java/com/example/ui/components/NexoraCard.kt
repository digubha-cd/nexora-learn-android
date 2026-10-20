package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NexoraBorder
import com.example.ui.theme.NexoraSurface

/**
 * Reusable Card component adhering to the premium rich aesthetic
 * with rounded corners, refined border, optional gradient fill, and premium shadow.
 */
@Composable
fun NexoraCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    backgroundBrush: Brush? = null,
    cornerRadius: Dp = 24.dp,
    contentPadding: Dp = 16.dp,
    elevation: Dp = 2.dp,
    testTag: String = "nexora_card",
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val actualContainerColor = if (containerColor == Color.White) MaterialTheme.colorScheme.surface else containerColor
    val cardColors = CardDefaults.cardColors(
        containerColor = if (backgroundBrush != null) Color.Transparent else actualContainerColor
    )
    val elevationState = CardDefaults.cardElevation(
        defaultElevation = elevation,
        pressedElevation = 0.5.dp,
        focusedElevation = elevation
    )

    if (onClick != null) {
        Card(
            onClick = onClick,
            shape = shape,
            border = BorderStroke(1.dp, borderColor),
            colors = cardColors,
            elevation = elevationState,
            modifier = modifier
                .fillMaxWidth()
                .testTag(testTag)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (backgroundBrush != null) Modifier.background(backgroundBrush) else Modifier)
            ) {
                Column(
                    modifier = Modifier.padding(contentPadding),
                    content = content
                )
            }
        }
    } else {
        Card(
            shape = shape,
            border = BorderStroke(1.dp, borderColor),
            colors = cardColors,
            elevation = elevationState,
            modifier = modifier
                .fillMaxWidth()
                .testTag(testTag)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (backgroundBrush != null) Modifier.background(backgroundBrush) else Modifier)
            ) {
                Column(
                    modifier = Modifier.padding(contentPadding),
                    content = content
                )
            }
        }
    }
}
