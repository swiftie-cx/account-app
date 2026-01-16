package com.swiftiecx.timeledger.data.local.dao

import androidx.room.Dao
import androidx.room.Query

@Dao
interface CategoryDao {

    // [新增] 根据 Key 更新主分类名称
    @Query("UPDATE main_categories SET title = :newTitle WHERE `key` = :key")
    suspend fun updateMainCategoryName(key: String, newTitle: String)

    // [新增] 根据 Key 更新子分类名称
    @Query("UPDATE sub_categories SET title = :newTitle WHERE `key` = :key")
    suspend fun updateSubCategoryName(key: String, newTitle: String)

    // 如果您还没有查询方法，可能也需要补上，例如：
    // @Query("SELECT * FROM main_categories")
    // fun getAllMainCategories(): Flow<List<MainCategory>>
}