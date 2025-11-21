package com.example.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

@Composable
fun NumericKeyboard(
    onNumberClick: (String) -> Unit,
    onOperatorClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onDateClick: () -> Unit,
    onDoneClick: () -> Unit,
    onEqualsClick: () -> Unit,
    isCalculation: Boolean,
    selectedDate: Long? = null // (修改) 改为可选参数，默认值为 null，这样就不会影响 Budget 页面了
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 第一行: 7, 8, 9, 日期
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberButton("7", Modifier.weight(1f)) { onNumberClick("7") }
            NumberButton("8", Modifier.weight(1f)) { onNumberClick("8") }
            NumberButton("9", Modifier.weight(1f)) { onNumberClick("9") }

            // (修改) 判断逻辑：如果有具体日期，显示日期按钮；否则显示通用图标
            if (selectedDate != null) {
                DateActionButton(
                    dateMillis = selectedDate,
                    modifier = Modifier.weight(1f),
                    onClick = onDateClick
                )
            } else {
                // 预算页面没有传日期，显示默认图标
                ActionButton(Icons.Default.DateRange, Modifier.weight(1f)) { onDateClick() }
            }
        }

        // 第二行: 4, 5, 6, +
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberButton("4", Modifier.weight(1f)) { onNumberClick("4") }
            NumberButton("5", Modifier.weight(1f)) { onNumberClick("5") }
            NumberButton("6", Modifier.weight(1f)) { onNumberClick("6") }
            OperatorButton("+", Modifier.weight(1f)) { onOperatorClick("+") }
        }

        // 第三行: 1, 2, 3, -
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberButton("1", Modifier.weight(1f)) { onNumberClick("1") }
            NumberButton("2", Modifier.weight(1f)) { onNumberClick("2") }
            NumberButton("3", Modifier.weight(1f)) { onNumberClick("3") }
            OperatorButton("-", Modifier.weight(1f)) { onOperatorClick("-") }
        }

        // 第四行: ., 0, 退格, 完成/等号
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberButton(".", Modifier.weight(1f)) { onNumberClick(".") }
            NumberButton("0", Modifier.weight(1f)) { onNumberClick("0") }
            ActionButton(Icons.AutoMirrored.Filled.Backspace, Modifier.weight(1f)) { onBackspaceClick() }
            if (isCalculation) {
                OperatorButton("=", Modifier.weight(1f), backgroundColor = MaterialTheme.colorScheme.primaryContainer) { onEqualsClick() }
            } else {
                ActionButton(Icons.Default.Check, Modifier.weight(1f), backgroundColor = MaterialTheme.colorScheme.primaryContainer) { onDoneClick() }
            }
        }
    }
}

@Composable
fun DateActionButton(
    dateMillis: Long,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = dateMillis
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH) + 1
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val dateText = "$year/$month/$day"

    Box(
        modifier = modifier
            .height(60.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = "Date",
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = dateText,
                fontSize = 10.sp,
                lineHeight = 10.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun NumberButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(60.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun OperatorButton(text: String, modifier: Modifier = Modifier, backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(60.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun ActionButton(icon: ImageVector, modifier: Modifier = Modifier, backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(60.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null)
    }
}