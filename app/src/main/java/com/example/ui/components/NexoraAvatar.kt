package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun NexoraAvatar(
    name: String,
    photoUrl: String? = null,
    avatarColorHex: String = "#00BCD4",
    size: Dp = 40.dp
) {
    val backgroundColor = try {
        Color(android.graphics.Color.parseColor(if (avatarColorHex.startsWith("#")) avatarColorHex else "#00BCD4"))
    } catch (e: Exception) {
        Color(0xFF00BCD4)
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        if (!photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = "Avatar",
                modifier = Modifier.size(size),
                contentScale = ContentScale.Crop
            )
        } else {
            val initial = if (name.isNotBlank()) name.take(1).uppercase() else "S"
            Text(
                text = initial,
                color = Color.White,
                fontSize = (size.value * 0.4).sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
