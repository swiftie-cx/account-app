package com.example.myapplication.ui.screen

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NumericKeyboard(
    onNumberClick: (String) -> Unit,
    onOperatorClick: (String) -> Unit, // New
    onBackspaceClick: () -> Unit,
    onDateClick: () -> Unit,
    onDoneClick: () -> Unit,
    onEqualsClick: () -> Unit, // New
    isCalculation: Boolean      // New
) {
    val buttons = listOf(
        "7", "8", "9", "日期",
        "4", "5", "6", "+",
        "1", "2", "3", "-",
        ".", "0", "<", if (isCalculation) "=" else "完成"
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.padding(2.dp)
    ) {
        items(buttons) { button ->
            Button(
                onClick = {
                    when (button) {
                        "日期" -> onDateClick()
                        "完成" -> onDoneClick()
                        "=" -> onEqualsClick()
                        "<" -> onBackspaceClick()
                        "+", "-" -> onOperatorClick(button)
                        else -> onNumberClick(button)
                    }
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .padding(2.dp)
                    .height(60.dp)
            ) {
                when (button) {
                    "完成" -> Icon(Icons.Filled.Check, contentDescription = "Done")
                    "日期" -> Icon(Icons.Filled.DateRange, contentDescription = "Date")
                    else -> Text(text = button, style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}