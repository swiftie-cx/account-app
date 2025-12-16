package com.swiftiecx.timeledger.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

data class Currency(val code: String, val name: String, val symbol: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencySelectionScreen(navController: NavController, onCurrencySelected: (String) -> Unit) {
    // 手动排序：主流货币置顶，其他按字母/区域排序
    val currencies = listOf(
        // --- 主流常用 ---
        Currency("CNY", "人民币", "¥"),
        Currency("USD", "美元", "$"),
        Currency("EUR", "欧元", "€"),
        Currency("JPY", "日元", "¥"),
        Currency("HKD", "港币", "HK$"),
        Currency("GBP", "英镑", "£"),
        Currency("AUD", "澳元", "A$"),
        Currency("CAD", "加元", "C$"),
        Currency("SGD", "新加坡元", "S$"),
        Currency("TWD", "新台币", "NT$"),

        // --- 亚洲其他 ---
        Currency("KRW", "韩元", "₩"),
        Currency("THB", "泰铢", "฿"),
        Currency("MYR", "马来西亚林吉特", "RM"),
        Currency("PHP", "菲律宾比索", "₱"),
        Currency("IDR", "印尼盾", "Rp"),
        Currency("INR", "印度卢比", "₹"),
        Currency("VND", "越南盾", "₫"),

        // --- 欧洲其他 ---
        Currency("CHF", "瑞士法郎", "Fr"),
        Currency("SEK", "瑞典克朗", "kr"),
        Currency("NOK", "挪威克朗", "kr"),
        Currency("DKK", "丹麦克朗", "kr"),
        Currency("RUB", "俄罗斯卢布", "₽"),
        Currency("TRY", "土耳其里拉", "₺"),
        Currency("CZK", "捷克克朗", "Kč"),
        Currency("HUF", "匈牙利福林", "Ft"),
        Currency("PLN", "波兰兹罗提", "zł"),
        Currency("BGN", "保加利亚列弗", "лв"),
        Currency("RON", "罗马尼亚列伊", "lei"),

        // --- 美洲/大洋洲/非洲其他 ---
        Currency("NZD", "新西兰元", "NZ$"),
        Currency("BRL", "巴西雷亚尔", "R$"),
        Currency("MXN", "墨西哥比索", "Mex$"),
        Currency("ZAR", "南非兰特", "R"),
        Currency("ILS", "以色列新谢克尔", "₪")
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("选择默认货币", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(currencies) { currency ->
                CurrencyItemCard(currency) {
                    onCurrencySelected(currency.code)
                    navController.popBackStack()
                }
            }
        }
    }
}

@Composable
private fun CurrencyItemCard(currency: Currency, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = currency.code,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = currency.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = currency.symbol,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}