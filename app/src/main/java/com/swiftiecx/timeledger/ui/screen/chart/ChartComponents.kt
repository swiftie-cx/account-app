package com.swiftiecx.timeledger.ui.screen.chart

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.swiftiecx.timeledger.R
import kotlin.math.abs

@Composable
fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize().padding(top = 100.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.chart_empty_state), color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun StatSelectionCard(
    title: String,
    amount: Double,
    currency: String,
    isSelected: Boolean,
    selectedBgColor: Color,
    unselectedBgColor: Color,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isHighlight: Boolean
) {
    // 选中态内部“厚阴影/脏边”主要来自 Surface 的 tonalElevation + 半透明背景叠色
    // 这里统一把背景色强制为不透明，并关闭 tonalElevation；
    // 同时对结余（isHighlight）在选中态关闭 shadow，以彻底避免“内圈”观感
    val targetBg = if (isSelected) selectedBgColor else unselectedBgColor
    val backgroundColor by animateColorAsState(
        targetValue = targetBg.copy(alpha = 1f),
        label = "bgColor"
    )

    val baseElevation = 2.dp
    val shadow = if (isHighlight && isSelected) 0.dp else baseElevation

    val isLightBg = backgroundColor.luminance() > 0.5f

    val finalAmountColor = if (isSelected) {
        if (isHighlight) {
            if (isLightBg) Color(0xFF212121) else Color.White
        } else {
            textColor
        }
    } else {
        textColor
    }

    val finalTitleColor = if (isSelected) {
        if (isLightBg) Color(0xFF212121).copy(alpha = 0.7f) else Color.White.copy(alpha = 0.8f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val displayAmount = if (isHighlight) amount else kotlin.math.abs(amount)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        shadowElevation = shadow,
        tonalElevation = 0.dp,
        modifier = modifier.padding(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = if (isHighlight) Alignment.CenterHorizontally else Alignment.Start
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = finalTitleColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.currency_amount_format_chart, currency, displayAmount),
                style = if (isHighlight) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = finalAmountColor,
                maxLines = 1
            )
        }
    }
}


@Composable
fun CategoryRankItem(
    name: String,
    amount: Double,
    percentage: Float,
    color: Color,
    ratio: Float,
    icon: ImageVector?,
    currency: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = name, tint = color, modifier = Modifier.size(20.dp))
            } else {
                Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Row {
                    Text(stringResource(R.string.percent_format, percentage), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.currency_amount_no_decimal_format, currency, amount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(ratio.coerceIn(0f, 1f))
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(color)
                )
            }
        }
    }
}

@Composable
fun SubCategoryRankItem(
    stat: SubCategoryStat,
    color: Color,
    currency: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.5f))
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = stat.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            modifier = Modifier.weight(1f)
        )

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = stringResource(R.string.currency_amount_no_decimal_format_small, currency, stat.amount),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            Text(
                text = stringResource(R.string.percent_format, stat.percentageOfParent),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}