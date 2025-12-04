package com.example.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.viewmodel.ExpenseViewModel

@Composable
fun LockScreen(
    viewModel: ExpenseViewModel,
    onUnlockSuccess: () -> Unit
) {
    val privacyType = viewModel.getPrivacyType()
    var inputPin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 24.dp), // 增加水平边距，视觉更聚拢
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // (修改) 顶部空白区域加大，让内容整体下移
            Spacer(modifier = Modifier.weight(0.8f))

            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp) // 稍微加大图标
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "请验证密码",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 错误提示区，固定高度防止跳动
            Box(modifier = Modifier.height(24.dp), contentAlignment = Alignment.Center) {
                if (errorMsg.isNotEmpty()) {
                    Text(text = errorMsg, color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // 根据类型渲染不同的解锁界面
            if (privacyType == "PIN") {
                // PIN 码点
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp), // 增加点间距
                    modifier = Modifier.padding(bottom = 48.dp)
                ) {
                    repeat(4) { index ->
                        val filled = index < inputPin.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(
                                    if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    CircleShape
                                )
                        )
                    }
                }

                // (修改) PIN 键盘位于底部
                Box(modifier = Modifier.weight(1.5f), contentAlignment = Alignment.BottomCenter) {
                    InteractivePinPad(
                        onNumberClick = { num ->
                            if (inputPin.length < 4) {
                                inputPin += num
                                errorMsg = "" // 清除错误提示
                                if (inputPin.length == 4) {
                                    if (viewModel.verifyPin(inputPin)) {
                                        onUnlockSuccess()
                                    } else {
                                        errorMsg = "密码错误"
                                        inputPin = "" // 清空重输
                                    }
                                }
                            }
                        },
                        onDeleteClick = { if (inputPin.isNotEmpty()) inputPin = inputPin.dropLast(1) }
                    )
                }

            } else if (privacyType == "PATTERN") {
                // 手势区域
                // (修改) 将手势区域放在中下方
                Box(
                    modifier = Modifier
                        .weight(2f) // 占据主要空间
                        .fillMaxWidth()
                        .aspectRatio(1f), // 保持正方形
                    contentAlignment = Alignment.Center
                ) {
                    InteractivePatternLock(
                        onPatternComplete = { pattern ->
                            if (viewModel.verifyPattern(pattern)) {
                                onUnlockSuccess()
                            } else {
                                errorMsg = "图案错误"
                            }
                        }
                    )
                }

                // 底部留白，防止图案太靠底
                Spacer(modifier = Modifier.weight(0.5f))
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}