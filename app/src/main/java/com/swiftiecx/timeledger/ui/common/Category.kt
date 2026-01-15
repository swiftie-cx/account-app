package com.swiftiecx.timeledger.ui.navigation

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.swiftiecx.timeledger.R

data class Category(
    val title: String,
    val icon: ImageVector,
    val key: String = title
)

typealias SubCategory = Category

data class MainCategory(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val subCategories: List<Category>
)

object CategoryData {

    // 旧数据兼容映射 (字符串映射非常快，保留)
    private val legacyKeyMap: Map<String, String> = mapOf(
        "sub_food" to "Food", "sub_snacks" to "Snacks", "sub_alcohol" to "Alcohol",
        "sub_cigarette" to "Cigarette", "sub_shopping" to "Shopping", "sub_clothes" to "Clothes",
        "sub_electronics" to "Electronics", "sub_gift" to "Gift", "sub_beauty" to "Beauty",
        "sub_traffic" to "Traffic", "sub_car" to "Car", "sub_repair" to "Repair",
        "sub_travel" to "Travel", "sub_housing" to "Housing", "sub_daily" to "Daily",
        "sub_phone" to "Phone", "sub_pets" to "Pets", "sub_service" to "Service",
        "sub_entertainment" to "Entertainment", "sub_sports" to "Sports", "sub_social" to "Social",
        "sub_movie" to "Movie", "sub_drama" to "Drama", "sub_medical" to "Medical",
        "sub_education" to "Education", "sub_child" to "Child", "sub_book" to "Book",
        "sub_donate" to "Donate", "sub_other" to "Other",
        "sub_salary" to "Salary", "sub_bonus" to "Bonus", "sub_reimbursement" to "Reimbursement",
        "sub_part_time" to "PartTime", "sub_finance" to "Finance", "sub_gift_money" to "GiftMoney",
        "sub_red_packet" to "RedPacket", "sub_second_hand" to "SecondHand", "sub_other" to "OtherIncome"
    )

    // --- 核心修改：使用 when 替代 Map，避免类加载阻塞 ---

    fun getIcon(nameOrKey: String, context: Context? = null): ImageVector {
        val key = legacyKeyMap[nameOrKey] ?: nameOrKey
        return when (key) {
            // 债务相关
            "借出", "Lend", "债务还款" -> IconMapper.getIcon("Lend")
            "借入", "Borrow", "债务收款" -> IconMapper.getIcon("Borrow")

            // 支出
            "Food" -> Icons.Default.Restaurant
            "Snacks" -> Icons.Default.Icecream
            "Alcohol" -> Icons.Default.LocalBar
            "Cigarette" -> Icons.Default.SmokingRooms
            "Shopping" -> Icons.Default.ShoppingCart
            "Clothes" -> Icons.Default.Checkroom
            "Electronics" -> Icons.Default.Laptop
            "Gift" -> Icons.Default.CardGiftcard
            "Beauty" -> Icons.Default.Face
            "Traffic" -> Icons.Default.Commute
            "Car" -> Icons.Default.DirectionsCar
            "Repair" -> Icons.Default.Build
            "Travel" -> Icons.Default.Flight
            "Housing" -> Icons.Default.Home
            "Daily" -> Icons.Default.Deck
            "Phone" -> Icons.Default.PhoneAndroid
            "Pets" -> Icons.Default.Pets
            "Service" -> Icons.Default.CleaningServices
            "Entertainment" -> Icons.Default.SportsEsports
            "Sports" -> Icons.Default.FitnessCenter
            "Social" -> Icons.Default.People
            "Movie" -> Icons.Default.Movie
            "Drama" -> Icons.Default.TheaterComedy
            "Education" -> Icons.Default.School
            "Medical" -> Icons.Default.LocalHospital
            "Child" -> Icons.Default.ChildFriendly
            "Book" -> Icons.Default.MenuBook
            "Donate" -> Icons.Default.Favorite
            "Other" -> Icons.Default.MoreHoriz

            // 收入
            "Salary" -> Icons.Default.AccountBalanceWallet
            "Bonus" -> Icons.Default.StackedBarChart
            "Reimbursement" -> Icons.Default.ReceiptLong
            "PartTime" -> Icons.Default.Work
            "Finance" -> Icons.Default.TrendingUp
            "GiftMoney" -> Icons.Default.Redeem
            "RedPacket" -> Icons.Default.CardGiftcard
            "SecondHand" -> Icons.Default.Storefront
            "OtherIncome" -> Icons.Default.MoreHoriz

            else -> Icons.Default.HelpOutline
        }
    }

