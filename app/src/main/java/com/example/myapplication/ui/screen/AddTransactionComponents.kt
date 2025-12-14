package com.example.myapplication.ui.screen

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.myapplication.data.Account
import com.example.myapplication.ui.navigation.Category
import com.example.myapplication.ui.navigation.IconMapper
import com.example.myapplication.ui.navigation.MainCategory
import com.example.myapplication.ui.navigation.SubCategory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import java.text.DecimalFormat
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
                Text("手续费", style = MaterialTheme.typography.titleMedium, color = contentColor)
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

@Composable
fun SimpleKeyboardToolbar(
    dateMillis: Long,
    onDateClick: () -> Unit,
    remark: String,
    onRemarkClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MM月dd日", Locale.getDefault()) }
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
            text = if (remark.isNotEmpty()) remark else "添加备注",
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
                    Text(
                        text = account?.name ?: "选择",
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
                Text(text = "选择分类", style = MaterialTheme.typography.titleMedium, color = contentColor.copy(alpha = 0.8f))
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
    val dateFormat = remember { SimpleDateFormat("MM月dd日", Locale.getDefault()) }
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ActionChipItem(icon = button1Icon, text = button1Text, onClick = button1OnClick, modifier = Modifier.weight(1f), isHighlight = button1Highlight)
        ActionChipItem(icon = Icons.Default.CalendarToday, text = dateFormat.format(Date(dateMillis)), onClick = onDateClick, modifier = Modifier.weight(1f))
        ActionChipItem(icon = if(remark.isNotEmpty()) Icons.AutoMirrored.Filled.Note else Icons.Default.Edit, text = if (remark.isNotEmpty()) remark else "添加备注", onClick = onRemarkClick, modifier = Modifier.weight(1.5f), isHighlight = remark.isNotEmpty())
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

@Composable
fun RemarkInputDialog(initialRemark: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initialRemark) }
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("添加备注") },
        text = { OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("请输入备注内容...") }, singleLine = true) },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun AccountPickerDialog(accounts: List<Account>, onAccountSelected: (Account) -> Unit, onDismissRequest: () -> Unit, navController: NavHostController) {
    AlertDialog(
        onDismissRequest = onDismissRequest, title = { Text("选择账户") },
        text = { LazyColumn { items(accounts.size) { index -> val account = accounts[index]; Row(modifier = Modifier.fillMaxWidth().clickable { onAccountSelected(account) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { val icon = IconMapper.getIcon(account.iconName); Icon(icon, contentDescription = account.name, modifier = Modifier.padding(end = 16.dp)); Text(account.name) } } } },
        confirmButton = { TextButton(onClick = onDismissRequest) { Text("取消") } },
        dismissButton = { TextButton(onClick = { navController.navigate("account_management"); onDismissRequest() }) { Text("账户管理") } }
    )
}

fun evaluateExpression(expression: String): Double {
    val tokens = expression.split(" ")
    if (tokens.isEmpty()) return 0.0
    var result = tokens[0].toDoubleOrNull() ?: 0.0
    for (i in 1 until tokens.size step 2) {
        if (i + 1 >= tokens.size) break
        val operator = tokens[i]
        val nextOperand = tokens[i + 1].toDoubleOrNull() ?: 0.0
        if (operator == "+") result += nextOperand else if (operator == "-") result -= nextOperand
    }
    return result
}

@Composable
fun MainCategoryItem(mainCategory: MainCategory, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor = if (isSelected) mainCategory.color.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
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