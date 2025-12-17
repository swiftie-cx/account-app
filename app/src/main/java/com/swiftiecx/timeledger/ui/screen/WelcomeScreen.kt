package com.swiftiecx.timeledger.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.swiftiecx.timeledger.R
import com.swiftiecx.timeledger.ui.viewmodel.ExpenseViewModel
import java.util.Currency
import java.util.Locale

@Composable
fun WelcomeScreen(
    navController: NavHostController,
    viewModel: ExpenseViewModel
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var amountStr by remember { mutableStateOf("") }

    // [修改 1] 监听 ViewModel 中已经自动检测好的货币代码 (例如 "KRW", "CNY", "JPY")
    val defaultCurrencyCode by viewModel.defaultCurrency.collectAsState()

    // [修改 2] 将代码 (KRW) 转换为符号 (₩) 用于显示
    // 这里的 key = defaultCurrencyCode 意味着如果 ViewModel 里的货币变了，符号也会跟着变
    val currencySymbol = remember(defaultCurrencyCode) {
        try {
            Currency.getInstance(defaultCurrencyCode).symbol
        } catch (e: Exception) {
            defaultCurrencyCode // 如果转换失败，直接显示代码
        }
    }

    // 背景渐变色 (清新晨光感)
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFE3F2FD), // 浅蓝
            Color(0xFFF3E5F5), // 浅紫
            MaterialTheme.colorScheme.background
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush)
            .padding(24.dp)
            .systemBarsPadding() // 避开状态栏
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 1. Logo / 图标区域
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WbSunny, // 使用太阳图标代表新的一天
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // 2. 欢迎语
            Text(
                // [i18n] 欢迎使用
                text = stringResource(R.string.welcome_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            // [i18n] 副标题
            Text(
                text = stringResource(R.string.welcome_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            // 3. 极简输入框
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    // [i18n] 初始余额标签
                    Text(
                        text = stringResource(R.string.initial_asset_balance),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 显示动态获取的符号
                        Text(
                            text = currencySymbol,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        // 使用无边框的 OutlinedTextField，视觉上更干净
                        OutlinedTextField(
                            value = amountStr,
                            onValueChange = { if (it.length <= 12) amountStr = it },
                            placeholder = { Text("0.00", color = Color.LightGray) },
                            textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { keyboardController?.hide() }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // 4. 开始按钮
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    // [逻辑] 这里的 completeOnboarding 内部会读取 _defaultCurrency.value
                    // 而界面显示的符号也是根据这个 value 转换的，实现了逻辑统一
                    viewModel.completeOnboarding(amount)

                    // 跳转到主页并清空回退栈
                    navController.navigate("details") {
                        popUpTo(0) // 清空所有回退栈，防止返回欢迎页
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                // [i18n] 开启旅程
                Text(
                    text = stringResource(R.string.start_journey),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
            }
        }
    }
}