    fun getDisplayName(nameOrKey: String, context: Context): String {
        return when (nameOrKey) {
            "借出", "Lend" -> context.getString(R.string.type_lend_out)
            "借入", "Borrow" -> context.getString(R.string.type_borrow_in)
            "债务还款" -> context.getString(R.string.type_repayment)
            "债务收款" -> context.getString(R.string.type_collection)
            else -> {
                val key = legacyKeyMap[nameOrKey] ?: nameOrKey
                val resId = when (key) {
                    "Food" -> R.string.sub_food
                    "Snacks" -> R.string.sub_snacks
                    "Alcohol" -> R.string.sub_alcohol
                    "Cigarette" -> R.string.sub_cigarette
                    "Shopping" -> R.string.sub_shopping
                    "Clothes" -> R.string.sub_clothes
                    "Electronics" -> R.string.sub_electronics
                    "Gift" -> R.string.sub_gift
                    "Beauty" -> R.string.sub_beauty
                    "Traffic" -> R.string.sub_traffic
                    "Car" -> R.string.sub_car
                    "Repair" -> R.string.sub_repair
                    "Travel" -> R.string.sub_travel
                    "Housing" -> R.string.sub_housing
                    "Daily" -> R.string.sub_daily
                    "Phone" -> R.string.sub_phone
                    "Pets" -> R.string.sub_pets
                    "Service" -> R.string.sub_service
                    "Entertainment" -> R.string.sub_entertainment
                    "Sports" -> R.string.sub_sports
                    "Social" -> R.string.sub_social
                    "Movie" -> R.string.sub_movie
                    "Drama" -> R.string.sub_drama
                    "Education" -> R.string.sub_education
                    "Medical" -> R.string.sub_medical
                    "Child" -> R.string.sub_child
                    "Book" -> R.string.sub_book
                    "Donate" -> R.string.sub_donate
                    "Other" -> R.string.sub_other
                    "Salary" -> R.string.sub_salary
                    "Bonus" -> R.string.sub_bonus
                    "Reimbursement" -> R.string.sub_reimbursement
                    "PartTime" -> R.string.sub_part_time
                    "Finance" -> R.string.sub_finance
                    "GiftMoney" -> R.string.sub_gift_money
                    "RedPacket" -> R.string.sub_red_packet
                    "SecondHand" -> R.string.sub_second_hand
                    "OtherIncome" -> R.string.sub_other
                    else -> null
                }
                if (resId != null) context.getString(resId) else nameOrKey
            }
        }
    }

    private fun getCategoryColorByKey(key: String): Color {
        return when (key) {
            "Food", "Snacks", "Alcohol", "Cigarette" -> Color(0xFFFFB74D)
            "Shopping", "Clothes", "Electronics", "Gift", "Beauty" -> Color(0xFFF06292)
            "Traffic", "Car", "Repair", "Travel" -> Color(0xFF4DB6AC)
            "Housing", "Daily", "Phone", "Pets", "Service" -> Color(0xFF7986CB)
            "Entertainment", "Sports", "Social", "Movie", "Drama" -> Color(0xFF9575CD)
            "Education", "Medical", "Child", "Book" -> Color(0xFF4FC3F7)
            "Donate", "Other" -> Color(0xFFA1887F)
            "Salary", "Bonus", "Reimbursement", "PartTime" -> Color(0xFF81C784)
            "Finance", "GiftMoney", "RedPacket", "SecondHand", "OtherIncome" -> Color(0xFFFFD54F)
            else -> Color(0xFF90A4AE)
        }
    }

    fun getColor(nameOrKey: String, typeInt: Int, context: Context): Color {
        // [修复] 颜色翻转：
        // 借入/收款 = 钱进来 = 绿色 (Income)
        // 借出/还款 = 钱出去 = 红色 (Expense)
        when (nameOrKey) {
            "借出", "Lend", "债务还款" -> return Color(0xFFE53935) // Red
            "借入", "Borrow", "债务收款" -> return Color(0xFF4CAF50) // Green
        }

        val searchKey = legacyKeyMap[nameOrKey] ?: nameOrKey
        return getCategoryColorByKey(searchKey)
    }

    fun getStableKey(nameOrKey: String, context: Context): String {
        return legacyKeyMap[nameOrKey] ?: nameOrKey
    }

