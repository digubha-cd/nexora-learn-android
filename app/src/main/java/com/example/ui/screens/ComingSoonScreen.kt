package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

private val MidnightNavy = Color(0xFF0F172A)
private val DeepIndigo = Color(0xFF1E293B)
private val ElectricCyan = Color(0xFF00E5FF)

/**
 * Polished "Coming Soon" screen for NEXORA LEARN modules in development (e.g. Chat).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComingSoonScreen(
    title: String = "Chat",
    heading: String = "Coming Soon!",
    description: String = "We're working on something exciting. Stay tuned!",
    onBackClick: () -> Unit
) {
    Scaffold(
        containerColor = MidnightNavy,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("coming_soon_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Return to Home",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(MidnightNavy, DeepIndigo)
                    )
                )
                .testTag("coming_soon_screen_root")
        ) {
            // Ambient particle effects
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                
                drawCircle(ElectricCyan.copy(alpha = 0.15f), radius = 300f, center = Offset(canvasWidth * 0.5f, canvasHeight * 0.35f))
                drawCircle(NexoraCyan.copy(alpha = 0.2f), radius = 2f, center = Offset(canvasWidth * 0.2f, canvasHeight * 0.2f))
                drawCircle(NexoraPurple.copy(alpha = 0.2f), radius = 3f, center = Offset(canvasWidth * 0.8f, canvasHeight * 0.3f))
                drawLine(NexoraCyan.copy(alpha = 0.1f), Offset(canvasWidth * 0.2f, canvasHeight * 0.2f), Offset(canvasWidth * 0.8f, canvasHeight * 0.3f))
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Central Glowing Icon Orb
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .drawBehind {
                            val brush = Brush.radialGradient(
                                colors = listOf(NexoraCyan.copy(alpha = 0.5f), Color.Transparent)
                            )
                            drawCircle(brush, radius = size.width / 2)
                            drawCircle(NexoraCyan.copy(alpha = 0.3f), radius = size.width / 2, style = Stroke(width = 2.dp.toPx()))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Forum,
                        contentDescription = "Chat Feature Icon",
                        tint = Color.White,
                        modifier = Modifier.size(54.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = heading,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = description,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                Surface(
                    color = NexoraCyan.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(100.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NexoraCyan.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "NEXORA LEARN • IN DEVELOPMENT",
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = NexoraCyan,
                        letterSpacing = 0.8.sp
                    )
                }
            }
        }
    }
}
