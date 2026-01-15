package com.swiftiecx.timeledger.ui.common

import android.content.Context
import androidx.compose.ui.graphics.vector.ImageVector

fun buildCategoryIconMap(context: Context): Map<String, ImageVector?> {
    val expenses = CategoryData.getExpenseCategories(context).flatMap { it.subCategories }
    val incomes = CategoryData.getIncomeCategories(context).flatMap { it.subCategories }
    return (expenses + incomes).associate { it.title to it.icon }
}