    // --- 列表生成方法 (仅在分类选择页使用，IO 线程调用) ---
    fun getExpenseCategories(context: Context): List<MainCategory> {
        return listOf(
            MainCategory(context.getString(R.string.cat_food), Icons.Default.Restaurant, Color(0xFFFFB74D), listOf(
                Category(context.getString(R.string.sub_food), Icons.Default.Restaurant, "Food"),
                Category(context.getString(R.string.sub_snacks), Icons.Default.Icecream, "Snacks"),
                Category(context.getString(R.string.sub_alcohol), Icons.Default.LocalBar, "Alcohol"),
                Category(context.getString(R.string.sub_cigarette), Icons.Default.SmokingRooms, "Cigarette")
            )),
            MainCategory(context.getString(R.string.cat_shopping), Icons.Default.ShoppingCart, Color(0xFFF06292), listOf(
                Category(context.getString(R.string.sub_shopping), Icons.Default.ShoppingCart, "Shopping"),
                Category(context.getString(R.string.sub_clothes), Icons.Default.Checkroom, "Clothes"),
                Category(context.getString(R.string.sub_electronics), Icons.Default.Laptop, "Electronics"),
                Category(context.getString(R.string.sub_gift), Icons.Default.CardGiftcard, "Gift"),
                Category(context.getString(R.string.sub_beauty), Icons.Default.Face, "Beauty")
            )),
            MainCategory(context.getString(R.string.cat_transport), Icons.Default.Commute, Color(0xFF4DB6AC), listOf(
                Category(context.getString(R.string.sub_traffic), Icons.Default.Commute, "Traffic"),
                Category(context.getString(R.string.sub_car), Icons.Default.DirectionsCar, "Car"),
                Category(context.getString(R.string.sub_repair), Icons.Default.Build, "Repair"),
                Category(context.getString(R.string.sub_travel), Icons.Default.Flight, "Travel")
            )),
            MainCategory(context.getString(R.string.cat_home), Icons.Default.Home, Color(0xFF7986CB), listOf(
                Category(context.getString(R.string.sub_housing), Icons.Default.Home, "Housing"),
                Category(context.getString(R.string.sub_daily), Icons.Default.Deck, "Daily"),
                Category(context.getString(R.string.sub_phone), Icons.Default.PhoneAndroid, "Phone"),
                Category(context.getString(R.string.sub_pets), Icons.Default.Pets, "Pets"),
                Category(context.getString(R.string.sub_service), Icons.Default.CleaningServices, "Service")
            )),
            MainCategory(context.getString(R.string.cat_entertainment), Icons.Default.SportsEsports, Color(0xFF9575CD), listOf(
                Category(context.getString(R.string.sub_entertainment), Icons.Default.SportsEsports, "Entertainment"),
                Category(context.getString(R.string.sub_sports), Icons.Default.FitnessCenter, "Sports"),
                Category(context.getString(R.string.sub_social), Icons.Default.People, "Social"),
                Category(context.getString(R.string.sub_movie), Icons.Default.Movie, "Movie"),
                Category(context.getString(R.string.sub_drama), Icons.Default.TheaterComedy, "Drama")
            )),
            MainCategory(context.getString(R.string.cat_medical_edu), Icons.Default.School, Color(0xFF4FC3F7), listOf(
                Category(context.getString(R.string.sub_education), Icons.Default.School, "Education"),
                Category(context.getString(R.string.sub_medical), Icons.Default.LocalHospital, "Medical"),
                Category(context.getString(R.string.sub_child), Icons.Default.ChildFriendly, "Child"),
                Category(context.getString(R.string.sub_book), Icons.Default.MenuBook, "Book")
            )),
            MainCategory(context.getString(R.string.cat_financial), Icons.Default.AttachMoney, Color(0xFFA1887F), listOf(
                Category(context.getString(R.string.sub_donate), Icons.Default.Favorite, "Donate"),
                Category(context.getString(R.string.sub_other), Icons.Default.MoreHoriz, "Other")
            ))
        )
    }

    fun getIncomeCategories(context: Context): List<MainCategory> {
        return listOf(
            MainCategory(context.getString(R.string.cat_income_job), Icons.Default.Work, Color(0xFF81C784), listOf(
                Category(context.getString(R.string.sub_salary), Icons.Default.AccountBalanceWallet, "Salary"),
                Category(context.getString(R.string.sub_bonus), Icons.Default.StackedBarChart, "Bonus"),
                Category(context.getString(R.string.sub_reimbursement), Icons.Default.ReceiptLong, "Reimbursement"),
                Category(context.getString(R.string.sub_part_time), Icons.Default.Work, "PartTime")
            )),
            MainCategory(context.getString(R.string.cat_income_other), Icons.Default.Savings, Color(0xFFFFD54F), listOf(
                Category(context.getString(R.string.sub_finance), Icons.Default.TrendingUp, "Finance"),
                Category(context.getString(R.string.sub_gift_money), Icons.Default.Redeem, "GiftMoney"),
                Category(context.getString(R.string.sub_red_packet), Icons.Default.CardGiftcard, "RedPacket"),
                Category(context.getString(R.string.sub_second_hand), Icons.Default.Storefront, "SecondHand"),
                Category(context.getString(R.string.sub_other), Icons.Default.MoreHoriz, "OtherIncome")
            ))
        )
    }
}