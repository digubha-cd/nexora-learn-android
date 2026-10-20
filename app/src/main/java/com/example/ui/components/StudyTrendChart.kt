package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.timer.FocusSessionRecord
import com.example.ui.theme.*
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineSpec
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.shader.verticalGradient
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.core.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.core.common.shader.DynamicShader
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Nexora Premium Study Trend Visualization using Vico 2.0.
 * Displays study hours trend over the last 7 days with subject-wise peak identification.
 */
@Composable
fun StudyTrendChart(
    sessions: List<FocusSessionRecord>,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val last7Days = (0..6).map { today.minusDays(it.toLong()) }.reversed()
    
    // Aggregate minutes per day
    val dailyMinutes = last7Days.map { date ->
        sessions.filter { 
            val sessionDate = Instant.ofEpochMilli(it.timestampEpochMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            sessionDate == date
        }.sumOf { it.durationMinutes }.toDouble() / 60.0 // Convert to hours
    }

    val model = remember(dailyMinutes) {
        CartesianChartModel(
            LineCartesianLayerModel.build {
                series(dailyMinutes)
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "Study Activity Trend (Last 7 Days)",
            color = NexoraTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Average focus intensity and peak study times",
            color = NexoraTextSecondary,
            fontSize = 11.sp
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Box(modifier = Modifier.height(180.dp)) {
            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberLineCartesianLayer(
                        lines = listOf(
                            rememberLineSpec(
                                shader = DynamicShader.verticalGradient(
                                    arrayOf(NexoraCyan, Color.Transparent)
                                )
                            )
                        )
                    ),
                    startAxis = rememberStartAxis(
                        label = rememberTextComponent(
                            color = NexoraTextSecondary,
                            textSize = 10.sp
                        ),
                        valueFormatter = { value, _, _ -> "${value.toInt()}h" }
                    ),
                    bottomAxis = rememberBottomAxis(
                        label = rememberTextComponent(
                            color = NexoraTextSecondary,
                            textSize = 10.sp
                        ),
                        valueFormatter = { value, _, _ ->
                            val date = last7Days.getOrNull(value.toInt())
                            date?.format(DateTimeFormatter.ofPattern("EEE")) ?: ""
                        }
                    )
                ),
                model = model,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Insight summary
        val peakDayIndex = dailyMinutes.indexOf(dailyMinutes.maxOrNull() ?: 0.0)
        val peakDay = last7Days.getOrNull(peakDayIndex)
        val peakHours = dailyMinutes.maxOrNull() ?: 0.0
        
        if (peakHours > 0) {
            Surface(
                color = NexoraSky.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🔥", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Peak focus reached on ${peakDay?.format(DateTimeFormatter.ofPattern("EEEE"))} with ${String.format("%.1f", peakHours)} hours.",
                        color = NexoraTextPrimary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
