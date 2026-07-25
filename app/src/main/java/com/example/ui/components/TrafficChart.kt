package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.SpeedPoint
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800

@Composable
fun TrafficChart(
    speedPoints: List<SpeedPoint>,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Slate800),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Live Traffic Speed",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))

                // Legend
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(CyanPrimary)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Down", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.width(12.dp))

                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(NeonGreen)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Up", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(110.dp)) {
                    val width = size.width
                    val height = size.height

                    // Draw grid lines
                    val gridLines = 3
                    for (i in 1..gridLines) {
                        val y = (height / (gridLines + 1)) * i
                        drawLine(
                            color = Slate700.copy(alpha = 0.5f),
                            start = androidx.compose.ui.geometry.Offset(0f, y),
                            end = androidx.compose.ui.geometry.Offset(width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    if (speedPoints.size < 2) return@Canvas

                    val maxSpeed = (speedPoints.maxOfOrNull { maxOf(it.downloadBps, it.uploadBps) } ?: 1024L)
                        .coerceAtLeast(1024L)

                    val stepX = width / (speedPoints.size - 1)

                    // Download curve (Cyan)
                    val downPath = Path()
                    val downFillPath = Path()

                    speedPoints.forEachIndexed { index, point ->
                        val x = index * stepX
                        val normalizedY = height - ((point.downloadBps.toFloat() / maxSpeed) * (height - 10.dp.toPx()))
                        if (index == 0) {
                            downPath.moveTo(x, normalizedY)
                            downFillPath.moveTo(x, height)
                            downFillPath.lineTo(x, normalizedY)
                        } else {
                            downPath.lineTo(x, normalizedY)
                            downFillPath.lineTo(x, normalizedY)
                        }
                    }
                    downFillPath.lineTo((speedPoints.size - 1) * stepX, height)
                    downFillPath.close()

                    drawPath(
                        path = downFillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(CyanPrimary.copy(alpha = 0.25f), Color.Transparent)
                        )
                    )
                    drawPath(
                        path = downPath,
                        color = CyanPrimary,
                        style = Stroke(width = 2.5.dp.toPx())
                    )

                    // Upload curve (Green)
                    val upPath = Path()
                    speedPoints.forEachIndexed { index, point ->
                        val x = index * stepX
                        val normalizedY = height - ((point.uploadBps.toFloat() / maxSpeed) * (height - 10.dp.toPx()))
                        if (index == 0) {
                            upPath.moveTo(x, normalizedY)
                        } else {
                            upPath.lineTo(x, normalizedY)
                        }
                    }
                    drawPath(
                        path = upPath,
                        color = NeonGreen,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }
    }
}
