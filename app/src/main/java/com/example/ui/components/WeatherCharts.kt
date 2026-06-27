package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SunsetOrange

@Composable
fun WeatherAnalyticsChart(
    dataPoints: List<Float>,
    labels: List<String>,
    chartType: ChartType,
    modifier: Modifier = Modifier
) {
    if (dataPoints.isEmpty()) {
        Box(modifier = modifier, contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text("No data available", color = Color.White.copy(alpha = 0.5f))
        }
        return
    }

    val maxVal = dataPoints.maxOrNull() ?: 100f
    val minVal = dataPoints.minOrNull() ?: 0f
    val range = (maxVal - minVal).coerceAtLeast(1f)

    // Colors matching type
    val strokeColor = when (chartType) {
        ChartType.TEMPERATURE -> SunsetOrange
        ChartType.HUMIDITY -> NeonCyan
        ChartType.WIND -> Color(0xFF2ECC71)
        ChartType.AQI -> Color(0xFFF1C40F)
        ChartType.PRESSURE -> Color(0xFF9B59B6)
    }

    val fillBrush = Brush.verticalGradient(
        colors = listOf(strokeColor.copy(alpha = 0.35f), strokeColor.copy(alpha = 0.0f))
    )

    Canvas(modifier = modifier.fillMaxWidth().height(160.dp)) {
        val w = size.width
        val h = size.height
        val paddingLeft = 30f
        val paddingRight = 30f
        val paddingTop = 30f
        val paddingBottom = 40f

        val graphWidth = w - paddingLeft - paddingRight
        val graphHeight = h - paddingTop - paddingBottom

        // Draw background grid lines (dashed)
        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        for (gridIdx in 0..3) {
            val gridY = paddingTop + (graphHeight / 3f) * gridIdx
            drawLine(
                color = Color.White.copy(alpha = 0.08f),
                start = Offset(paddingLeft, gridY),
                end = Offset(w - paddingRight, gridY),
                pathEffect = pathEffect
            )
        }

        // Calculate coordinates for nodes
        val stepX = graphWidth / (dataPoints.size - 1).coerceAtLeast(1)
        val points = dataPoints.mapIndexed { index, value ->
            val ratio = (value - minVal) / range
            val x = paddingLeft + index * stepX
            val y = h - paddingBottom - (ratio * graphHeight)
            Offset(x, y)
        }

        // 1. Draw smooth cubic Bezier path
        val strokePath = Path()
        val fillPath = Path()

        if (points.isNotEmpty()) {
            strokePath.moveTo(points[0].x, points[0].y)
            fillPath.moveTo(points[0].x, h - paddingBottom)
            fillPath.lineTo(points[0].x, points[0].y)

            for (i in 0 until points.size - 1) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val controlX = (p0.x + p1.x) / 2f
                
                strokePath.cubicTo(
                    controlX, p0.y,
                    controlX, p1.y,
                    p1.x, p1.y
                )
                fillPath.cubicTo(
                    controlX, p0.y,
                    controlX, p1.y,
                    p1.x, p1.y
                )
            }

            fillPath.lineTo(points.last().x, h - paddingBottom)
            fillPath.close()

            // Draw Area Fill
            drawPath(path = fillPath, brush = fillBrush)

            // Draw Line Stroke
            drawPath(
                path = strokePath,
                color = strokeColor,
                style = Stroke(width = 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )

            // Draw data points & values
            points.forEachIndexed { idx, point ->
                // Draw circle node
                drawCircle(
                    color = Color.White,
                    radius = 3.5.dp.toPx(),
                    center = point
                )
                drawCircle(
                    color = strokeColor,
                    radius = 2.dp.toPx(),
                    center = point
                )

                // Optional label indices for spacing (draw every alternate label if tight)
                val labelInterval = if (dataPoints.size > 8) 2 else 1
                if (idx % labelInterval == 0) {
                    // Draw text label below node on X axis in code if we use Compose Text or draw it manually.
                    // To keep drawings simple and cross-platform stable, X axis text label handles can be written in Composable Row below.
                }
            }
        }
    }

    // Composable Row for X-Axis Labels to ensure perfect font scaling & styling
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val labelInterval = if (labels.size > 8) 3 else 1
        labels.forEachIndexed { idx, label ->
            if (idx % labelInterval == 0) {
                Text(
                    text = label,
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.5f)
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

enum class ChartType {
    TEMPERATURE, HUMIDITY, WIND, AQI, PRESSURE
}
