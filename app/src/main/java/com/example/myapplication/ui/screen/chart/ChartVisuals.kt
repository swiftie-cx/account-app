package com.example.myapplication.ui.screen.chart

import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun LineChart(
    dataPoints: List<LineChartPoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    showAllLabels: Boolean = false, // [新增] 控制是否显示所有标签
    onPointClick: (LineChartPoint) -> Unit
) {
    if (dataPoints.isEmpty()) return

    val actualMax = remember(dataPoints) { dataPoints.maxOfOrNull { it.value } ?: 0f }
    val actualMin = remember(dataPoints) { dataPoints.minOfOrNull { it.value } ?: 0f }

    val density = LocalDensity.current.density
    val textPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.LTGRAY
            textSize = 10f * density
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }
    val tooltipTextPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 12f * density
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }
    val tooltipDateFormat = remember { SimpleDateFormat("MM-dd", Locale.CHINA) }
    val gradientColors = listOf(lineColor.copy(alpha = 0.3f), lineColor.copy(alpha = 0.0f))
    var selectedIndex by remember { mutableIntStateOf(-1) }

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(onTap = { offset ->
                    val width = size.width.toFloat()
                    val leftPadding = 32.dp.toPx()
                    val chartWidth = width - leftPadding - 16.dp.toPx()
                    val spacing = chartWidth / (dataPoints.size - 1).coerceAtLeast(1)
                    if (selectedIndex != -1 && selectedIndex in dataPoints.indices) {
                        val point = dataPoints[selectedIndex]
                        val chartHeight = size.height.toFloat() - 24.dp.toPx()
                        val rangeTop = if (actualMax > 0) actualMax * 1.2f else 100f
                        val rangeBottom = if (actualMin < 0) actualMin * 1.2f else 0f
                        val drawingRange = (rangeTop - rangeBottom).coerceAtLeast(1f)
                        val x = leftPadding + selectedIndex * spacing
                        val y = chartHeight - ((point.value - rangeBottom) / drawingRange * chartHeight)
                        val clickRadius = 40.dp.toPx()
                        if (kotlin.math.abs(offset.x - x) < clickRadius && kotlin.math.abs(offset.y - y) < clickRadius) {
                            onPointClick(point)
                            return@detectTapGestures
                        }
                    }
                    val index = ((offset.x - leftPadding) / spacing).MathRound().coerceIn(0, dataPoints.lastIndex)
                    selectedIndex = index
                })
            }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        val width = size.width.toFloat()
                        val leftPadding = 32.dp.toPx()
                        val spacing = (width - leftPadding - 16.dp.toPx()) / (dataPoints.size - 1).coerceAtLeast(1)
                        selectedIndex = ((offset.x - leftPadding) / spacing).MathRound().coerceIn(0, dataPoints.lastIndex)
                    },
                    onDrag = { change, _ ->
                        val width = size.width.toFloat()
                        val leftPadding = 32.dp.toPx()
                        val spacing = (width - leftPadding - 16.dp.toPx()) / (dataPoints.size - 1).coerceAtLeast(1)
                        selectedIndex = ((change.position.x - leftPadding) / spacing).MathRound().coerceIn(0, dataPoints.lastIndex)
                    }
                )
            }
    ) {
        val width = size.width
        val height = size.height
        val bottomPadding = 24.dp.toPx()
        val leftPadding = 32.dp.toPx()
        val chartWidth = width - leftPadding - 16.dp.toPx()
        val chartHeight = height - bottomPadding
        val rangeTop = if (actualMax > 0) actualMax * 1.2f else 100f
        val rangeBottom = if (actualMin < 0) actualMin * 1.2f else 0f
        val drawingRange = (rangeTop - rangeBottom).coerceAtLeast(1f)

        val gridLines = 4
        for (i in 0..gridLines) {
            val ratio = i.toFloat() / gridLines
            val y = chartHeight - (ratio * chartHeight)
            drawLine(
                color = Color.LightGray.copy(alpha = 0.5f),
                start = Offset(leftPadding, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
        }

        val spacing = chartWidth / (dataPoints.size - 1).coerceAtLeast(1)
        val path = Path()
        val fillPath = Path()

        dataPoints.forEachIndexed { index, point ->
            val x = leftPadding + index * spacing
            val y = chartHeight - ((point.value - rangeBottom) / drawingRange * chartHeight)
            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, chartHeight)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }

            // [关键修改] 根据 showAllLabels 决定是否显示所有标签
            if (showAllLabels || dataPoints.size <= 7 || index % (dataPoints.size / 5) == 0) {
                drawContext.canvas.nativeCanvas.drawText(point.label, x, height - 5.dp.toPx(), textPaint)
            }
        }
        fillPath.lineTo(leftPadding + (dataPoints.size - 1) * spacing, chartHeight)
        fillPath.close()
        drawPath(path = fillPath, brush = Brush.verticalGradient(colors = gradientColors, startY = 0f, endY = chartHeight))
        drawPath(path = path, color = lineColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))

        dataPoints.forEachIndexed { index, point ->
            val x = leftPadding + index * spacing
            val y = chartHeight - ((point.value - rangeBottom) / drawingRange * chartHeight)
            drawCircle(color = Color.White, radius = 4.dp.toPx(), center = Offset(x, y), style = androidx.compose.ui.graphics.drawscope.Fill)
            drawCircle(color = lineColor, radius = 4.dp.toPx(), center = Offset(x, y), style = Stroke(width = 2.dp.toPx()))
        }

        if (selectedIndex != -1 && selectedIndex in dataPoints.indices) {
            val point = dataPoints[selectedIndex]
            val x = leftPadding + selectedIndex * spacing
            val y = chartHeight - ((point.value - rangeBottom) / drawingRange * chartHeight)
            drawLine(color = lineColor.copy(alpha = 0.5f), start = Offset(x, 0f), end = Offset(x, chartHeight), strokeWidth = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
            drawCircle(color = lineColor, radius = 6.dp.toPx(), center = Offset(x, y))
            drawCircle(color = Color.White, radius = 3.dp.toPx(), center = Offset(x, y))

            val dateText = tooltipDateFormat.format(Date(point.timeMillis))
            val amountText = String.format("%.0f", point.value)
            val textWidth = tooltipTextPaint.measureText(dateText).coerceAtLeast(tooltipTextPaint.measureText(amountText)) + 24.dp.toPx()
            val textHeight = 44.dp.toPx()
            var tooltipX = x - textWidth / 2
            if (tooltipX < 0) tooltipX = 0f
            if (tooltipX + textWidth > width) tooltipX = width - textWidth
            val tooltipY = if (y - textHeight - 12.dp.toPx() < 0) y + 12.dp.toPx() else y - textHeight - 12.dp.toPx()
            drawRoundRect(color = lineColor, topLeft = Offset(tooltipX, tooltipY), size = Size(textWidth, textHeight), cornerRadius = CornerRadius(8.dp.toPx()))
            drawContext.canvas.nativeCanvas.drawText(dateText, tooltipX + textWidth / 2, tooltipY + 16.dp.toPx(), tooltipTextPaint)
            drawContext.canvas.nativeCanvas.drawText(amountText, tooltipX + textWidth / 2, tooltipY + 34.dp.toPx(), tooltipTextPaint)
        }
    }
}

