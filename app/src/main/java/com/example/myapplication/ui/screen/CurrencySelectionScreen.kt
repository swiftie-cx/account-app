package com.example.myapplication.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    // 注意：这里删除了 .sortedBy { it.code } 以保留手动排序

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("选择默认货币") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }
            )
        }
    ) {
        LazyColumn(modifier = Modifier.padding(it)) {
            items(currencies) { currency ->
                CurrencyListItem(currency = currency) {
                    onCurrencySelected(currency.code)
                    navController.popBackStack()
                }
            }
        }
    }
}

@Composable
private fun CurrencyListItem(currency: Currency, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("${currency.name} (${currency.symbol})")
        Text(currency.code)
    }
}