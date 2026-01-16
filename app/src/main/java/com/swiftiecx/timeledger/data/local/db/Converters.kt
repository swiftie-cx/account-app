package com.swiftiecx.timeledger.data.local.db

import androidx.room.TypeConverter
import com.swiftiecx.timeledger.data.model.CategoryType
import java.util.Date

/**
 * Room 类型转换器：
 * - Date <-> Long
 * - CategoryType(enum) <-> String
 *
 * AppDatabase 上使用 @TypeConverters(Converters::class)，这里的转换会全局生效。
 */
class Converters {

    // -------------------------
    // Date <-> Long
    // -------------------------
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time

    // -------------------------
    // CategoryType(enum) <-> String
    // -------------------------
    @TypeConverter
    fun categoryTypeToString(type: CategoryType?): String? = type?.name

    @TypeConverter
    fun stringToCategoryType(value: String?): CategoryType? =
        value?.let { CategoryType.valueOf(it) }
}
