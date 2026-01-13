package com.swiftiecx.timeledger.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource // [关键] 引入资源引用
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.swiftiecx.timeledger.R // [关键] 引入资源ID

@Composable
fun NumericKeyboard(
    onNumberClick: (String) -> Unit,
    onOperatorClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onAgainClick: (() -> Unit)? = null,
    onDoneClick: () -> Unit,
    onEqualsClick: () -> Unit,
    isCalculation: Boolean,
    selectedDate: Long? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 第一行: 7, 8, 9, 删除
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberButton("7", Modifier.weight(1f)) { onNumberClick("7") }
            NumberButton("8", Modifier.weight(1f)) { onNumberClick("8") }
            NumberButton("9", Modifier.weight(1f)) { onNumberClick("9") }
            ActionButton(Icons.AutoMirrored.Filled.Backspace, Modifier.weight(1f)) { onBackspaceClick() }
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

        // 第四行: ., 0(变宽), 再记(可选), 完成/等号
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberButton(".", Modifier.weight(1f)) { onNumberClick(".") }

            // (修改) 动态计算 0 的权重
            val zeroWeight = if (onAgainClick != null) 1f else 2f
            NumberButton("0", Modifier.weight(zeroWeight)) { onNumberClick("0") }

            if (onAgainClick != null) {
                TextActionButton(
                    // [i18n]
                    text = stringResource(R.string.add_again),
                    modifier = Modifier.weight(1f),
                    onClick = onAgainClick
                )
            }

            // 完成/等号按钮
            if (isCalculation) {
                OperatorButton(
                    text = "=",
                    modifier = Modifier.weight(1f),
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer
                ) { onEqualsClick() }
            } else {
                TextActionButton(
                    // [i18n]
                    text = stringResource(R.string.finish),
                    modifier = Modifier.weight(1f),
                    backgroundColor = MaterialTheme.colorScheme.primary,
                    textColor = MaterialTheme.colorScheme.onPrimary,
                    onClick = onDoneClick
                )
            }
        }
    }
}

// 辅助组件保持不变
@Composable
fun TextActionButton(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(60.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
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