@Composable
fun PieChart(data: Map<String, Long>, title: String) {
    if (data.isEmpty()) return

    val chartData = remember(data) {
        val sorted = data.entries.sortedByDescending { it.value }
        val allColors = getChartColors()
        val maxCategories = 9

        if (sorted.size <= maxCategories) {
            sorted.mapIndexed { index, entry ->
                ChartData(entry.key, entry.value, allColors[index % allColors.size])
            }
        } else {
            val topList = sorted.take(maxCategories).mapIndexed { index, entry ->
                ChartData(entry.key, entry.value, allColors[index % allColors.size])
            }
            val otherSum = sorted.drop(maxCategories).sumOf { it.value }
            topList + ChartData("其他", otherSum, Color(0xFFBDBDBD))
        }
    }

    val total = chartData.sumOf { it.value }.toFloat()
    if (total <= 0f) return

    var selectedIndex by remember { mutableIntStateOf(-1) }
    val density = LocalDensity.current.density

    val labelTextPaint = remember {
        android.graphics.Paint().apply {
            textSize = 11f * density
            color = android.graphics.Color.parseColor("#333333")
            typeface = Typeface.DEFAULT
        }
    }

    val centerTitlePaint = remember {
        android.graphics.Paint().apply {
            textSize = 14f * density
            textAlign = android.graphics.Paint.Align.CENTER
            color = android.graphics.Color.GRAY
        }
    }

    val centerValuePaint = remember {
        android.graphics.Paint().apply {
            textSize = 20f * density
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            color = android.graphics.Color.BLACK
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val canvasWidth = size.width.toFloat()
                        val canvasHeight = size.height.toFloat()
                        val center = Offset(canvasWidth / 2, canvasHeight / 2)
                        val vec = offset - center
                        val dist = sqrt(vec.x.pow(2) + vec.y.pow(2))
                        val minDimension = kotlin.math.min(canvasWidth, canvasHeight)
                        val radius = minDimension / 2 * 0.55f
                        val strokeWidth = radius * 0.5f
                        val innerRadiusThreshold = radius - strokeWidth / 2 - 20
                        val outerRadiusThreshold = radius + strokeWidth / 2 + 40

                        if (dist in innerRadiusThreshold..outerRadiusThreshold) {
                            var angle = Math.toDegrees(atan2(vec.y.toDouble(), vec.x.toDouble())).toFloat()
                            if (angle < 0) angle += 360f
                            var touchAngleRelativeToStart = angle + 90f
                            if (touchAngleRelativeToStart >= 360f) touchAngleRelativeToStart -= 360f
                            var currentStartAngle = 0f
                            var foundIndex = -1
                            for (i in chartData.indices) {
                                val sweep = 360f * (chartData[i].value / total)
                                if (touchAngleRelativeToStart >= currentStartAngle && touchAngleRelativeToStart < currentStartAngle + sweep) {
                                    foundIndex = i
                                    break
                                }
                                currentStartAngle += sweep
                            }
                            selectedIndex = if (selectedIndex == foundIndex) -1 else foundIndex
                        } else {
                            selectedIndex = -1
                        }
                    }
                }
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2 * 0.55f
            val strokeWidth = radius * 0.5f
            var startAngle = -90f

            chartData.forEachIndexed { index, slice ->
                val sweepAngle = 360f * (slice.value / total)
                val isSelected = index == selectedIndex
                val currentStrokeWidth = if (isSelected) strokeWidth * 1.15f else strokeWidth

                drawArc(
                    color = slice.color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = currentStrokeWidth, cap = StrokeCap.Butt)
                )

                if (sweepAngle > 8f) {
                    val midAngle = startAngle + sweepAngle / 2
                    val midRad = Math.toRadians(midAngle.toDouble())
                    val outerRadius = radius + strokeWidth / 2
                    val lineStart = Offset(
                        (center.x + outerRadius * cos(midRad)).toFloat(),
                        (center.y + outerRadius * sin(midRad)).toFloat()
                    )
                    val lineOffset = 20.dp.toPx()
                    val elbow = Offset(
                        (center.x + (outerRadius + lineOffset) * cos(midRad)).toFloat(),
                        (center.y + (outerRadius + lineOffset) * sin(midRad)).toFloat()
                    )
                    val isRightSide = cos(midRad) > 0
                    val endLineLength = 25.dp.toPx()
                    val endX = if (isRightSide) elbow.x + endLineLength else elbow.x - endLineLength
                    val lineEnd = Offset(endX, elbow.y)

                    val path = Path().apply {
                        moveTo(lineStart.x, lineStart.y)
                        lineTo(elbow.x, elbow.y)
                        lineTo(lineEnd.x, lineEnd.y)
                    }

                    drawPath(path = path, color = slice.color, style = Stroke(width = 1.dp.toPx()))

                    labelTextPaint.textAlign = if (isRightSide) android.graphics.Paint.Align.LEFT else android.graphics.Paint.Align.RIGHT
                    val textOffset = if (isRightSide) 5.dp.toPx() else -5.dp.toPx()

                    drawContext.canvas.nativeCanvas.drawText(
                        slice.name,
                        endX + textOffset,
                        elbow.y + labelTextPaint.textSize / 3,
                        labelTextPaint
                    )
                }
                startAngle += sweepAngle
            }

            drawIntoCanvas { canvas ->
                if (selectedIndex != -1) {
                    val selectedSlice = chartData[selectedIndex]
                    centerTitlePaint.color = selectedSlice.color.toArgb()
                    centerValuePaint.color = android.graphics.Color.BLACK
                    canvas.nativeCanvas.drawText(selectedSlice.name, center.x, center.y - 10.dp.toPx(), centerTitlePaint)
                    canvas.nativeCanvas.drawText(selectedSlice.value.toString(), center.x, center.y + 16.dp.toPx(), centerValuePaint)
                } else {
                    centerTitlePaint.color = android.graphics.Color.GRAY
                    centerValuePaint.color = android.graphics.Color.BLACK
                    canvas.nativeCanvas.drawText(title, center.x, center.y - 10.dp.toPx(), centerTitlePaint)
                    canvas.nativeCanvas.drawText(String.format("%.0f", total), center.x, center.y + 16.dp.toPx(), centerValuePaint)
                }
            }
        }
    }
}