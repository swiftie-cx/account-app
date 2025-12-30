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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.swiftiecx.timeledger.R

data class Currency(val code: String, val name: String, val symbol: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencySelectionScreen(navController: NavController, onCurrencySelected: (String) -> Unit) {
    // 浅灰色背景常量
    val backgroundColor = Color(0xFFF5F5F5)

    // 手动排序：主流货币置顶，其他按字母/区域排序
    val currencies = listOf(
        // --- 主流常用 ---
        Currency("CNY", stringResource(R.string.currency_name_cny), "¥"),
        Currency("USD", stringResource(R.string.currency_name_usd), "$"),
        Currency("EUR", stringResource(R.string.currency_name_eur), "€"),
        Currency("JPY", stringResource(R.string.currency_name_jpy), "¥"),
        Currency("HKD", stringResource(R.string.currency_name_hkd), "HK$"),
        Currency("GBP", stringResource(R.string.currency_name_gbp), "£"),
        Currency("AUD", stringResource(R.string.currency_name_aud), "A$"),
        Currency("CAD", stringResource(R.string.currency_name_cad), "C$"),
        Currency("SGD", stringResource(R.string.currency_name_sgd), "S$"),
        Currency("TWD", stringResource(R.string.currency_name_twd), "NT$"),

        // --- 亚洲其他 ---
        Currency("KRW", stringResource(R.string.currency_name_krw), "₩"),
        Currency("THB", stringResource(R.string.currency_name_thb), "฿"),
        Currency("MYR", stringResource(R.string.currency_name_myr), "RM"),
        Currency("PHP", stringResource(R.string.currency_name_php), "₱"),
        Currency("IDR", stringResource(R.string.currency_name_idr), "Rp"),
        Currency("INR", stringResource(R.string.currency_name_inr), "₹"),
        Currency("VND", stringResource(R.string.currency_name_vnd), "₫"),

        // --- 欧洲其他 ---
        Currency("CHF", stringResource(R.string.currency_name_chf), "Fr"),
        Currency("SEK", stringResource(R.string.currency_name_sek), "kr"),
        Currency("NOK", stringResource(R.string.currency_name_nok), "kr"),
        Currency("DKK", stringResource(R.string.currency_name_dkk), "kr"),
        Currency("RUB", stringResource(R.string.currency_name_rub), "₽"),
        Currency("TRY", stringResource(R.string.currency_name_try), "₺"),
        Currency("CZK", stringResource(R.string.currency_name_czk), "Kč"),
        Currency("HUF", stringResource(R.string.currency_name_huf), "Ft"),
        Currency("PLN", stringResource(R.string.currency_name_pln), "zł"),
        Currency("BGN", stringResource(R.string.currency_name_bgn), "лв"),
        Currency("RON", stringResource(R.string.currency_name_ron), "lei"),

        // --- 美洲/大洋洲/非洲其他 ---
        Currency("NZD", stringResource(R.string.currency_name_nzd), "NZ$"),
        Currency("BRL", stringResource(R.string.currency_name_brl), "R$"),
        Currency("MXN", stringResource(R.string.currency_name_mxn), "Mex$"),
        Currency("ZAR", stringResource(R.string.currency_name_zar), "R"),
        Currency("ILS", stringResource(R.string.currency_name_ils), "₪")
    )

    Scaffold(
        // [修改 1] 设置页面整体背景为浅灰
        containerColor = backgroundColor,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.select_default_currency), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    // [修改 2] TopBar 背景也设为浅灰，与页面融为一体
                    containerColor = backgroundColor
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
        shape = RoundedCornerShape(20.dp), // 圆角稍微大一点，更有现代感
        colors = CardDefaults.cardColors(
            containerColor = Color.White // 卡片本身纯白
        ),
        // [修改 3] 移除边框，改用 elevation (阴影) 制造悬浮感
        // border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
        elevation = CardDefaults.cardElevation(2.dp)
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