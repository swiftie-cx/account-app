package com.swiftiecx.timeledger.ui.navigation

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.swiftiecx.timeledger.R

// [关键修复]
// 1. 恢复 typealias SubCategory，解决 AddTransactionComponents 报错
// 2. 给 key 设置默认值 = title，解决 Repository 和 AddCategoryScreen 报错
data class Category(
    val title: String,
    val icon: ImageVector,
    val key: String = title // <--- 加上这个默认值，其他文件就不报错了
)

typealias SubCategory = Category

data class MainCategory(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val subCategories: List<Category>
)

// --- 动态分类数据源 ---
object CategoryData {

    // 在这里我们显式传入 key="Food" 等英文，用于从数据库匹配
    fun getExpenseCategories(context: Context): List<MainCategory> {
        return listOf(
            MainCategory(
                title = context.getString(R.string.cat_food),
                icon = Icons.Default.Restaurant,
                color = Color(0xFFFFB74D),
                subCategories = listOf(
                    Category(context.getString(R.string.sub_food), Icons.Default.Restaurant, key = "Food"),
                    Category(context.getString(R.string.sub_snacks), Icons.Default.Icecream, key = "Snacks"),
                    Category(context.getString(R.string.sub_alcohol), Icons.Default.LocalBar, key = "Alcohol"),
                    Category(context.getString(R.string.sub_cigarette), Icons.Default.SmokingRooms, key = "Cigarette")
                )
            ),
            MainCategory(
                title = context.getString(R.string.cat_shopping),
                icon = Icons.Default.ShoppingCart,
                color = Color(0xFFF06292),
                subCategories = listOf(
                    Category(context.getString(R.string.sub_shopping), Icons.Default.ShoppingCart, key = "Shopping"),
                    Category(context.getString(R.string.sub_clothes), Icons.Default.Checkroom, key = "Clothes"),
                    Category(context.getString(R.string.sub_electronics), Icons.Default.Laptop, key = "Electronics"),
                    Category(context.getString(R.string.sub_gift), Icons.Default.CardGiftcard, key = "Gift"),
                    Category(context.getString(R.string.sub_beauty), Icons.Default.Face, key = "Beauty")
                )
            ),
            MainCategory(
                title = context.getString(R.string.cat_transport),
                icon = Icons.Default.Commute,
                color = Color(0xFF4DB6AC),
                subCategories = listOf(
                    Category(context.getString(R.string.sub_traffic), Icons.Default.Commute, key = "Traffic"),
                    Category(context.getString(R.string.sub_car), Icons.Default.DirectionsCar, key = "Car"),
                    Category(context.getString(R.string.sub_repair), Icons.Default.Build, key = "Repair"),
                    Category(context.getString(R.string.sub_travel), Icons.Default.Flight, key = "Travel")
                )
            ),
            MainCategory(
                title = context.getString(R.string.cat_home),
                icon = Icons.Default.Home,
                color = Color(0xFF7986CB),
                subCategories = listOf(
                    Category(context.getString(R.string.sub_housing), Icons.Default.Home, key = "Housing"),
                    Category(context.getString(R.string.sub_daily), Icons.Default.Deck, key = "Daily"),
                    Category(context.getString(R.string.sub_phone), Icons.Default.PhoneAndroid, key = "Phone"),
                    Category(context.getString(R.string.sub_pets), Icons.Default.Pets, key = "Pets"),
                    Category(context.getString(R.string.sub_service), Icons.Default.CleaningServices, key = "Service")
                )
            ),
            MainCategory(
                title = context.getString(R.string.cat_entertainment),
                icon = Icons.Default.SportsEsports,
                color = Color(0xFF9575CD),
                subCategories = listOf(
                    Category(context.getString(R.string.sub_entertainment), Icons.Default.SportsEsports, key = "Entertainment"),
                    Category(context.getString(R.string.sub_sports), Icons.Default.FitnessCenter, key = "Sports"),
                    Category(context.getString(R.string.sub_social), Icons.Default.People, key = "Social"),
                    Category(context.getString(R.string.sub_movie), Icons.Default.Movie, key = "Movie"),
                    Category(context.getString(R.string.sub_drama), Icons.Default.TheaterComedy, key = "Drama")
                )
            ),
            MainCategory(
                title = context.getString(R.string.cat_medical_edu),
                icon = Icons.Default.School,
                color = Color(0xFF4FC3F7),
                subCategories = listOf(
                    Category(context.getString(R.string.sub_education), Icons.Default.School, key = "Education"),
                    Category(context.getString(R.string.sub_medical), Icons.Default.LocalHospital, key = "Medical"),
                    Category(context.getString(R.string.sub_child), Icons.Default.ChildFriendly, key = "Child"),
                    Category(context.getString(R.string.sub_book), Icons.Default.MenuBook, key = "Book")
                )
            ),
            MainCategory(
                title = context.getString(R.string.cat_financial),
                icon = Icons.Default.AttachMoney,
                color = Color(0xFFA1887F),
                subCategories = listOf(
                    Category(context.getString(R.string.sub_donate), Icons.Default.Favorite, key = "Donate"),
                    Category(context.getString(R.string.sub_other), Icons.Default.MoreHoriz, key = "Other")
                )
            )
        )
    }

