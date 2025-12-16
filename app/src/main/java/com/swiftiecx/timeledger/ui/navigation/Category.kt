package com.swiftiecx.timeledger.ui.navigation

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.swiftiecx.timeledger.R

// --- 基础数据结构 ---
data class Category(
    val title: String,
    val icon: ImageVector,
    val key: String? = null // 可选：用于唯一标识系统预设分类
)

typealias SubCategory = Category

data class MainCategory(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val subCategories: List<Category>
)

// --- [新] 动态分类数据源 ---
object CategoryData {

    // 获取支出大类 (需要 Context 来读取 strings.xml)
    fun getExpenseCategories(context: Context): List<MainCategory> {
        return listOf(
            MainCategory(
                title = context.getString(R.string.cat_food),
                icon = Icons.Default.Restaurant,
                color = Color(0xFFFFB74D),
                subCategories = listOf(
                    Category(context.getString(R.string.sub_food), Icons.Default.Restaurant),
                    Category(context.getString(R.string.sub_snacks), Icons.Default.Icecream),
                    Category(context.getString(R.string.sub_alcohol), Icons.Default.LocalBar),
                    Category(context.getString(R.string.sub_cigarette), Icons.Default.SmokingRooms)
                )
            ),
            MainCategory(
                title = context.getString(R.string.cat_shopping),
                icon = Icons.Default.ShoppingCart,
                color = Color(0xFFF06292),
                subCategories = listOf(
                    Category(context.getString(R.string.sub_shopping), Icons.Default.ShoppingCart),
                    Category(context.getString(R.string.sub_clothes), Icons.Default.Checkroom),
                    Category(context.getString(R.string.sub_electronics), Icons.Default.Laptop),
                    Category(context.getString(R.string.sub_gift), Icons.Default.CardGiftcard),
                    Category(context.getString(R.string.sub_beauty), Icons.Default.Face)
                )
            ),
            MainCategory(
                title = context.getString(R.string.cat_transport),
                icon = Icons.Default.Commute,
                color = Color(0xFF4DB6AC),
                subCategories = listOf(
                    Category(context.getString(R.string.sub_traffic), Icons.Default.Commute),
                    Category(context.getString(R.string.sub_car), Icons.Default.DirectionsCar),
                    Category(context.getString(R.string.sub_repair), Icons.Default.Build),
                    Category(context.getString(R.string.sub_travel), Icons.Default.Flight)
                )
            ),
            MainCategory(
                title = context.getString(R.string.cat_home),
                icon = Icons.Default.Home,
                color = Color(0xFF7986CB),
                subCategories = listOf(
                    Category(context.getString(R.string.sub_housing), Icons.Default.Home),
                    Category(context.getString(R.string.sub_daily), Icons.Default.Deck),
                    Category(context.getString(R.string.sub_phone), Icons.Default.PhoneAndroid),
                    Category(context.getString(R.string.sub_pets), Icons.Default.Pets),
                    Category(context.getString(R.string.sub_service), Icons.Default.CleaningServices)
                )
            ),
            MainCategory(
                title = context.getString(R.string.cat_entertainment),
                icon = Icons.Default.SportsEsports,
                color = Color(0xFF9575CD),
                subCategories = listOf(
                    Category(context.getString(R.string.sub_entertainment), Icons.Default.SportsEsports),
                    Category(context.getString(R.string.sub_sports), Icons.Default.FitnessCenter),
                    Category(context.getString(R.string.sub_social), Icons.Default.People),
                    Category(context.getString(R.string.sub_movie), Icons.Default.Movie),
                    Category(context.getString(R.string.sub_drama), Icons.Default.TheaterComedy)
                )
            ),
            MainCategory(
                title = context.getString(R.string.cat_medical_edu),
                icon = Icons.Default.School,
                color = Color(0xFF4FC3F7),
                subCategories = listOf(
                    Category(context.getString(R.string.sub_education), Icons.Default.School),
                    Category(context.getString(R.string.sub_medical), Icons.Default.LocalHospital),
                    Category(context.getString(R.string.sub_child), Icons.Default.ChildFriendly),
                    Category(context.getString(R.string.sub_book), Icons.Default.MenuBook)
                )
            ),
            MainCategory(
                title = context.getString(R.string.cat_financial),
                icon = Icons.Default.AttachMoney,
                color = Color(0xFFA1887F),
                subCategories = listOf(
                    Category(context.getString(R.string.sub_donate), Icons.Default.Favorite),
                    Category(context.getString(R.string.sub_other), Icons.Default.MoreHoriz)
                )
            )
        )
    }

    // 获取收入大类
    fun getIncomeCategories(context: Context): List<MainCategory> {
        return listOf(
            MainCategory(
                title = context.getString(R.string.cat_income_job),
                icon = Icons.Default.Work,
                color = Color(0xFF81C784),
                subCategories = listOf(
                    Category(context.getString(R.string.sub_salary), Icons.Default.AccountBalanceWallet),
                    Category(context.getString(R.string.sub_bonus), Icons.Default.StackedBarChart),
                    Category(context.getString(R.string.sub_reimbursement), Icons.Default.ReceiptLong),
                    Category(context.getString(R.string.sub_part_time), Icons.Default.Work)
                )
            ),
            MainCategory(
                title = context.getString(R.string.cat_income_other),
                icon = Icons.Default.Savings,
                color = Color(0xFFFFD54F),
                subCategories = listOf(
                    Category(context.getString(R.string.sub_finance), Icons.Default.TrendingUp),
                    Category(context.getString(R.string.sub_gift_money), Icons.Default.Redeem),
                    Category(context.getString(R.string.sub_red_packet), Icons.Default.CardGiftcard),
                    Category(context.getString(R.string.sub_second_hand), Icons.Default.Storefront),
                    Category(context.getString(R.string.sub_other), Icons.Default.MoreHoriz)
                )
            )
        )
    }
}

// 辅助工具类：现在需要传入 Context 才能查找
object CategoryHelper {
    // 查找颜色
    fun getCategoryColor(categoryName: String, type: Int, context: Context): Color {
        val list = if (type == 1) CategoryData.getIncomeCategories(context) else CategoryData.getExpenseCategories(context)
        return list.find { main -> main.subCategories.any { sub -> sub.title == categoryName } }?.color
            ?: if(type == 1) Color(0xFF4CAF50) else Color(0xFFE53935)
    }

    // 通过名称找图标
    fun getIcon(categoryName: String, context: Context): ImageVector {
        val allSubs = CategoryData.getExpenseCategories(context).flatMap { it.subCategories } +
                CategoryData.getIncomeCategories(context).flatMap { it.subCategories }
        return allSubs.find { it.title == categoryName }?.icon ?: Icons.Default.HelpOutline
    }

    // 查找主分类对象
    fun getMainCategory(categoryName: String, context: Context): MainCategory? {
        val allMains = CategoryData.getExpenseCategories(context) + CategoryData.getIncomeCategories(context)
        return allMains.find { main ->
            main.subCategories.any { sub -> sub.title == categoryName }
        }
    }
}