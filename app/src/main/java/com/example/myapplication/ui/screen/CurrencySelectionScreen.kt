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
    val currencies = listOf(
        Currency("CNY", "中国人民币", "元"),
        Currency("USD", "美国美元", "$"),
        Currency("JPY", "日圆", "¥"),
        Currency("EUR", "欧元", "€"),
        Currency("GBP", "英镑", "£"),
        Currency("CAD", "加拿大元", "C$"),
        Currency("AUD", "澳大利亚元", "A$"),
        Currency("HKD", "港元", "HK$"),
        Currency("KRW", "韩元", "₩"),
        Currency("SGD", "新加坡元", "S$"),
        Currency("INR", "印度卢比", "₹"),
        Currency("IDR", "印尼盾", "Rp")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("选择") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }
            )
        }
    ) {
        LazyColumn(modifier = Modifier.padding(it)) {
            items(currencies) {
                CurrencyListItem(currency = it) {
                    onCurrencySelected(it.code)
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
