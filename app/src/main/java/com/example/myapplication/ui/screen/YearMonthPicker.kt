package com.example.myapplication.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.* // 使用 * 导入
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// 只包含年月选择逻辑
@Composable
fun YearMonthPicker(year: Int, month: Int, onConfirm: (Int, Int) -> Unit, onDismiss: () -> Unit) {
    var tempYear by remember { mutableStateOf(year) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ){
                IconButton(onClick = { tempYear-- }) { Text("<") }
                Text("$tempYear")
                IconButton(onClick = { tempYear++ }) { Text(">") }
            }
        },
        text = {
            LazyVerticalGrid(columns = GridCells.Fixed(4)) {
                items(12) { i ->
                    val m = i + 1
                    Button(onClick = { onConfirm(tempYear, m) }, modifier = Modifier.padding(2.dp)) {
                        Text("${m}月")
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(tempYear, month) }) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}