    fun getIncomeCategories(context: Context): List<MainCategory> {
        return listOf(
            MainCategory(
                title = context.getString(R.string.cat_income_job),
                icon = Icons.Default.Work,
                color = Color(0xFF81C784),
                subCategories = listOf(
                    Category(context.getString(R.string.sub_salary), Icons.Default.AccountBalanceWallet, key = "Salary"),
                    Category(context.getString(R.string.sub_bonus), Icons.Default.StackedBarChart, key = "Bonus"),
                    Category(context.getString(R.string.sub_reimbursement), Icons.Default.ReceiptLong, key = "Reimbursement"),
                    Category(context.getString(R.string.sub_part_time), Icons.Default.Work, key = "PartTime")
                )
            ),
            MainCategory(
                title = context.getString(R.string.cat_income_other),
                icon = Icons.Default.Savings,
                color = Color(0xFFFFD54F),
                subCategories = listOf(
                    Category(context.getString(R.string.sub_finance), Icons.Default.TrendingUp, key = "Finance"),
                    Category(context.getString(R.string.sub_gift_money), Icons.Default.Redeem, key = "GiftMoney"),
                    Category(context.getString(R.string.sub_red_packet), Icons.Default.CardGiftcard, key = "RedPacket"),
                    Category(context.getString(R.string.sub_second_hand), Icons.Default.Storefront, key = "SecondHand"),
                    Category(context.getString(R.string.sub_other), Icons.Default.MoreHoriz, key = "OtherIncome")
                )
            )
        )
    }

    // =========================
    //  Helpers for Budget/Legacy
    // =========================

    /**
     * 把“显示名(中文/日文等)”转换成数据库用的 stable key(英文)。
     * 如果传入本来就是 key，则原样返回。
     */
    fun getStableKey(nameOrKey: String, context: Context): String {
        val allSubs = (getExpenseCategories(context) + getIncomeCategories(context))
            .flatMap { it.subCategories }

        val hit = allSubs.firstOrNull { it.title == nameOrKey || it.key == nameOrKey }
        return hit?.key ?: nameOrKey
    }

    /**
     * 把 stable key(英文) 转回当前语言的显示名。
     * 如果传入本来就是显示名，则原样返回。
     */
    fun getDisplayName(nameOrKey: String, context: Context): String {
        val allSubs = (getExpenseCategories(context) + getIncomeCategories(context))
            .flatMap { it.subCategories }

        val hit = allSubs.firstOrNull { it.title == nameOrKey || it.key == nameOrKey }
        return hit?.title ?: nameOrKey
    }

    /**
     * 根据子分类(显示名或key)拿到所属主分类颜色。
     * typeInt: 0=支出 1=收入
     */
    fun getColor(nameOrKey: String, typeInt: Int, context: Context): Color {
        val mains = if (typeInt == 0) getExpenseCategories(context) else getIncomeCategories(context)

        val mainHit = mains.firstOrNull { main ->
            main.subCategories.any { sub -> sub.title == nameOrKey || sub.key == nameOrKey }
        }

        return mainHit?.color ?: Color(0xFF90A4AE)
    }

    /**
     * 根据子分类(显示名或key)拿到图标。
     */
    fun getIcon(nameOrKey: String, context: Context): ImageVector {
        val allMains = getExpenseCategories(context) + getIncomeCategories(context)

        for (main in allMains) {
            val subHit = main.subCategories.firstOrNull { it.title == nameOrKey || it.key == nameOrKey }
            if (subHit != null) return subHit.icon
        }

        return Icons.Default.HelpOutline
    }
}
