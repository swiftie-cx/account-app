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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.swiftiecx.timeledger.R
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

@Composable
fun SimpleKeyboardToolbar(
    dateMillis: Long,
    onDateClick: () -> Unit,
    remark: String,
    onRemarkClick: () -> Unit
) {
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
    val dateFormat = remember { SimpleDateFormat("MM-dd", Locale.getDefault()) }
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ActionChipItem(icon = button1Icon, text = button1Text, onClick = button1OnClick, modifier = Modifier.weight(1f), isHighlight = button1Highlight)
        ActionChipItem(icon = Icons.Default.CalendarToday, text = dateFormat.format(Date(dateMillis)), onClick = onDateClick, modifier = Modifier.weight(1f))
        ActionChipItem(
            icon = if(remark.isNotEmpty()) Icons.AutoMirrored.Filled.Note else Icons.Default.Edit,
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

@Composable
fun RemarkInputDialog(initialRemark: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initialRemark) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.remark_label)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.remark_hint)) },
                singleLine = true
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text(stringResource(R.string.confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
fun AccountPickerDialog(accounts: List<Account>, onAccountSelected: (Account) -> Unit, onDismissRequest: () -> Unit, navController: NavHostController) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
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
        dismissButton = { TextButton(onClick = { navController.navigate(Routes.ACCOUNT_MANAGEMENT); onDismissRequest() }) { Text(stringResource(R.string.manage_account)) } }
    )
}

private val ZERO_DECIMAL_CURRENCIES = setOf("JPY", "KRW", "VND", "IDR", "HUF", "CLP", "PYG")
fun getCurrencyDecimalLimit(currencyCode: String): Int = if (currencyCode.uppercase() in ZERO_DECIMAL_CURRENCIES) 0 else 2

fun smartFormat(value: Double, currencyCode: String = ""): String {
    val limit = getCurrencyDecimalLimit(currencyCode)
    if (limit == 0) return value.roundToInt().toString()
    val isInteger = abs(value - value.toLong()) < 0.001
    return if (isInteger) value.toLong().toString() else String.format(Locale.US, "%.${limit}f", value)
}

fun validateInputPrecision(input: String, currencyCode: String): Boolean {
    if (input.isEmpty() || input == ".") return true
    if (input.toDoubleOrNull() == null) return false
    val limit = getCurrencyDecimalLimit(currencyCode)
    val dotIndex = input.indexOf('.')
    if (dotIndex == -1) return true
    if (limit == 0) return false
    val decimals = input.length - dotIndex - 1
    return decimals <= limit
}

// --- 核心修复：手写 AutoSizeText 组件 ---
// 这是一个自动缩放文字大小的组件，不需要引入外部库
@Composable
fun AutoSizeText(
    text: String,
    style: TextStyle,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = 12.sp, // 起始大小
    minFontSize: TextUnit = 8.sp, // 最小大小
    fontWeight: FontWeight? = null,
    maxLines: Int = 1
) {
    var resizedTextStyle by remember { mutableStateOf(style.copy(fontSize = fontSize)) }
    var readyToDraw by remember { mutableStateOf(false) }

    Text(
        text = text,
        color = color,
        maxLines = maxLines,
        softWrap = false,
        overflow = TextOverflow.Visible, // 暂时允许溢出以测量
        style = resizedTextStyle,
        fontWeight = fontWeight,
        textAlign = TextAlign.Center,
        onTextLayout = { textLayoutResult ->
            if (textLayoutResult.didOverflowWidth || textLayoutResult.didOverflowHeight) {
                // 如果溢出，减小字体
                if (resizedTextStyle.fontSize > minFontSize) {
                    val newSize = resizedTextStyle.fontSize * 0.9f
                    resizedTextStyle = resizedTextStyle.copy(fontSize = newSize)
                } else {
                    readyToDraw = true // 到了最小也没办法了，显示吧
                }
            } else {
                readyToDraw = true
            }
        },
        modifier = Modifier.drawWithContent {
            if (readyToDraw) drawContent()
        }
    )
}

// [修改] 使用我们手写的 AutoSizeText
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
            .padding(8.dp) // [恢复] 正方形比例
    ) {
        Icon(
            imageVector = mainCategory.icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(32.dp)
        )
        Spacer(Modifier.height(8.dp))

        // [修改] 调用手写的 AutoSizeText
        AutoSizeText(
            text = mainCategory.title,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 11.sp,
            color = contentColor,
            fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Medium,
            minFontSize = 8.sp
        )
    }
}

// [修改] 使用我们手写的 AutoSizeText
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

        // [修改] 调用手写的 AutoSizeText
        AutoSizeText(
            text = subCategory.title,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 11.sp,
            color = if(isSelected) mainColor else Color.Unspecified,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            minFontSize = 8.sp
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

private fun calculate(operand1: Double, operator: String, operand2: Double): Double {
    return when (operator) {
        "+" -> operand1 + operand2
        "-" -> operand1 - operand2
        "*" -> operand1 * operand2
        "/" -> operand1 / operand2
        else -> operand1
    }
}