package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.Subject
import com.example.core.model.WeeklySummaryData
import com.example.ui.theme.*
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.core.cartesian.data.ColumnCartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.common.shape.Shape

/**
 * Nexora Premium Study Hours Visualization using Vico 2.0.
 * Displays study hours spent on each subject over the last week.
 */
@Composable
fun WeeklySubjectHoursCard(
    weeklySummary: WeeklySummaryData,
    modifier: Modifier = Modifier
) {
    val subjects = Subject.OFFICIAL_SUBJECTS
    val hoursData = subjects.map { subject ->
        (weeklySummary.subjectBreakdown[subject.id] ?: 0L).toDouble() / 60.0
    }

    val model = remember(hoursData) {
        CartesianChartModel(
            ColumnCartesianLayerModel.build {
                series(hoursData)
            }
        )
    }

    NexoraCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        contentPadding = 20.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(NexoraPurple.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.BarChart,
                        contentDescription = null,
                        tint = NexoraPurple,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "Weekly Study Breakdown",
                        color = NexoraTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Study hours per subject (Last 7 days)",
                        color = NexoraTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(modifier = Modifier.height(200.dp)) {
                CartesianChartHost(
                    chart = rememberCartesianChart(
                        rememberColumnCartesianLayer(
                            columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                                listOf(
                                    rememberLineComponent(
                                        color = NexoraPurple,
                                        thickness = 16.dp,
                                        shape = Shape.rounded(allDp = 4f)
                                    )
                                )
                            )
                        ),
                        startAxis = rememberStartAxis(
                            label = rememberTextComponent(color = NexoraTextSecondary, textSize = 10.sp),
                            valueFormatter = { value, _, _ -> "${value.toInt()}h" }
                        ),
                        bottomAxis = rememberBottomAxis(
                            label = rememberTextComponent(color = NexoraTextSecondary, textSize = 8.sp, lineCount = 2),
                            valueFormatter = { value, _, _ ->
                                subjects.getOrNull(value.toInt())?.name?.take(6) ?: ""
                            }
                        )
                    ),
                    model = model,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val totalHours = weeklySummary.totalMinutes / 60.0
            Surface(
                color = NexoraSurfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "Total Focus: ${String.format("%.1f", totalHours)} hrs this week",
                    color = NexoraTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}
