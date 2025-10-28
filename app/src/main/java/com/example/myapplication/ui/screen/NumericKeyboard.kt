package com.example.myapplication.ui.screen

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NumericKeyboard(
    onNumberClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onDateClick: () -> Unit,
    onDoneClick: () -> Unit
) {
    val buttons = listOf(
        "7", "8", "9", "日期",
        "4", "5", "6", "+",
        "1", "2", "3", "-",
        ".", "0", "<", "完成"
    )
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.padding(8.dp)
    ) {
        items(buttons) {
            button ->
            Button(
                onClick = {
                    when (button) {
                        "日期" -> onDateClick()
                        "完成" -> onDoneClick()
                        "<" -> onBackspaceClick()
                        "+", "-" -> { /* TODO: Handle operators */ }
                        else -> onNumberClick(button)
                    }
                },
                modifier = Modifier
                    .padding(4.dp)
                    .height(60.dp)
            ) {
                Text(text = button, style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
            }
        }
    }
}
