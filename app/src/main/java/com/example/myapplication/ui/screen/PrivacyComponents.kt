package com.example.myapplication.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// 数字键盘组件
@Composable
fun InteractivePinPad(
    onNumberClick: (String) -> Unit,
    onDeleteClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "del")
        )

        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { char ->
                    if (char.isEmpty()) {
                        Spacer(modifier = Modifier.size(72.dp))
                    } else if (char == "del") {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clickable { onDeleteClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "删除")
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clickable { onNumberClick(char) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = char,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

// 手势锁组件
@Composable
fun InteractivePatternLock(
    onPatternComplete: (List<Int>) -> Unit
) {
    var dotPositions by remember { mutableStateOf(List(9) { Offset.Zero }) }
    var selectedDots by remember { mutableStateOf(listOf<Int>()) }
    var currentDragPos by remember { mutableStateOf<Offset?>(null) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val index = findDotIndex(offset, dotPositions)
                        if (index != -1) {
                            selectedDots = listOf(index)
                            currentDragPos = offset
                        } else {
                            selectedDots = emptyList()
                            currentDragPos = null
                        }
                    },
                    onDrag = { change, _ ->
                        val pos = change.position
                        currentDragPos = pos
                        val index = findDotIndex(pos, dotPositions)
                        if (index != -1 && index !in selectedDots) {
                            selectedDots = selectedDots + index
                        }
                    },
                    onDragEnd = {
                        if (selectedDots.isNotEmpty()) {
                            onPatternComplete(selectedDots)
                        }
                        selectedDots = emptyList()
                        currentDragPos = null
                    }
                )
            }
    ) {
        val width = size.width
        val height = size.height
        val spacing = width / 4

        if (dotPositions[0] == Offset.Zero) {
            val dots = mutableListOf<Offset>()
            val startX = width / 2 - spacing
            val startY = height / 2 - spacing
            for (row in 0..2) {
                for (col in 0..2) {
                    dots.add(Offset(x = startX + col * spacing, y = startY + row * spacing))
                }
            }
            dotPositions = dots
        }

        if (selectedDots.isNotEmpty()) {
            val path = Path()
            val startDot = dotPositions[selectedDots[0]]
            path.moveTo(startDot.x, startDot.y)

            for (i in 1 until selectedDots.size) {
                val dot = dotPositions[selectedDots[i]]
                path.lineTo(dot.x, dot.y)
            }
            if (currentDragPos != null) {
                path.lineTo(currentDragPos!!.x, currentDragPos!!.y)
            }

            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        dotPositions.forEachIndexed { index, offset ->
            val isSelected = index in selectedDots
            drawCircle(
                color = if (isSelected) primaryColor.copy(alpha = 0.3f) else trackColor,
                radius = 12.dp.toPx(),
                center = offset
            )
            if (isSelected) {
                drawCircle(
                    color = primaryColor,
                    radius = 6.dp.toPx(),
                    center = offset
                )
            }
        }
    }
}

private fun findDotIndex(touchPos: Offset, dots: List<Offset>, threshold: Float = 100f): Int {
    dots.forEachIndexed { index, dotPos ->
        val dx = touchPos.x - dotPos.x
        val dy = touchPos.y - dotPos.y
        if (dx * dx + dy * dy < threshold * threshold) {
            return index
        }
    }
    return -1
}