// timeledger/ui/screen/AddTransactionComponents.kt
package com.swiftiecx.timeledger.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource // [关键] 引入资源引用
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.swiftiecx.timeledger.R // [关键] 引入资源ID
import com.swiftiecx.timeledger.data.Account
import com.swiftiecx.timeledger.ui.navigation.Category
import com.swiftiecx.timeledger.ui.navigation.IconMapper
import com.swiftiecx.timeledger.ui.navigation.MainCategory
import com.swiftiecx.timeledger.ui.navigation.Routes
import com.swiftiecx.timeledger.ui.navigation.SubCategory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

// --- 组件定义 ---

@Composable
fun FeeInputCard(
    fee: String,
    currency: String,
    isFocused: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val containerColor = if (enabled) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    }
    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }
    val border = if (isFocused && enabled) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = border
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AttachMoney, null, tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else contentColor)
                Spacer(Modifier.width(8.dp))
                // [i18n]
                Text(stringResource(R.string.fee_label), style = MaterialTheme.typography.titleMedium, color = contentColor)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(fee, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = contentColor)
                if (currency.isNotEmpty()) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = currency,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else contentColor
                    )
                }
            }
        }
    }
}

@Composable
fun ModeSelectionButton(
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        color = containerColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

// [i18n] 改造
@Composable
fun SimpleKeyboardToolbar(
    dateMillis: Long,
    onDateClick: () -> Unit,
    remark: String,
    onRemarkClick: () -> Unit
) {
    // [Fix] 使用更通用的日期格式
    val dateFormat = remember { SimpleDateFormat("MM-dd", Locale.getDefault()) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ActionChipItem(
            icon = Icons.Default.CalendarToday,
            text = dateFormat.format(Date(dateMillis)),
            onClick = onDateClick,
            modifier = Modifier.weight(1f)
        )
        ActionChipItem(
            icon = if(remark.isNotEmpty()) Icons.AutoMirrored.Filled.Note else Icons.Default.Edit,
            // [i18n]
            text = if (remark.isNotEmpty()) remark else stringResource(R.string.remark_label),
            onClick = onRemarkClick,
            modifier = Modifier.weight(1f),
            isHighlight = remark.isNotEmpty()
        )
    }
}

@Composable
fun TransferAccountCard(
    account: Account?,
    label: String,
    displayValue: String,
    amountColor: Color,
    onClick: () -> Unit,
    onCardClick: () -> Unit,
    isFocused: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = if (isFocused) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onClick)
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    if (account != null) {
                        Icon(
                            imageVector = IconMapper.getIcon(account.iconName),
                            contentDescription = null,
                            tint = amountColor,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(Icons.Default.CalendarToday, null, tint = Color.Gray)
                    }
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    // [i18n]
                    Text(
                        text = account?.name ?: stringResource(R.string.select_account),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = displayValue,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                if (account != null) {
                    Text(
                        text = account.currency,
                        style = MaterialTheme.typography.labelSmall,
                        color = amountColor.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

// [i18n] 改造
@Composable
fun NewAmountDisplay(
    category: Category?,
    amount: String,
    backgroundColor: Color = MaterialTheme.colorScheme.primary
) {
    val contentColor = Color.White

    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp).height(100.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (category != null) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = contentColor,
                    fontWeight = FontWeight.Bold
                )
            } else {
                // [i18n]
                Text(stringResource(R.string.select_category), style = MaterialTheme.typography.titleMedium, color = contentColor.copy(alpha = 0.8f))
            }
            Spacer(modifier = Modifier.weight(1f))
            val textStyle = if(amount.length > 8) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.displaySmall
            Text(
                text = amount,
                style = textStyle,
                color = contentColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// [i18n] 改造
@Composable
fun KeyboardActionToolbar(
    button1Icon: ImageVector,
    button1Text: String,
    button1OnClick: () -> Unit,
    button1Highlight: Boolean = false,
    dateMillis: Long,
    onDateClick: () -> Unit,
    remark: String,
    onRemarkClick: () -> Unit
) {
    // [Fix] 使用更通用的日期格式
    val dateFormat = remember { SimpleDateFormat("MM-dd", Locale.getDefault()) }
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // 账户/默认按钮
        ActionChipItem(icon = button1Icon, text = button1Text, onClick = button1OnClick, modifier = Modifier.weight(1f), isHighlight = button1Highlight)

        // 日期按钮
        ActionChipItem(icon = Icons.Default.CalendarToday, text = dateFormat.format(Date(dateMillis)), onClick = onDateClick, modifier = Modifier.weight(1f))

        // 备注按钮
        ActionChipItem(
            icon = if(remark.isNotEmpty()) Icons.AutoMirrored.Filled.Note else Icons.Default.Edit,
            // [i18n]
            text = if (remark.isNotEmpty()) remark else stringResource(R.string.remark_hint),
            onClick = onRemarkClick,
            modifier = Modifier.weight(1.5f),
            isHighlight = remark.isNotEmpty()
        )
    }
}

@Composable
fun ActionChipItem(icon: ImageVector, text: String, onClick: () -> Unit, modifier: Modifier = Modifier, isHighlight: Boolean = false) {
    Surface(onClick = onClick, shape = RoundedCornerShape(8.dp), color = if (isHighlight) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = modifier.height(40.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (isHighlight) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = text, style = MaterialTheme.typography.bodyMedium, maxLines = 1, color = if (isHighlight) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
        }
    }
}

// [i18n] 改造
@Composable
fun RemarkInputDialog(initialRemark: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initialRemark) }
    AlertDialog(
        onDismissRequest = onDismiss,
        // [i18n]
        title = { Text(stringResource(R.string.remark_label)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                // [i18n]
                placeholder = { Text(stringResource(R.string.remark_hint)) },
                singleLine = true
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text(stringResource(R.string.confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

// [i18n] 改造
@Composable
fun AccountPickerDialog(accounts: List<Account>, onAccountSelected: (Account) -> Unit, onDismissRequest: () -> Unit, navController: NavHostController) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        // [i18n]
        title = { Text(stringResource(R.string.select_account)) },
        text = {
            LazyColumn {
                items(accounts.size) { index ->
                    val account = accounts[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAccountSelected(account) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val icon = IconMapper.getIcon(account.iconName)
                        Icon(icon, contentDescription = account.name, modifier = Modifier.padding(end = 16.dp))
                        Text(account.name)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.cancel)) } },
        // [i18n]
        dismissButton = { TextButton(onClick = { navController.navigate(Routes.ACCOUNT_MANAGEMENT); onDismissRequest() }) { Text(stringResource(R.string.manage_account)) } }
    )
}

// ... evaluateExpression, MainCategoryItem, SubCategoryItem, validateInputPrecision, smartFormat 等辅助函数保持不变
// 定义不需要小数位的货币代码集合
private val ZERO_DECIMAL_CURRENCIES = setOf(
    "JPY", // 日元
    "KRW", // 韩元
    "VND", // 越南盾
    "IDR", // 印尼盾
    "HUF", // 匈牙利福林
    "CLP", // 智利比索
    "PYG"  // 巴拉圭瓜拉尼
)
// 获取货币允许的最大小数位数
fun getCurrencyDecimalLimit(currencyCode: String): Int {
    return if (currencyCode.uppercase() in ZERO_DECIMAL_CURRENCIES) 0 else 2
}

/**
 * 智能金额格式化 (用于显示计算结果)
 */
fun smartFormat(value: Double, currencyCode: String = ""): String {
    val limit = getCurrencyDecimalLimit(currencyCode)

    // 如果是0位小数货币，直接四舍五入取整
    if (limit == 0) {
        return value.roundToInt().toString()
    }

    // 对于2位小数货币：
    // 如果非常接近整数，显示整数 (100.00 -> 100)
    // 否则保留2位小数 (100.8 -> 100.80)
    val isInteger = abs(value - value.toLong()) < 0.001
    return if (isInteger) {
        value.toLong().toString()
    } else {
        String.format(Locale.US, "%.${limit}f", value)
    }
}

/**
 * 验证输入是否符合货币精度要求 (用于键盘输入拦截)
 * @param input 用户想要输入的新字符串 (例如 "10.825")
 * @param currencyCode 当前货币代码
 */
fun validateInputPrecision(input: String, currencyCode: String): Boolean {
    // 允许空或中间状态 (如 "10.")
    if (input.isEmpty() || input == ".") return true

    // 如果无法转为数字，视为非法
    if (input.toDoubleOrNull() == null) return false

    val limit = getCurrencyDecimalLimit(currencyCode)
    val dotIndex = input.indexOf('.')

    // 如果没有小数点，通过
    if (dotIndex == -1) return true

    // 如果是0位小数货币，却输入了小数点，拦截
    if (limit == 0) return false

    // 计算小数位数
    val decimals = input.length - dotIndex - 1
    return decimals <= limit
}
@Composable
fun MainCategoryItem(mainCategory: MainCategory, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor = if (isSelected) mainCategory.color.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val contentColor = if (isSelected) mainCategory.color else MaterialTheme.colorScheme.onSurfaceVariant
    val border = if (isSelected) BorderStroke(1.dp, mainCategory.color.copy(alpha = 0.5f)) else null

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .background(bgColor)
            .then(if (border != null) Modifier.padding(1.dp) else Modifier)
            .padding(vertical = 12.dp, horizontal = 4.dp)
    ) {
        Icon(
            imageVector = mainCategory.icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(32.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = mainCategory.title,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor,
            fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
fun SubCategoryItem(
    subCategory: SubCategory,
    mainColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) mainColor else Color.Transparent
    val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = subCategory.icon,
                contentDescription = subCategory.title,
                tint = if(isSelected) contentColor else mainColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subCategory.title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if(isSelected) mainColor else Color.Unspecified
        )
    }
}
fun evaluateExpression(expression: String): Double {
    val parts = expression.trim().split(" ")
    if (parts.size == 1) return parts[0].toDoubleOrNull() ?: 0.0

    var result = parts[0].toDoubleOrNull() ?: 0.0
    var i = 1
    while (i < parts.size - 1) {
        val operator = parts[i]
        val nextValue = parts[i + 1].toDoubleOrNull()
        if (nextValue != null) {
            result = calculate(result, operator, nextValue)
        }
        i += 2
    }
    return result
}

/**
 * 执行基本的加减乘除运算
 */
private fun calculate(operand1: Double, operator: String, operand2: Double): Double {
    return when (operator) {
        "+" -> operand1 + operand2
        "-" -> operand1 - operand2
        "*" -> operand1 * operand2
        "/" -> operand1 / operand2
        else -> operand1
    }
}