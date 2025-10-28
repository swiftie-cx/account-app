package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Add a unique index to prevent duplicate budget entries for the same category in the same month.
@Entity(tableName = "budgets", indices = [Index(value = ["category", "year", "month"], unique = true)])
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String, // 预算对应的分类名称
    val amount: Double,     // 预算金额
    val year: Int,        // 年份，例如 2025
    val month: Int        // 月份，例如 10
)
