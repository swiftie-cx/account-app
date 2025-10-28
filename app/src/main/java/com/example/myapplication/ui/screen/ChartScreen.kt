package com.example.myapplication.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.viewmodel.ExpenseViewModel
import kotlin.math.abs
@Composable
fun ChartScreen(viewModel: ExpenseViewModel) {
    val allTransactions by viewModel.allExpenses.collectAsState(initial = emptyList())

    // (新) 只过滤出支出 (金额 < 0)
    val expenses = remember(allTransactions) {
        allTransactions.filter { it.amount < 0 }
    }

    val categorySums = remember(expenses) {
        expenses.groupBy { it.category }
            // (修改) 使用 abs() 来合计金额
            .mapValues { entry -> entry.value.sumOf { abs(it.amount) } }
            .toList().sortedByDescending { it.second }
    }

    val totalAmount = remember(categorySums) {
        categorySums.sumOf { it.second }
    }

    val colors = remember {
        listOf(
            Color(0xFFF44336), Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF673AB7),
            Color(0xFF3F51B5), Color(0xFF2196F3), Color(0xFF03A9F4), Color(0xFF00BCD4),
            Color(0xFF009688), Color(0xFF4CAF50), Color(0xFF8BC34A), Color(0xFFCDDC39)
        )
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("支出图表", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(16.dp))

        Box(modifier = Modifier
            .fillMaxWidth()
            .height(200.dp), contentAlignment = Alignment.Center) {
            if (totalAmount > 0) {
                Canvas(modifier = Modifier.size(200.dp)) {
                    var startAngle = -90f
                    categorySums.onEachIndexed { index, (_, sum) ->
                        val sweepAngle = (sum / totalAmount * 360).toFloat()
                        val color = colors[index % colors.size]

                        drawArc(
                            color = color,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(width = 50f, cap = StrokeCap.Round)
                        )
                        startAngle += sweepAngle
                    }
                }
                // 在环形图中间显示总金额
                Text(text = "¥${totalAmount}", style = MaterialTheme.typography.headlineMedium)
            }
        }
        
        Spacer(Modifier.height(16.dp))

        // 图例列表
        LazyColumn {
            itemsIndexed(categorySums) { index, (category, sum) ->
                val percentage = (sum / totalAmount * 100)
                LegendItem(
                    color = colors[index % colors.size],
                    category = category,
                    amount = sum,
                    percentage = percentage
                )
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, category: String, amount: Double, percentage: Double) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier
            .size(16.dp)
            .background(color) // 直接使用系统自带的 background
        )
        Text(text = category, modifier = Modifier.weight(1f))
        Text(text = "${String.format("%.2f", percentage)}%", modifier = Modifier.padding(start = 8.dp))
        Text(text = "¥${amount}", modifier = Modifier.padding(start = 8.dp))
    }
}
